package com.beepsterr.betterkeepinventory.Library;

import com.beepsterr.betterkeepinventory.api.Exceptions.ConditionParseError;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parsing a placeholder condition. No PlaceholderAPI and no server needed -- only
 * {@code test(Player)} reaches out to PAPI, and everything interesting happens before that.
 * <p>
 * The operator accepts a lot of spellings. Each alias is a branch that either maps to the right
 * comparison or silently does the wrong one, which is the kind of thing nobody notices until a
 * rule quietly stops matching.
 */
class PlaceholderItemTest {

    private static MemoryConfiguration config(String placeholder, String operator, String value) {
        MemoryConfiguration cfg = new MemoryConfiguration();
        if (placeholder != null) cfg.set("placeholder", placeholder);
        if (operator != null) cfg.set("operator", operator);
        if (value != null) cfg.set("value", value);
        return cfg;
    }

    private static PlaceholderItem parse(String operator) throws ConditionParseError {
        return new PlaceholderItem(config("some_placeholder", operator, "x"));
    }

    // --- operator aliases ---------------------------------------------------------------------

    @ParameterizedTest(name = "\"{0}\" means {1}")
    @CsvSource({
            "EQUALS,                  EQUALS",
            "==,                      EQUALS",
            "=,                       EQUALS",
            "!=,                      NOT_EQUALS",
            "<>,                      NOT_EQUALS",
            "NOT,                     NOT_EQUALS",
            "INCLUDES,                CONTAINS",
            "CONTAINS,                CONTAINS",
            ".*,                      STARTS_WITH",
            "BEGINS_WITH,             STARTS_WITH",
            "STARTS_WITH,             STARTS_WITH",
            "*.,                      ENDS_WITH",
            "STOPS_WITH,              ENDS_WITH",
            "ENDS_WITH,               ENDS_WITH",
            ">,                       GREATER_THAN",
            "GREATER_THAN,            GREATER_THAN",
            ">=,                      GREATER_THAN_OR_EQUALS",
            "GREATER_THAN_OR_EQUALS,  GREATER_THAN_OR_EQUALS",
            "<,                       LESS_THAN",
            "LESS_THAN,               LESS_THAN",
            "<=,                      LESS_THAN_OR_EQUALS",
            "LESS_THAN_OR_EQUALS,     LESS_THAN_OR_EQUALS",
    })
    void operatorAliasesMapToTheRightComparison(String written, String expected) throws ConditionParseError {
        // toString already renders the parsed operator, so the assertion needs no accessor that
        // exists only for tests.
        assertTrue(parse(written).toString().contains("operator=" + expected + ","),
                "the alias '" + written + "' should mean " + expected + ", got: " + parse(written));
    }

    @ParameterizedTest
    @ValueSource(strings = {"equals", "Equals", "eQuAlS"})
    void operatorsAreCaseInsensitive(String written) {
        assertDoesNotThrow(() -> parse(written));
    }

    @Test
    void anUnknownOperatorIsRejectedWithTheListOfValidOnes() {
        ConditionParseError error = assertThrows(ConditionParseError.class, () -> parse("APPROXIMATELY"));
        assertTrue(error.getMessage().contains("must be one of"),
                "the error should tell the author what is allowed");
    }

    // --- required fields ----------------------------------------------------------------------

    @Test
    void placeholderIsRequired() {
        assertThrows(ConditionParseError.class,
                () -> new PlaceholderItem(config(null, "EQUALS", "x")));
    }

    @Test
    void operatorIsRequired() {
        assertThrows(ConditionParseError.class,
                () -> new PlaceholderItem(config("some_placeholder", null, "x")));
    }

    @Test
    void valueIsRequired() {
        assertThrows(ConditionParseError.class,
                () -> new PlaceholderItem(config("some_placeholder", "EQUALS", null)));
    }

    // --- placeholder shape --------------------------------------------------------------------

    @Test
    void abarePlaceholderIsWrappedInPercentSigns() throws ConditionParseError {
        // PlaceholderAPI needs the %...% form; config authors routinely leave them off.
        PlaceholderItem item = new PlaceholderItem(config("vault_eco_balance", "EQUALS", "0"));

        assertTrue(item.toString().contains("placeholder='%vault_eco_balance%'"),
                "got: " + item);
    }

    @Test
    void anAlreadyWrappedPlaceholderIsLeftAlone() throws ConditionParseError {
        PlaceholderItem item = new PlaceholderItem(config("%vault_eco_balance%", "EQUALS", "0"));

        assertTrue(item.toString().contains("placeholder='%vault_eco_balance%'"),
                "it should not gain a second pair of percent signs; got: " + item);
    }
}
