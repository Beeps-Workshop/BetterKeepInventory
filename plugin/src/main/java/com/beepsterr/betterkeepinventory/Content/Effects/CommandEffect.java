package com.beepsterr.betterkeepinventory.Content.Effects;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.api.DeathContext;
import com.beepsterr.betterkeepinventory.api.Utilities;
import com.beepsterr.betterkeepinventory.api.Effect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.List;

/**
 * Effect that executes commands on death and/or respawn.
 * Commands can include placeholders:
 * - %player% - Player name
 * - %uuid% - Player UUID
 * - %world% - World name
 * - %x%, %y%, %z% - Death coordinates
 * - %killer% - Killer name (or "Unknown" if none)
 *
 * Commands can be run as:
 * - CONSOLE (default) - Run as server console
 * - PLAYER - Run as the player
 */
public class CommandEffect implements Effect {

    public enum Executor {
        CONSOLE,
        PLAYER
    }

    private final List<String> onDeathCommands;
    private final List<String> onRespawnCommands;
    private final Executor executor;

    public CommandEffect(ConfigurationSection config) {
        this.onDeathCommands = Utilities.ConfigList(config, "on_death");
        this.onRespawnCommands = Utilities.ConfigList(config, "on_respawn");
        this.executor = Executor.valueOf(config.getString("executor", "CONSOLE").toUpperCase());
    }

    @Override
    public void onDeath(DeathContext ctx) {
        for (String command : onDeathCommands) {
            executeCommand(ctx.player(), command, ctx.deathLocation(), ctx.deathEvent());
        }
    }

    @Override
    public void onRespawn(DeathContext ctx) {
        // The coordinate placeholders mean where they died, in both phases -- which is why the
        // location comes off the context rather than from a static map this effect used to keep.
        for (String command : onRespawnCommands) {
            executeCommand(ctx.player(), command, ctx.deathLocation(), null);
        }
    }

    private void executeCommand(Player ply, String command, Location loc, PlayerDeathEvent deathEvent) {
        String processed = command
                .replace("%player%", ply.getName())
                .replace("%uuid%", ply.getUniqueId().toString())
                .replace("%world%", loc.getWorld().getName())
                .replace("%x%", String.valueOf(loc.getBlockX()))
                .replace("%y%", String.valueOf(loc.getBlockY()))
                .replace("%z%", String.valueOf(loc.getBlockZ()));

        // Handle killer placeholder
        String killerName = "Unknown";
        if (ply.getKiller() != null) {
            killerName = ply.getKiller().getName();
        } else if (ply.getLastDamageCause() instanceof org.bukkit.event.entity.EntityDamageByEntityEvent edbe) {
            killerName = edbe.getDamager().getType().name();
        }
        processed = processed.replace("%killer%", killerName);

        final String finalCommand = processed;

        // Execute on the next tick to ensure event processing is complete
        BetterKeepInventory.getScheduler().getScheduler().runAtEntityLater(ply, () -> {
            switch (executor) {
                case CONSOLE -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                case PLAYER -> ply.performCommand(finalCommand);
            }
        }, 1L);
    }
}
