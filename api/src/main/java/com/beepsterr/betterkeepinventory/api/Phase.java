package com.beepsterr.betterkeepinventory.api;

/**
 * Which half of a death the rules are currently being evaluated for.
 * <p>
 * There are exactly two. Resurrection (a totem, or a plugin un-cancelling
 * {@code EntityResurrectEvent}) is not a third phase: it happens <em>before</em> any of this
 * and, when it succeeds, {@code PlayerDeathEvent} never fires and the rules never run at all.
 * At that point there is no snapshot, no drops and no experience split, so nothing a
 * {@link DeathContext} exposes would be meaningful.
 */
public enum Phase {

    /**
     * The player has died. The buckets on {@link DeathContext} are live and effects may still
     * move items between them; nothing has been handed to the world yet.
     */
    DEATH,

    /**
     * The player is respawning. The buckets have already been applied, so changes to them have
     * no effect -- this phase is for work that can only happen once the player is back.
     * <p>
     * A player who is kicked, banned, or who quits at the death screen reaches this phase
     * late rather than never: they rejoin still dead and have to press respawn.
     */
    RESPAWN
}
