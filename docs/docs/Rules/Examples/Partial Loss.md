# Partial Loss
Players keep their inventory, but lose part of their resources and experience when they die. Death still hurts, without wiping out hours of progress.

## Required Plugins
- None

## Rule
```yaml
default_behavior: KEEP

rules:
  partial_loss:
    name: "Lose some resources and exp"
    enabled: true
    effects:
      drop:
        mode: PERCENTAGE
        min: 25.0
        max: 50.0            # <-- Drop 25% to 50% of each stack...
        filters:
          items:
            - "G:RESOURCES"  # <-- ...of ores, ingots and gems
      exp:
        mode: PERCENTAGE
        how: DROP            # <-- Drop the lost exp as orbs so it can be picked back up
        min: 10.0
        max: 20.0            # <-- Lose 10% to 20% of their levels
```

## How it works
The `drop` effect takes a random percentage between `min` and `max` from **each stack** of resources, and drops it where the player died. A player with 64 iron ingots drops between 16 and 32 of them.

The `exp` effect takes a percentage of the player's levels. With `how: DROP` they're dropped as exp orbs, use `how: DELETE` to remove them for good.

:::note
Percentages round down per stack, so items that don't stack (like tools and armor) are never dropped by a `PERCENTAGE` drop. To lose some gear too, use the [Damage](../Effects/Damage.md) effect, or a separate `drop` with `mode: ALL` and an item filter.
:::

## Variations
Drop more than just resources by changing the item filter, see [Resource Lists](../../Types/ResourceLists.md):
```yaml
        filters:
          items:
            - "*"              # <-- Everything...
            - "!G:EQUIPMENT"   # <-- ...except gear
```
