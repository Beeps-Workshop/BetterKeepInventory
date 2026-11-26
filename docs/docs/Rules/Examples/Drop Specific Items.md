# Drop Specific Items
Players keep their inventory, except for a few items you pick. Good for valuables, or for items that shouldn't be safe to carry around.

## Required Plugins
- None

## Rule
```yaml
default_behavior: KEEP

rules:
  drop_valuables:
    name: "Drop valuables"
    enabled: true
    effects:
      drop:
        mode: ALL
        filters:
          items:
            - "G:DIAMOND"          # <-- Diamonds, diamond ore and blocks
            - "G:NETHERITE"        # <-- Netherite and ancient debris
            - "TOTEM_OF_UNDYING"   # <-- A single item
            - "minecraft:elytra"   # <-- Minecraft IDs work too
```

## How it works
`mode: ALL` drops every item that matches the filter, and everything else stays in the player's inventory. You can mix single items and [resource groups](../../Types/ResourceLists.md) in the same list.

## Variations
Only drop part of each stack, see [Partial Loss](./Partial%20Loss.md):
```yaml
      drop:
        mode: PERCENTAGE
        min: 50.0
        max: 50.0          # <-- Always drop half
```

Only drop items from the hotbar, using a [slot list](../../Types/SlotLists.md):
```yaml
        filters:
          slots:
            - "HOTBAR"
```

Protect items with a custom name, like crate keys or event rewards:
```yaml
        filters:
          items:
            - "G:DIAMOND"
          name:
            - "!*Key*"     # <-- Unless "Key" is in the item's name
```
