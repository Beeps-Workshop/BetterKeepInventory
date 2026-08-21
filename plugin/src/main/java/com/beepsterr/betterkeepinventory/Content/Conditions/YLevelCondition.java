package com.beepsterr.betterkeepinventory.Content.Conditions;

import com.beepsterr.betterkeepinventory.api.Types.NumberRange;
import com.beepsterr.betterkeepinventory.api.Condition;
import com.beepsterr.betterkeepinventory.api.LoggerInterface;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

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
    public boolean check(Player ply, PlayerDeathEvent deathEvent, PlayerRespawnEvent respawnEvent, LoggerInterface logger) {
        double y = ply.getLocation().getY();
        return range.contains(y);
    }
}
