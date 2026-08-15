package com.beepsterr.betterkeepinventory.Library;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers {@link Config#countRules()}, which feeds the {@code rules_count} metric.
 * It walks the raw configuration rather than building {@link ConfigRule} objects,
 * so it has to agree with how ConfigRule reads the same sections: every entry
 * under {@code rules}, plus everything under each rule's {@code children}.
 */
class ConfigTest {

    /** A rules tree: two top-level rules, one with two children, one grandchild. */
    private MemoryConfiguration nestedRules() {
        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("first.name", "First");
        cfg.set("first.children.child_a.name", "Child A");
        cfg.set("first.children.child_b.name", "Child B");
        cfg.set("first.children.child_b.children.grandchild.name", "Grandchild");
        cfg.set("second.name", "Second");
        return cfg;
    }

    @Test
    void missingRulesSectionCountsZero() {
        assertEquals(0, Config.countRules(null));
    }

    @Test
    void emptyRulesSectionCountsZero() {
        assertEquals(0, Config.countRules(new MemoryConfiguration()));
    }

    @Test
    void countsEveryTopLevelRule() {
        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("one.name", "One");
        cfg.set("two.name", "Two");
        cfg.set("three.name", "Three");

        assertEquals(3, Config.countRules(cfg));
    }

    @Test
    void countsNestedChildrenAtEveryDepth() {
        assertEquals(5, Config.countRules(nestedRules()));
    }

    @Test
    void ignoresKeysThatAreNotRuleSections() {
        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("real_rule.name", "Real");
        cfg.set("stray_value", "not a rule");
        cfg.set("stray_list", List.of("also not a rule"));

        assertEquals(1, Config.countRules(cfg));
    }

    /**
     * The instance method has to look under the "rules" key of the live config.
     * The shipped default config.yml contains exactly one (disabled) example rule.
     */
    @Test
    void countsRulesFromTheLoadedConfiguration() {
        MockBukkit.mock();
        try {
            MockBukkit.load(BetterKeepInventory.class);
            assertEquals(1, Config.getInstance().countRules());
        } finally {
            MockBukkit.unmock();
        }
    }
}
