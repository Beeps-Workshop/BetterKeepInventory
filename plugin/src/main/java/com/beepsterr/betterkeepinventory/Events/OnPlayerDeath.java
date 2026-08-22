package com.beepsterr.betterkeepinventory.Events;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.Library.ConfigRule;
import com.beepsterr.betterkeepinventory.Library.DeathApplication;
import com.beepsterr.betterkeepinventory.Library.DeathContextImpl;
import com.beepsterr.betterkeepinventory.api.Events.BKIPlayerDeathAppliedEvent;
import com.beepsterr.betterkeepinventory.api.Events.BKIPlayerDeathProcessedEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Map;

public class OnPlayerDeath  implements Listener {

    BetterKeepInventory plugin;


    public OnPlayerDeath(BetterKeepInventory main){
        plugin = main;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player ply = event.getEntity();

        // Captured before anything touches the player, and kept until the respawn phase claims it.
        // The two buckets start out according to default_behavior: everything the player was
        // carrying is either kept or dropped, and the rules move things between the two.
        // The rule tree is handed to the context rather than looked up again later, so both
        // phases of this death see the same rules even if a plugin registers something in between.
        DeathContextImpl ctx = new DeathContextImpl(
                ply, event, plugin.config.getDefaultBehavior(), plugin.config.getRules());
        plugin.pendingDeaths.put(ctx);

        // The context binds the player, phase, world and behavior onto every line from here on,
        // so this only has to say what is not already a field.
        ctx.logger().debug("player died", Map.of(
                "configured_behavior", plugin.config.getDefaultBehavior().toString(),
                "gamerule_keep_inventory", String.valueOf(ply.getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY)),
                "rules", ctx.rules().size()));

        BetterKeepInventory.instance.metrics.deathsProcessed +=1;

        // Time to process the top level rules
        for(ConfigRule rule : ctx.rules()){
            rule.trigger(ctx);
        }

        // Last chance to change what this death costs: the buckets are final but nothing has
        // reached the world yet. A buyback shop drains ctx.drops() here; a "purchased life"
        // plugin moves it all back into ctx.inventory().
        Bukkit.getPluginManager().callEvent(new BKIPlayerDeathProcessedEvent(ctx));

        DeathApplication.apply(ctx, event);

        // Settled. For observers only -- changing the buckets now has no effect.
        Bukkit.getPluginManager().callEvent(new BKIPlayerDeathAppliedEvent(ctx));
    }
}
