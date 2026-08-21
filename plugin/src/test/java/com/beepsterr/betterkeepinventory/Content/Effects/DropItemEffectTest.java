package com.beepsterr.betterkeepinventory.Content.Effects;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
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
 * MockBukkit. Then we drive onDeath() directly and assert on the world/inventory.
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

        effect("ALL").onDeath(TestContexts.death(player));

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
        new DropItemEffect(cfg).onDeath(TestContexts.death(player));

        assertTrue(player.getInventory().contains(Material.DIAMOND), "diamonds should be kept (not in filter)");
        assertFalse(player.getInventory().contains(Material.DIRT), "dirt should be dropped (matches filter)");
    }
}
