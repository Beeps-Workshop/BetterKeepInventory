---
description: "Every BetterKeepInventory command and permission node, including the permission groups for players and admins."
---
# Commands & Permissions

## Commands
All commands live under `/betterkeepinventory`, which can be shortened to `/betterki` or `/bki`.

| Command                         | Permission                                  | Description                                                                 |
|---------------------------------|---------------------------------------------|-----------------------------------------------------------------------------|
| `/bki`                          | `betterkeepinventory.command.help`          | Shows the plugin version, update status, useful links and available commands |
| `/bki reload`                   | `betterkeepinventory.command.reload`        | Reloads `config.yml` and `messages.yml`                                     |
| `/bki registry effects`         | `betterkeepinventory.command.registry`      | Lists every effect available on your server, including ones added by other plugins |
| `/bki registry conditions`      | `betterkeepinventory.command.registry`      | Lists every condition available on your server, including ones added by other plugins |
| `/bki debug verbose`            | `betterkeepinventory.command.debug.verbose` | Toggles live rule debugging in your chat (players only)                     |
| `/bki debug dump`               | `betterkeepinventory.command.debug.dump`    | Writes a debug report to `plugins/BetterKeepInventory/dumps/`               |

:::tip
If `/bki reload` finds an error in your config, the new config is **not** loaded and the plugin keeps running with the last working one. Fix the error and reload again.
:::

See [Troubleshooting](./Troubleshooting.md) for how to use the debug commands.

## Permissions

| Permission                                  | Default  | Description                                                     |
|---------------------------------------------|----------|-----------------------------------------------------------------|
| `betterkeepinventory.command.help`          | op       | Use `/bki`                                                      |
| `betterkeepinventory.command.reload`        | op       | Use `/bki reload`                                               |
| `betterkeepinventory.command.registry`      | op       | Use `/bki registry`                                             |
| `betterkeepinventory.command.debug.verbose` | op       | Use `/bki debug verbose`                                        |
| `betterkeepinventory.command.debug.dump`    | op       | Use `/bki debug dump`                                           |
| `betterkeepinventory.version`               | op       | See the plugin version and update status in `/bki`              |
| `betterkeepinventory.version.notify`        | op       | Get a message on join when an update is available               |

### Permission groups
These grant several permissions at once.

| Permission                          | Default  | Includes                                                                                                                           |
|-------------------------------------|----------|------------------------------------------------------------------------------------------------------------------------------------|
| `betterkeepinventory.player`        | everyone | `command.help`, `version`                                                                                                          |
| `betterkeepinventory.admin`         | op       | `command.help`, `command.reload`, `command.registry`, `command.debug`, `version`, `version.notify`                                  |
| `betterkeepinventory.command.debug` | op       | `command.debug.verbose`, `command.debug.dump`                                                                                      |

:::note
Because `betterkeepinventory.player` is given to everyone by default, all players can run `/bki` and see the plugin version. Negate `betterkeepinventory.player` in your permissions plugin if you'd rather hide it.
:::

Conditions can also check permissions, see the [Permissions condition](./Rules/Conditions/Permissions.md).
