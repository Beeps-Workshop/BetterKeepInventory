package com.beepsterr.betterkeepinventory.Events;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.Exceptions.UnloadableConfiguration;
import com.beepsterr.betterkeepinventory.Library.Config;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
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
 * On a keepInventory world it collects nothing, so {@code event.getDrops()} arrives empty and
 * says nothing about what the player was carrying. The handler builds both buckets from its own
 * snapshot instead of asking, and distributes them itself.
 * <p>
 * These therefore assert on <em>outcome</em> -- what the player is left holding and what is on
 * the ground -- rather than on the event's flags. The flags are an implementation detail of how
 * the handler stops the server acting on its own idea of the death; the outcome is the contract.
 * <p>
 * Several tests assert conservation: items kept plus items dropped equals items carried. That is
 * the invariant the 2.3.2 dupe/destroy bug violated, and it holds regardless of behavior or
 * gamerule.
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
     * the rules are running.
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
        world.setGameRule(org.bukkit.GameRule.KEEP_INVENTORY, worldKeeps);
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

    // --- observations -------------------------------------------------------------------------

    private List<ItemStack> groundItems() {
        return world.getEntitiesByClass(Item.class).stream().map(Item::getItemStack).toList();
    }

    private int countOnGround(Material type) {
        return groundItems().stream().filter(i -> i.getType() == type).mapToInt(ItemStack::getAmount).sum();
    }

    private int countHeld(Material type) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == type) total += item.getAmount();
        }
        return total;
    }

    /** Items kept plus items dropped must equal items carried, whatever the behavior. */
    private void assertConserved(Material type, int carried) {
        assertEquals(carried, countHeld(type) + countOnGround(type),
                "kept + dropped must equal what was carried (" + type + ")");
    }

    private int droppedExperience() {
        return world.getEntitiesByClass(ExperienceOrb.class).stream()
                .mapToInt(ExperienceOrb::getExperience).sum();
    }

    // --- DROP on a world that keeps: the case that used to delete inventories ------------------

    @Test
    void dropOnKeepingWorldDoesNotDestroyTheInventory() throws UnloadableConfiguration {
        behavior("DROP");
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 64));
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 3));

        // World keeps inventory, so the server collected nothing for us.
        fire(death(true, new ArrayList<>()));

        assertConserved(Material.COBBLESTONE, 64);
        assertConserved(Material.DIAMOND, 3);
        assertTrue(player.getInventory().isEmpty(), "DROP means the player keeps nothing");
    }

    @Test
    void dropOnKeepingWorldDropsEveryStack() throws UnloadableConfiguration {
        behavior("DROP");
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 64));
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 3));

        fire(death(true, new ArrayList<>()));

        assertEquals(2, groundItems().size(), "both stacks should have been dropped");
        assertEquals(64, countOnGround(Material.COBBLESTONE));
        assertEquals(3, countOnGround(Material.DIAMOND));
    }

    @Test
    void dropOnKeepingWorldDropsCappedExperience() throws UnloadableConfiguration {
        behavior("DROP");
        player.setLevel(30);

        fire(death(true, new ArrayList<>()));

        assertEquals(100, droppedExperience(), "vanilla caps death experience at 100 points");
        assertEquals(0, player.getLevel(), "DROP means the player keeps no levels");
    }

    @Test
    void dropOnKeepingWorldDropsExperienceBelowTheCap() throws UnloadableConfiguration {
        behavior("DROP");
        player.setLevel(5);

        fire(death(true, new ArrayList<>()));

        assertEquals(35, droppedExperience(), "below the cap a death drops 7 points per level");
    }

    /**
     * The experience payout is worked out from the level the player died at, before any effect
     * ran -- that is what the server itself would have computed. Measuring what an effect left
     * behind instead would make the payout depend on the world's gamerule.
     * <p>
     * Under DROP the levels are already in the drop bucket when the rules run, so an exp effect
     * has nothing left to take: dropping is a transfer, and the source is empty. Previously the
     * effect took five levels off a player who was <em>also</em> having the full ten levels'
     * worth dropped for them, which double-counted.
     */
    @Test
    void dropExperienceIsBasedOnTheLevelDiedAtAndIsNotDoubleCounted() throws UnloadableConfiguration {
        behaviorWithExpRule("DROP", 5);
        player.setLevel(10);

        fire(death(true, new ArrayList<>()));

        assertEquals(0, player.getLevel(), "DROP means the player keeps no levels");
        assertEquals(70, droppedExperience(),
                "7 points per level of the 10 died at, counted once");
    }

    @Test
    void dropOnKeepingWorldWithNothingCarriedDropsNothing() throws UnloadableConfiguration {
        behavior("DROP");

        fire(death(true, new ArrayList<>()));

        assertTrue(groundItems().isEmpty(), "an empty inventory has nothing to drop");
    }

    // --- DROP on a world that already drops: same outcome, no duplication ---------------------

    @Test
    void dropOnDroppingWorldDropsEachStackExactlyOnce() throws UnloadableConfiguration {
        behavior("DROP");
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 64));

        // The world drops on death, so the server already collected the loot for us. We ignore
        // that list entirely and build from our own snapshot, so it must not be dropped as well.
        List<ItemStack> vanillaLoot = new ArrayList<>();
        vanillaLoot.add(new ItemStack(Material.COBBLESTONE, 64));
        fire(death(false, vanillaLoot));

        assertConserved(Material.COBBLESTONE, 64);
        assertEquals(64, countOnGround(Material.COBBLESTONE),
                "dropping our snapshot on top of the server's collected loot would duplicate it");
    }

    // --- KEEP and INHERIT: regression guards --------------------------------------------------

    @Test
    void keepLeavesThePlayerHoldingEverything() throws UnloadableConfiguration {
        behavior("KEEP");
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 64));
        player.setLevel(10);

        List<ItemStack> vanillaLoot = new ArrayList<>();
        vanillaLoot.add(new ItemStack(Material.COBBLESTONE, 64));
        fire(death(false, vanillaLoot));

        assertConserved(Material.COBBLESTONE, 64);
        assertEquals(64, countHeld(Material.COBBLESTONE), "KEEP means the player keeps everything");
        assertTrue(groundItems().isEmpty(),
                "keeping the inventory AND dropping the collected loot would duplicate it");
        assertEquals(10, player.getLevel());
        assertEquals(0, droppedExperience());
    }

    @Test
    void inheritFollowsAKeepingWorld() throws UnloadableConfiguration {
        behavior("INHERIT");
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 64));

        fire(death(true, new ArrayList<>()));

        assertConserved(Material.COBBLESTONE, 64);
        assertEquals(64, countHeld(Material.COBBLESTONE), "INHERIT must follow the world, which keeps");
        assertTrue(groundItems().isEmpty());
    }

    @Test
    void inheritFollowsADroppingWorld() throws UnloadableConfiguration {
        behavior("INHERIT");
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 64));

        List<ItemStack> vanillaLoot = new ArrayList<>();
        vanillaLoot.add(new ItemStack(Material.COBBLESTONE, 64));
        fire(death(false, vanillaLoot));

        assertConserved(Material.COBBLESTONE, 64);
        assertEquals(64, countOnGround(Material.COBBLESTONE), "INHERIT must follow the world, which drops");
        assertTrue(player.getInventory().isEmpty());
    }
}
