package com.beepsterr.betterkeepinventory.Events;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.Library.ConfigRule;
import com.beepsterr.betterkeepinventory.Library.DeathContextImpl;
import com.beepsterr.betterkeepinventory.Library.NestedLogBuilder;
import org.bukkit.GameRule;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

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
        DeathContextImpl ctx = new DeathContextImpl(ply, event, plugin.config.getDefaultBehavior(), nlb);
        plugin.pendingDeaths.put(ctx);

        // The server only collects death loot when it intends to drop it. On a world where the
        // keepInventory gamerule is on it collects nothing, so event.getDrops() arrives EMPTY.
        // Turning keepInventory off from under it would then have the server clear the inventory
        // and spawn that empty list -- destroying everything the player was carrying. When we
        // find ourselves in that case, stay pinned to "keep" for the duration of the rules and
        // build the drops ourselves afterwards, once the effects have had their turn.
        boolean collectDropsOurselves = false;

        // Vanilla works out how much experience a death drops from the level the player died at,
        // before any plugin gets a say. Capture it now so that we hand out the same amount the
        // server would have -- reading it back after the rules would instead measure whatever an
        // exp effect left behind, and make the payout depend on the world's gamerule.
        int levelAtDeath = ply.getLevel();

        // TODO(3.0): this whole block, and collectDrops() below, are replaced by the application
        // step -- one place that writes ctx.inventory() back to the player and distributes
        // ctx.drops(). Until the item-moving effects are ported onto the buckets they still
        // mutate the player directly, so the buckets on the context are informational for now.
        switch(plugin.config.getDefaultBehavior()){
            case KEEP:
                // these are needed to prevent dupes!!
                event.getDrops().clear();
                event.setDroppedExp(0);
                event.setKeepLevel(true);
                event.setKeepInventory(true);
                nlb.log("Default Behavior: KEEP");
                break;
            case DROP:
                collectDropsOurselves = event.getKeepInventory();
                if(!collectDropsOurselves){
                    event.setKeepLevel(false);
                    event.setKeepInventory(false);
                }
                nlb.log("Default Behavior: DROP" + (collectDropsOurselves ? " (world keeps inventory, collecting drops ourselves)" : ""));
                break;
            // No case needed for INHERIT, as it will default to the world/other plugins behavior
        }

        BetterKeepInventory.instance.metrics.deathsProcessed +=1;

        // Time to process the top level rules
        for(ConfigRule rule : plugin.config.getRules()){
            rule.trigger(ctx);
        }

        if(collectDropsOurselves){
            collectDrops(ply, event, levelAtDeath, nlb);
        }

    }

    /**
     * Build the death loot ourselves and hand control back to the server.
     * <p>
     * Only used when the world was keeping the inventory but our behavior is DROP, so the drops
     * list the server handed us is empty. Runs after the rules so that whatever the effects left
     * in the inventory is what actually drops -- collecting before them would drop the pre-effect
     * state on top of anything an effect already dropped by hand, and duplicate it.
     *
     * @param levelAtDeath the player's level before the rules ran, which is what vanilla would
     *                     have based its experience drop on.
     */
    private void collectDrops(Player ply, PlayerDeathEvent event, int levelAtDeath, NestedLogBuilder nlb) {

        PlayerInventory inv = ply.getInventory();

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) continue;

            event.getDrops().add(item.clone());
        }

        // Vanilla caps the experience a death drops, match it so this behaves like a normal death.
        event.setDroppedExp(Math.min(levelAtDeath * 7, 100));
        event.setKeepLevel(false);
        event.setKeepInventory(false);

        nlb.log("Collected " + event.getDrops().size() + " stacks and " + event.getDroppedExp() + " experience for dropping.");
    }


}
