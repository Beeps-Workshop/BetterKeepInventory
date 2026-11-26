# Commands

All commands live under `/betterkeepinventory`, with `/betterki` and `/bki` as aliases.

| Command       | Permission                   | Description                                       |
|---------------|------------------------------|---------------------------------------------------|
| `/bki`        | —                            | Shows the plugin version and the subcommand list  |
| `/bki info`   | —                            | Prints the settings the plugin is currently using |
| `/bki test`   | `betterkeepinventory.test`   | Shows which punishments are overridden where you stand |
| `/bki reload` | `betterkeepinventory.reload` | Re-reads `config.yml`                             |

:::info
`/bki` and `/bki info` work without a permissions plugin — neither checks a permission node before running. The `betterkeepinventory.help` and `betterkeepinventory.info` nodes exist and default to everyone, but 1.6.2 does not actually test them.
:::

## /bki info

The most useful command when setting things up. It reports, per section, the mode in use, the `min`/`max` range, and whether enchantments and unbreaking are being taken into account — plus the full list of kept potion effects.

A section showing `VANILLA` in yellow means its `mode` is `NONE`, so the plugin is leaving it alone.

If `main.enabled` is `false`, the command says so and prints nothing else.

## /bki test

Players only — the console has no location to test. Stand where you want to check and run it.

You get either `✔ All modes are overridden`, or a line listing every [punishment key](./Overrides.md#punishment-keys) with a ✔ (overridden here) or ✖ (will apply here).

:::warning
`/bki test` checks **location-based overrides only** — worlds, damage types, Towny and GriefPrevention. It tells you so in its own output. Gamemode, the `keepInventory` gamerule and bypass permissions are not taken into account, so a ✖ does not guarantee the punishment will actually run for that player.

The damage-type overrides it reports are based on how you were last hurt, so they will read differently after taking damage.
:::

## /bki reload

Reloads `config.yml` from disk and replies `Reloaded!`. Follow it with `/bki info` if you want to confirm the new values were picked up.

Console can always run this. Players need `betterkeepinventory.reload`, which is granted to operators by default.
