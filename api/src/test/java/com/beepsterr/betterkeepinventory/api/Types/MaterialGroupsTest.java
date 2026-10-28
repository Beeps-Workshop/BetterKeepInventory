package com.beepsterr.betterkeepinventory.api.Types;

import com.beepsterr.betterkeepinventory.api.Exceptions.TypeError;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure logic — MaterialGroups only touches the Material enum constants and a static map,
 * so no MockBukkit server is needed.
 */
class MaterialGroupsTest {

    @Test
    void parsesPlainGroupName() throws TypeError {
        assertEquals(MaterialGroups.ARMOR, MaterialGroups.parse("ARMOR"));
    }

    @ParameterizedTest(name = "\"{0}\" resolves to the ARMOR group")
    @ValueSource(strings = {"ARMOR", "G:ARMOR", "armor", "g:armor", "  ARMOR  ", "G: ARMOR"})
    void acceptsPrefixCaseAndWhitespace(String token) throws TypeError {
        assertEquals(MaterialGroups.ARMOR, MaterialGroups.parse(token));
    }

    @Test
    void armorGroupContainsExpectedMaterials() throws TypeError {
        var armor = MaterialGroups.parse("ARMOR");
        assertTrue(armor.contains(Material.DIAMOND_HELMET));
        assertTrue(armor.contains(Material.NETHERITE_BOOTS));
        assertFalse(armor.contains(Material.DIAMOND_SWORD));
    }

    @Test
    void aggregateGroupsFlattenTheirMembers() throws TypeError {
        // RESOURCES is the union of the per-metal resource groups
        var resources = MaterialGroups.parse("RESOURCES");
        assertTrue(resources.contains(Material.DIAMOND));
        assertTrue(resources.contains(Material.IRON_INGOT));
        assertTrue(resources.contains(Material.ANCIENT_DEBRIS));

        // EQUIPMENT is armor + weapons + tools
        var equipment = MaterialGroups.parse("EQUIPMENT");
        assertTrue(equipment.contains(Material.NETHERITE_CHESTPLATE)); // armor
        assertTrue(equipment.contains(Material.DIAMOND_SWORD));        // weapon
        assertTrue(equipment.contains(Material.IRON_PICKAXE));         // tool
    }

    @ParameterizedTest(name = "documented group \"{0}\" is registered")
    @ValueSource(strings = {"RESOURCE_MISC", "MISC_TOOLS"})
    void documentedSubGroupsAreRegistered(String group) {
        assertDoesNotThrow(() -> MaterialGroups.parse(group));
    }

    @Test
    void equipmentCoversAllWearableAndHeldGear() throws TypeError {
        var equipment = MaterialGroups.parse("EQUIPMENT");
        assertTrue(equipment.contains(Material.SHIELD));
        assertTrue(equipment.contains(Material.ELYTRA));
        assertTrue(equipment.contains(Material.TURTLE_HELMET));
        assertTrue(equipment.contains(Material.SPYGLASS));
    }

    @ParameterizedTest(name = "\"{0}\" is equipment on servers that have it")
    @ValueSource(strings = {"MACE", "COPPER_CHESTPLATE", "COPPER_SWORD", "COPPER_PICKAXE", "NETHERITE_SPEAR", "BRUSH"})
    void newerGearIsEquipment(String name) throws TypeError {
        Material material = Material.getMaterial(name);
        assertNotNull(material, name + " should exist on the test server's API");
        assertTrue(MaterialGroups.parse("EQUIPMENT").contains(material));
    }

    @Test
    void invalidGroupThrows() {
        assertThrows(TypeError.class, () -> MaterialGroups.parse("NOT_A_GROUP"));
    }

    @Test
    void nullTokenThrows() {
        assertThrows(TypeError.class, () -> MaterialGroups.parse(null));
    }
}
