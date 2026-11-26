# Basic Configuration
This section provides an overview of the basic configuration options available for setting up your application. It covers essential settings that you need to configure to get started.

## The `config.yml` file
The primary configuration file for the plugin is `config.yml`. This file is located in `/plugins/BetterKeepInventory/config.yml` after the server has been started with the plugin installed.
Please take care when editing this file, as it may become corrupted if you use invalid YAML formatting.

### Version
```yaml
version: 2.1.0
```
:::warning
This field is managed by the plugin, Manually changing it may cause your config to be reset.
:::
Specifies the version of the configuration file. The plugin uses this to determine any migrations needed when updating.

### hash
```yaml
hash: 84d5071
```
:::warning
This field is managed by the plugin, Manually changing it may cause your config to be reset.
:::
A Hash identifier of the specific build of the plugin. Can be used for debugging purposes.

### notify_channel
```yaml
notify_channel: STABLE
```
What channel to use when checking for plugin updated. Must be one of the following values:
- `NONE`: Disables the update checker
- `SNAPSHOT`: Notify when any new snapshot build is available.
- `LATEST`: Notify when a new release build is available.
- `STABLE`: Notify when any new stable release build is available.

:::info
Stable releases are normal releases that have been marked as stable after being available for a month without critical issues.
:::

The Update Checker will check for a new version on server start, and once every 12 hours thereafter. If a new version is found, a message will be logged to the console and any player who logs in with the `betterkeepinventory.notify` permission will receive a message.

### debug
```yaml
debug: false
```
Enables or disables debug logging. When set to `true`, the plugin will log additional information useful while you are configuring the plugin.

### default_behavior
```yaml
default_behavior: INHERIT
```
What behavior to use by default for deaths. Overrides the server's default keep inventory gamerule. Must be one of the following values:
- `INHERIT`: Inherit the worlds default keep inventory gamerule.
- `KEEP`: Keep inventory on death.
- `DROP`: Drop inventory on death.

### rules
```yaml
rules:
  ...
```
The rules section is where you define the specific behavior the plugin should use. Each rule can target specific worlds, players, or placeholders, and define how inventory should be handled on death.
Please refer to the [Rules Configuration](./Rules/Intro.md) section for detailed information on how to set up and customize rules.