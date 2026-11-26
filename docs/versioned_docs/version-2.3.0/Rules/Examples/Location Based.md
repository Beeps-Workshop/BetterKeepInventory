# Location Based
Death is forgiving in the overworld, but gets harsher the more dangerous the place a player dies in.

- **Overworld:** keep everything
- **Deep underground** (below Y 0): gear takes durability damage
- **Nether and End:** drop all resources too

## Required Plugins
- None

## Rule
```yaml
default_behavior: KEEP

rules:
  deep_underground:
    name: "Deep underground"
    enabled: true
    conditions:
      y_level:
        range: "< 0"           # <-- Below Y 0
    effects:
      damage:
        mode: PERCENTAGE
        min: 10.0
        max: 20.0              # <-- 10% to 20% of max durability
        dont_break: true       # <-- Never break the item completely
        filters:
          items:
            - "G:EQUIPMENT"

  dangerous_dimensions:
    name: "Nether and End"
    enabled: true
    conditions:
      worlds:
        nodes:
          - "world_nether"
          - "world_the_end"
    effects:
      drop:
        mode: ALL
        filters:
          items:
            - "G:RESOURCES"
```

## How it works
Rules are checked from top to bottom, and each one only applies when **all** of its conditions match. A death below Y 0 in the overworld only triggers the first rule, a death in the Nether only triggers the second.

`world_nether` and `world_the_end` are the default world names on Spigot and Paper. If your worlds are named differently, use those names instead. `*` works as a wildcard, so `"*_nether"` matches every world ending in `_nether`.

:::note
The first rule has no world condition, so it applies below Y 0 in **every** world. A player who falls out of the End triggers both rules.
:::

## Variations
Make a rule only apply below Y 0 **in the overworld** by giving it both conditions:
```yaml
    conditions:
      worlds:
        nodes:
          - "world"
      y_level:
        range: "< 0"
```

Or use [child rules](../Intro.md#children) to build on a parent rule's conditions:
```yaml
rules:
  dangerous_dimensions:
    name: "Nether and End"
    enabled: true
    conditions:
      worlds:
        nodes:
          - "world_nether"
          - "world_the_end"
    effects:
      drop:
        mode: ALL
        filters:
          items:
            - "G:RESOURCES"
    children:
      high_up:
        name: "Nether roof / End islands"
        enabled: true
        conditions:
          y_level:
            range: "> 128"     # <-- Only checked for Nether and End deaths
        effects:
          drop:
            mode: ALL          # <-- Drop everything
```
