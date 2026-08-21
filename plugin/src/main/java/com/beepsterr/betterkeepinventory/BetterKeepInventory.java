package com.beepsterr.betterkeepinventory;

import com.beepsterr.betterkeepinventory.Commands.MainCommand;
import com.beepsterr.betterkeepinventory.Content.Conditions.*;
import com.beepsterr.betterkeepinventory.Content.Conditions.PlaceholderAPI.PlaceholderCondition;
import com.beepsterr.betterkeepinventory.Content.Conditions.Vault.VaultCondition;
import com.beepsterr.betterkeepinventory.Content.Effects.*;
import com.beepsterr.betterkeepinventory.Content.Effects.Vault.VaultEffect;
import com.beepsterr.betterkeepinventory.Depends.BetterKeepInventoryPlaceholderExpansion;
import com.beepsterr.betterkeepinventory.Events.OnPlayerDeath;
import com.beepsterr.betterkeepinventory.Events.OnPlayerJoin;
import com.beepsterr.betterkeepinventory.Events.OnPlayerRespawn;
import com.beepsterr.betterkeepinventory.Exceptions.UnloadableConfiguration;
import com.beepsterr.betterkeepinventory.Library.Config;
import com.beepsterr.betterkeepinventory.Library.Debugger;
import com.beepsterr.betterkeepinventory.Library.MetricContainer;
import com.beepsterr.betterkeepinventory.Library.NestedLogBuilder;
import com.beepsterr.betterkeepinventory.Library.Versions.Version;
import com.beepsterr.betterkeepinventory.Library.Versions.VersionChannel;
import com.beepsterr.betterkeepinventory.Library.Versions.VersionChecker;
import com.beepsterr.betterkeepinventory.Registries.PluginConditionRegistry;
import com.beepsterr.betterkeepinventory.Registries.PluginEffectRegistry;
import com.beepsterr.betterkeepinventory.api.BetterKeepInventoryAPI;
import com.beepsterr.betterkeepinventory.api.BetterKeepInventoryAPIImpl;
import com.tcoded.folialib.FoliaLib;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;

public class BetterKeepInventory extends JavaPlugin implements Listener {

    public Config config;
    static public BetterKeepInventory instance;
    private FoliaLib foliaLib;

    public Version version = new Version(getDescription().getVersion());
    public VersionChecker versionChecker;

    public Random rng = new Random();
    public MetricContainer metrics;
    public Debugger debugger = new Debugger();

    // Plugin Registries
    private final PluginConditionRegistry conditionRegistry = new PluginConditionRegistry(this);
    private final PluginEffectRegistry effectRegistry = new PluginEffectRegistry(this);


    @Override
    public void onEnable() {

        instance = this;
        foliaLib = new FoliaLib(BetterKeepInventory.getInstance());

        NestedLogBuilder nlb = new NestedLogBuilder();
        nlb.log("BetterKeepInventory is starting up...");
        nlb.cont("v" + version.toString());
        nlb.spacer();

        // Initialize API
        BetterKeepInventoryAPI api = new BetterKeepInventoryAPIImpl(conditionRegistry, effectRegistry);
        getServer().getServicesManager().register(BetterKeepInventoryAPI.class, api, this, ServicePriority.Highest);

        this.registerConditions(api, nlb);
        this.registerEffects(api, nlb);

        try {
            config = new Config(getConfig(), nlb);
        }catch (UnloadableConfiguration e){
            CrashAndDisable("Configuration failed to load!\n" + e.getMessage());
            return;
        }

        // Rebuild the rule tree whenever an addon adds or removes a condition or effect. Plugins
        // can be enabled and disabled at any time, so this cannot just happen once at startup.
        conditionRegistry.setChangeListener(this::onRegistrationsChanged);
        effectRegistry.setChangeListener(this::onRegistrationsChanged);

        // Our own conditions and effects are registered above, so the tree can be built now.
        // Addons enable after us and will invalidate it again through the listeners.
        config.buildRules(nlb);

        // event handlers
        getServer().getPluginManager().registerEvents(new OnPlayerDeath(this), this);
        getServer().getPluginManager().registerEvents(new OnPlayerRespawn(this), this);
        getServer().getPluginManager().registerEvents(new OnPlayerJoin(this), this);

        // Command registration
        Objects.requireNonNull(this.getCommand("betterkeepinventory")).setExecutor(new MainCommand());
        Objects.requireNonNull(this.getCommand("betterkeepinventory")).setTabCompleter(new MainCommand());

        // misc
        getServer().getPluginManager().registerEvents(this, this);
        metrics = new MetricContainer();

        // Enable PAPI Integration
        if(checkDependency("PlaceholderAPI")){
            nlb.log("Hello PlaceholderAPI! Registering expansion...");
            new BetterKeepInventoryPlaceholderExpansion().register();
        }

        if(config.getNotifyChannel() != VersionChannel.NONE){
            nlb.log("Setting up Version Checker...");
            versionChecker = new VersionChecker(config.getNotifyChannel());
        }

        nlb.end();

    }

