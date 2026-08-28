# BetterKeepInventory 3.0
[![Dynamic JSON Badge](https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fapi.spiget.org%2Fv2%2Fresources%2F93081&query=downloads&logo=spigotmc&label=Downloads&color=%23ED8106)](https://www.spigotmc.org/resources/betterkeepinventory.93081/)
[![Discord](https://img.shields.io/discord/850836877924761610?label=BeepSterr's%20Workshop&color=%239191c9)](https://beepsterr.com/discord)

Take control of KeepInventory.
No longer shall your players jump into lava to get home. Weep they shall, for the mercy of their benevolent admins.

*(This plugin lets take fine grained control over KeepInventory, choosing to drop certain items, damage durability and many more effects)*

## Downloads
The plugin can be downloaded via [Modrinth](https://modrinth.com/plugin/betterkeepinventory) or [Curseforge](https://www.curseforge.com/minecraft/bukkit-plugins/betterkeepinventory) 

## Supported Versions
BetterKeepInventory supports the latest 3 major Minecraft versions, and is compiled using the native Java version of the oldest supported Minecraft version.

| Minecraft Version | Plugin Version                                                         | Minimum Java Version |
|-------------------|------------------------------------------------------------------------|----------------------|
| 1.16 and below    | [1.3](https://modrinth.com/plugin/betterkeepinventory/version/1.3)     | 11                   |
| 1.17, 1.18        | [1.6.2](https://modrinth.com/plugin/betterkeepinventory/version/1.6.2) | 16                   |
| 1.19-1.20         | [2.2.0](https://modrinth.com/plugin/betterkeepinventory/version/2.2.0) | 17                   |
| 1.21+             | [Latest](https://modrinth.com/plugin/betterkeepinventory/versions)     | 21                   |

> [!NOTE]  
> Older versions can contain bugs that are not present in the latest version. I Recommend always running the latest minecraft version so you can benefit from the most recent plugins. You can always use a plugin like [ViaBackwards](https://www.spigotmc.org/resources/viabackwards.27448/) to allow players on older versions to connect to your server.

### Stable Versions
The latest and stable version of BetterKeepInventory is not always the same version. The latest release will be posted to SpigotMC but the stable release is defined in [This File](versions/stable.txt).
Set your `notify_channel` to `stable` to receive notifications whenever the stable version is newer than the version you have installed.

### Snapshots
Snapshots are in-development builds of the plugin, they are compiled daily and distributed in the [Supporter Discord](https://ko-fi.com/sterr).

## Having Trouble?
Please join the [Discord](https://discord.gg/fFvFXPvtty) for support

## Developer API
BetterKeepInventory exposes a API to allow other plugins to extend its features.

### Installation
The easiest way to get a hold of the API is to use JitPack. Just add your dependencies like this:
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<depencencies>
    <dependency>
        <!-- Keep not that we're using the 'betterkeepinventory-api' artifact -->
        <groupId>com.github.BeepSterr.BetterKeepInventory</groupId>
        <artifactId>betterkeepinventory-api</artifactId>
        <version>v2.0.0</version>
        <!-- You should not shade the API, but use it as a provided dependency -->
        <!-- If you don't do this, you will not be able to register anything -->
        <scope>provided</scope>
    </dependency>
</depencencies>
```

### Adding a Condition
A condition is a checked when it is applied to a rule, it allows end users to create their own rules
for when certain effects should be ran. some examples could be: if player is in a party, or the player has a hardcore difficulty
#### Definition
```java
// An example condition definition
public static class AlwaysTrueCondition implements Condition {
    public AlwaysTrueCondition(ConfigurationSection section) {
        // no config needed in this example
        // but here you can use standard bukkit config API to read your conditions values
    }

    @Override
    public boolean check(Player player, PlayerDeathEvent deathEvent, PlayerRespawnEvent respawnEvent) {
        return true;
    }
}
```

#### Register the condition
```java
BetterKeepInventoryAPI api = Bukkit.getServicesManager().load(BetterKeepInventoryAPI.class);
// don't forget null checks!
api.conditionRegistry().register(this, "always_true", AlwaysTrueCondition::new);
```
And your condition is now available under `always_true` and `plugin_name.always_true`

### Adding a Effect
a effect is a "thing" that happens whenever a rule is triggered.
Some examples could be: loss levels, temporary restrictions or anything your plugin can offer
#### Definition
```java
// An example condition definition
public static class BroadcastEffect implements Effect {
    public BroadcastEffect(ConfigurationSection section) {
        // no config needed in this example
        // but here you can use standard bukkit config API to read your conditions values
    }

    @Override
    public void onRespawn(Player player, PlayerRespawnEvent event) {
        // This effect doesn't do anything on respawn (yet)
    }

    @Override
    public void onDeath(Player ply, PlayerDeathEvent event) {
        plugin.getServer().broadcastMessage(ply.getName() + " died a gruesome death.");
    }
}
```
#### Register the effect
```java
BetterKeepInventoryAPI api = Bukkit.getServicesManager().load(BetterKeepInventoryAPI.class);
// don't forget null checks!
api.effectRegistry().register(this, "broadcast", BroadcastEffect::new);
```
