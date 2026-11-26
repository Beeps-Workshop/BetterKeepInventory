# Configuration

The only configuration file is `plugins/BetterKeepInventory/config.yml`. Take care when editing it — invalid YAML will stop the plugin from reading your settings.

Run `/bki reload` after saving to apply changes without restarting.

Switching punishments off in specific places is covered separately in [Overrides](./Overrides.md).

## main

```yaml
main:
  enabled: true
  debug: false
  config_version: 4
  grace: 0
```

### enabled
Turns every punishment off in one go. When `false` the plugin ignores deaths entirely.

### debug
Logs a detailed breakdown to the console for every death — which slots were handled, what damage was rolled, why an item was skipped, and every override that was evaluated. Leave this off unless you are working out why a setting isn't doing what you expect. It is very noisy.

### config_version
:::warning
This field is managed by the plugin. Changing it by hand can wipe your config.
:::
Used to decide whether your file needs migrating.

### grace
A window, in seconds, after a death during which the player's **next** death is ignored completely. Useful when players respawn into the same hazard that killed them.

:::warning[Use `false` to disable grace, not `0`]
Setting `grace: 0` does not switch the feature off — it leaves a window of up to one second. To disable it properly:

```yaml
main:
  grace: false
```
:::

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
    ignored_name: "NONE"
    ignored_lore: "NONE"
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

Items whose material has no durability — blocks, food, ingredients — are always skipped.

### mode
- `NONE` — leave this section alone
- `SIMPLE` — deal a random number of **durability points** between `min` and `max`
- `PERCENTAGE` — deal a random **percentage** of the item's total durability between `min` and `max`

### min / max
The bounds of the roll. In `SIMPLE` mode these are durability points, in `PERCENTAGE` mode they are percentages.

### use_enchantments
When `true`:
- **Unbreaking** divides the damage roll by the enchantment level plus one, so higher levels take proportionally less damage.
- **Curse of Vanishing** removes one item from the stack, on top of the durability handling.

### dont_break
- `true` — items are never destroyed. Anything that would break is left at zero durability instead (one hit away from breaking).
- `false` — items that run out of durability lose one from the stack, with the usual breaking sound.

### ignored_name
Items whose custom name contains this text are skipped. The match is case-insensitive and matches anywhere in the name. Set to `"NONE"` to disable the check.

```yaml
    ignored_name: "[Protected]"
```

### ignored_lore
Items with a lore line containing this text are skipped. The match is case-insensitive and matches anywhere in any line. Set to `"NONE"` to disable the check.

### ignored_materials
Materials in this list are skipped entirely. Use the names from the [Bukkit Material list](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html).

```yaml
    ignored_materials:
      - GOLDEN_HELMET
      - GOLDEN_CHESTPLATE
```

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
When `true`, the progress bar towards the next level is emptied as well. This is separate from losing whole levels — you can use either on its own, and it has its own override key (`EXP_LEVEL`).

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
This option does nothing in 1.6.2 — the plugin never reads it. It stays in the file because it ships in the default config.
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
  enabled: false
  mode: "SIMPLE"
  amount: 0
  max_amount: -1
  min_balance: 0
  pay_to_killer: false
```

Requires [Vault](https://www.spigotmc.org/resources/vault.34315/) and an economy plugin. If Vault is missing, a warning is logged and nothing is taken.

### enabled
Master switch for the economy penalty. Nothing here applies while this is `false`.

### mode
- `SIMPLE` — take a fixed sum
- `PERCENTAGE` — take a percentage of the player's current balance

### amount
The sum to take, or the percentage when `mode` is `PERCENTAGE`. The result is rounded to two decimals, and the player is told how much they lost.

### min_balance
Players below this balance are not charged at all.

### max_amount
:::warning
This option does nothing in 1.6.2 — the plugin never reads it. Percentage losses are not capped.
:::

### pay_to_killer
When `true` and the death was caused by another player, the killer receives what the victim lost and is told so. Only applies in PvP.