    @Override
    public void onDisable() {

        getServer().getServicesManager().unregister(BetterKeepInventoryAPI.class, this);

        // Cancel version checks (not sure if needed in onDisable? but can't hurt. (hopefully))
        if(versionChecker != null){
            versionChecker.CancelCheck();
        }
    }

    private void onRegistrationsChanged() {
        if (config != null) {
            config.invalidateRules();
        }
    }

    private void registerEffects(BetterKeepInventoryAPI api, NestedLogBuilder nlb) {

        nlb.child("Registry: Effects");

        // register built-in effects
        nlb.log("damage");
        api.effectRegistry().register(this, "damage", DamageItemEffect::new);
        nlb.log("drop");
        api.effectRegistry().register(this, "drop", DropItemEffect::new);
        nlb.log("exp");
        api.effectRegistry().register(this, "exp", ExpEffect::new);
        nlb.log("hunger");
        api.effectRegistry().register(this, "hunger", HungerEffect::new);
        nlb.log("kick");
        api.effectRegistry().register(this, "kick", KickEffect::new);
        nlb.log("ban");
        api.effectRegistry().register(this, "ban", BanEffect::new);
        nlb.log("command");
        api.effectRegistry().register(this, "command", CommandEffect::new);
        nlb.log("lightning");
        api.effectRegistry().register(this, "lightning", LightningEffect::new);

        if(checkDependency("Vault")){
            nlb.log("vault");
            api.effectRegistry().register(this, "vault", VaultEffect::new);
        }

        nlb.parent();

    }

    private void registerConditions(BetterKeepInventoryAPI api, NestedLogBuilder nlb) {

        nlb.child("Registry: Conditions");

        // register built-in conditions
        nlb.log("worlds");
        api.conditionRegistry().register(this, "worlds", WorldsCondition::new);
        nlb.log("permissions");
        api.conditionRegistry().register(this, "permissions", PermissionsCondition::new);
        nlb.log("cause");
        api.conditionRegistry().register(this, "cause", CauseCondition::new);
        nlb.log("y_level");
        api.conditionRegistry().register(this, "y_level", YLevelCondition::new);
        nlb.log("light_level");
        api.conditionRegistry().register(this, "light_level", LightLevelCondition::new);

        if(checkDependency("Vault")){
            nlb.log("vault");
            api.conditionRegistry().register(this, "vault", VaultCondition::new);
        }
        if(checkDependency("PlaceholderAPI")){
            nlb.log("placeholders");
            api.conditionRegistry().register(this, "placeholders", PlaceholderCondition::new);
        }

        nlb.parent();

    }

    public static BetterKeepInventory getInstance(){
        return instance;
    }

    public boolean checkDependency(String dep){
        return Bukkit.getServer().getPluginManager().getPlugin(dep) != null;
    }

    public void CrashAndDisable(String message) {
        String alert = "\n" +
                ChatColor.DARK_RED + "=====================[ CRITICAL ERROR ]=====================\n"
                + ChatColor.RED +  "BetterKeepInventory encountered a irrecoverable error:\n\n"
                + ChatColor.YELLOW +  message + "\n\n"
                + ChatColor.RED + "The plugin has been disabled, and deaths will be handled by vanilla (!!)\n"
                + ChatColor.RED + "You should fix the issue, and restart the server to re-enable the plugin\n"
                + ChatColor.DARK_RED + "============================================================\n";

        getServer().getPluginManager().disablePlugin(this);
        getLogger().log(Level.SEVERE, alert);
        getServer().getConsoleSender().sendMessage(alert);
        getLogger().log(Level.INFO, "Continuing with server start in 10 seconds...");

        try{
            // Intentionally introduces a large delay during startup to hopefully catch the administrator's attention
            // Because this plugin is critical to death handling, we want to make sure the admin sees the error
            // This method should only be called if the plugin CANNOT continue working.
            Thread.sleep(10000);
        }catch (InterruptedException e){
           // do nothing, we're crashing anyway
        }

        // Disable the plugin
        getServer().getPluginManager().disablePlugin(this);

    }

    public void debug(Player player, String message){
        if(config.isDebug()){
            log("[DEBUG] (" + player.getName() + ") " + message);
        }
    }

    public void debug(String message){
        if(config.isDebug()){
            getLogger().log(Level.INFO, "[DEBUG] " + message);
        }
    }

    public void log(String message){
        getLogger().log(Level.INFO, message);
    }

    public static FoliaLib getScheduler(){
        return instance.foliaLib;
    }

}
