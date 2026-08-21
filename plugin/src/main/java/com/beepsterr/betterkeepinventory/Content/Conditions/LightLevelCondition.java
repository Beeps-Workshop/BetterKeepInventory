package com.beepsterr.betterkeepinventory.Content.Conditions;

import com.beepsterr.betterkeepinventory.api.Types.NumberRange;
import com.beepsterr.betterkeepinventory.api.Condition;
import com.beepsterr.betterkeepinventory.api.LoggerInterface;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Condition that checks the light level at the death location.
 * <p>
 * Useful for applying different rules based on lighting:
 * <ul>
 *   <li>Dark areas (light level 0-7, mobs can spawn)</li>
 *   <li>Lit areas (light level 8-15, safe from mob spawns)</li>
 * </ul>
 * <p>
 * Configuration:
 * <ul>
 *   <li>{@code range: "< 8"} - dark areas where mobs spawn</li>
 *   <li>{@code range: ">= 8"} - lit areas</li>
 *   <li>{@code range: "0..7"} - equivalent to "< 8"</li>
 *   <li>{@code type: BLOCK|SKY|ANY} - which light level to check (default: ANY)</li>
 * </ul>
 */
public class LightLevelCondition implements Condition {

    public enum LightType {
        BLOCK,
        SKY,
        ANY
    }

    private final NumberRange range;
    private final LightType type;

    public LightLevelCondition(ConfigurationSection config) {
        String rangeExpr = config.getString("range", "0..15");
        this.range = NumberRange.parse(rangeExpr);
        this.type = LightType.valueOf(config.getString("type", "ANY").toUpperCase());
    }

    @Override
    public boolean check(Player ply, PlayerDeathEvent deathEvent, PlayerRespawnEvent respawnEvent, LoggerInterface logger) {
        Block block = ply.getLocation().getBlock();

        int lightLevel = switch (type) {
            case BLOCK -> block.getLightFromBlocks();
            case SKY -> block.getLightFromSky();
            case ANY -> block.getLightLevel();
        };

        return range.contains(lightLevel);
    }
}
