package com.beepsterr.betterkeepinventory.Content.Conditions;

import com.beepsterr.betterkeepinventory.api.DeathContext;
import com.beepsterr.betterkeepinventory.api.Condition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

public class PermissionsCondition implements Condition {

    private final List<String> permissions;

    public PermissionsCondition(ConfigurationSection config) {
        this.permissions = config.getStringList("nodes");
    }

    @Override
    public boolean check(DeathContext ctx) {
        Player ply = ctx.player();

        for (String perm : permissions) {
            boolean negated = perm.startsWith("!");
            String actual = negated ? perm.substring(1) : perm;

            if (negated != ply.hasPermission(actual)) {
                return true;
            }
        }
        return false;
    }
}
