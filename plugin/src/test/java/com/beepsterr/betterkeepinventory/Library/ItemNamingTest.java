package com.beepsterr.betterkeepinventory.Library;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Layer 2: Utilities item naming needs a server for ItemStack/ItemMeta, but no plugin.
 *
 * An item can carry two different names since 1.20.5: custom_name (what an anvil or
 * /give sets) and item_name (what most custom item plugins set). Only the first is
 * visible through ItemMeta#getDisplayName, so both have to be consulted.
 */
class ItemNamingTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static ItemStack named(String displayName, String itemName) {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (displayName != null) meta.setDisplayName(displayName);
        if (itemName != null) meta.setItemName(itemName);
        item.setItemMeta(meta);
        return item;
    }

    @Test
    void itemNameReturnsNullWhenTheItemOnlyHasItsMaterialName() {
        assertNull(Utilities.itemName(named(null, null).getItemMeta()));
    }

    @Test
    void itemNameReadsTheItemNameComponent() {
        assertEquals("Ruby Sword", Utilities.itemName(named(null, "Ruby Sword").getItemMeta()));
    }

    @Test
    void itemNamePrefersCustomNameOverItemName() {
        assertEquals("Excalibur", Utilities.itemName(named("Excalibur", "Ruby Sword").getItemMeta()));
    }

    @Test
    void displayNameFallsBackToTheReadableMaterialName() {
        assertEquals("Diamond Sword", Utilities.itemDisplayName(named(null, null)));
    }

    @Test
    void displayNameUsesTheItemNameComponent() {
        assertEquals("Ruby Sword", Utilities.itemDisplayName(named(null, "Ruby Sword")));
    }
}
