package com.beepsterr.betterkeepinventory.Content.Effects;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.api.Effect;
import com.beepsterr.betterkeepinventory.api.LoggerInterface;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HungerEffect implements Effect {

    private final int min;
    private final int amount;
    public static final Map<UUID, Integer> hungerMap = new HashMap<>();

    public HungerEffect(ConfigurationSection config) {
        this.min = config.getInt("min", 0);
        this.amount = config.getInt("amount", 0);
    }

    @Override
    public void onDeath(Player ply, PlayerDeathEvent event, LoggerInterface logger) {
        int currentHunger = ply.getFoodLevel();
        int newHunger = Math.max(currentHunger - amount, min);
        hungerMap.put(ply.getUniqueId(), newHunger);
        BetterKeepInventory.getInstance().debug(ply, "saving hunger level " + newHunger + " for respawn.");
    }

    @Override
    public void onRespawn(Player ply, PlayerRespawnEvent event, LoggerInterface logger) {
        BetterKeepInventory.getScheduler().getScheduler().runAtEntityLater(ply, () -> {
            Integer saved = hungerMap.remove(ply.getUniqueId());
            if (saved != null) {
                ply.setFoodLevel(saved);
                BetterKeepInventory.getInstance().debug(ply, "set hunger level to " + saved + " after respawn.");
            }
        }, 5L);
    }
}