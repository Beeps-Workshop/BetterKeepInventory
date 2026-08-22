package com.beepsterr.betterkeepinventory.api;

public interface Effect {

    /**
     * Called for a matching rule when the player dies.
     * <p>
     * One instance serves every death concurrently, because the rule tree is parsed once rather
     * than per death. Treat fields as immutable configuration and put anything per-death in
     * {@link DeathContext#setExtraData}.
     */
    void onDeath(DeathContext ctx);

    /**
     * Called for a matching rule when the player respawns.
     * <p>
     * Reached late rather than never for a player who is kicked, banned, or who quits at the
     * death screen: they rejoin still dead and have to press respawn.
     */
    void onRespawn(DeathContext ctx);
}
