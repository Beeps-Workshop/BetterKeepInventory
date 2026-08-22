package com.beepsterr.betterkeepinventory.Content.Effects;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.api.DeathContext;
import com.beepsterr.betterkeepinventory.api.Effect;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KickEffect implements Effect {

    private final String message;

    public KickEffect(ConfigurationSection config) {
        this.message = config.getString("message", "You died!");
    }

    @Override
    public void onDeath(DeathContext ctx) {
        Player ply = ctx.player();


        // Delaying the kick by 1 tick to prevent item dupe issue if used with drop effect.
        BetterKeepInventory.getScheduler().getScheduler().runAtEntityLater( ply, () -> {
            ply.kickPlayer(message);
        }, 1L);

    }

    @Override
    public void onRespawn(DeathContext ctx) {
        // No action needed on respawn for this effect
    }
}