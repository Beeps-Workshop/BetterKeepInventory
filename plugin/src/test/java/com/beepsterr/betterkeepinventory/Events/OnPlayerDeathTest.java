package com.beepsterr.betterkeepinventory.Events;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.Exceptions.UnloadableConfiguration;
import com.beepsterr.betterkeepinventory.Library.Config;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
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
 * Death-handler tests for {@link OnPlayerDeath}, covering how each {@code default_behavior}
 * interacts with the world's keepInventory decision.
 * <p>
 * The interaction that matters: the server only collects death loot when it intends to drop it.
 * On a keepInventory world it collects nothing, so {@code event.getDrops()} arrives empty. A
 * handler that flips keepInventory off without noticing leaves the server clearing the inventory
 * and spawning an empty list, which destroys the player's items outright.
 * <p>
 * These assert on the event's final state rather than on spawned entities, because the server --
 * not the plugin -- is what acts on that state once every handler has run.
 */
class OnPlayerDeathTest {

    private ServerMock server;
    private WorldMock world;
    private PlayerMock player;
    private BetterKeepInventory plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        plugin = MockBukkit.load(BetterKeepInventory.class);
        player = server.addPlayer();
        player.teleport(world.getSpawnLocation());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /**
     * Swap in a config with the behavior under test. The version string has to be anything other
     * than "default", which sends Config off to rewrite the file from disk and would discard it.
     */
    private void behavior(String value) throws UnloadableConfiguration {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("version", "2.3.0");
        cfg.set("default_behavior", value);
        plugin.config = new Config(cfg, null);
    }

    /**
     * As {@link #behavior}, plus a rule whose exp effect deletes a fixed number of levels while
     * the rules are running -- so the player's level differs before and after them.
     */
    private void behaviorWithExpRule(String value, int levelsToLose) throws UnloadableConfiguration {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("version", "2.3.0");
        cfg.set("default_behavior", value);
        cfg.set("rules.exp_rule.name", "exp");
        cfg.set("rules.exp_rule.enabled", true);
        cfg.set("rules.exp_rule.effects.exp.mode", "SIMPLE");
        cfg.set("rules.exp_rule.effects.exp.how", "DELETE");
        cfg.set("rules.exp_rule.effects.exp.min", (double) levelsToLose);
        cfg.set("rules.exp_rule.effects.exp.max", (double) levelsToLose);
        plugin.config = new Config(cfg, null);
    }

    /**
     * A death event in the state the server would hand us.
     *
     * @param worldKeeps whether the world's keepInventory gamerule is on -- which decides both the
     *                   event's initial flags and whether the server bothered to collect any loot.
     */
    private PlayerDeathEvent death(boolean worldKeeps, List<ItemStack> vanillaLoot) {
        DamageSource source = DamageSource.builder(DamageType.GENERIC).build();
        PlayerDeathEvent event = new PlayerDeathEvent(player, source, vanillaLoot, 0, "died");
        event.setKeepInventory(worldKeeps);
        event.setKeepLevel(worldKeeps);
        return event;
    }

    private PlayerDeathEvent fire(PlayerDeathEvent event) {
        server.getPluginManager().callEvent(event);
        return event;
    }

    // --- DROP on a world that keeps: the case that used to delete inventories ------------------

