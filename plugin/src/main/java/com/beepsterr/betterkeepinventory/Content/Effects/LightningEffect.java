package com.beepsterr.betterkeepinventory.Content.Effects;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.api.DeathContext;
import com.beepsterr.betterkeepinventory.api.Effect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class LightningEffect implements Effect {

    private final boolean onDeath;
    private final boolean onRespawn;
    private final boolean damage;

    public LightningEffect(ConfigurationSection config) {
        this.onDeath = config.getBoolean("on_death", true);
        this.onRespawn = config.getBoolean("on_respawn", false);
        this.damage = config.getBoolean("damage", false);
    }

    @Override
    public void onDeath(DeathContext ctx) {
        if (onDeath) {
            strikeLightning(ctx.deathLocation());
        }
    }

    @Override
    public void onRespawn(DeathContext ctx) {
        if (!onRespawn) {
            return;
        }

        Player ply = ctx.player();
        // Delay slightly to ensure player has respawned
        BetterKeepInventory.getScheduler().getScheduler().runAtEntityLater(ply, () -> {
            strikeLightning(ply.getLocation());
        }, 5L);
    }

    private void strikeLightning(Location loc) {
        if (damage) {
            loc.getWorld().strikeLightning(loc);
        } else {
            loc.getWorld().strikeLightningEffect(loc);
        }
    }
}
