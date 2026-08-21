package com.beepsterr.betterkeepinventory.Library;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.api.BetterKeepInventoryAPI;
import com.beepsterr.betterkeepinventory.api.Condition;
import com.beepsterr.betterkeepinventory.support.NoopLogger;
import com.beepsterr.betterkeepinventory.support.TestContexts;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rule tree is built once and reused, so the interesting behaviour is not the parsing --
 * {@link ConfigRuleTest} covers that -- but when a rebuild does and does not happen.
 */
class RulesetTest {

    private ServerMock server;
    private WorldMock world;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        MockBukkit.load(BetterKeepInventory.class);
        player = server.addPlayer();
        player.teleport(world.getSpawnLocation());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** A rules section holding {@code count} enabled rules, each with a `drop: ALL` effect. */
    private MemoryConfiguration rulesSection(int count) {
        MemoryConfiguration cfg = new MemoryConfiguration();
        for (int i = 0; i < count; i++) {
            cfg.set("rule" + i + ".name", "rule" + i);
            cfg.set("rule" + i + ".enabled", true);
            cfg.set("rule" + i + ".effects.drop.mode", "ALL");
        }
        return cfg;
    }

    private PlayerDeathEvent deathEvent() {
        DamageSource source = DamageSource.builder(DamageType.GENERIC).build();
        return new PlayerDeathEvent(player, source, new ArrayList<>(), 0, "died");
    }

    private void fillInventory() {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));
    }

    private void triggerAll(Ruleset ruleset) {
        for (ConfigRule rule : ruleset.rules()) {
            rule.trigger(TestContexts.death(player, deathEvent()));
        }
    }

    @Test
    void startsStaleSoNothingIsParsedBeforeItIsNeeded() {
        Ruleset ruleset = new Ruleset(rulesSection(2));

        assertTrue(ruleset.isStale());
        assertEquals(0, ruleset.size(), "nothing built yet");
    }

    @Test
    void buildParsesTheConfiguredRules() {
        Ruleset ruleset = new Ruleset(rulesSection(3));

        ruleset.build(null);

        assertFalse(ruleset.isStale());
        assertEquals(3, ruleset.size());
    }

    @Test
    void aNullRulesSectionIsAnEmptyRuleset() {
        Ruleset ruleset = new Ruleset(null);

        ruleset.build(null);

        assertEquals(0, ruleset.size());
        assertTrue(ruleset.rules().isEmpty());
    }

    @Test
    void childrenAreNotCountedAsTopLevelRules() {
        MemoryConfiguration cfg = rulesSection(1);
        cfg.set("rule0.children.nested.name", "nested");
        cfg.set("rule0.children.nested.enabled", true);

        Ruleset ruleset = new Ruleset(cfg);
        ruleset.build(null);

        assertEquals(1, ruleset.size(), "children belong to their parent, not to the top level");
    }

    @Test
    void rulesBuildsOnDemandWhenStale() {
        Ruleset ruleset = new Ruleset(rulesSection(2));

        // Never explicitly built -- a death arriving first must still see current rules.
        assertEquals(2, ruleset.rules().size());
        assertFalse(ruleset.isStale());
    }

    @Test
    void rulesDoesNotRebuildWhenFresh() {
        Ruleset ruleset = new Ruleset(rulesSection(2));
        ruleset.build(null);

        List<ConfigRule> first = ruleset.rules();
        List<ConfigRule> second = ruleset.rules();

        assertSameInstances(first, second);
    }

    @Test
    void invalidateCausesTheNextAccessToRebuild() {
        Ruleset ruleset = new Ruleset(rulesSection(2));
        List<ConfigRule> before = ruleset.rules();

        ruleset.invalidate();
        assertTrue(ruleset.isStale());

        List<ConfigRule> after = ruleset.rules();
        assertNotSame(before, after, "a rebuild should produce a new tree");
        assertEquals(2, after.size());
    }

    @Test
    void aRebuildSwapsTheListInsteadOfMutatingIt() {
        Ruleset ruleset = new Ruleset(rulesSection(2));
        List<ConfigRule> held = ruleset.rules();

        ruleset.invalidate();
        ruleset.rules();

        assertEquals(2, held.size(), "a death already walking the old tree must not see it change");
    }

    /**
     * The reason invalidation is registration-driven rather than startup-only.
     * <p>
     * An addon registers its conditions in its own onEnable, which runs after ours -- so the
     * first build happens before that condition exists, and a rule using it parses as though the
     * condition were absent. Without a rebuild the addon is silently ignored: the config looks
     * fine, the addon looks loaded, and the rule quietly does the wrong thing.
     */
    @Test
    void aConditionRegisteredAfterTheFirstBuildIsPickedUpOnRebuild() {

        MemoryConfiguration cfg = rulesSection(1);
        cfg.set("rule0.conditions.late_addon_condition.whatever", true);

        Ruleset ruleset = new Ruleset(cfg);
        ruleset.build(null);

        // First build: the condition does not exist yet, so it is skipped and the rule runs
        // unconditionally.
        fillInventory();
        triggerAll(ruleset);
        assertTrue(player.getInventory().isEmpty(),
                "with the condition unknown the rule should have run unconditionally");

        // The addon enables and registers a condition that refuses every death.
        BetterKeepInventoryAPI api = Bukkit.getServicesManager().load(BetterKeepInventoryAPI.class);
        Plugin addon = MockBukkit.createMockPlugin("LateAddon");
        api.conditionRegistry().register(addon, "late_addon_condition", section -> (Condition)
                deathContext -> false);

        ruleset.invalidate();

        fillInventory();
        triggerAll(ruleset);
        assertFalse(player.getInventory().isEmpty(),
                "after the rebuild the addon's condition should be in force and block the effect");
    }

    private static void assertSameInstances(List<ConfigRule> a, List<ConfigRule> b) {
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(System.identityHashCode(a.get(i)), System.identityHashCode(b.get(i)),
                    "rules should be reused, not reparsed");
        }
    }
}
