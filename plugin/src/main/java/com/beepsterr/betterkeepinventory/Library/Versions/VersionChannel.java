package com.beepsterr.betterkeepinventory.Library.Versions;

/**
 * Which releases a server wants to hear about.
 * <p>
 * Ordered from least to most adventurous. Every channel other than {@link #NONE} only ever
 * considers versions Modrinth says are compatible with this server's Minecraft version and
 * server software, so no channel can advise an update that will not run here.
 */
public enum VersionChannel {

    /** Check nothing. No requests are made at all. */
    NONE,

    /**
     * Releases that also appear in the recommended list -- known-good versions, including for
     * servers that cannot move past a particular Minecraft version.
     * <p>
     * If nothing compatible is on that list, this reports no update rather than falling back to
     * {@link #LATEST}: quietly widening what the server owner asked for is how they end up on a
     * version they did not choose.
     */
    STABLE,

    /** Any full release. */
    LATEST,

    /** Full releases and betas. */
    BETA
}
