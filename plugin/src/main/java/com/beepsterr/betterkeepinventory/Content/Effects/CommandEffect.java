package com.beepsterr.betterkeepinventory.Content.Effects;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.api.Utilities;
import com.beepsterr.betterkeepinventory.api.Effect;
import com.beepsterr.betterkeepinventory.api.LoggerInterface;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    private static final Map<UUID, Location> deathLocations = new HashMap<>();

    public CommandEffect(ConfigurationSection config) {
        this.onDeathCommands = Utilities.ConfigList(config, "on_death");
        this.onRespawnCommands = Utilities.ConfigList(config, "on_respawn");
        this.executor = Executor.valueOf(config.getString("executor", "CONSOLE").toUpperCase());
    }

    @Override
    public void onDeath(Player ply, PlayerDeathEvent event, LoggerInterface logger) {
        // Store death location for respawn commands
        deathLocations.put(ply.getUniqueId(), ply.getLocation().clone());

        for (String command : onDeathCommands) {
            executeCommand(ply, command, ply.getLocation(), event);
        }
    }

    @Override
    public void onRespawn(Player ply, PlayerRespawnEvent event, LoggerInterface logger) {
        Location deathLoc = deathLocations.remove(ply.getUniqueId());
        if (deathLoc == null) {
            deathLoc = ply.getLocation();
        }

        for (String command : onRespawnCommands) {
            executeCommand(ply, command, deathLoc, null);
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
