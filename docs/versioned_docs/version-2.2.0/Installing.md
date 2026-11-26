# Installing

## Requirements
- Java 17 or higher
- A Minecraft Server running either Spigot, Paper, PurPur or Folia
- [BetterKeepInventory](https://modrinth.com/plugin/betterkeepinventory) plugin jar file

## Installation Steps
1. Download the latest version of the BetterKeepInventory plugin from [Modrinth](https://modrinth.com/plugin/betterkeepinventory)
2. Stop your Minecraft server if it is currently running.
3. Locate your server's `plugins` directory. This is typically found in the root directory of your Minecraft server installation.
4. Copy the downloaded BetterKeepInventory plugin jar file into the `plugins` directory.
5. Start your Minecraft server. The plugin should automatically load and create its configuration files in the `plugins/BetterKeepInventory` directory.
6. (Optional) Open the `plugins/BetterKeepInventory/config.yml` file to customize the plugin's settings according to your preferences.
7. Save any changes made to the configuration file and run `/betterkeepinventory reload` command in-game or in the server console to apply the new settings.

## Verification
To verify that the plugin has been installed correctly, you can use the following command in the server console or in-game with the appropriate permissions: `/betterkeepinventory`