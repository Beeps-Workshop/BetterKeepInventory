# Installing

## Requirements
- Java 11 or higher
- A Minecraft server running Spigot, Paper or PurPur — 1.16 or below
- The `keepInventory` gamerule enabled in the worlds you want the plugin to affect
- [Vault](https://www.spigotmc.org/resources/vault.34315/) and an economy plugin — only if you want to use the `eco` section

:::info
1.3 is built against the Spigot 1.16.5 API. It is the version to use when your server is too old for 1.6.2 (1.17+) or the current release.
:::

## Installation Steps
1. Download BetterKeepInventory 1.3 from [Modrinth](https://modrinth.com/plugin/betterkeepinventory/version/1.3).
2. Stop your Minecraft server if it is currently running.
3. Locate your server's `plugins` directory. This is typically found in the root directory of your Minecraft server installation.
4. Copy the downloaded jar file into the `plugins` directory.
5. Start your Minecraft server. The plugin will create `plugins/BetterKeepInventory/config.yml`.
6. Enable the gamerule in every world you want the plugin to act in: `/gamerule keepInventory true`.
7. Open `plugins/BetterKeepInventory/config.yml` and set up your punishments. See [Configuration](./Configuration.md).
8. Run `/bki reload` in-game or in the console to apply your changes.

## Upgrading from 1.2 and below

The first start after upgrading migrates your old flat config to the 1.3 format automatically.

- Your previous file is renamed to `config.old.yml`.
- The settings you had are carried over, but all comments and any new options are lost.
- If the rename fails, the plugin logs `Moving old config file failed, Disabling plugin!` and disables itself. Fix the file permissions on the `plugins/BetterKeepInventory` directory and restart.

The permission nodes also changed in 1.3 — all `.breaking` nodes were removed and `betterkeepinventory.bypass.all` became `betterkeepinventory.bypass`. See [Permissions](./Permissions.md).

## Verification
Run `/betterkeepinventory` (or `/bki`) in-game or from the console. You should get the plugin header with its version number and the list of subcommands.

`/bki info` prints the settings the plugin is actually using, which is the quickest way to confirm your config was read.
