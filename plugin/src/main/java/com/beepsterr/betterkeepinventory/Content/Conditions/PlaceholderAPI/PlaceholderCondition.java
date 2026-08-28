package com.beepsterr.betterkeepinventory.Content.Conditions.PlaceholderAPI;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.Library.PlaceholderItem;
import com.beepsterr.betterkeepinventory.api.DeathContext;
import com.beepsterr.betterkeepinventory.api.Condition;
import com.beepsterr.betterkeepinventory.api.Exceptions.ConditionParseError;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PlaceholderCondition implements Condition {

    private final List<PlaceholderItem> placeholderConditions;

    public PlaceholderCondition(ConfigurationSection config) throws ConditionParseError {
        this.placeholderConditions = new ArrayList<>();
        if (config != null) {
            for (String key : config.getKeys(false)) {
                ConfigurationSection inner = config.getConfigurationSection(key);
                if (inner != null) {
                    this.placeholderConditions.add(new PlaceholderItem(inner));
                }
            }
        }
    }

    @Override
    public boolean check(DeathContext ctx) {
        Player ply = ctx.player();

        BetterKeepInventory.instance.debug(ply, "Going to test " + this.placeholderConditions.size() + " placeholder conditions");

        for (PlaceholderItem item : this.placeholderConditions) {
            BetterKeepInventory.instance.debug(ply, item.toString() + " Result: " + item.test(ply));
            if (item.test(ply)) {
                return true;
            }
        }
        return false;
    }
}
