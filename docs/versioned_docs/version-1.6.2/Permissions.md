# Permissions

## Bypass nodes

Bypass nodes let a player skip part — or all — of the plugin's punishments, so their death behaves like plain vanilla KeepInventory. None of them are granted by default.

| Node                                  | Effect                                              |
|---------------------------------------|-----------------------------------------------------|
| `betterkeepinventory.bypass`          | Skip **everything**. Checked first, before any other punishment runs |
| `betterkeepinventory.bypass.hotbar`   | Don't damage items in the hotbar or off-hand         |
| `betterkeepinventory.bypass.inventory`| Don't damage items in the main inventory             |
| `betterkeepinventory.bypass.armor`    | Don't damage worn armor                              |
| `betterkeepinventory.bypass.exp`      | Don't touch XP levels or level progress              |
| `betterkeepinventory.bypass.hunger`   | Respawn with a vanilla full hunger bar               |
| `betterkeepinventory.bypass.potions`  | Don't carry potion effects through death             |
| `betterkeepinventory.bypass.eco`      | Don't lose money                                     |

`betterkeepinventory.bypass` is also a group node that grants all of the children above.

:::warning
The group node lists a child called `betterkeepinventory.bypass.econ`, but the node the plugin actually checks is `betterkeepinventory.bypass.eco`. Grant `.eco` explicitly if you want a group to skip only the money penalty.
This has no effect on `betterkeepinventory.bypass` itself, which short-circuits every punishment before the economy check is reached.
:::

Bypass permissions are separate from [overrides](./Overrides.md) — permissions follow the player, overrides follow the location. Either one is enough to skip a punishment.

## Admin nodes

| Node                          | Default  | Effect                              |
|-------------------------------|----------|-------------------------------------|
| `betterkeepinventory.admin`   | op       | Group node granting help, info and reload |
| `betterkeepinventory.help`    | everyone | View the plugin help and version    |
| `betterkeepinventory.info`    | everyone | View the current settings           |
| `betterkeepinventory.reload`  | op       | Reload the config                   |
| `betterkeepinventory.test`    | nobody   | Run `/bki test` to check overrides at your location |

:::info
Only `betterkeepinventory.reload` and `betterkeepinventory.test` are enforced. `/bki` and `/bki info` run for anyone who can type the command — see [Commands](./Commands.md).

`betterkeepinventory.test` is **not** part of the `betterkeepinventory.admin` group, so grant it separately.
:::
