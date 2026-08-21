package com.beepsterr.betterkeepinventory.Content.Conditions;

import com.beepsterr.betterkeepinventory.support.NoopLogger;
import com.beepsterr.betterkeepinventory.support.TestContexts;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PermissionsCondition checks player.hasPermission with '!' negation — mock server + a mock plugin for attachments. */
class PermissionsConditionTest {

    private ServerMock server;
    private Plugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin(); // needed to grant permission attachments
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static PermissionsCondition condition(String... nodes) {
        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("nodes", List.of(nodes));
        return new PermissionsCondition(cfg);
    }

    private PlayerMock playerWith(String... perms) {
        PlayerMock player = server.addPlayer();
        for (String p : perms) {
            player.addAttachment(plugin, p, true);
        }
        return player;
    }

    @Test
    void matchesWhenPlayerHasPermission() {
        assertTrue(condition("bki.vip").check(TestContexts.death(playerWith("bki.vip"))));
    }

    @Test
    void doesNotMatchWhenPlayerLacksPermission() {
        assertFalse(condition("bki.vip").check(TestContexts.death(playerWith())));
    }

    @Test
    void negatedNodeMatchesWhenPlayerLacksPermission() {
        assertTrue(condition("!bki.vip").check(TestContexts.death(playerWith())));
    }

    @Test
    void negatedNodeDoesNotMatchWhenPlayerHasPermission() {
        assertFalse(condition("!bki.vip").check(TestContexts.death(playerWith("bki.vip"))));
    }

    @Test
    void emptyNodeListNeverMatches() {
        assertFalse(condition().check(TestContexts.death(playerWith("bki.vip"))));
    }
}
