package com.beepsterr.plugin_api_consumer;

import com.beepsterr.betterkeepinventory.api.BetterKeepInventoryAPI;
import com.beepsterr.betterkeepinventory.api.Condition;
import com.beepsterr.betterkeepinventory.api.DeathContext;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public class BKITestPlugin extends JavaPlugin {

    private BetterKeepInventoryAPI api;

    @Override
    public void onEnable() {
        api = Bukkit.getServicesManager().load(BetterKeepInventoryAPI.class);
        // don't forget null checks!
        if(api == null){
            getLogger().severe("❌ BetterKeepInventoryAPI service not found!");
            return;
        }

        // API_VERSION is a compile-time constant, so this compares the version you built against
        // with the one actually installed. They only differ if the plugin was updated under you.
        if(api.apiVersion() != BetterKeepInventoryAPI.API_VERSION){
            getLogger().severe("❌ Built for BKI API v" + BetterKeepInventoryAPI.API_VERSION
                    + " but the server is running v" + api.apiVersion() + ".");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        api.conditionRegistry().register(this, "always_true", AlwaysTrueCondition::new);
        getLogger().info("✅ Custom test condition registered!");
    }

    @Override
    public void onDisable() {
        // Without this, a disabled addon leaves entries behind pointing at classes from a
        // classloader that may no longer be live.
        if(api != null){
            api.conditionRegistry().unregisterAll(this);
        }
    }

    public static class AlwaysTrueCondition implements Condition {

        public AlwaysTrueCondition(ConfigurationSection section) {
            // no config needed in this example
            // but here you can use standard bukkit config API to read your conditions values
        }

        @Override
        public boolean check(DeathContext ctx) {
            ctx.logger().log("Hello from AlwaysTrueCondition!");
            return false;
        }
    }
}
