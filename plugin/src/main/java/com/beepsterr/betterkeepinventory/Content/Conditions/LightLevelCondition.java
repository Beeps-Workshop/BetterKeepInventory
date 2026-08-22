package com.beepsterr.betterkeepinventory.Content.Conditions;

import com.beepsterr.betterkeepinventory.api.DeathContext;
import com.beepsterr.betterkeepinventory.api.Types.NumberRange;
import com.beepsterr.betterkeepinventory.api.Condition;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

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
    public boolean check(DeathContext ctx) {
        // Where they died, not where they are. Reading the live location measured the light at
        // the spawn point during the respawn phase, for the same death.
        Block block = ctx.deathLocation().getBlock();

        int lightLevel = switch (type) {
            case BLOCK -> block.getLightFromBlocks();
            case SKY -> block.getLightFromSky();
            case ANY -> block.getLightLevel();
        };

        return range.contains(lightLevel);
    }
}
