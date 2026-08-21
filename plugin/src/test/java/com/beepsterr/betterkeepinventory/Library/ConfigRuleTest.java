package com.beepsterr.betterkeepinventory.Library;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.support.NoopLogger;
import org.bukkit.Material;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rule-engine test for {@link ConfigRule}. A rule is built from a
 * {@link org.bukkit.configuration.ConfigurationSection} exactly the way
 * {@link Ruleset} builds it in production, then driven through
 * {@link ConfigRule#trigger}. We observe whether a rule's effects fired by using
 * a real {@code drop} effect (mode ALL) whose side effect is emptying the
 * player's inventory into the world — the same effect covered by
 * {@code DropItemEffectTest}.
 *
 * The plugin is loaded via MockBukkit because ConfigRule resolves the
 * condition/effect registries through the BetterKeepInventory API service and
 * logs through a NestedLogBuilder that reads the plugin singleton.
 */
class ConfigRuleTest {

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

    /** Give the player a couple of item stacks so a fired drop effect is observable. */
    private void fillInventory() {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));
        player.getInventory().addItem(new ItemStack(Material.DIRT, 64));
    }

    /** Mirror the last-damage-cause setup CauseCondition reads. */
    private void killedBy(DamageCause cause) {
        player.setLastDamageCause(new EntityDamageEvent(player, cause, 10.0));
    }

    /** A minimal, non-null PlayerDeathEvent so trigger() takes the onDeath branch. */
    private PlayerDeathEvent deathEvent() {
        DamageSource source = DamageSource.builder(DamageType.GENERIC).build();
        return new PlayerDeathEvent(player, source, new ArrayList<>(), 0, "died");
    }

    /** Build a ConfigRule the same way production does (top-level rule, no parent). */
    private ConfigRule rule(MemoryConfiguration cfg) {
        return new ConfigRule(cfg, null, null);
    }

    /** Base rule scaffold: enabled, named, with a single `drop: ALL` effect. */
    private MemoryConfiguration dropRule(boolean enabled) {
        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("name", "test-rule");
        cfg.set("enabled", enabled);
        cfg.set("effects.drop.mode", "ALL");
        return cfg;
    }

    private void assertEffectsFired() {
        assertTrue(player.getInventory().isEmpty(), "inventory should have been dropped by the effect");
    }

    private void assertEffectsDidNotFire() {
        assertFalse(player.getInventory().isEmpty(), "inventory should be untouched (effect must not have run)");
        assertTrue(player.getInventory().contains(Material.DIAMOND));
        assertTrue(player.getInventory().contains(Material.DIRT));
    }

    @Test
    void effectsRunWhenAllConditionsMet() {
        fillInventory();
        killedBy(DamageCause.LAVA);

        MemoryConfiguration cfg = dropRule(true);
        cfg.set("conditions.cause.nodes", List.of("LAVA", "FIRE"));

        rule(cfg).trigger(player, deathEvent(), null, new NoopLogger());

        assertEffectsFired();
    }

    @Test
    void ruleWithNoConditionsRunsEffects() {
        fillInventory();

        // No conditions section at all -> conditions list is empty -> effects always run.
        rule(dropRule(true)).trigger(player, deathEvent(), null, new NoopLogger());

        assertEffectsFired();
    }

    @Test
    void effectsDoNotRunWhenConditionUnmet() {
        fillInventory();
        killedBy(DamageCause.FALL); // rule wants LAVA

        MemoryConfiguration cfg = dropRule(true);
        cfg.set("conditions.cause.nodes", List.of("LAVA"));

        rule(cfg).trigger(player, deathEvent(), null, new NoopLogger());

        assertEffectsDidNotFire();
    }

    @Test
    void disabledRuleIsSkipped() {
        fillInventory();
        killedBy(DamageCause.LAVA); // conditions WOULD match, but rule is disabled

        MemoryConfiguration cfg = dropRule(false);
        cfg.set("conditions.cause.nodes", List.of("LAVA"));

        ConfigRule r = rule(cfg);
        assertFalse(r.isEnabled(), "rule should report itself disabled");
        r.trigger(player, deathEvent(), null, new NoopLogger());

        assertEffectsDidNotFire();
    }

    @Test
    void effectsDoNotRunWhenOneOfMultipleConditionsFails() {
        fillInventory();
        killedBy(DamageCause.LAVA); // cause matches...

        MemoryConfiguration cfg = dropRule(true);
        cfg.set("conditions.cause.nodes", List.of("LAVA"));
        cfg.set("conditions.worlds.nodes", List.of("some_other_world")); // ...but world does not

        rule(cfg).trigger(player, deathEvent(), null, new NoopLogger());

        assertEffectsDidNotFire();
    }

    @Test
    void effectsRunWhenAllOfMultipleConditionsMet() {
        fillInventory();
        killedBy(DamageCause.LAVA);

        MemoryConfiguration cfg = dropRule(true);
        cfg.set("conditions.cause.nodes", List.of("LAVA"));
        cfg.set("conditions.worlds.nodes", List.of("world")); // player IS in "world"

        rule(cfg).trigger(player, deathEvent(), null, new NoopLogger());

        assertEffectsFired();
    }

    @Test
    void ruleExposesNameAndEnabledFlag() {
        MemoryConfiguration cfg = dropRule(true);
        cfg.set("name", "My Rule");

        ConfigRule r = rule(cfg);

        assertEquals("My Rule", r.getName());
        assertTrue(r.isEnabled());
        assertTrue(r.toString().contains("My Rule"));
    }
}
