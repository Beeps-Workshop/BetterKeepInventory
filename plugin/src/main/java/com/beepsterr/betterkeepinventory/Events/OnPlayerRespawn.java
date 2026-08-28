package com.beepsterr.betterkeepinventory.Events;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.Library.ConfigRule;
import com.beepsterr.betterkeepinventory.Library.DeathContextImpl;
import com.beepsterr.betterkeepinventory.Library.StructuredLogger;
import com.beepsterr.betterkeepinventory.api.Events.BKIPlayerRespawnProcessedEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Map;

public class OnPlayerRespawn implements Listener {

    BetterKeepInventory plugin;


    public OnPlayerRespawn(BetterKeepInventory main){
        plugin = main;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {

        Player ply = event.getPlayer();

        // The death that this respawn belongs to. Absent only if we never saw the death -- the
        // plugin was loaded while the player was already dead, or the server restarted in
        // between. There is nothing to carry across in that case, so the respawn phase is
        // skipped rather than run against a death we know nothing about.
        DeathContextImpl ctx = plugin.pendingDeaths.take(ply.getUniqueId());
        if (ctx == null) {
            // No context, so nothing to bind: this is the one respawn line that has to name the
            // player itself.
            StructuredLogger.plugin().debug("respawn with no record of the death; skipping.",
                    Map.of("player", ply.getName(), "uuid", ply.getUniqueId().toString()));
            return;
        }

        ctx.enterRespawnPhase(ply, event);

        // The bound fields already carry the player, and phase now reads RESPAWN.
        ctx.logger().debug("player respawned", Map.of(
                "respawn_world", ply.getWorld().getName()));

        // The tree this death started against, not whatever is current -- see DeathContextImpl.
        for(ConfigRule rule : ctx.rules()){
            rule.trigger(ctx);
        }

        // No application step in this phase, so one event is enough -- a second would fire with
        // nothing in between.
        Bukkit.getPluginManager().callEvent(new BKIPlayerRespawnProcessedEvent(ctx));
    }

}
