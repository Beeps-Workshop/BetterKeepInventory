# Death Cause
Players keep their items when they die to an accident, but drop their resources when they die in a fight.

## Required Plugins
- None

## Rule
```yaml
default_behavior: KEEP

rules:
  died_fighting:
    name: "Died in a fight"
    enabled: true
    conditions:
      cause:
        nodes:
          - "ENTITY_ATTACK"     # <-- Melee attacks from mobs and players
          - "ENTITY_SWEEP_ATTACK"
          - "PROJECTILE"        # <-- Arrows, tridents, etc.
          - "ENTITY_EXPLOSION"  # <-- Creepers
    effects:
      drop:
        mode: ALL
        filters:
          items:
            - "G:RESOURCES"
```

## How it works
The [Cause](../Conditions/Cause.md) condition matches the damage that killed the player. The rule only applies when it matches **any** of the listed causes, so falls, drowning, lava and other accidents keep everything.

:::note
Damage causes can't tell mobs and players apart. `ENTITY_ATTACK` is used both when a zombie and when another player kills you. To only punish PvP deaths, use a combat tagging plugin instead, see the [Combat Tag](./Combat%20Tag.md) example.
:::

## Avoid dropping items into the void
Items dropped in the void are gone for good. If you want players to drop items on every death, leave void deaths out:
```yaml
rules:
  died_anywhere:
    name: "Drop resources, unless in the void"
    enabled: true
    conditions:
      cause:
        nodes:
          - "!VOID"             # <-- Any cause except the void
    effects:
      drop:
        mode: ALL
        filters:
          items:
            - "G:RESOURCES"
```

:::warning
A death matches the condition if it matches **any** entry, so don't mix `!` entries with normal ones. `["FALL", "!VOID"]` matches every death except the void, including `FALL`, because every non-void death already matches `!VOID`.
:::
