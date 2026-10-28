package com.beepsterr.betterkeepinventory.api.Types;

import com.beepsterr.betterkeepinventory.api.Exceptions.TypeError;
import org.bukkit.Material;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public final class MaterialGroups {

    /**
     * Appends materials that only exist on newer Minecraft versions. They're looked up by name
     * because the API compiles against an older spigot-api, and missing ones are skipped so the
     * groups still load on older servers.
     */
    private static List<Material> withNewer(List<Material> base, String... newer) {
        return Stream.concat(base.stream(), Stream.of(newer).map(Material::getMaterial).filter(Objects::nonNull))
                .toList();
    }

    public static final List<Material> RESOURCE_NETHERITE = List.of(
            Material.NETHERITE_SCRAP,
            Material.NETHERITE_INGOT,
            Material.NETHERITE_BLOCK,
            Material.ANCIENT_DEBRIS
    );

    public static final List<Material> RESOURCE_DIAMOND = List.of(
            Material.DIAMOND,
            Material.DIAMOND_ORE,
            Material.DEEPSLATE_DIAMOND_ORE,
            Material.DIAMOND_BLOCK
    );

    public static final List<Material> RESOURCE_GOLD = List.of(
            Material.GOLD_ORE,
            Material.DEEPSLATE_GOLD_ORE,
            Material.RAW_GOLD,
            Material.RAW_GOLD_BLOCK,
            Material.GOLD_NUGGET,
            Material.GOLD_INGOT,
            Material.GOLD_BLOCK
    );

    public static final List<Material> RESOURCE_IRON = List.of(
            Material.IRON_ORE,
            Material.DEEPSLATE_IRON_ORE,
            Material.RAW_IRON,
            Material.RAW_IRON_BLOCK,
            Material.IRON_NUGGET,
            Material.IRON_INGOT,
            Material.IRON_BLOCK
    );

    public static final List<Material> RESOURCE_COPPER = List.of(
            Material.COPPER_ORE,
            Material.DEEPSLATE_COPPER_ORE,
            Material.RAW_COPPER,
            Material.RAW_COPPER_BLOCK,
//            Material.COPPER_NUGGET,
            Material.COPPER_INGOT,
            Material.COPPER_BLOCK,
            Material.WAXED_COPPER_BLOCK,
            Material.CUT_COPPER,
            Material.WAXED_CUT_COPPER,
            Material.CUT_COPPER_SLAB,
            Material.WAXED_CUT_COPPER_SLAB,
            Material.CUT_COPPER_STAIRS,
            Material.WAXED_CUT_COPPER_STAIRS,
            Material.EXPOSED_COPPER,
            Material.WAXED_EXPOSED_COPPER,
            Material.EXPOSED_CUT_COPPER,
            Material.WAXED_EXPOSED_CUT_COPPER,
            Material.EXPOSED_CUT_COPPER_SLAB,
            Material.WAXED_EXPOSED_CUT_COPPER_SLAB,
            Material.EXPOSED_CUT_COPPER_STAIRS,
            Material.WAXED_EXPOSED_CUT_COPPER_STAIRS,
            Material.WEATHERED_COPPER,
            Material.WAXED_WEATHERED_COPPER,
            Material.WEATHERED_CUT_COPPER,
            Material.WAXED_WEATHERED_CUT_COPPER,
            Material.WEATHERED_CUT_COPPER_SLAB,
            Material.WAXED_WEATHERED_CUT_COPPER_SLAB,
            Material.WEATHERED_CUT_COPPER_STAIRS,
            Material.WAXED_WEATHERED_CUT_COPPER_STAIRS,
            Material.OXIDIZED_COPPER,
            Material.WAXED_OXIDIZED_COPPER,
            Material.OXIDIZED_CUT_COPPER,
            Material.WAXED_OXIDIZED_CUT_COPPER,
            Material.OXIDIZED_CUT_COPPER_SLAB,
            Material.WAXED_OXIDIZED_CUT_COPPER_SLAB,
            Material.OXIDIZED_CUT_COPPER_STAIRS,
            Material.WAXED_OXIDIZED_CUT_COPPER_STAIRS
    );

    public static final List<Material> RESOURCE_MISC = List.of(
            Material.EMERALD,
            Material.EMERALD_ORE,
            Material.DEEPSLATE_EMERALD_ORE,
            Material.EMERALD_BLOCK,
            Material.LAPIS_LAZULI,
            Material.LAPIS_ORE,
            Material.DEEPSLATE_LAPIS_ORE,
            Material.LAPIS_BLOCK,
            Material.COAL,
            Material.COAL_ORE,
            Material.DEEPSLATE_COAL_ORE,
            Material.COAL_BLOCK,
            Material.REDSTONE,
            Material.REDSTONE_ORE,
            Material.DEEPSLATE_REDSTONE_ORE,
            Material.REDSTONE_BLOCK,
            Material.QUARTZ,
            Material.NETHER_QUARTZ_ORE,
            Material.QUARTZ_BLOCK,
            Material.AMETHYST_SHARD,
            Material.BUDDING_AMETHYST,
            Material.AMETHYST_BLOCK
    );

    public static final List<Material> RESOURCES = Stream.of(RESOURCE_COPPER, RESOURCE_IRON,
                    RESOURCE_GOLD, RESOURCE_DIAMOND,
                    RESOURCE_NETHERITE, RESOURCE_MISC)
            .flatMap(List::stream)
            .toList();

    public static final List<Material> ARMOR = withNewer(List.of(
            Material.TURTLE_HELMET,
            Material.ELYTRA,
            Material.SHIELD,
            Material.LEATHER_HELMET,
            Material.LEATHER_CHESTPLATE,
            Material.LEATHER_LEGGINGS,
            Material.LEATHER_BOOTS,
            Material.CHAINMAIL_HELMET,
            Material.CHAINMAIL_CHESTPLATE,
            Material.CHAINMAIL_LEGGINGS,
            Material.CHAINMAIL_BOOTS,
            Material.IRON_HELMET,
            Material.IRON_CHESTPLATE,
            Material.IRON_LEGGINGS,
            Material.IRON_BOOTS,
            Material.GOLDEN_HELMET,
            Material.GOLDEN_CHESTPLATE,
            Material.GOLDEN_LEGGINGS,
            Material.GOLDEN_BOOTS,
            Material.DIAMOND_HELMET,
            Material.DIAMOND_CHESTPLATE,
            Material.DIAMOND_LEGGINGS,
            Material.DIAMOND_BOOTS,
            Material.NETHERITE_HELMET,
            Material.NETHERITE_CHESTPLATE,
            Material.NETHERITE_LEGGINGS,
            Material.NETHERITE_BOOTS
    ), "COPPER_HELMET", "COPPER_CHESTPLATE", "COPPER_LEGGINGS", "COPPER_BOOTS");

    public static final List<Material> WEAPONS = withNewer(List.of(
            Material.WOODEN_SWORD,
            Material.STONE_SWORD,
            Material.IRON_SWORD,
            Material.GOLDEN_SWORD,
            Material.DIAMOND_SWORD,
            Material.NETHERITE_SWORD,
            Material.BOW,
            Material.CROSSBOW,
            Material.TRIDENT
    ), "COPPER_SWORD", "MACE", "WOODEN_SPEAR", "STONE_SPEAR", "COPPER_SPEAR", "IRON_SPEAR",
            "GOLDEN_SPEAR", "DIAMOND_SPEAR", "NETHERITE_SPEAR");

    public static final List<Material> SWORDS = withNewer(List.of(
            Material.WOODEN_SWORD,
            Material.STONE_SWORD,
            Material.IRON_SWORD,
            Material.GOLDEN_SWORD,
            Material.DIAMOND_SWORD,
            Material.NETHERITE_SWORD
    ), "COPPER_SWORD");

    public static final List<Material> AXES = withNewer(List.of(
            Material.WOODEN_AXE,
            Material.STONE_AXE,
            Material.IRON_AXE,
            Material.GOLDEN_AXE,
            Material.DIAMOND_AXE,
            Material.NETHERITE_AXE
    ), "COPPER_AXE");

    public static final List<Material> PICKAXES = withNewer(List.of(
            Material.WOODEN_PICKAXE,
            Material.STONE_PICKAXE,
            Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE,
            Material.DIAMOND_PICKAXE,
            Material.NETHERITE_PICKAXE
    ), "COPPER_PICKAXE");

    public static final List<Material> SHOVELS = withNewer(List.of(
            Material.WOODEN_SHOVEL,
            Material.STONE_SHOVEL,
            Material.IRON_SHOVEL,
            Material.GOLDEN_SHOVEL,
            Material.DIAMOND_SHOVEL,
            Material.NETHERITE_SHOVEL
    ), "COPPER_SHOVEL");

    public static final List<Material> HOES = withNewer(List.of(
            Material.WOODEN_HOE,
            Material.STONE_HOE,
            Material.IRON_HOE,
            Material.GOLDEN_HOE,
            Material.DIAMOND_HOE,
            Material.NETHERITE_HOE
    ), "COPPER_HOE");

    public static final List<Material> POTIONS = List.of(
            Material.POTION,
            Material.SPLASH_POTION,
            Material.LINGERING_POTION
    );

    public static final List<Material> MISC_TOOLS = withNewer(List.of(
            Material.FISHING_ROD,
            Material.SHEARS,
            Material.FLINT_AND_STEEL,
            Material.CARROT_ON_A_STICK,
            Material.WARPED_FUNGUS_ON_A_STICK,
            Material.SPYGLASS
    ), "BRUSH");

    public static final List<Material> TOOLS = Stream.of(AXES, PICKAXES, SHOVELS, HOES, MISC_TOOLS)
            .flatMap(List::stream)
            .toList();

    public static final List<Material> EQUIPMENT = Stream.of(ARMOR, WEAPONS, TOOLS)
            .flatMap(List::stream)
            .toList();

    private static final Map<String, List<Material>> GROUP_MAP = createGroupMap();

    private static Map<String, List<Material>> createGroupMap() {
        Map<String, List<Material>> m = new HashMap<>();
        m.put("RESOURCES", RESOURCES);
        m.put("NETHERITE", RESOURCE_NETHERITE);
        m.put("DIAMOND", RESOURCE_DIAMOND);
        m.put("GOLD", RESOURCE_GOLD);
        m.put("IRON", RESOURCE_IRON);
        m.put("COPPER", RESOURCE_COPPER);
        m.put("RESOURCE_MISC", RESOURCE_MISC);
        m.put("TOOLS", TOOLS);
        m.put("WEAPONS", WEAPONS);
        m.put("SWORDS", SWORDS);
        m.put("AXES", AXES);
        m.put("PICKAXES", PICKAXES);
        m.put("SHOVELS", SHOVELS);
        m.put("HOES", HOES);
        m.put("MISC_TOOLS", MISC_TOOLS);
        m.put("ARMOR", ARMOR);
        m.put("EQUIPMENT", EQUIPMENT);
        m.put("POTIONS", POTIONS);
        return Collections.unmodifiableMap(m);
    }

    /**
     * Parse a token like "G:ARMOR" or "ARMOR" and return the corresponding materials.
     */
    public static List<Material> parse(String token) throws TypeError {
        if (token == null) {
            throw new TypeError("Tried to parse null material group");
        }
        String key = token.trim().toUpperCase();
        if (key.startsWith("G:")) {
            key = key.substring(2).trim();
        }
        List<Material> group = GROUP_MAP.get(key);
        if (group == null) {
            throw new TypeError("Tried to parse invalid material group: " + token);
        }
        return group;
    }
}

