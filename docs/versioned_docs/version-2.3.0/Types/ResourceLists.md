# Resource Lists
Wherever a list of items is accepted, you can use the following resource groups to refer to them or a group of items

| Value           | Includes Materials                                                                                             |
|-----------------|----------------------------------------------------------------------------------------------------------------|
| minecraft:id     | A Namespaced Minecraft ID (minecraft:oak_log)                                                                  |
| `Material`      | A specific [Spigot Material](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html)                |
| G:RESOURCES     | G:COPPER, G:IRON, G:GOLD, G:DIAMOND, G:NETHERITE, and EMERALD, LAPIS_LAZULI, COAL, REDSTONE, QUARTZ, AMETHYST with their ores/blocks |
| G:NETHERITE     | NETHERITE_SCRAP, NETHERITE_INGOT, NETHERITE_BLOCK, ANCIENT_DEBRIS                                              |
| G:DIAMOND       | DIAMOND, DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE, DIAMOND_BLOCK                                                     |
| G:GOLD          | GOLD_ORE, DEEPSLATE_GOLD_ORE, RAW_GOLD, RAW_GOLD_BLOCK, GOLD_NUGGET, GOLD_INGOT, GOLD_BLOCK                    |
| G:IRON          | IRON_ORE, DEEPSLATE_IRON_ORE, RAW_IRON, RAW_IRON_BLOCK, IRON_NUGGET, IRON_INGOT, IRON_BLOCK                    |
| G:COPPER        | COPPER_ORE, DEEPSLATE_COPPER_ORE, RAW_COPPER, RAW_COPPER_BLOCK, COPPER_INGOT, COPPER_BLOCK, WAXED_COPPER_BLOCK |
| G:TOOLS         | G:AXES, G:PICKAXES, G:SHOVELS, G:HOES, FISHING_ROD, SHEARS, FLINT_AND_STEEL, CARROT_ON_A_STICK, WARPED_FUNGUS_ON_A_STICK |
| G:WEAPONS       | G:SWORDS, BOW, CROSSBOW, TRIDENT                                                                               |
| G:SWORDS        | WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD, DIAMOND_SWORD, NETHERITE_SWORD                            |
| G:AXES          | WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE, DIAMOND_AXE, NETHERITE_AXE                                        |
| G:PICKAXES      | WOODEN_PICKAXE, STONE_PICKAXE, IRON_PICKAXE, GOLDEN_PICKAXE, DIAMOND_PICKAXE, NETHERITE_PICKAXE                |
| G:SHOVELS       | WOODEN_SHOVEL, STONE_SHOVEL, IRON_SHOVEL, GOLDEN_SHOVEL, DIAMOND_SHOVEL, NETHERITE_SHOVEL                      |
| G:HOES          | WOODEN_HOE, STONE_HOE, IRON_HOE, GOLDEN_HOE, DIAMOND_HOE, NETHERITE_HOE                                        |
| G:ARMOR         | All leather, chainmail, iron, gold, diamond and netherite armor pieces                                         |
| G:EQUIPMENT     | G:ARMOR, G:TOOLS, G:WEAPONS                                                                                    |
| G:POTIONS       | POTION, SPLASH_POTION, LINGERING_POTION                                                                        |

:::note
The groups don't include newer gear like copper armor and tools, the mace or spears, or the SHIELD, ELYTRA and TURTLE_HELMET. Add those by name if you need them.
:::

## Everything, and excluding items
| Value           | Effect                                                                 |
|-----------------|------------------------------------------------------------------------|
| `*`             | Every item in the game                                                 |
| `!<value>`      | Removes an item or group from the list, e.g. `!DIAMOND` or `!G:ARMOR`  |

Unlike name and lore filters, item lists don't support partial wildcards: `*` only works on its own, so something like `*_SWORD` won't match anything. Use a group like `G:SWORDS` instead.

Entries are applied **from top to bottom**, so an exclusion only removes items that were added above it. This means you can select everything except a group:
```yaml
items:
  - "*"              # Every item...
  - "!G:EQUIPMENT"   # ...except equipment
```

:::warning
An exclusion can only remove items that were added above it. On its own it removes nothing, and an empty list means the filter matches **every** item:
```yaml
items:
  - "!G:EQUIPMENT"   # Nothing to remove yet, so this matches every item!
```
`*` also replaces the whole list, so anything excluded *before* it is added right back:
```yaml
items:
  - "!G:EQUIPMENT"
  - "*"              # Adds equipment back. This matches every item!
```
:::

See the [Keep Gear](../Rules/Examples/Keep%20Gear.md) example for a full rule using this.
