package com.beepsterr.betterkeepinventory.Library;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.support.NoopLogger;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How the two buckets are filled before any rule gets a turn. */
class DeathContextImplTest {

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

    private DeathContextImpl contextWith(Config.DefaultBehavior behavior) {
        return new DeathContextImpl(player, null, behavior, new NoopLogger(), List.of());
    }

    private static ItemStack cursed(Material type) {
        ItemStack item = new ItemStack(type);
        item.addUnsafeEnchantment(Enchantment.VANISHING_CURSE, 1);
        return item;
    }

    private static boolean holds(List<ItemStack> stacks, Material type) {
        return stacks.stream().anyMatch(i -> i != null && i.getType() == type);
    }

    private static boolean holds(ItemStack[] stacks, Material type) {
        for (ItemStack item : stacks) {
            if (item != null && item.getType() == type) return true;
        }
        return false;
    }

    /**
     * Dying destroys a vanishing-cursed item rather than dropping it. The server normally applies
     * that while collecting death loot, but we build both buckets ourselves and pin keepInventory
     * on, so it never gets the chance -- and putting the item in drops would resurrect something
     * vanilla had decided to destroy.
     * <p>
     * Note a conservation check cannot catch this: a destroyed item and a dropped item both
     * conserve. The assertion has to be that it is in *neither* bucket.
     */
    @Test
    void aVanishingCursedItemIsDestroyedRatherThanDropped() {
        player.getInventory().addItem(cursed(Material.DIAMOND_SWORD));
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));

        DeathContextImpl ctx = contextWith(Config.DefaultBehavior.DROP);

        assertFalse(holds(ctx.drops(), Material.DIAMOND_SWORD), "the cursed sword must not drop");
        assertFalse(holds(ctx.inventory(), Material.DIAMOND_SWORD), "nor be kept -- it is destroyed");
        assertTrue(holds(ctx.drops(), Material.DIAMOND), "everything else still drops");
    }

    /** The curse only destroys on a death that drops. Keeping your inventory keeps the item too. */
    @Test
    void aVanishingCursedItemSurvivesWhenTheInventoryIsKept() {
        player.getInventory().addItem(cursed(Material.DIAMOND_SWORD));

        DeathContextImpl ctx = contextWith(Config.DefaultBehavior.KEEP);

        assertTrue(holds(ctx.inventory(), Material.DIAMOND_SWORD),
                "keepInventory overrides the curse, as it does in vanilla");
        assertTrue(ctx.drops().isEmpty());
    }

    @Test
    void theOriginalInventoryStillShowsWhatWasCarried() {
        player.getInventory().addItem(cursed(Material.DIAMOND_SWORD));

        DeathContextImpl ctx = contextWith(Config.DefaultBehavior.DROP);

        assertTrue(holds(ctx.originalInventory(), Material.DIAMOND_SWORD),
                "the snapshot records what they died holding, destroyed or not");
    }

    @Test
    void dropStartsWithEverythingAndKeepStartsWithNothing() {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));

        assertEquals(1, contextWith(Config.DefaultBehavior.DROP).drops().size());
        assertTrue(contextWith(Config.DefaultBehavior.KEEP).drops().isEmpty());
    }
}
