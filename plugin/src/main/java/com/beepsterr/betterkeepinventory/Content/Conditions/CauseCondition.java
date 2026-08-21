package com.beepsterr.betterkeepinventory.Content.Conditions;

import com.beepsterr.betterkeepinventory.api.Utilities;
import com.beepsterr.betterkeepinventory.api.Condition;
import com.beepsterr.betterkeepinventory.api.LoggerInterface;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.List;

public class CauseCondition implements Condition {

    private final List<String> causes;

    public CauseCondition(ConfigurationSection config) {
        this.causes = config.getStringList("nodes");
    }

    @Override
    public boolean check(Player ply, PlayerDeathEvent deathEvent, PlayerRespawnEvent respawnEvent, LoggerInterface logger) {
        EntityDamageEvent lastDamage = ply.getLastDamageCause();
        if (lastDamage == null) {
            return false;
        }

        String causeName = lastDamage.getCause().name();
        return Utilities.advancedStringCompare(causeName, causes);
    }
}