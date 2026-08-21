package com.beepsterr.betterkeepinventory.Library;

import com.beepsterr.betterkeepinventory.api.LoggerInterface;
import org.bukkit.Location;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Hands the two buckets to the world. The single point at which anything actually leaves the
 * player.
 * <p>
 * We distribute the drops ourselves rather than handing the server a list, because the server
 * only collects death loot when it intends to drop it: on a world where the keepInventory
 * gamerule is on it collects nothing, so {@code event.getDrops()} arrives empty and cannot be
 * trusted to say anything about what the player was carrying. Building both buckets from our own
 * snapshot means we never have to ask, which is what removed the {@code collectDropsOurselves}
 * special case this replaces.
 */
public final class DeathApplication {

    private DeathApplication() {}

    /**
     * @param event the death event, or null when applying outside a real death (tests). When
     *              present, its flags are set so the server does not also act on its own idea of
     *              what this death costs.
     */
    public static void apply(DeathContextImpl ctx, PlayerDeathEvent event, LoggerInterface logger) {

        Player ply = ctx.player();

        if (event != null) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
        }

        // ------------------------------------------------------------------------------------
        // The order of the two steps below is a correctness constraint, not a stylistic one.
        //
        // The player is updated before anything is spawned into the world, so a crash partway
        // through loses items rather than duplicating them. If the process dies with items
        // already on the ground but the player's reduced inventory not yet saved, the chunk save
        // and the player save disagree and the items exist twice. Nothing on our side can make
        // those two saves atomic, so failing in the direction of loss is the whole of the
        // protection. DO NOT REORDER.
        // ------------------------------------------------------------------------------------

        // 1. The player.
        ply.getInventory().setContents(ctx.inventory());
        ply.setLevel(ctx.levels());
        ply.setExp(ctx.progress());

        // 2. The world.
        Location at = ctx.deathLocation();
        int droppedStacks = 0;
        for (ItemStack item : ctx.drops()) {
            if (item == null || item.getType().isAir() || item.getAmount() <= 0) continue;
            at.getWorld().dropItemNaturally(at, item);
            droppedStacks++;
        }

        if (ctx.droppedExp() > 0) {
            ExperienceOrb orb = at.getWorld().spawn(at, ExperienceOrb.class);
            orb.setExperience(ctx.droppedExp());
        }

        if (logger != null) {
            logger.log("Applied: kept " + countStacks(ctx.inventory()) + " stacks and " + ctx.levels()
                    + " levels, dropped " + droppedStacks + " stacks and " + ctx.droppedExp() + " experience.");
        }
    }

    private static int countStacks(ItemStack[] inventory) {
        int count = 0;
        for (ItemStack item : inventory) {
            if (item != null && !item.getType().isAir()) count++;
        }
        return count;
    }
}
