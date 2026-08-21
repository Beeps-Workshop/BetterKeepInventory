package com.beepsterr.betterkeepinventory.Content.Conditions;

import com.beepsterr.betterkeepinventory.api.Utilities;
import com.beepsterr.betterkeepinventory.api.Condition;
import com.beepsterr.betterkeepinventory.api.LoggerInterface;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.List;
import java.util.regex.Pattern;

public class WorldsCondition implements Condition {

    private final List<String> worlds;

    public WorldsCondition(ConfigurationSection config) {
        this.worlds = config.getStringList("nodes");
    }

    @Override
    public boolean check(Player ply, PlayerDeathEvent deathEvent, PlayerRespawnEvent respawnEvent, LoggerInterface logger) {
        return Utilities.advancedStringCompare(ply.getWorld().getName(), worlds);
    }
}
