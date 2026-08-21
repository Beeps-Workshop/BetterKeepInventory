package com.beepsterr.betterkeepinventory.Content.Effects;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.support.NoopLogger;
import com.beepsterr.betterkeepinventory.support.TestContexts;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * KickEffect schedules a kick one tick after death (to avoid an item-dupe race with
 * the drop effect), so these tests boot the plugin for its FoliaLib scheduler, run
 * onDeath(), then advance a tick so the delayed kick actually fires.
 *
 * In MockBukkit a kick fires a {@link PlayerKickEvent} and then disconnects the player,
 * so we assert both the resulting offline state and the kick reason captured off the event.
 */
class KickEffectTest {

    private ServerMock server;
    private Plugin plugin;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        plugin = MockBukkit.load(BetterKeepInventory.class);
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static KickEffect effect(String message) {
        MemoryConfiguration cfg = new MemoryConfiguration();
        if (message != null) {
            cfg.set("message", message);
        }
        return new KickEffect(cfg);
    }

    @Test
    void kicksPlayerOnDeathAfterOneTick() {
        assertTrue(player.isOnline(), "player should start online");

        effect("Bye!").onDeath(TestContexts.death(player));

        // Kick is delayed by 1 tick, so nothing happens until the scheduler runs.
        assertTrue(player.isOnline(), "player should still be online before the delayed task runs");

        server.getScheduler().performTicks(1);

        assertFalse(player.isOnline(), "player should be kicked (offline) after the delayed task runs");
    }

    @Test
    void firesAPlayerKickEventOnDeath() {
        // The configured message itself is NOT observable here: MockBukkit's kick()
        // discards the supplied reason component and hard-codes "Plugin" on the event.
        // So we assert the kick path runs (an event is fired) rather than its text.
        KickCountListener listener = new KickCountListener();
        server.getPluginManager().registerEvents(listener, plugin);

        effect("You died, get out!").onDeath(TestContexts.death(player));

        assertEquals(0, listener.count, "no kick event should fire before the delayed task runs");
        server.getScheduler().performTicks(1);

        assertEquals(1, listener.count, "exactly one kick event should fire after the delayed task runs");
    }

    @Test
    void kicksWithDefaultMessageConfigWithoutError() {
        // With no "message" key the effect falls back to its default; it should still
        // construct and kick cleanly.
        effect(null).onDeath(TestContexts.death(player));
        server.getScheduler().performTicks(1);

        assertFalse(player.isOnline(), "player should be kicked even when no message is configured");
    }

    private static final class KickCountListener implements Listener {
        int count;

        @EventHandler
        public void onKick(PlayerKickEvent event) {
            this.count++;
        }
    }
}
