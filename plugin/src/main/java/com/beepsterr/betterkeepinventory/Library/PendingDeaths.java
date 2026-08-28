package com.beepsterr.betterkeepinventory.Library;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deaths waiting for their respawn phase.
 * <p>
 * Replaces the per-effect {@code static Map<UUID, ...>} stashes that {@code HungerEffect},
 * {@code CommandEffect} and {@code LightningEffect} each grew independently. Those leaked
 * whenever the respawn never arrived for that rule, and then handed the stale value to an
 * unrelated later death.
 * <p>
 * Keyed by UUID because the {@link org.bukkit.entity.Player} object is replaced when a player
 * rejoins, which is exactly what happens to anyone who quits at the death screen.
 *
 * <h2>Lifetime</h2>
 * Evicted when the respawn phase consumes it, and at no other time. A disconnect only defers
 * that phase -- a kicked, banned or departed player rejoins still dead and has to press respawn
 * -- so the only contexts that strand belong to players who never come back at all. That is one
 * inventory snapshot each, cleared on restart. A timed sweep to reclaim them would cost a
 * scheduled task and a TTL that is wrong in both directions: short enough to reclaim anything
 * meaningful is short enough to discard the context of someone returning tomorrow.
 */
public class PendingDeaths {

    private final Map<UUID, DeathContextImpl> pending = new ConcurrentHashMap<>();

    public void put(DeathContextImpl context) {
        pending.put(context.playerUuid(), context);
    }

    /**
     * Take the pending death for this player, if there is one. Removes it.
     *
     * @return the context, or null if this player has no death waiting -- which is normal for a
     *         respawn the plugin never saw the death for, such as one that happened before the
     *         plugin loaded.
     */
    public DeathContextImpl take(UUID playerId) {
        return pending.remove(playerId);
    }

    public int size() {
        return pending.size();
    }
}
