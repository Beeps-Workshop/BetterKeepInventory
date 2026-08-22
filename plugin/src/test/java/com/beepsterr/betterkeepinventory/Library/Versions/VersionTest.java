package com.beepsterr.betterkeepinventory.Library.Versions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure parsing, no server needed.
 * <p>
 * A version string has to be major.minor.patch. Some very old releases (1.3 through 1.6) were
 * published with only two components and are therefore rejected -- which is harmless rather than
 * a gap: they are the newest release only for Minecraft versions that will never see another
 * build, and the servers on them are already running the version in question. The update checker
 * skips what it cannot parse, so the effect is that those servers are told nothing, which is the
 * correct answer anyway.
 */
class VersionTest {

    @Test
    void parsesMajorMinorPatch() {
        Version v = new Version("2.3.4");

        assertEquals(2, v.major);
        assertEquals(3, v.minor);
        assertEquals(4, v.patch);
        assertEquals("STABLE", v.flavor);
    }

    @Test
    void parsesFlavourAndBuild() {
        Version v = new Version("2.3.0-SNAPSHOT-2607271");

        assertEquals("SNAPSHOT", v.flavor);
        assertEquals(2607271, v.build);
    }

    /** Anything that is not three components is rejected; the checker skips those. */
    @ParameterizedTest
    @ValueSource(strings = {"1.3", "1.6", "3", "not.a.version", ""})
    void rejectsAnythingElse(String bad) {
        assertThrows(IllegalArgumentException.class, () -> new Version(bad));
    }

    @Test
    void ordersByComponent() {
        assertTrue(new Version("2.3.4").compareTo(new Version("2.3.3")) > 0);
        assertTrue(new Version("3.0.0").compareTo(new Version("2.3.4")) > 0);
        assertTrue(new Version("2.4.0").compareTo(new Version("2.3.9")) > 0);
    }

    @Test
    void aStableBuildOutranksAPrereleaseOfTheSameNumber() {
        assertTrue(new Version("3.0.0").compareTo(new Version("3.0.0-BETA")) > 0);
    }
}
