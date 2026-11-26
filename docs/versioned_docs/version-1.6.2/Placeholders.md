# Placeholders

1.6.2 registers a [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) expansion when PlaceholderAPI is installed. The console logs `PlaceholderAPI found, enabling extension` on startup when it does.

All placeholders are prefixed with `BetterKeepInventory_` and are resolved for the player they are shown to.

## Grace period

| Placeholder | Returns |
|---|---|
| `%BetterKeepInventory_grace_timer_active%` | `true` while the player is in their [grace period](./Configuration.md#grace), otherwise `false` |
| `%BetterKeepInventory_grace_timer_seconds%` | Whole seconds of grace remaining |

:::warning
`grace_timer_seconds` returns the literal text `null` when the player has no grace running, not `0`. Gate it behind `grace_timer_active` if you're putting it on a scoreboard or in an action bar.
:::

## Override state

`%BetterKeepInventory_is_active_<key>%` returns `true` when an override is in effect for that punishment at the player's current location — meaning the punishment is switched **off** there.

`<key>` is any of the [punishment keys](./Overrides.md#punishment-keys), in lowercase:

| Placeholder | `true` when |
|---|---|
| `%BetterKeepInventory_is_active_all%` | The plugin is fully overridden here |
| `%BetterKeepInventory_is_active_items%` | Durability damage is off here |
| `%BetterKeepInventory_is_active_armor%` | Armor damage is off here |
| `%BetterKeepInventory_is_active_hotbar%` | Hotbar and off-hand damage is off here |
| `%BetterKeepInventory_is_active_inventory%` | Main inventory damage is off here |
| `%BetterKeepInventory_is_active_exp%` | XP level loss is off here |
| `%BetterKeepInventory_is_active_exp_level%` | Level progress reset is off here |
| `%BetterKeepInventory_is_active_potions%` | Potion carry-over is off here |
| `%BetterKeepInventory_is_active_hunger%` | Hunger restoration is off here |
| `%BetterKeepInventory_is_active_eco%` | The money penalty is off here |

If `ALL` is overridden at the player's position, every one of these returns `true`.

:::info
These read the player's **current** position, so they update as the player walks around — handy for a scoreboard line that shows whether someone is standing somewhere safe. They do not account for gamemode, the `keepInventory` gamerule, or bypass permissions.
:::
