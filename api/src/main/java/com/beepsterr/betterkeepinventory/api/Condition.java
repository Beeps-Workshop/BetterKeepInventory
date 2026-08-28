package com.beepsterr.betterkeepinventory.api;

public interface Condition {

    /**
     * Whether this condition is satisfied for the death being processed.
     * <p>
     * Read circumstance from the context rather than from the player: {@link
     * DeathContext#deathLocation()} means the same thing in both phases, whereas the player's
     * live location by the respawn phase is wherever the server has since put them.
     */
    boolean check(DeathContext ctx);
}
