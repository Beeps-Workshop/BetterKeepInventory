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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** WorldsCondition matches the death world name via advancedStringCompare — mock server, no plugin. */
class WorldsConditionTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static WorldsCondition condition(String... nodes) {
        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("nodes", List.of(nodes));
        return new WorldsCondition(cfg);
    }

    private PlayerMock playerInWorld(String worldName) {
        var world = server.addSimpleWorld(worldName);
        PlayerMock player = server.addPlayer();
        player.teleport(world.getSpawnLocation());
        return player;
    }

    @Test
    void exactWorldMatches() {
        assertTrue(condition("world").check(TestContexts.death(playerInWorld("world"))));
    }

    @Test
    void wildcardMatches() {
        assertTrue(condition("world_*").check(TestContexts.death(playerInWorld("world_nether"))));
    }

    @Test
    void differentWorldDoesNotMatch() {
        assertFalse(condition("nether").check(TestContexts.death(playerInWorld("world"))));
    }

    @Test
    void negationExcludesNamedWorld() {
        assertFalse(condition("!world").check(TestContexts.death(playerInWorld("world"))));
        assertTrue(condition("!world").check(TestContexts.death(playerInWorld("nether"))));
    }

    @Test
    void emptyNodeListNeverMatches() {
        assertFalse(condition().check(TestContexts.death(playerInWorld("world"))));
    }
}
