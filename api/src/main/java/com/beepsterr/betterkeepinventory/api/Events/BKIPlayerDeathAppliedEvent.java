package com.beepsterr.betterkeepinventory.api.Events;

import com.beepsterr.betterkeepinventory.api.DeathContext;
import org.bukkit.event.HandlerList;

/**
 * Fired once the death has been settled: the player is holding what they keep, and the drops and
 * experience are in the world.
 * <p>
 * For observers — statistics, logging, webhooks, anything that wants to know what a death
 * actually cost. {@link DeathContext#originalInventory()} compared against
 * {@link DeathContext#inventory()} is that answer without having to follow the rules that
 * produced it.
 *
 * <h2>Observational</h2>
 * The context is still technically mutable here, but changing the buckets has no effect: they
 * have already been handed over. To alter a death, listen to {@link BKIPlayerDeathProcessedEvent}
 * instead.
 *
 * <h2>Threading</h2>
 * Fired synchronously inside the death handler, so it stays on the region thread under Folia.
 * Do not block in a listener.
 */
public class BKIPlayerDeathAppliedEvent extends BKIDeathEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public BKIPlayerDeathAppliedEvent(DeathContext context) {
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
