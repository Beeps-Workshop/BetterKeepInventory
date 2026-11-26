# Installing

## Requirements
- Java 16 or higher
- A Minecraft server running Spigot, Paper or PurPur — 1.17 or newer
- The `keepInventory` gamerule enabled in the worlds you want the plugin to affect

### Optional integrations
None of these are required. The plugin detects them at startup and only uses what is present.

| Plugin | Used for |
|---|---|
| [Vault](https://www.spigotmc.org/resources/vault.34315/) + an economy plugin | The `eco` section |
| [Towny](https://www.spigotmc.org/resources/towny-advanced.72694/) | Town and nation [overrides](./Overrides.md#towny) |
| [GriefPrevention](https://www.spigotmc.org/resources/griefprevention.1884/) | Claim [overrides](./Overrides.md#griefprevention) |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | The [placeholders](./Placeholders.md) |

:::info
1.6.2 is built against the Spigot 1.17 API. Use it on 1.17 and 1.18, where the current 2.x releases can't run.
:::

## Installation Steps
1. Download BetterKeepInventory 1.6.2 from [Modrinth](https://modrinth.com/plugin/betterkeepinventory/version/1.6.2).
2. Stop your Minecraft server if it is currently running.
3. Locate your server's `plugins` directory. This is typically found in the root directory of your Minecraft server installation.
4. Copy the downloaded jar file into the `plugins` directory.
5. Start your Minecraft server. The plugin will create `plugins/BetterKeepInventory/config.yml`.
6. Enable the gamerule in every world you want the plugin to act in: `/gamerule keepInventory true`.
7. Open `plugins/BetterKeepInventory/config.yml` and set up your punishments. See [Configuration](./Configuration.md).
8. Run `/bki reload` in-game or in the console to apply your changes.

## About `config.old.yml`

The plugin migrates older config formats on startup. Before it rewrites anything it renames your existing file to `config.old.yml`, so there is always a copy of what you had.

:::warning
The config file that ships inside the 1.6.2 jar is still marked as format 4, so the **second** start after a fresh install runs the format 5 migration. Your settings are carried over and backed up, but the rewritten file loses all of its explanatory comments. This is expected — the settings themselves are intact.
:::

If the rename fails, the plugin logs `Moving old config file failed, Disabling plugin!` and disables itself. Fix the permissions on the `plugins/BetterKeepInventory` directory and restart.

## Verification
Run `/betterkeepinventory` (or `/bki`) in-game or from the console. You should get the plugin header with its version number and the list of subcommands.

`/bki info` prints the settings the plugin is actually using, and `/bki test` tells you which punishments are overridden where you're standing.
