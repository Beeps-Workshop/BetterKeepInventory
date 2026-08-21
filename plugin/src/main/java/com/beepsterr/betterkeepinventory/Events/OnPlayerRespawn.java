package com.beepsterr.betterkeepinventory.Events;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.Library.ConfigRule;
import com.beepsterr.betterkeepinventory.Library.NestedLogBuilder;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.logging.Level;

public class OnPlayerRespawn implements Listener {

    BetterKeepInventory plugin;


    public OnPlayerRespawn(BetterKeepInventory main){
        plugin = main;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {

        Player ply = event.getPlayer();
        // Match the death handler: full tree only when debug is on (still captured at FINE for uploads).
        NestedLogBuilder nlb = new NestedLogBuilder(plugin.config.isDebug() ? Level.INFO : Level.FINE);

        nlb.log("Player" + ply.getName() + " (" + ply.getUniqueId() + ") died.");
        nlb.spacer();
        nlb.cont("Phase 2/2 (Respawn)");
        nlb.cont("World: " + ply.getWorld().getName());
        nlb.cont("Behavior: " + plugin.config.getDefaultBehavior().toString());
        nlb.spacer();

        // Time to process the top level rules
        for(ConfigRule rule : plugin.config.getRules()){
            rule.trigger(event.getPlayer(), null, event, nlb);
        }
    }

}
