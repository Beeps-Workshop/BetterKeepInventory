# Keep Gear
Players keep their armor, weapons and tools when they die, and drop everything else. Similar to how Terraria's mediumcore works, but in reverse.

## Required Plugins
- None

## Rule
```yaml
default_behavior: KEEP # <-- Start from keeping everything

rules:
  keep_gear:
    name: "Keep gear, drop everything else"
    enabled: true
    effects:
      drop:
        mode: ALL
        filters:
          items:
            - "*"              # <-- Every item...
            - "!G:EQUIPMENT"   # <-- ...except armor, weapons and tools
```

## How it works
`default_behavior: KEEP` makes every death keep the whole inventory, and the rule then drops everything that isn't equipment. See [Resource Lists](../../Types/ResourceLists.md) for exactly what `G:EQUIPMENT` includes.

The order of the items list matters: `*` selects every item, and `!G:EQUIPMENT` then removes equipment from that selection. Written the other way around, `*` would add equipment straight back.

## Variations
Keep more than just gear by adding more exclusions below `*`:
```yaml
          items:
            - "*"
            - "!G:EQUIPMENT"
            - "!G:POTIONS"          # <-- Keep potions
            - "!TOTEM_OF_UNDYING"   # <-- Keep totems
            - "!ENDER_CHEST"        # <-- Keep ender chests
```

:::tip
This rule only drops items, so any rules **below** it still see the equipment the player kept. You could add a rule after it that [damages](../Effects/Damage.md) the kept gear, for example.
:::
