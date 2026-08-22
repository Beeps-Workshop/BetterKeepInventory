package com.beepsterr.betterkeepinventory.Content.Conditions;

import com.beepsterr.betterkeepinventory.api.DeathContext;
import com.beepsterr.betterkeepinventory.api.Utilities;
import com.beepsterr.betterkeepinventory.api.Condition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.List;

public class CauseCondition implements Condition {

    private final List<String> causes;

    public CauseCondition(ConfigurationSection config) {
        this.causes = config.getStringList("nodes");
    }

    @Override
    public boolean check(DeathContext ctx) {
        Player ply = ctx.player();

        EntityDamageEvent lastDamage = ply.getLastDamageCause();
        if (lastDamage == null) {
            return false;
        }

        String causeName = lastDamage.getCause().name();
        return Utilities.advancedStringCompare(causeName, causes);
    }
}