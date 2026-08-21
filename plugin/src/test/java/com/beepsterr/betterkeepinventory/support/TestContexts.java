package com.beepsterr.betterkeepinventory.support;

import com.beepsterr.betterkeepinventory.Library.Config;
import com.beepsterr.betterkeepinventory.Library.DeathApplication;
import com.beepsterr.betterkeepinventory.Library.DeathContextImpl;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Builds the {@link DeathContextImpl} that conditions and effects are driven with, so tests do
 * not each hand-roll one.
 * <p>
 * Defaults to {@code KEEP} so the buckets start with the player's items in
 * {@code inventory()} and nothing in {@code drops()} -- the arrangement most effects under test
 * expect. Pass a behavior explicitly to exercise the other direction.
 */
public final class TestContexts {

    private TestContexts() {}

    public static DeathContextImpl death(Player player) {
        return death(player, null);
    }

    public static DeathContextImpl death(Player player, PlayerDeathEvent event) {
        return death(player, event, Config.DefaultBehavior.KEEP);
    }

    public static DeathContextImpl death(Player player, PlayerDeathEvent event, Config.DefaultBehavior behavior) {
        return new DeathContextImpl(player, event, behavior, new NoopLogger());
    }

    /**
     * A context that has already been through the death phase and is now respawning -- the same
     * object the death produced, as production would hand it over.
     */
    public static DeathContextImpl respawn(Player player, PlayerRespawnEvent event) {
        DeathContextImpl ctx = death(player);
        ctx.enterRespawnPhase(player, event, new NoopLogger());
        return ctx;
    }

    /**
     * Hand the context's buckets to the player and the world, exactly as a real death does.
     * <p>
     * Effects no longer touch the player directly -- they move items between the two buckets --
     * so a test that wants to assert on the player's inventory or on ground items has to apply
     * first. Calling the production code rather than reimplementing it keeps the two from
     * drifting.
     */
    public static void apply(DeathContextImpl ctx) {
        DeathApplication.apply(ctx, null, new NoopLogger());
    }
}
