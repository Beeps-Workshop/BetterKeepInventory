package com.beepsterr.betterkeepinventory.Content.Conditions;

import com.beepsterr.betterkeepinventory.api.Condition;
import com.beepsterr.betterkeepinventory.api.DeathContext;
import com.beepsterr.betterkeepinventory.api.Types.NumberRange;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Condition that checks the Y-level (height) where the player died.
 * <p>
 * Useful for applying different rules for:
 * <ul>
 *   <li>Void deaths (very low Y)</li>
 *   <li>Cave deaths (below surface)</li>
 *   <li>Sky/height deaths (high Y)</li>
 * </ul>
 * <p>
 * Configuration uses a range expression:
 * <ul>
 *   <li>{@code range: "< 0"} - below Y 0 (void deaths)</li>
 *   <li>{@code range: "> 200"} - above Y 200</li>
 *   <li>{@code range: "-64..64"} - between Y -64 and 64 (caves)</li>
 *   <li>{@code range: "<= -60"} - at or below Y -60</li>
 * </ul>
 */
public class YLevelCondition implements Condition {

    private final NumberRange range;

    public YLevelCondition(ConfigurationSection config) {
        String rangeExpr = config.getString("range", "0..320");
        this.range = NumberRange.parse(rangeExpr);
    }

    @Override
    public boolean check(DeathContext ctx) {
        // The death location, not the player's current one. This used to read the live location,
        // which in the respawn phase is wherever the server has since put them -- so a rule
        // matching on the depth someone died at could evaluate against their spawn point instead.
        return range.contains(ctx.deathLocation().getY());
    }
}
