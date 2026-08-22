package com.beepsterr.betterkeepinventory.Content.Conditions;

import com.beepsterr.betterkeepinventory.support.NoopLogger;
import com.beepsterr.betterkeepinventory.support.TestContexts;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Layer 2: condition test on a mock server, but WITHOUT loading the plugin.
 * CauseCondition only reads the player's last damage cause, so all it needs is
 * a PlayerMock — no BetterKeepInventory singleton required.
 */
class CauseConditionTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private CauseCondition condition(String... causes) {
        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("nodes", List.of(causes));
        return new CauseCondition(cfg);
    }

    private PlayerMock playerKilledBy(DamageCause cause) {
        PlayerMock player = server.addPlayer();
        player.setLastDamageCause(new EntityDamageEvent(player, cause, 10.0));
        return player;
    }

    @Test
    void matchesConfiguredCause() {
        PlayerMock player = playerKilledBy(DamageCause.LAVA);
        assertTrue(condition("LAVA", "FIRE").check(TestContexts.death(player)));
    }

    @Test
    void doesNotMatchOtherCause() {
        PlayerMock player = playerKilledBy(DamageCause.FALL);
        assertFalse(condition("LAVA", "FIRE").check(TestContexts.death(player)));
    }

    @Test
    void noDamageCauseDoesNotMatch() {
        PlayerMock player = server.addPlayer(); // never took damage
        assertFalse(condition("LAVA").check(TestContexts.death(player)));
    }
}
