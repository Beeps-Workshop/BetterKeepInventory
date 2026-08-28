package com.beepsterr.betterkeepinventory.api.Events;

import com.beepsterr.betterkeepinventory.api.DeathContext;
import org.bukkit.event.HandlerList;

/**
 * Fired once every rule has run, but before anything has been handed to the world.
 * <p>
 * This is the only seam where the two buckets are final and still changeable — the last chance
 * to alter what a death costs. The motivating case is a buyback shop: record
 * {@link DeathContext#drops()} into its own storage, then empty the list so nothing reaches the
 * ground.
 *
 * <h2>Prefer a condition where one will do</h2>
 * Taking the last word here overrides whatever the server owner configured, which is a large
 * hammer. A plugin that only needs to contribute a <em>fact</em> about the player — whether they
 * are combat tagged, whether they hold a purchased life — should register a
 * {@link com.beepsterr.betterkeepinventory.api.Condition} instead. That leaves the owner deciding
 * what the fact protects and composes with the rest of their rules, where an override cannot.
 * Reach for this event when the plugin genuinely needs to redirect where things go.
 *
 * <h2>Not cancellable</h2>
 * There is no coherent meaning for cancelling: the items have to go somewhere, and "do not
 * apply" would simply evaporate them. To change the outcome, change the buckets.
 *
 * <h2>Threading</h2>
 * Fired synchronously inside the death handler, so it stays on the region thread under Folia.
 * Do not block in a listener.
 */
public class BKIPlayerDeathProcessedEvent extends BKIDeathEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public BKIPlayerDeathProcessedEvent(DeathContext context) {
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
