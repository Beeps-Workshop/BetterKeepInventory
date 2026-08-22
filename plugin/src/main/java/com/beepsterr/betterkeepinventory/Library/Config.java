package com.beepsterr.betterkeepinventory.Library;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.Exceptions.UnloadableConfiguration;
import com.beepsterr.betterkeepinventory.Library.Versions.Version;
import com.beepsterr.betterkeepinventory.Library.Versions.VersionChannel;
import com.beepsterr.betterkeepinventory.api.BkiLogger;
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
    private final String debugBinUploadUrl;
    private final String debugBinViewUrl;
    private final DefaultBehavior defaultBehavior;
    private Ruleset ruleset;

    public Config(FileConfiguration config, BkiLogger log) throws UnloadableConfiguration {

        BetterKeepInventory plugin = BetterKeepInventory.getInstance();

        if(log == null){
            log = StructuredLogger.plugin();
        }

        instance = this;
        this.rawConfig = config;

        log.debug("loading configuration", Map.of("folder", plugin.getDataFolder().toString()));

        if(Objects.equals(this.rawConfig.getString("version", "default"), "default")) {
            log.info("config.yml has the default version string; writing a fresh one.");
            plugin.saveDefaultConfig();
            plugin.reloadConfig();
            this.rawConfig = plugin.getConfig();
        }

        if(!new File(plugin.getDataFolder(), "messages.yml").exists()){
            log.info("messages.yml did not exist; creating it.");
            plugin.saveResource("messages.yml", false);
        }
        this.rawMessages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));

        // Everything below reads this.rawConfig rather than the `config` parameter. They are the
        // same object right up until the "default" branch above swaps rawConfig for a freshly
        // reloaded one -- after which reads from `config` see a stale file, and anything
        // MigrateConfiguration writes is invisible to the parse that follows.
        version = this.rawConfig.getString("version", "2.1.0");

        // Before anything is read out of the file. A migration that renames or removes a value
        // has to run first, or the parse below fails on the old spelling -- and
        // VersionChannel.valueOf throws rather than returning a default, which would take the
        // whole plugin down for anyone who had picked a channel that no longer exists.
        MigrateConfiguration();

        notifyChannel = VersionChannel.valueOf(this.rawConfig.getString("notify_channel", "STABLE").toUpperCase());
        hash = this.rawConfig.getString("hash", "OLD");
        debug = this.rawConfig.getBoolean("debug", false);

        // Defaulted rather than required: these were added in 3.0 and every configuration written
        // before it is missing them, so an absent section has to mean "the bin we ship with".
        debugBinUploadUrl = this.rawConfig.getString("debug_bin.upload_url", DebugBin.DEFAULT_UPLOAD_URL);
        debugBinViewUrl = this.rawConfig.getString("debug_bin.view_url", DebugBin.DEFAULT_VIEW_URL);

        try{
            LoadMessages(plugin);
        }catch(IOException e){
            throw new UnloadableConfiguration(e.getMessage());
        }

        defaultBehavior = DefaultBehavior.valueOf(this.rawConfig.getString("default_behavior", "INHERIT").toUpperCase());

        // Constructed, deliberately not built. Addons register their conditions and effects in
        // their own onEnable, which runs after ours -- building now would parse the rules before
        // they exist and quietly drop every rule that uses one.
        this.ruleset = new Ruleset(this.rawConfig.getConfigurationSection("rules"));

        log.debug("configuration loaded", Map.of(
                "version", version,
                "debug", debug,
                "default_behavior", defaultBehavior.toString(),
                "notify_channel", notifyChannel.toString()));

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

    /** Where {@code /bki debug upload} sends a report. */
    public String getDebugBinUploadUrl() {
        return debugBinUploadUrl;
    }

    /** Where the link it hands back points, with {@code {key}} replaced by the bin's id. */
    public String getDebugBinViewUrl() {
        return debugBinViewUrl;
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
     * Build the rules now, and narrate it into {@code log}.
     * <p>
     * Used where the result is wanted immediately and in context: plugin startup, and
     * {@code /bki reload}. Everywhere else {@link #invalidateRules()} is enough.
     */
    public void buildRules(BkiLogger log) {
        ruleset.build(log);
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

            // 3.0: snapshots moved to private distribution, so the SNAPSHOT channel is gone.
            // BETA is the closest surviving intent -- someone on SNAPSHOT was asking for
            // pre-release builds. Without this the plugin refuses to start for them.
            if("SNAPSHOT".equalsIgnoreCase(rawConfig.getString("notify_channel", ""))){
                rawConfig.set("notify_channel", "BETA");
                plugin.getLogger().info(
                        "The SNAPSHOT update channel no longer exists; snapshots are now distributed privately. "
                        + "Switching notify_channel to BETA.");
            }
        }

//         Migration completed, write new versions
        rawConfig.set("version", installedVersion);
        rawConfig.set("hash", Version.getCommitHash());
        BetterKeepInventory.getInstance().saveConfig();
    }
}
