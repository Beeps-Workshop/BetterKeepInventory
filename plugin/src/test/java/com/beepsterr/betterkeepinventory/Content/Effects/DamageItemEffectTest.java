package com.beepsterr.betterkeepinventory.Content.Effects;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.support.NoopLogger;
import org.bukkit.Material;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Layer 3: effect test with the full plugin loaded. DamageItemEffect reads the
 * BetterKeepInventory singleton (rng, config, metrics, debug), so we boot the
 * plugin via MockBukkit, then drive onDeath() directly and assert on item
 * durability (Damageable#getDamage()).
 *
 * All damage tests use PERCENTAGE/SIMPLE with min == max so the RNG term
 * ((max - min) * rng.nextDouble()) drops out, making the applied damage
 * deterministic.
 */
class DamageItemEffectTest {

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

    // ---- helpers -----------------------------------------------------------

    private static int damageOf(ItemStack item) {
        return ((Damageable) item.getItemMeta()).getDamage();
    }

    private static ItemStack withDamage(Material material, int damage) {
        ItemStack item = new ItemStack(material);
        Damageable meta = (Damageable) item.getItemMeta();
        meta.setDamage(damage);
        item.setItemMeta(meta);
        return item;
    }

    private static DamageItemEffect effect(String mode, double min, double max) {
        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("mode", mode);
        cfg.set("min", min);
        cfg.set("max", max);
        return new DamageItemEffect(cfg);
    }

    // ---- tests -------------------------------------------------------------

    @Test
    void percentageModeDamagesArmorAndTools() {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_CHESTPLATE));
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_PICKAXE));

        // PERCENTAGE 10%: damage = (int)(maxDurability * 10 / 100)
        effect("PERCENTAGE", 10.0, 10.0).onDeath(player, null, new NoopLogger());

        int chestMax = Material.DIAMOND_CHESTPLATE.getMaxDurability(); // 528
        int pickMax = Material.DIAMOND_PICKAXE.getMaxDurability();     // 1561

        assertEquals((int) (chestMax * 0.10), damageOf(player.getInventory().getItem(0)),
                "diamond chestplate should take 10% durability damage");
        assertEquals((int) (pickMax * 0.10), damageOf(player.getInventory().getItem(1)),
                "diamond pickaxe should take 10% durability damage");
    }

    @Test
    void unbreakableItemsAreLeftAlone() {
        ItemStack pickaxe = new ItemStack(Material.DIAMOND_PICKAXE);
        var meta = pickaxe.getItemMeta();
        meta.setUnbreakable(true);
        pickaxe.setItemMeta(meta);
        player.getInventory().addItem(pickaxe);
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD));

        effect("PERCENTAGE", 10.0, 10.0).onDeath(player, null, new NoopLogger());

        assertEquals(0, damageOf(player.getInventory().getItem(0)),
                "unbreakable pickaxe should take no durability damage");
        assertEquals((int) (Material.DIAMOND_SWORD.getMaxDurability() * 0.10),
                damageOf(player.getInventory().getItem(1)),
                "breakable sword should still be damaged");
    }

    @Test
    void unbreakableItemsDoNotBreakAtNearMaxDamage() {
        ItemStack pickaxe = withDamage(Material.DIAMOND_PICKAXE,
                Material.DIAMOND_PICKAXE.getMaxDurability() - 1);
        var meta = pickaxe.getItemMeta();
        meta.setUnbreakable(true);
        pickaxe.setItemMeta(meta);
        player.getInventory().addItem(pickaxe);

        effect("PERCENTAGE", 50.0, 50.0).onDeath(player, null, new NoopLogger());

        assertEquals(Material.DIAMOND_PICKAXE, player.getInventory().getItem(0).getType(),
                "unbreakable pickaxe should survive damage that would break a normal one");
        assertEquals(1, player.getInventory().getItem(0).getAmount(),
                "unbreakable pickaxe should not be consumed by the break branch");
        assertEquals(Material.DIAMOND_PICKAXE.getMaxDurability() - 1,
                damageOf(player.getInventory().getItem(0)),
                "unbreakable pickaxe should keep its existing damage value");
    }

    @Test
    void maxDamageComponentOverridesMaterialDurability() {
        ItemStack pickaxe = new ItemStack(Material.DIAMOND_PICKAXE);
        Damageable meta = (Damageable) pickaxe.getItemMeta();
        meta.setMaxDamage(100);
        pickaxe.setItemMeta(meta);
        player.getInventory().addItem(pickaxe);

        // PERCENTAGE 10% of the item's own max_damage (100), not the material's 1561
        effect("PERCENTAGE", 10.0, 10.0).onDeath(player, null, new NoopLogger());

        assertEquals(10, damageOf(player.getInventory().getItem(0)),
                "damage should scale off the max_damage component, not Material#getMaxDurability");
    }

    @Test
    void nameFilterMatchesTheItemNameComponent() {
        ItemStack named = new ItemStack(Material.DIAMOND_PICKAXE);
        var namedMeta = named.getItemMeta();
        namedMeta.setItemName("Ruby Pickaxe");
        named.setItemMeta(namedMeta);
        player.getInventory().addItem(named);
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_PICKAXE));

        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("mode", "PERCENTAGE");
        cfg.set("min", 10.0);
        cfg.set("max", 10.0);
        cfg.set("filters.name", List.of("Ruby Pickaxe"));
        new DamageItemEffect(cfg).onDeath(player, null, new NoopLogger());

        int expected = (int) (Material.DIAMOND_PICKAXE.getMaxDurability() * 0.10);
        assertEquals(expected, damageOf(player.getInventory().getItem(0)),
                "item named via the item_name component should match the name filter");
        assertEquals(0, damageOf(player.getInventory().getItem(1)),
                "unnamed pickaxe should be filtered out");
    }

    @Test
    void unnamedItemsSurviveAConfiguredNameFilter() {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_PICKAXE));

        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("mode", "PERCENTAGE");
        cfg.set("min", 10.0);
        cfg.set("max", 10.0);
        cfg.set("filters.name", List.of("Ruby Pickaxe"));
        new DamageItemEffect(cfg).onDeath(player, null, new NoopLogger());

        assertEquals(0, damageOf(player.getInventory().getItem(0)),
                "an item with no name at all must not blow up or match the filter");
    }

    @Test
    void nonDamageableItemsAreLeftAlone() {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_PICKAXE));

        effect("PERCENTAGE", 10.0, 10.0).onDeath(player, null, new NoopLogger());

        // Diamonds have no durability -> untouched (still a full stack of 5).
        assertEquals(5, player.getInventory().getItem(0).getAmount(),
                "non-damageable diamonds should be untouched");
        assertEquals((int) (Material.DIAMOND_PICKAXE.getMaxDurability() * 0.10),
                damageOf(player.getInventory().getItem(1)),
                "pickaxe should still be damaged");
    }

    @Test
    void itemsFilterRestrictsWhichItemsAreDamaged() {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_CHESTPLATE));
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_PICKAXE));

        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("mode", "PERCENTAGE");
        cfg.set("min", 10.0);
        cfg.set("max", 10.0);
        cfg.set("filters.items", List.of("DIAMOND_PICKAXE"));
        new DamageItemEffect(cfg).onDeath(player, null, new NoopLogger());

        assertEquals(0, damageOf(player.getInventory().getItem(0)),
                "chestplate is not in the item filter and must be untouched");
        assertEquals((int) (Material.DIAMOND_PICKAXE.getMaxDurability() * 0.10),
                damageOf(player.getInventory().getItem(1)),
                "pickaxe matches the item filter and must be damaged");
    }

    @Test
    void slotsFilterRestrictsDamageToArmorSlots() {
        // Pickaxe in a main-inventory slot, chestplate worn in an armor slot.
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_PICKAXE)); // slot 0
        player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE)); // slot 38

        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("mode", "PERCENTAGE");
        cfg.set("min", 10.0);
        cfg.set("max", 10.0);
        cfg.set("filters.slots", List.of("ARMOR"));
        new DamageItemEffect(cfg).onDeath(player, null, new NoopLogger());

        assertEquals(0, damageOf(player.getInventory().getItem(0)),
                "pickaxe sits in a non-armor slot and must be untouched");
        assertEquals((int) (Material.DIAMOND_CHESTPLATE.getMaxDurability() * 0.10),
                damageOf(player.getInventory().getChestplate()),
                "worn chestplate is in an ARMOR slot and must be damaged");
    }

    @Test
    void dontBreakSavesItemAtNearMaxDamage() {
        int max = Material.DIAMOND_PICKAXE.getMaxDurability();
        player.getInventory().addItem(withDamage(Material.DIAMOND_PICKAXE, max - 1));

        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("mode", "PERCENTAGE");
        cfg.set("min", 10.0);
        cfg.set("max", 10.0);
        cfg.set("dont_break", true);
        new DamageItemEffect(cfg).onDeath(player, null, new NoopLogger());

        ItemStack pick = player.getInventory().getItem(0);
        assertTrue(pick != null && pick.getType() == Material.DIAMOND_PICKAXE,
                "dont_break should keep the item in the inventory");
        assertEquals(1, pick.getAmount(), "the item must not be consumed");
        assertEquals(max, damageOf(pick),
                "dont_break should clamp damage to max durability instead of breaking");
    }

    @Test
    void withoutDontBreakItemBreaksAtNearMaxDamage() {
        int max = Material.DIAMOND_PICKAXE.getMaxDurability();
        player.getInventory().addItem(withDamage(Material.DIAMOND_PICKAXE, max - 1));

        // dont_break defaults to false: 10% of max overflows durability -> breaks.
        effect("PERCENTAGE", 10.0, 10.0).onDeath(player, null, new NoopLogger());

        // The production code breaks the item via item.setAmount(amount - 1),
        // taking the single pickaxe to amount 0. MockBukkit keeps the slot's
        // ItemStack reference rather than nulling it, so an amount-0 (or
        // AIR/null) stack all represent "broken and gone".
        ItemStack pick = player.getInventory().getItem(0);
        boolean broken = pick == null || pick.getType() == Material.AIR || pick.getAmount() == 0;
        assertTrue(broken, "without dont_break the item should break (consumed to amount 0)");
    }

    @Test
    void simpleModeAppliesFlatDamageRegardlessOfMaxDurability() {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_CHESTPLATE)); // maxDur 528
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_PICKAXE));    // maxDur 1561

        // SIMPLE min==max==7 -> both items take a flat 7 points, independent of max.
        effect("SIMPLE", 7.0, 7.0).onDeath(player, null, new NoopLogger());

        assertEquals(7, damageOf(player.getInventory().getItem(0)),
                "SIMPLE mode applies a flat damage to the chestplate");
        assertEquals(7, damageOf(player.getInventory().getItem(1)),
                "SIMPLE mode applies the same flat damage to the pickaxe");
    }

    @Test
    void percentageAndSimpleModesDifferForHighDurabilityItems() {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_PICKAXE));
        effect("SIMPLE", 5.0, 5.0).onDeath(player, null, new NoopLogger());
        int simpleDamage = damageOf(player.getInventory().getItem(0));

        // fresh player/inventory for the percentage run
        PlayerMock player2 = server.addPlayer();
        player2.getInventory().addItem(new ItemStack(Material.DIAMOND_PICKAXE));
        effect("PERCENTAGE", 5.0, 5.0).onDeath(player2, null, new NoopLogger());
        int percentageDamage = damageOf(player2.getInventory().getItem(0));

        assertEquals(5, simpleDamage, "SIMPLE 5 -> flat 5 points");
        assertEquals((int) (Material.DIAMOND_PICKAXE.getMaxDurability() * 0.05), percentageDamage,
                "PERCENTAGE 5 -> 5% of 1561");
        assertTrue(percentageDamage > simpleDamage,
                "for a high-durability item PERCENTAGE should exceed the flat SIMPLE damage");
    }
}
