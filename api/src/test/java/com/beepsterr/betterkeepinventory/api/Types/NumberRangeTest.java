package com.beepsterr.betterkeepinventory.api.Types;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Layer 1: pure-logic test. No Bukkit, no MockBukkit — the fastest kind.
 * NumberRange backs the y_level and light_level conditions, so its parsing and
 * boundary behavior are worth locking down directly.
 */
class NumberRangeTest {

    @ParameterizedTest(name = "\"{0}\" contains {1} -> {2}")
    @CsvSource({
            // inclusive range
            "0..320,   0,   true",
            "0..320,   320, true",
            "0..320,   -1,  false",
            "0..320,   321, false",
            // negative bounds (deep-dark rule uses -64..0)
            "-64..0,   -64, true",
            "-64..0,   0,   true",
            "-64..0,   1,   false",
            // comparisons
            "< 0,      -1,  true",
            "< 0,      0,   false",
            ">= 5,     5,   true",
            ">= 5,     4,   false",
            // exact
            "10,       10,  true",
            "10,       11,  false",
    })
    void contains(String expr, int value, boolean expected) {
        assertEquals(expected, NumberRange.parse(expr).contains(value));
    }

    @Test
    void blankExpressionThrows() {
        assertThrows(IllegalArgumentException.class, () -> NumberRange.parse(""));
    }

    @Test
    void garbageExpressionThrows() {
        assertThrows(IllegalArgumentException.class, () -> NumberRange.parse("not-a-range"));
    }
}
