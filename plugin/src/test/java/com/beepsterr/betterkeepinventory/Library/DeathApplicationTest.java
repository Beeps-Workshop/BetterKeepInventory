package com.beepsterr.betterkeepinventory.Library;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.Content.Effects.DropItemEffect;
import com.beepsterr.betterkeepinventory.support.NoopLogger;
import com.beepsterr.betterkeepinventory.support.TestContexts;
import org.bukkit.Material;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The step where things actually leave the player, and therefore the last place an invariant can
 * be enforced.
 */
class DeathApplicationTest {

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

    private static ItemStack cursed(Material type) {
        ItemStack item = new ItemStack(type);
        item.addUnsafeEnchantment(Enchantment.VANISHING_CURSE, 1);
        return item;
    }

    private long groundCountOf(Material type) {
        return world.getEntitiesByClass(Item.class).stream()
                .filter(e -> e.getItemStack().getType() == type)
                .count();
    }

    private static DropItemEffect dropEverything() {
        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("mode", "ALL");
        return new DropItemEffect(cfg);
    }

    /**
     * The bucket is filled with the curse in mind, but an effect can move items into it
     * afterwards -- and so can an addon, from BKIPlayerDeathProcessedEvent. Enforcing it only at
     * fill time left `KEEP` plus a `drop` rule spawning an item vanilla destroys.
     */
    @Test
    void aCursedItemMovedIntoDropsByAnEffectIsStillDestroyed() {
        player.getInventory().addItem(cursed(Material.DIAMOND_SWORD));
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));

        DeathContextImpl ctx = TestContexts.death(player, null, Config.DefaultBehavior.KEEP);
        dropEverything().onDeath(ctx);
        TestContexts.apply(ctx);

        assertEquals(0, groundCountOf(Material.DIAMOND_SWORD),
                "the cursed sword must not reach the ground, whichever route put it in drops");
        assertEquals(1, groundCountOf(Material.DIAMOND), "everything else drops as normal");
        assertFalse(player.getInventory().contains(Material.DIAMOND_SWORD),
                "and it is gone rather than quietly kept");
    }

    /** The same hole, reached the way an addon would reach it. */
    @Test
    void aCursedItemAddedStraightToDropsIsStillDestroyed() {
        DeathContextImpl ctx = TestContexts.death(player, null, Config.DefaultBehavior.KEEP);
        ctx.drops().add(cursed(Material.ELYTRA));

        TestContexts.apply(ctx);

        assertEquals(0, groundCountOf(Material.ELYTRA));
    }

    @Test
    void anUncursedItemInDropsStillReachesTheGround() {
        DeathContextImpl ctx = TestContexts.death(player, null, Config.DefaultBehavior.KEEP);
        ctx.drops().add(new ItemStack(Material.ELYTRA));

        TestContexts.apply(ctx);

        assertEquals(1, groundCountOf(Material.ELYTRA));
    }

    @Test
    void whatTheContextKeepsIsWhatThePlayerEndsUpHolding() {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));

        DeathContextImpl ctx = TestContexts.death(player, null, Config.DefaultBehavior.KEEP);
        TestContexts.apply(ctx);

        assertTrue(player.getInventory().contains(Material.DIAMOND));
        assertTrue(world.getEntitiesByClass(Item.class).isEmpty());
    }
}