    @Test
    void dropOnKeepingWorldDoesNotDestroyTheInventory() throws UnloadableConfiguration {
        behavior("DROP");
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 64));
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 3));

        // World keeps inventory, so the server collected nothing for us.
        PlayerDeathEvent event = fire(death(true, new ArrayList<>()));

        assertFalse(event.getKeepInventory(), "DROP has to end with the server dropping the loot");
        assertFalse(event.getDrops().isEmpty(),
                "the inventory must be handed to the server as drops, not left to be cleared into nothing");
    }

    @Test
    void dropOnKeepingWorldCollectsEveryStack() throws UnloadableConfiguration {
        behavior("DROP");
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 64));
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 3));

        PlayerDeathEvent event = fire(death(true, new ArrayList<>()));

        assertEquals(2, event.getDrops().size(), "both stacks should have been collected");
        assertTrue(event.getDrops().stream()
                        .anyMatch(i -> i.getType() == Material.COBBLESTONE && i.getAmount() == 64),
                "the full cobblestone stack should drop");
        assertTrue(event.getDrops().stream()
                        .anyMatch(i -> i.getType() == Material.DIAMOND && i.getAmount() == 3),
                "the full diamond stack should drop");
    }

    @Test
    void dropOnKeepingWorldDropsCappedExperience() throws UnloadableConfiguration {
        behavior("DROP");
        player.setLevel(30);

        PlayerDeathEvent event = fire(death(true, new ArrayList<>()));

        assertFalse(event.getKeepLevel(), "DROP has to end with the server dropping levels");
        assertEquals(100, event.getDroppedExp(), "vanilla caps death experience at 100 points");
    }

    @Test
    void dropOnKeepingWorldDropsExperienceBelowTheCap() throws UnloadableConfiguration {
        behavior("DROP");
        player.setLevel(5);

        PlayerDeathEvent event = fire(death(true, new ArrayList<>()));

        assertEquals(35, event.getDroppedExp(), "below the cap a death drops 7 points per level");
    }

    @Test
    void dropOnKeepingWorldDropsExperienceForTheLevelDiedAt() throws UnloadableConfiguration {
        // An exp effect takes 5 of the player's 10 levels while the rules run. The experience we
        // hand the server still has to be based on the 10 they died at, because that is what the
        // server itself would have worked out on a dropping world. Measuring the 5 left over
        // instead would make the payout depend on the world's gamerule.
        behaviorWithExpRule("DROP", 5);
        player.setLevel(10);

        PlayerDeathEvent event = fire(death(true, new ArrayList<>()));

        assertEquals(5, player.getLevel(), "the exp effect should have taken its 5 levels");
        assertEquals(70, event.getDroppedExp(),
                "7 points per level of the 10 died at, not of the 5 the effect left behind");
    }

    @Test
    void dropOnKeepingWorldWithNothingCarriedDropsNothing() throws UnloadableConfiguration {
        behavior("DROP");

        PlayerDeathEvent event = fire(death(true, new ArrayList<>()));

        assertTrue(event.getDrops().isEmpty(), "an empty inventory has nothing to collect");
        assertFalse(event.getKeepInventory());
    }

    // --- DROP on a world that already drops: must stay exactly as it was ----------------------

    @Test
    void dropOnDroppingWorldLeavesTheServersLootAlone() throws UnloadableConfiguration {
        behavior("DROP");
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 64));

        // The world drops on death, so the server already collected the loot for us.
        List<ItemStack> vanillaLoot = new ArrayList<>();
        vanillaLoot.add(new ItemStack(Material.COBBLESTONE, 64));
        PlayerDeathEvent event = fire(death(false, vanillaLoot));

        assertFalse(event.getKeepInventory());
        assertEquals(1, event.getDrops().size(),
                "collecting on top of loot the server already gathered would duplicate it");
    }

    // --- KEEP and INHERIT: regression guards --------------------------------------------------

    @Test
    void keepClearsTheServersLootSoItIsNotDuplicated() throws UnloadableConfiguration {
        behavior("KEEP");
        List<ItemStack> vanillaLoot = new ArrayList<>();
        vanillaLoot.add(new ItemStack(Material.COBBLESTONE, 64));

        PlayerDeathEvent event = fire(death(false, vanillaLoot));

        assertTrue(event.getKeepInventory(), "KEEP has to end with the server keeping the inventory");
        assertTrue(event.getDrops().isEmpty(),
                "keeping the inventory AND dropping the collected loot would duplicate it");
        assertEquals(0, event.getDroppedExp());
    }

    @Test
    void inheritLeavesAKeepingWorldAlone() throws UnloadableConfiguration {
        behavior("INHERIT");
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 64));

        PlayerDeathEvent event = fire(death(true, new ArrayList<>()));

        assertTrue(event.getKeepInventory(), "INHERIT must not overrule the world");
        assertTrue(event.getDrops().isEmpty());
    }

    @Test
    void inheritLeavesADroppingWorldAlone() throws UnloadableConfiguration {
        behavior("INHERIT");
        List<ItemStack> vanillaLoot = new ArrayList<>();
        vanillaLoot.add(new ItemStack(Material.COBBLESTONE, 64));

        PlayerDeathEvent event = fire(death(false, vanillaLoot));

        assertFalse(event.getKeepInventory(), "INHERIT must not overrule the world");
        assertEquals(1, event.getDrops().size(), "INHERIT must not touch the collected loot");
    }
}
