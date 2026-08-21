package com.beepsterr.betterkeepinventory.api.Types;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and evaluates numeric range expressions.
 * <p>
 * Supported formats:
 * <ul>
 *   <li>{@code < 0} or {@code <0} - below 0</li>
 *   <li>{@code > 64} or {@code >64} - above 64</li>
 *   <li>{@code <= 10} or {@code <=10} - at or below 10</li>
 *   <li>{@code >= 5} or {@code >=5} - at or above 5</li>
 *   <li>{@code 0..64} - between 0 and 64 (inclusive)</li>
 *   <li>{@code 10} - exactly 10</li>
 * </ul>
 */
public class NumberRange {

    private static final Pattern COMPARISON_PATTERN = Pattern.compile("^\\s*([<>]=?)\\s*(-?[\\d.]+)\\s*$");
    private static final Pattern RANGE_PATTERN = Pattern.compile("^\\s*(-?[\\d.]+)\\s*\\.\\.\\s*(-?[\\d.]+)\\s*$");
    private static final Pattern EXACT_PATTERN = Pattern.compile("^\\s*(-?[\\d.]+)\\s*$");

    private final double min;
    private final double max;
    private final boolean minInclusive;
    private final boolean maxInclusive;

    private NumberRange(double min, double max, boolean minInclusive, boolean maxInclusive) {
        this.min = min;
        this.max = max;
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
    }

    /**
     * Parse a range expression string.
     *
     * @param expression The range expression (e.g., "< 0", "> 64", "0..64", "10")
     * @return A NumberRange that can test values
     * @throws IllegalArgumentException if the expression is invalid
     */
    public static NumberRange parse(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Range expression cannot be empty");
        }

        // Try comparison operators: < > <= >=
        Matcher compMatcher = COMPARISON_PATTERN.matcher(expression);
        if (compMatcher.matches()) {
            String operator = compMatcher.group(1);
            double value = Double.parseDouble(compMatcher.group(2));

            return switch (operator) {
                case "<" -> new NumberRange(Double.NEGATIVE_INFINITY, value, true, false);
                case "<=" -> new NumberRange(Double.NEGATIVE_INFINITY, value, true, true);
                case ">" -> new NumberRange(value, Double.POSITIVE_INFINITY, false, true);
                case ">=" -> new NumberRange(value, Double.POSITIVE_INFINITY, true, true);
                default -> throw new IllegalArgumentException("Unknown operator: " + operator);
            };
        }

        // Try range: min..max
        Matcher rangeMatcher = RANGE_PATTERN.matcher(expression);
        if (rangeMatcher.matches()) {
            double minVal = Double.parseDouble(rangeMatcher.group(1));
            double maxVal = Double.parseDouble(rangeMatcher.group(2));
            return new NumberRange(minVal, maxVal, true, true);
        }

        // Try exact value
        Matcher exactMatcher = EXACT_PATTERN.matcher(expression);
        if (exactMatcher.matches()) {
            double value = Double.parseDouble(exactMatcher.group(1));
            return new NumberRange(value, value, true, true);
        }

        throw new IllegalArgumentException("Invalid range expression: " + expression);
    }

    /**
     * Test if a value falls within this range.
     */
    public boolean contains(double value) {
        boolean aboveMin = minInclusive ? value >= min : value > min;
        boolean belowMax = maxInclusive ? value <= max : value < max;
        return aboveMin && belowMax;
    }

    /**
     * Test if an integer value falls within this range.
     */
    public boolean contains(int value) {
        return contains((double) value);
    }

    @Override
    public String toString() {
        if (min == Double.NEGATIVE_INFINITY) {
            return (maxInclusive ? "<= " : "< ") + max;
        }
        if (max == Double.POSITIVE_INFINITY) {
            return (minInclusive ? ">= " : "> ") + min;
        }
        if (min == max) {
            return String.valueOf(min);
        }
        return min + ".." + max;
    }
}
