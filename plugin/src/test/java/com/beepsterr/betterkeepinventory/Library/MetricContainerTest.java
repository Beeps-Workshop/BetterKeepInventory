package com.beepsterr.betterkeepinventory.Library;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Boundary test for the {@code rules_count} bStats bucket. The chart reports a
 * coarse scale instead of a raw number, so the only thing worth pinning down is
 * where one bucket ends and the next begins.
 */
class MetricContainerTest {

    @Test
    void noRulesReportsNone() {
        assertEquals("None", MetricContainer.ruleCountScale(0));
    }

    @Test
    void negativeCountIsTreatedAsNone() {
        // Should be unreachable, but the scale must not fall through to "Extreme".
        assertEquals("None", MetricContainer.ruleCountScale(-1));
    }

    @Test
    void oneRuleReportsSingle() {
        assertEquals("Single", MetricContainer.ruleCountScale(1));
    }

    @Test
    void twoToThreeRulesReportLight() {
        assertEquals("Light", MetricContainer.ruleCountScale(2));
        assertEquals("Light", MetricContainer.ruleCountScale(3));
    }

    @Test
    void fourToNineRulesReportMedium() {
        assertEquals("Medium", MetricContainer.ruleCountScale(4));
        assertEquals("Medium", MetricContainer.ruleCountScale(9));
    }

    @Test
    void tenToTwentyRulesReportHeavy() {
        assertEquals("Heavy", MetricContainer.ruleCountScale(10));
        assertEquals("Heavy", MetricContainer.ruleCountScale(20));
    }

    @Test
    void twentyOneOrMoreRulesReportExtreme() {
        assertEquals("Extreme", MetricContainer.ruleCountScale(21));
        assertEquals("Extreme", MetricContainer.ruleCountScale(500));
    }
}
