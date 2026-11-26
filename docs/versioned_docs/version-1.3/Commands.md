# Commands

All commands live under `/betterkeepinventory`, with `/betterki` and `/bki` as aliases.

| Command       | Permission                  | Description                                     |
|---------------|-----------------------------|-------------------------------------------------|
| `/bki`        | —                           | Shows the plugin version and the subcommand list |
| `/bki info`   | —                           | Prints the settings the plugin is currently using |
| `/bki reload` | `betterkeepinventory.reload` | Re-reads `config.yml` and prints the new settings |

:::info
`/bki` and `/bki info` work without a permissions plugin — neither checks a permission node before running. The `betterkeepinventory.help` and `betterkeepinventory.info` nodes exist and default to everyone, but 1.3 does not actually test them.
:::

## /bki info

The most useful command when setting things up. It reports, per section, the mode in use, the `min`/`max` range, and whether enchantments and unbreaking are being taken into account — plus the full list of kept potion effects.

A section showing `VANILLA` in yellow means its `mode` is `NONE`, so the plugin is leaving it alone.

If `main.enabled` is `false`, the command says so and prints nothing else.

## /bki reload

Reloads `config.yml` from disk and rebuilds the plugin's settings, then prints the same overview as `/bki info` so you can confirm the change landed.

Console can always run this. Players need `betterkeepinventory.reload`, which is granted to operators by default.
