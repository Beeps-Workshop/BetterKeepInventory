package com.beepsterr.betterkeepinventory.Library;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.Exceptions.UnloadableConfiguration;
import com.beepsterr.betterkeepinventory.Library.Versions.Version;
import com.beepsterr.betterkeepinventory.Library.Versions.VersionChannel;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Config {

    public enum DefaultBehavior {
        INHERIT, DROP, KEEP
    }

    private static Config instance;

    private FileConfiguration rawConfig;
    private FileConfiguration rawMessages;
    private final String version;
    private final VersionChannel notifyChannel;
    private final String hash;
    private final boolean debug;
    private final DefaultBehavior defaultBehavior;
    private Ruleset ruleset;

    public Config(FileConfiguration config, NestedLogBuilder nlb) throws UnloadableConfiguration {

        BetterKeepInventory plugin = BetterKeepInventory.getInstance();

        if(nlb == null){
            nlb = new NestedLogBuilder();
        }
        nlb.child("Loading Plugin Configuration");

        instance = this;
        this.rawConfig = config;

        nlb.log("Loading config.yml from " + plugin.getDataFolder());


        if(Objects.equals(this.rawConfig.getString("version", "default"), "default")) {
            nlb.log("Configuration has default version string");
            nlb.cont("Creating a new configuration file for you.");
            plugin.saveDefaultConfig();
            plugin.reloadConfig();
            this.rawConfig = plugin.getConfig();
        }

        nlb.log("Loading messages.yml from " + plugin.getDataFolder());
        if(!new File(plugin.getDataFolder(), "messages.yml").exists()){
            nlb.cont("messages.yml did not exist, creating it now.");
            plugin.saveResource("messages.yml", false);
        }
        this.rawMessages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));

        version = config.getString("version", "2.1.0");
        notifyChannel = VersionChannel.valueOf(config.getString("notify_channel", "STABLE").toUpperCase());
        hash = config.getString("hash", "OLD");
        debug = config.getBoolean("debug", false);

        try{
            LoadMessages(plugin);
        }catch(IOException e){
            throw new UnloadableConfiguration(e.getMessage());
        }

        MigrateConfiguration();

        defaultBehavior = DefaultBehavior.valueOf(config.getString("default_behavior", "INHERIT").toUpperCase());

        // Constructed, deliberately not built. Addons register their conditions and effects in
        // their own onEnable, which runs after ours -- building now would parse the rules before
        // they exist and quietly drop every rule that uses one.
        this.ruleset = new Ruleset(this.rawConfig.getConfigurationSection("rules"));

        nlb.parent();

    }

    public static Config getInstance() {
        return instance;
    }

    public String getVersion() {
        return version;
    }

    public boolean isDebug() {
        return debug;
    }

    public DefaultBehavior getDefaultBehavior() {
        return defaultBehavior;
    }

    public Ruleset getRuleset() {
        return ruleset;
    }

    /**
     * The rules to evaluate for a death.
     */
    public List<ConfigRule> getRules() {
        return ruleset.rules();
    }

    /**
     * Build the rules now, and narrate it into {@code nlb}.
     * <p>
     * Used where the result is wanted immediately and in context: plugin startup, and
     * {@code /bki reload}. Everywhere else {@link #invalidateRules()} is enough.
     */
    public void buildRules(NestedLogBuilder nlb) {
        ruleset.build(nlb);
    }

    /**
     * Mark the rules as needing a rebuild, and schedule one for the next tick.
     * <p>
     * Called whenever a condition or effect is registered or unregistered, which mostly happens
     * in bursts: the plugin's own registrations at startup, then one burst per addon as it
     * enables. Deferring to the next tick collapses each burst into a single parse instead of
     * reparsing the whole tree once per registration.
     * <p>
     * The scheduled rebuild is an optimisation, not a guarantee -- {@link Ruleset#rules()}
     * rebuilds on demand if a death gets there first.
     */
    public void invalidateRules() {

        ruleset.invalidate();

        BetterKeepInventory plugin = BetterKeepInventory.getInstance();
        if (plugin == null || BetterKeepInventory.getScheduler() == null) {
            return;
        }

        BetterKeepInventory.getScheduler().getScheduler().runNextTick(task -> {
            if (ruleset.isStale()) {
                ruleset.build(null);
            }
        });
    }

    public VersionChannel getNotifyChannel() {
        return notifyChannel;
    }

    public int countRules() {
        return countRules(this.rawConfig.getConfigurationSection("rules"));
    }

    static int countRules(ConfigurationSection section) {
        if (section == null) {
            return 0;
        }
        int count = 0;
        for (String key : section.getKeys(false)) {
            ConfigurationSection rule = section.getConfigurationSection(key);
            if (rule == null) continue;
            count++;
            count += countRules(rule.getConfigurationSection("children"));
        }
        return count;
    }

    public String getMessage(String key, Map<String, String> replacements) {
        if(rawMessages == null){
            BetterKeepInventory.getInstance().getLogger().warning("Messages not loaded?? Check if messages.yml exists.");
            return key;
        }

        if(!rawMessages.contains(key)){
            BetterKeepInventory.getInstance().getLogger().warning("Messages not loaded?? Check if messages.yml exists.");
            return key;
        }

        String message = rawMessages.getString(key, key);
        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void sendMessage(Player ply, String key, Map<String, String> replacements) {
        String message = getMessage(key, replacements);
        if(!message.isEmpty()){
            ply.sendMessage(message);
        }
        // An empty message means the user intentionally disabled it, so send nothing.
    }

    public void LoadMessages(BetterKeepInventory plugin) throws IOException {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(file);

        // Load default from jar
        InputStream defConfigStream = plugin.getResource("messages.yml");
        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));

        // Copy missing keys
        for (String key : defaultConfig.getKeys(true)) {
            if (!userConfig.contains(key)) {
                userConfig.set(key, defaultConfig.get(key));
            }
        }

        // Save updated file
        userConfig.save(file);
        this.rawMessages = userConfig;
    }

    public void MigrateConfiguration() throws UnloadableConfiguration {

        BetterKeepInventory plugin = BetterKeepInventory.getInstance();

        // Detect pre 2.0 configuration files
        int legacyConfigVersion = rawConfig.getInt("main.config_version", 0);
        if(legacyConfigVersion > 0){
            throw new UnloadableConfiguration(
                    "Detected a legacy configuration file, refusing to load it.\n" +
                    "Please read the migration instructions at:\n" +
                    "https://beeps.notion.site/Migrating-to-2-0-244f258220598076bbbacc7af661f068"
            );
        }

        String installedVersion = plugin.version.major + "." + plugin.version.minor + "." + plugin.version.patch;
        if(!installedVersion.equals(this.version)){
            // ... Create migrations here when needed
        }

//         Migration completed, write new versions
        rawConfig.set("version", installedVersion);
        rawConfig.set("hash", Version.getCommitHash());
        BetterKeepInventory.getInstance().saveConfig();
    }
}
