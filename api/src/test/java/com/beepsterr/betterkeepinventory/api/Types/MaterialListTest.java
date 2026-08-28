package com.beepsterr.betterkeepinventory.api.Types;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MaterialList resolves strings to Materials via Material.matchMaterial and formats item
 * names via ItemMeta, both of which need a (mock) server — hence MockBukkit.
 */
class MaterialListTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static List<Material> materials(String... tokens) {
        return new MaterialList(List.of(tokens)).getMaterials();
    }

    @Test
    void plainMaterialNames() {
        assertEquals(List.of(Material.DIAMOND), materials("DIAMOND"));
    }

    @Test
    void materialNamesAreCaseInsensitive() {
        assertEquals(List.of(Material.DIAMOND), materials("diamond"));
    }

    @Test
    void acceptsNamespacedIds() {
        assertEquals(List.of(Material.OAK_LOG), materials("minecraft:oak_log"));
    }

    @Test
    void deduplicatesRepeatedEntries() {
        assertEquals(List.of(Material.DIAMOND), materials("DIAMOND", "DIAMOND"));
    }

    @Test
    void unknownMaterialsAreSilentlyIgnored() {
        assertTrue(materials("NOT_A_REAL_MATERIAL").isEmpty());
    }

    @Test
    void groupTokenExpandsToItsMembers() {
        var result = materials("G:ARMOR");
        assertTrue(result.contains(Material.DIAMOND_HELMET));
        assertTrue(result.contains(Material.LEATHER_BOOTS));
    }

    @Test
    void exclusionRemovesFromAPreviouslyAddedGroup() {
        var result = materials("G:ARMOR", "!DIAMOND_HELMET");
        assertFalse(result.contains(Material.DIAMOND_HELMET), "excluded item should be gone");
        assertTrue(result.contains(Material.IRON_HELMET), "other armor should remain");
    }

    @Test
    void starAddsEveryNonLegacyMaterial() {
        var result = materials("*");
        assertTrue(result.contains(Material.DIAMOND));
        assertTrue(result.size() > 800, "should include the full non-legacy material set");
        assertTrue(result.stream().noneMatch(Material::isLegacy), "legacy materials must be excluded");
    }

    @Test
    void getNameFallsBackToTitleCasedEnumName() {
        assertEquals("Diamond Sword", MaterialList.GetName(new ItemStack(Material.DIAMOND_SWORD)));
    }

    @Test
    void getNameUsesItemNameComponentWhenThereIsNoDisplayName() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.setItemName("Ruby Sword");
        item.setItemMeta(meta);

        assertEquals("Ruby Sword", MaterialList.GetName(item));
    }

    @Test
    void getNamePrefersDisplayNameOverItemName() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.setItemName("Ruby Sword");
        meta.setDisplayName("Excalibur");
        item.setItemMeta(meta);

        assertEquals("Excalibur", MaterialList.GetName(item));
    }

    @Test
    void getNamePrefersCustomDisplayName() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("Excalibur");
        item.setItemMeta(meta);

        assertEquals("Excalibur", MaterialList.GetName(item));
    }
}
