# Damage Armor
Players keep everything, but the armor they're wearing takes some durability damage when they die.

## Required Plugins
- None

## Rule
```yaml
default_behavior: KEEP

rules:
  damage_armor:
    name: "Damage worn armor"
    enabled: true
    effects:
      damage:
        mode: PERCENTAGE
        min: 10.0
        max: 20.0               # <-- Lose 10% to 20% of max durability
        dont_break: true        # <-- Never break the armor completely
        use_enchantments: true  # <-- Unbreaking reduces the damage
        filters:
          slots:
            - "ARMOR"           # <-- Only the armor slots
```

## How it works
The `ARMOR` [slot list](../../Types/SlotLists.md) only covers the four armor slots, so only armor the player is **wearing** gets damaged. Spare armor in their inventory is left alone.

With `dont_break: true` armor is left on the edge of breaking instead of being destroyed, so players won't lose their gear outright from a death.

## Variations
Damage all armor, including spare pieces in the inventory, by filtering on items instead of slots:
```yaml
        filters:
          items:
            - "G:ARMOR"
```

Damage everything that can be damaged, like tools and weapons too:
```yaml
        filters:
          items:
            - "G:EQUIPMENT"
```
