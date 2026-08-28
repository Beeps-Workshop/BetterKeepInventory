package com.beepsterr.betterkeepinventory.Content.Conditions;

import com.beepsterr.betterkeepinventory.api.DeathContext;
import com.beepsterr.betterkeepinventory.api.Utilities;
import com.beepsterr.betterkeepinventory.api.Condition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Pattern;

public class WorldsCondition implements Condition {

    private final List<String> worlds;

    public WorldsCondition(ConfigurationSection config) {
        this.worlds = config.getStringList("nodes");
    }

    @Override
    public boolean check(DeathContext ctx) {
        Player ply = ctx.player();

        return Utilities.advancedStringCompare(ply.getWorld().getName(), worlds);
    }
}
