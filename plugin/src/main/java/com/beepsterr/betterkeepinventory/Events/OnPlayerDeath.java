package com.beepsterr.betterkeepinventory.Events;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.Library.ConfigRule;
import com.beepsterr.betterkeepinventory.Library.DeathApplication;
import com.beepsterr.betterkeepinventory.Library.DeathContextImpl;
import com.beepsterr.betterkeepinventory.Library.NestedLogBuilder;
import org.bukkit.GameRule;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.logging.Level;

public class OnPlayerDeath  implements Listener {

    BetterKeepInventory plugin;


    public OnPlayerDeath(BetterKeepInventory main){
        plugin = main;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player ply = event.getEntity();
        // Only surface the full rule-evaluation tree in the console when debug is on.
        // At FINE it is still captured for `/bki debug upload`, just hidden from the default handler.
        NestedLogBuilder nlb = new NestedLogBuilder(plugin.config.isDebug() ? Level.INFO : Level.FINE);

        nlb.log("Player" + ply.getName() + " (" + ply.getUniqueId() + ") died.");
        nlb.spacer();
        nlb.cont("Phase 1/2 (Death)");
        nlb.cont("World: " + ply.getWorld().getName() + " ( KI: " + ply.getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY) + ")");
        nlb.cont("Behavior: " + plugin.config.getDefaultBehavior().toString());
        nlb.spacer();

        // Captured before anything touches the player, and kept until the respawn phase claims it.
        // The two buckets start out according to default_behavior: everything the player was
        // carrying is either kept or dropped, and the rules move things between the two.
        DeathContextImpl ctx = new DeathContextImpl(ply, event, plugin.config.getDefaultBehavior(), nlb);
        plugin.pendingDeaths.put(ctx);

        BetterKeepInventory.instance.metrics.deathsProcessed +=1;

        // Time to process the top level rules
        for(ConfigRule rule : plugin.config.getRules()){
            rule.trigger(ctx);
        }

        DeathApplication.apply(ctx, event, nlb);
    }
}
