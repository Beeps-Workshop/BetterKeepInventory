package com.beepsterr.betterkeepinventory.Content.Effects;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.api.DeathContext;
import com.beepsterr.betterkeepinventory.api.Effect;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public class BanEffect implements Effect {

    private final String message;
    private Duration expiration = null;

    public BanEffect(ConfigurationSection config) {
        this.message = config.getString("message", "You died!");
        String durationString = config.getString("duration", "5m").toLowerCase();

        if(durationString.equals("forever")) {
            // No time limit; banned until manually unbanned.
            this.expiration = null;
        } else if(durationString.endsWith("s")){
            long seconds = Long.parseLong(durationString.replace("s", ""));
            this.expiration = Duration.ofSeconds(seconds);
        } else if(durationString.endsWith("m")){
            long minutes = Long.parseLong(durationString.replace("m", ""));
            this.expiration = Duration.ofMinutes(minutes);
        } else if(durationString.endsWith("h")){
            long hours = Long.parseLong(durationString.replace("h", ""));
            this.expiration = Duration.ofHours(hours);
        } else if(durationString.endsWith("d")){
            long days = Long.parseLong(durationString.replace("d", ""));
            this.expiration = Duration.ofDays(days);
        } else {
            // Default to minutes if no suffix is provided
            long minutes = Long.parseLong(durationString);
            this.expiration = Duration.ofMinutes(minutes);
        }

    }

    @Override
    public void onDeath(DeathContext ctx) {
        Player ply = ctx.player();


        // Delaying the ban and kick by 1 tick to prevent item dupe issue if used with drop effect.
        BetterKeepInventory.getScheduler().getScheduler().runAtEntityLater( ply, () -> {

            Date expires = null;
            if (this.expiration != null) {
                expires = Date.from(Instant.now().plus(this.expiration));
            }

            Bukkit.getBanList(BanList.Type.NAME).addBan(ply.getName(), this.message, expires, String.valueOf(ply.getUniqueId()));
            ply.kickPlayer(this.message);

        }, 1L);

    }

    @Override
    public void onRespawn(DeathContext ctx) {
        // No action needed on respawn for this effect
    }
}