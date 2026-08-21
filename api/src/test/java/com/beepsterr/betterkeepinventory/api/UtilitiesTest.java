package com.beepsterr.betterkeepinventory.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * advancedStringCompare is pure regex/wildcard matching — no Bukkit needed.
 * Rules: '*' is a wildcard, a leading '!' negates an entry, and the list is OR-matched
 * (the first entry that "passes" wins). A full (anchored) match is required per entry.
 */
class UtilitiesTest {

    @ParameterizedTest(name = "\"{0}\" vs [{1}] -> {2}")
    @CsvSource({
            // exact
            "LAVA,          LAVA,        true",
            "LAVA,          FIRE,        false",
            // wildcard
            "DIAMOND_SWORD, DIAMOND_*,   true",
            "STONE,         DIAMOND_*,   false",
            "OAK_LOG,       *_LOG,       true",
            "OAK_PLANKS,    *_LOG,       false",
            "WHATEVER,      *,           true",
            // full-match semantics: a bare substring does NOT match
            "DIAMOND_SWORD, DIAMOND,     false",
            // negation
            "STONE,         !DIAMOND_*,  true",
            "DIAMOND_SWORD, !DIAMOND_*,  false",
    })
    void singlePattern(String input, String pattern, boolean expected) {
        assertEquals(expected, Utilities.advancedStringCompare(input, List.of(pattern)));
    }

    @Test
    void orSemanticsAcrossMultiplePatterns() {
        assertTrue(Utilities.advancedStringCompare("LAVA", List.of("FIRE", "LAVA")));
        assertFalse(Utilities.advancedStringCompare("LAVA", List.of("FIRE", "WATER")));
    }

    @Test
    void emptyPatternListNeverMatches() {
        assertFalse(Utilities.advancedStringCompare("ANYTHING", List.of()));
    }
}
