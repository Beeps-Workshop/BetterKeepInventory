# Per World
Players keep their inventory in the overworld, but drop everything in the Nether and the End.

## Required Plugins
- None

## Rule
```yaml
default_behavior: KEEP

rules:
  dangerous_dimensions:
    name: "Drop everything in the Nether and End"
    enabled: true
    conditions:
      worlds:
        nodes:
          - "world_nether"
          - "world_the_end"
    effects:
      drop:
        mode: ALL
      exp:
        mode: ALL
        how: DROP
```

## How it works
The [Worlds](../Conditions/Worlds.md) condition matches when the player died in **any** of the listed worlds. A `drop` effect without filters drops every item, and the `exp` effect drops all of their levels as orbs. Deaths in any other world keep everything.

`world_nether` and `world_the_end` are the default world names on Spigot and Paper. If your worlds are named differently, use those names instead.

## Variations
Match several worlds at once with a `*` wildcard, for example every Nether world on a server with multiple:
```yaml
      worlds:
        nodes:
          - "*_nether"
```

Use a different rule per world by adding more rules, each with its own world:
```yaml
rules:
  nether:
    name: "Nether: drop resources"
    enabled: true
    conditions:
      worlds:
        nodes:
          - "world_nether"
    effects:
      drop:
        mode: ALL
        filters:
          items:
            - "G:RESOURCES"
  the_end:
    name: "End: drop everything"
    enabled: true
    conditions:
      worlds:
        nodes:
          - "world_the_end"
    effects:
      drop:
        mode: ALL
```

:::tip
Want the opposite, dropping everywhere **except** one world? Rules can only take items away, not give them back, so keep `default_behavior: KEEP` and match every other world with `!`:
```yaml
      worlds:
        nodes:
          - "!world"   # <-- Any world except the overworld
```
:::
