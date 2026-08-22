package com.beepsterr.betterkeepinventory.Events;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.Library.ConfigRule;
import com.beepsterr.betterkeepinventory.Library.DeathContextImpl;
import com.beepsterr.betterkeepinventory.Library.NestedLogBuilder;
import com.beepsterr.betterkeepinventory.api.Events.BKIPlayerRespawnProcessedEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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

        // The death that this respawn belongs to. Absent only if we never saw the death -- the
        // plugin was loaded while the player was already dead, or the server restarted in
        // between. There is nothing to carry across in that case, so the respawn phase is
        // skipped rather than run against a death we know nothing about.
        DeathContextImpl ctx = plugin.pendingDeaths.take(ply.getUniqueId());
        if (ctx == null) {
            nlb.log("Player " + ply.getName() + " respawned, but we have no record of their death. Skipping.");
            nlb.end();
            return;
        }

        nlb.log("Player" + ply.getName() + " (" + ply.getUniqueId() + ") died.");
        nlb.spacer();
        nlb.cont("Phase 2/2 (Respawn)");
        nlb.cont("World: " + ply.getWorld().getName());
        nlb.cont("Behavior: " + plugin.config.getDefaultBehavior().toString());
        nlb.spacer();

        ctx.enterRespawnPhase(ply, event, nlb);

        // The tree this death started against, not whatever is current -- see DeathContextImpl.
        for(ConfigRule rule : ctx.rules()){
            rule.trigger(ctx);
        }

        // No application step in this phase, so one event is enough -- a second would fire with
        // nothing in between.
        Bukkit.getPluginManager().callEvent(new BKIPlayerRespawnProcessedEvent(ctx));
    }

}
