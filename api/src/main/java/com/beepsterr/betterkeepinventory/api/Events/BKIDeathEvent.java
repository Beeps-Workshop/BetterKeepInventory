package com.beepsterr.betterkeepinventory.api.Events;

import com.beepsterr.betterkeepinventory.api.DeathContext;
import org.bukkit.event.player.PlayerEvent;

/**
 * Shared base for the events BetterKeepInventory fires around a death.
 * <p>
 * Every one of them is prefixed {@code BKI} so it cannot be mistaken for a Bukkit event of a
 * similar name, and carries the {@link DeathContext} for the death being processed.
 *
 * <h2>Listening instead of registering an effect</h2>
 * An effect is something the server owner opts into and positions in their rules; it gets the
 * condition system for free and runs where the config puts it. A listener is something a plugin
 * does unconditionally — it always runs after the rules, it is ordered against other listeners by
 * {@code EventPriority}, and it does its own filtering. A plugin that should work the moment it
 * is installed, without anyone editing a rules tree, wants a listener.
 * <p>
 * These fire on <em>every</em> death, whether or not any rule matched.
 */
public abstract class BKIDeathEvent extends PlayerEvent {

    private final DeathContext context;

    protected BKIDeathEvent(DeathContext context) {
        super(context.player());
        this.context = context;
    }

    /**
     * The death being processed: where it happened, what caused it, what the player was
     * carrying, and what they are about to be left with.
     */
    public DeathContext context() {
        return context;
    }

    // getPlayer() comes from PlayerEvent, which is final there -- it holds the same player the
    // context was constructed with.
}
