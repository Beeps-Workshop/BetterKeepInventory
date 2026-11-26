# Configuration

The only configuration file is `plugins/BetterKeepInventory/config.yml`. Take care when editing it — invalid YAML will stop the plugin from reading your settings.

Run `/bki reload` after saving to apply changes without restarting.

## main

```yaml
main:
  enabled: true
  debug: false
  config_version: 2
```

### enabled
Turns every punishment off in one go. When `false` the plugin ignores deaths entirely.

### debug
Logs a detailed breakdown to the console for every death — which slots were handled, what damage was rolled, and why an item was skipped. Leave this off unless you are working out why a setting isn't doing what you expect.

### config_version
:::warning
This field is managed by the plugin. Changing it by hand can wipe your config.
:::
Used to decide whether your file needs migrating. On 1.3 the value is `2`.

## items

Three identical sections — `hotbar`, `inventory` and `armor` — each configured separately.

```yaml
items:
  hotbar:
    mode: "NONE"
    min: 0
    max: 0
    use_enchantments: true
    dont_break: true
    ignored_materials:
      - SHEARS
      - FISHING_ROD
      - CARROT_ON_A_STICK
      - FLINT_AND_STEEL
      - WARPED_FUNGUS_ON_A_STICK
```

Which section an item falls into is decided by the slot it was in when the player died:

| Section     | Slots                                     |
|-------------|-------------------------------------------|
| `hotbar`    | The nine hotbar slots **and the off-hand** |
| `armor`     | The four armor slots                       |
| `inventory` | Everything else                            |

### mode
- `NONE` — leave this section alone
- `SIMPLE` — deal a random number of **durability points** between `min` and `max`
- `PERCENTAGE` — deal a random **percentage** of the item's total durability between `min` and `max`

### min / max
The bounds of the roll. In `SIMPLE` mode these are durability points, in `PERCENTAGE` mode they are percentages.

### use_enchantments
:::danger[This option is inverted in 1.3]
Because of a bug, Unbreaking is only taken into account when `use_enchantments` is set to **`false`**. Setting it to `true` makes enchanted gear take the full damage roll.
This was corrected in later versions. See [Known issues](./Troubleshooting.md#known-issues).
:::
When it does apply, the damage roll is divided by the Unbreaking level plus one.

### dont_break
- `true` — items are never destroyed. Anything that would break is left at zero durability instead (one hit away from breaking).
- `false` — items that run out of durability are destroyed.

:::warning
With `dont_break: false`, 1.3 clears the **entire slot**, so a stack loses every item in it, not just one. Later versions remove a single item instead.
:::

### ignored_materials
Materials in this list are skipped entirely. Use the names from the [Bukkit Material list](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html).

```yaml
    ignored_materials:
      - GOLDEN_HELMET
      - GOLDEN_CHESTPLATE
```

:::info
1.3 has no `ignored_name` or `ignored_lore` option — you cannot exempt items by their custom name or lore. Those were added in 1.6.2.
:::

## exp

```yaml
exp:
  levels:
    mode: "NONE"
    min: 0
    max: 0
  reset_level: false
```

### levels.mode
- `NONE` — keep all levels
- `ALL` — drop to level 0
- `SIMPLE` — lose a random number of **levels** between `min` and `max`
- `PERCENTAGE` — lose a random **percentage** of the player's current levels between `min` and `max`

### reset_level
When `true`, the progress bar towards the next level is emptied as well. This is separate from losing whole levels — you can use either on its own.

## hunger

```yaml
hunger:
  mode: "KEEP"
  min: 8
  max: 20
  extra_removed_on_death: 1
```

### mode
- `NONE` — vanilla behaviour, players respawn with a full hunger bar
- `KEEP` — the hunger level from the moment of death is restored a second after respawning, clamped between `min` and `max`

### min / max
Half-drumsticks, from 0 to 20. `6` is the point where players can no longer sprint, `20` is a full bar.

### extra_removed_on_death
:::warning
This option does nothing in 1.3 — the plugin never reads it. It stays in the file because it ships in the default config.
:::

## potions

```yaml
potions:
  kept_effects:
    - ABSORPTION
    - BAD_OMEN
    # ...
  max_duration: 30
  reduce_potency: 1
```

Effects are captured on death and re-applied a second after the player respawns.

### kept_effects
Only the effects listed here survive death — everything else is lost. Empty the list to disable potion keeping entirely. Use the names from the [Bukkit PotionEffectType list](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/potion/PotionEffectType.html).

### max_duration
The longest an effect may last after respawning, in seconds. An effect with two minutes left comes back with 30 seconds on the default setting. Set it to `false` to keep the original durations.

### reduce_potency
Levels to knock off each effect. With `1`, Speed II comes back as Speed I; with `2`, Speed IV comes back as Speed II. Effects never drop below level I.

### duration_penalty
Seconds subtracted from each effect before `max_duration` is applied. This option is **not** in the default config file but the plugin does read it — add it under `potions:` yourself if you want it. Without it, no flat penalty is applied.

```yaml
potions:
  duration_penalty: 10
```

## eco

```yaml
eco:
  amount: 0
```

Requires [Vault](https://www.spigotmc.org/resources/vault.34315/) and an economy plugin. If Vault is missing, a warning is logged and nothing is taken.

### amount
A flat sum taken from the player on death. `0` disables it. The player is told how much they lost.

:::info
1.3 only supports a fixed amount. Percentages, a minimum balance and paying the killer were added in 1.6.2.
:::
