package com.beepsterr.betterkeepinventory.Content.Effects;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.api.DeathContext;
import com.beepsterr.betterkeepinventory.api.Effect;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Moves experience between what the player keeps and what hits the ground.
 * <p>
 * Nothing is spawned here; the application step at the end of the death turns
 * {@link DeathContext#droppedExp()} into an orb.
 */
public class ExpEffect implements Effect {

    public enum Mode {
        SIMPLE, PERCENTAGE, ALL
    }

    public enum How {
        DELETE, DROP
    }

    private final Mode mode;
    private final How how;
    private final float min;
    private final float max;

    public ExpEffect(ConfigurationSection config) {
        this.mode = Mode.valueOf(config.getString("mode", "PERCENTAGE").toUpperCase());
        this.how = How.valueOf(config.getString("how", "DROP").toUpperCase());
        this.min = (float) config.getDouble("min", 0.0);
        this.max = (float) config.getDouble("max", 0.0);
    }

    @Override
    public void onRespawn(DeathContext ctx) {
        // Nothing on respawn
    }

    @Override
    public void onDeath(DeathContext ctx) {

        BetterKeepInventory plugin = BetterKeepInventory.getInstance();
        Random rng = plugin.rng;

        int currentLevels = ctx.levels();
        int levelsToLose = switch (mode) {
            case SIMPLE -> (int) (min + (max - min) * rng.nextDouble());
            case PERCENTAGE -> (int) (currentLevels * ((min + (max - min) * rng.nextDouble()) / 100.0));
            case ALL -> currentLevels;
        };

        plugin.debug(ctx.player(), "is losing " + levelsToLose + " levels of experience.");

        if (levelsToLose < 1 && mode != Mode.ALL) {
            return;
        }

        int newLevels = Math.max(0, currentLevels - levelsToLose);

        // Progress survives losing levels. It used to be zeroed unconditionally, so a player at
        // level 30 with 90% of the way to 31 silently lost that 90% to any exp effect. Losing
        // *everything* is the one case where there is no partial level left to keep.
        boolean losingEverything = mode == Mode.ALL || newLevels == 0;
        float keptProgress = losingEverything ? 0f : ctx.progress();

        Map<String, String> replacements = new HashMap<>();
        replacements.put("amount", String.valueOf(Math.min(levelsToLose, currentLevels)));

        switch (how) {
            case DELETE -> {
                ctx.setLevels(newLevels);
                ctx.setProgress(keptProgress);
                plugin.config.sendMessage(ctx.player(), "effects.exp_loss", replacements);
            }
            case DROP -> {
                int pointsBefore = totalPoints(currentLevels, ctx.progress());
                int pointsAfter = totalPoints(newLevels, keptProgress);
                int pointsToDrop = Math.max(0, pointsBefore - pointsAfter);

                ctx.setLevels(newLevels);
                ctx.setProgress(keptProgress);

                if (pointsToDrop > 0) {
                    ctx.setDroppedExp(ctx.droppedExp() + pointsToDrop);
                    plugin.debug(ctx.player(), "dropping " + pointsToDrop + " experience points.");
                    plugin.config.sendMessage(ctx.player(), "effects.exp_dropped", replacements);
                }
            }
        }
    }

    /**
     * Total experience points held at a given level plus partial progress toward the next.
     * <p>
     * The partial level has to be converted through {@link #pointsToNextLevel} rather than added
     * straight on: progress is a 0..1 fraction, and a fraction of a level is worth a different
     * number of points at level 5 than at level 50.
     */
    static int totalPoints(int level, float progress) {
        return getExpAtLevel(level) + Math.round(progress * pointsToNextLevel(level));
    }

    /** Points required to go from {@code level} to {@code level + 1}. */
    static int pointsToNextLevel(int level) {
        if (level <= 15) return 2 * level + 7;
        if (level <= 30) return 5 * level - 38;
        return 9 * level - 158;
    }

    /** Total points accumulated to reach {@code level} from zero. */
    static int getExpAtLevel(int level) {
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (int) (2.5 * level * level - 40.5 * level + 360);
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }
}
