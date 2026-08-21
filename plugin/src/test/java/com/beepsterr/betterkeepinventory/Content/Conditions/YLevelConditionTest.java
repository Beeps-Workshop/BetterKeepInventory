package com.beepsterr.betterkeepinventory.Content.Conditions;

import com.beepsterr.betterkeepinventory.Library.DeathContextImpl;
import com.beepsterr.betterkeepinventory.support.NoopLogger;
import com.beepsterr.betterkeepinventory.support.TestContexts;
import org.bukkit.Location;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** YLevelCondition tests the death Y against a NumberRange — mock server, no plugin. */
class YLevelConditionTest {

    private ServerMock server;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static YLevelCondition condition(String range) {
        MemoryConfiguration cfg = new MemoryConfiguration();
        if (range != null) cfg.set("range", range);
        return new YLevelCondition(cfg);
    }

    private PlayerMock playerAtY(double y) {
        PlayerMock player = server.addPlayer();
        player.teleport(new Location(world, 0, y, 0));
        return player;
    }

    @Test
    void insideRangeMatches() {
        assertTrue(condition("50..150").check(TestContexts.death(playerAtY(100))));
    }

    @Test
    void outsideRangeDoesNotMatch() {
        assertFalse(condition("0..10").check(TestContexts.death(playerAtY(100))));
    }

    @Test
    void comparisonExpressions() {
        assertTrue(condition("> 90").check(TestContexts.death(playerAtY(100))));
        assertFalse(condition("< 0").check(TestContexts.death(playerAtY(100))));
    }

    @Test
    void belowZeroVoidDeath() {
        assertTrue(condition("< 0").check(TestContexts.death(playerAtY(-20))));
    }

    @Test
    void defaultRangeIsFullBuildHeight() {
        // default "0..320" when no range configured
        assertTrue(condition(null).check(TestContexts.death(playerAtY(100))));
        assertFalse(condition(null).check(TestContexts.death(playerAtY(400))));
    }

    /**
     * The condition asks where the player <em>died</em>, so it has to keep meaning that once they
     * have respawned somewhere else. Reading the player's live location -- which is what this
     * used to do -- made a rule evaluate against the death Y in the death phase and against the
     * spawn point in the respawn phase, for the same death.
     */
    @Test
    void usesTheDeathYEvenAfterRespawningElsewhere() {
        PlayerMock player = playerAtY(5);                       // died deep underground
        DeathContextImpl ctx = TestContexts.death(player);

        player.teleport(new Location(world, 0, 300, 0));        // server moves them to spawn
        ctx.enterRespawnPhase(player, null, new NoopLogger());

        assertTrue(condition("0..64").check(ctx), "should still match where they died");
        assertFalse(condition("250..320").check(ctx), "must not match where they respawned");
    }
}
