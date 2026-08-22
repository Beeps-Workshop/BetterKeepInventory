package com.beepsterr.betterkeepinventory.Content.Effects;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.Library.DeathContextImpl;
import com.beepsterr.betterkeepinventory.support.NoopLogger;
import com.beepsterr.betterkeepinventory.support.TestContexts;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import org.bukkit.configuration.MemoryConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Layer 3: effect test with the full plugin loaded. DropItemEffect reads the
 * BetterKeepInventory singleton (rng, config, debug), so we boot the plugin via
 * MockBukkit. The effect moves items between the context's buckets rather than touching the
 * player, so each test applies the context afterwards and then asserts on the world/inventory --
 * the same two steps a real death performs.
 */
class DropItemEffectTest {

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

    private static DropItemEffect effect(String mode) {
        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("mode", mode);
        return new DropItemEffect(cfg);
    }

    @Test
    void modeAllDropsEntireInventory() {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));
        player.getInventory().addItem(new ItemStack(Material.DIRT, 64));

        DeathContextImpl ctx = TestContexts.death(player);
        effect("ALL").onDeath(ctx);
        TestContexts.apply(ctx);

        assertTrue(player.getInventory().isEmpty(), "inventory should be emptied by mode ALL");
        assertEquals(2, world.getEntitiesByClass(Item.class).size(), "both stacks should be dropped as ground items");
    }

    @Test
    void itemFilterOnlyDropsMatchingItems() {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));
        player.getInventory().addItem(new ItemStack(Material.DIRT, 64));

        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("mode", "ALL");
        cfg.set("filters.items", java.util.List.of("DIRT"));
        DeathContextImpl ctx = TestContexts.death(player);
        new DropItemEffect(cfg).onDeath(ctx);
        TestContexts.apply(ctx);

        assertTrue(player.getInventory().contains(Material.DIAMOND), "diamonds should be kept (not in filter)");
        assertFalse(player.getInventory().contains(Material.DIRT), "dirt should be dropped (matches filter)");
    }

    // --- modes ---------------------------------------------------------------------------------

    private static DropItemEffect effect(String mode, double min, double max) {
        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("mode", mode);
        cfg.set("min", min);
        cfg.set("max", max);
        return new DropItemEffect(cfg);
    }

    /** How many of this material the player is left holding. */
    private int held(Material type) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == type) total += item.getAmount();
        }
        return total;
    }

    @Test
    void simpleModeDropsAFixedCountFromEachStack() {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 10));

        DeathContextImpl ctx = TestContexts.death(player);
        effect("SIMPLE", 3, 3).onDeath(ctx);
        TestContexts.apply(ctx);

        assertEquals(7, held(Material.DIAMOND), "3 of the 10 should have gone");
        assertEquals(1, world.getEntitiesByClass(Item.class).size());
    }

    @Test
    void percentageModeDropsAShareOfEachStack() {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 10));

        DeathContextImpl ctx = TestContexts.death(player);
        effect("PERCENTAGE", 40, 40).onDeath(ctx);
        TestContexts.apply(ctx);

        assertEquals(6, held(Material.DIAMOND), "40% of 10 should have gone");
    }

    @Test
    void droppingMoreThanTheStackHoldsTakesTheWholeStack() {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 3));

        DeathContextImpl ctx = TestContexts.death(player);
        effect("SIMPLE", 99, 99).onDeath(ctx);
        TestContexts.apply(ctx);

        assertEquals(0, held(Material.DIAMOND), "it cannot drop more than there was");
        assertEquals(3, world.getEntitiesByClass(Item.class).stream()
                .mapToInt(e -> e.getItemStack().getAmount()).sum(),
                "the whole stack should be on the floor");
    }

    @Test
    void aZeroCountDropsNothing() {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 10));

        DeathContextImpl ctx = TestContexts.death(player);
        effect("SIMPLE", 0, 0).onDeath(ctx);
        TestContexts.apply(ctx);

        assertEquals(10, held(Material.DIAMOND));
        assertTrue(world.getEntitiesByClass(Item.class).isEmpty());
    }

    // --- filters -------------------------------------------------------------------------------

    @Test
    void slotFilterOnlyDropsFromTheListedSlots() {
        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 5));
        player.getInventory().setItem(1, new ItemStack(Material.DIAMOND, 5));

        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("mode", "ALL");
        cfg.set("filters.slots", java.util.List.of("0"));

        DeathContextImpl ctx = TestContexts.death(player);
        new DropItemEffect(cfg).onDeath(ctx);
        TestContexts.apply(ctx);

        assertEquals(5, held(Material.DIAMOND), "only the stack in slot 0 should have gone");
    }

    @Test
    void nameFilterOnlyDropsMatchingNames() {
        ItemStack named = new ItemStack(Material.DIAMOND_SWORD);
        var meta = named.getItemMeta();
        meta.setDisplayName("Cursed Blade");
        named.setItemMeta(meta);

        player.getInventory().setItem(0, named);
        player.getInventory().setItem(1, new ItemStack(Material.DIAMOND_PICKAXE));

        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("mode", "ALL");
        cfg.set("filters.name", java.util.List.of("*Cursed*"));

        DeathContextImpl ctx = TestContexts.death(player);
        new DropItemEffect(cfg).onDeath(ctx);
        TestContexts.apply(ctx);

        assertFalse(player.getInventory().contains(Material.DIAMOND_SWORD), "the named blade matches");
        assertTrue(player.getInventory().contains(Material.DIAMOND_PICKAXE), "the unnamed pickaxe does not");
    }

    /**
     * Every lore line has to match, not just one of them.
     * <p>
     * Pinned as-is because it is what 2.x did, but it is worth questioning: a config author
     * writing `lore: ["*soulbound*"]` almost certainly means "has a line saying soulbound", and
     * an item whose lore also carries a second, unrelated line will not match.
     */
    @Test
    void loreFilterRequiresEveryLineToMatch() {
        ItemStack oneLine = new ItemStack(Material.DIAMOND_SWORD);
        var a = oneLine.getItemMeta();
        a.setLore(java.util.List.of("Soulbound"));
        oneLine.setItemMeta(a);

        ItemStack twoLines = new ItemStack(Material.DIAMOND_PICKAXE);
        var b = twoLines.getItemMeta();
        b.setLore(java.util.List.of("Soulbound", "Mining tool"));
        twoLines.setItemMeta(b);

        player.getInventory().setItem(0, oneLine);
        player.getInventory().setItem(1, twoLines);

        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("mode", "ALL");
        cfg.set("filters.lore", java.util.List.of("*Soulbound*"));

        DeathContextImpl ctx = TestContexts.death(player);
        new DropItemEffect(cfg).onDeath(ctx);
        TestContexts.apply(ctx);

        assertFalse(player.getInventory().contains(Material.DIAMOND_SWORD),
                "every line matches, so it drops");
        assertTrue(player.getInventory().contains(Material.DIAMOND_PICKAXE),
                "the second line does not match, so the whole item is skipped");
    }

    @Test
    void filtersCombine() {
        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 5));
        player.getInventory().setItem(1, new ItemStack(Material.COBBLESTONE, 5));

        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("mode", "ALL");
        cfg.set("filters.items", java.util.List.of("DIAMOND"));
        cfg.set("filters.slots", java.util.List.of("1"));

        DeathContextImpl ctx = TestContexts.death(player);
        new DropItemEffect(cfg).onDeath(ctx);
        TestContexts.apply(ctx);

        assertEquals(5, held(Material.DIAMOND), "diamond matches the item filter but not the slot");
        assertEquals(5, held(Material.COBBLESTONE), "cobblestone matches the slot but not the item");
        assertTrue(world.getEntitiesByClass(Item.class).isEmpty(), "so nothing drops");
    }
}
