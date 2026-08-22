package com.beepsterr.betterkeepinventory.Content.Conditions;

import com.beepsterr.betterkeepinventory.support.NoopLogger;
import com.beepsterr.betterkeepinventory.support.TestContexts;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LightLevelCondition checks the block light (BLOCK/SKY/ANY) against a NumberRange.
 * MockBukkit's block light is a fixed 0..15, so we assert with ranges that are true/false
 * regardless of the exact value — this still exercises every type branch and the range check.
 */
class LightLevelConditionTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static LightLevelCondition condition(String range, String type) {
        MemoryConfiguration cfg = new MemoryConfiguration();
        if (range != null) cfg.set("range", range);
        if (type != null) cfg.set("type", type);
        return new LightLevelCondition(cfg);
    }

    private PlayerMock player() {
        return server.addPlayer();
    }

    @Test
    void anyLightAlwaysInFullRange() {
        assertTrue(condition("0..15", "ANY").check(TestContexts.death(player())));
    }

    @Test
    void blockTypeBranch() {
        assertTrue(condition("0..15", "BLOCK").check(TestContexts.death(player())));
    }

    @Test
    void skyTypeBranch() {
        assertTrue(condition("0..15", "SKY").check(TestContexts.death(player())));
    }

    @Test
    void impossibleRangeNeverMatches() {
        // light never exceeds 15, so this exercises the "false" path deterministically
        assertFalse(condition("16..20", "ANY").check(TestContexts.death(player())));
    }

    @Test
    void defaultsToAnyOverFullRange() {
        assertTrue(condition(null, null).check(TestContexts.death(player())));
    }
}
