package com.beepsterr.betterkeepinventory.api.Events;

import com.beepsterr.betterkeepinventory.api.DeathContext;
import com.beepsterr.betterkeepinventory.api.Phase;
import org.bukkit.event.HandlerList;

/**
 * Fired once every rule has run for the respawn half of a death.
 * <p>
 * There is only one respawn event, where a death has two. The death pair exists because there is
 * an application step between them; the respawn phase has no such step, so a second event would
 * fire with nothing in between.
 * <p>
 * The context is the same object the death produced, now in {@link Phase#RESPAWN} — so
 * {@link DeathContext#deathLocation()} still means where they died, and anything an effect
 * stashed with {@code setExtraData} during the death phase is still readable.
 * <p>
 * A player who is kicked, banned, or who quits at the death screen reaches this late rather than
 * never: they rejoin still dead and have to press respawn. It will not fire at all if the server
 * restarts in between, since the pending death is held in memory.
 *
 * <h2>Threading</h2>
 * Fired synchronously inside the respawn handler, so it stays on the region thread under Folia.
 * Do not block in a listener.
 */
public class BKIPlayerRespawnProcessedEvent extends BKIDeathEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public BKIPlayerRespawnProcessedEvent(DeathContext context) {
        super(context);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
