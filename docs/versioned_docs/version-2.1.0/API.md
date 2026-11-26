# Developer API
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