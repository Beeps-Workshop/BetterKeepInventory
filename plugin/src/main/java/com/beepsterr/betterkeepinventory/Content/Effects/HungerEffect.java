package com.beepsterr.betterkeepinventory.Content.Effects;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.api.DeathContext;
import com.beepsterr.betterkeepinventory.api.Effect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class HungerEffect implements Effect {

    /** Namespaced so two effects stashing "hunger" cannot collide. */
    private static final String SAVED_HUNGER = "bki:hunger";

    private final int min;
    private final int amount;

    public HungerEffect(ConfigurationSection config) {
        this.min = config.getInt("min", 0);
        this.amount = config.getInt("amount", 0);
    }

    @Override
    public void onDeath(DeathContext ctx) {

        int currentHunger = ctx.player().getFoodLevel();
        int newHunger = Math.max(currentHunger - amount, min);

        // Kept on the context rather than in a static map keyed by UUID. The old map was never
        // cleaned unless this same rule also matched at respawn, so a rule paired with `kick` or
        // `ban` -- where the respawn never arrives for it -- leaked an entry that would later be
        // applied to an unrelated death.
        ctx.setExtraData(SAVED_HUNGER, newHunger);
        BetterKeepInventory.getInstance().debug(ctx.player(), "saving hunger level " + newHunger + " for respawn.");
    }

    @Override
    public void onRespawn(DeathContext ctx) {

        Integer saved = ctx.getExtraData(SAVED_HUNGER, Integer.class);
        if (saved == null) {
            return;
        }

        Player ply = ctx.player();
        // Deferred a few ticks: the server resets food level as part of respawning, so setting it
        // during the event would be overwritten.
        BetterKeepInventory.getScheduler().getScheduler().runAtEntityLater(ply, () -> {
            ply.setFoodLevel(saved);
            BetterKeepInventory.getInstance().debug(ply, "set hunger level to " + saved + " after respawn.");
        }, 5L);
    }
}
