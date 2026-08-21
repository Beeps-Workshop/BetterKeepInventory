package com.beepsterr.betterkeepinventory.Content.Effects;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.support.NoopLogger;
import com.beepsterr.betterkeepinventory.support.TestContexts;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BanEffect parses a duration string ("5m", "30s", "2h", "1d", or bare minutes), then a
 * tick after death it adds a ban and kicks the player. MockBukkit maps
 * {@code BanList.Type.NAME} onto its profile ban list, so we assert against
 * {@link Bukkit#getBanList(BanList.Type)} after advancing one tick.
 *
 * The duration parsing is deterministic, so the ban expiry is the main thing asserted here
 * (with a generous tolerance to absorb the time spent running the scheduler).
 */
class BanEffectTest {

    private ServerMock server;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        MockBukkit.load(BetterKeepInventory.class);
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static BanEffect effect(String message, String duration) {
        MemoryConfiguration cfg = new MemoryConfiguration();
        if (message != null) {
            cfg.set("message", message);
        }
        if (duration != null) {
            cfg.set("duration", duration);
        }
        return new BanEffect(cfg);
    }

    private BanList<?> banList() {
        return Bukkit.getBanList(BanList.Type.NAME);
    }

    @Test
    void bansAndKicksPlayerOnDeathAfterOneTick() {
        assertFalse(banList().isBanned(player.getName()), "player should not be banned before death");

        effect("Banned for dying", "5m").onDeath(TestContexts.death(player));

        // Ban + kick are delayed by one tick.
        assertFalse(banList().isBanned(player.getName()), "player should not be banned before the delayed task runs");
        assertTrue(player.isOnline(), "player should still be online before the delayed task runs");

        server.getScheduler().performTicks(1);

        assertTrue(banList().isBanned(player.getName()), "player should be banned after the delayed task runs");
        assertFalse(player.isOnline(), "player should be kicked (offline) after being banned");
    }

    @Test
    void banEntryRecordsConfiguredReasonAndSource() {
        effect("No respawns for you", "5m").onDeath(TestContexts.death(player));
        server.getScheduler().performTicks(1);

        BanEntry<?> entry = banList().getBanEntry(player.getName());
        assertNotNull(entry, "a ban entry should exist for the player");
        assertEquals("No respawns for you", entry.getReason(), "ban reason should be the configured message");
        assertEquals(player.getUniqueId().toString(), entry.getSource(), "ban source should be the player's UUID");
    }

    @Test
    void defaultDurationIsFiveMinutes() {
        // No "duration" configured -> defaults to "5m".
        effect("dead", null).onDeath(TestContexts.death(player));
        server.getScheduler().performTicks(1);

        assertExpiryApprox(Duration.ofMinutes(5));
    }

    @Test
    void secondsSuffixIsParsed() {
        effect("dead", "30s").onDeath(TestContexts.death(player));
        server.getScheduler().performTicks(1);

        assertExpiryApprox(Duration.ofSeconds(30));
    }

    @Test
    void minutesSuffixIsParsed() {
        effect("dead", "15m").onDeath(TestContexts.death(player));
        server.getScheduler().performTicks(1);

        assertExpiryApprox(Duration.ofMinutes(15));
    }

    @Test
    void hoursSuffixIsParsed() {
        effect("dead", "2h").onDeath(TestContexts.death(player));
        server.getScheduler().performTicks(1);

        assertExpiryApprox(Duration.ofHours(2));
    }

    @Test
    void daysSuffixIsParsed() {
        effect("dead", "3d").onDeath(TestContexts.death(player));
        server.getScheduler().performTicks(1);

        assertExpiryApprox(Duration.ofDays(3));
    }

    @Test
    void bareNumberDefaultsToMinutes() {
        effect("dead", "45").onDeath(TestContexts.death(player));
        server.getScheduler().performTicks(1);

        assertExpiryApprox(Duration.ofMinutes(45));
    }

    @Test
    void foreverDurationCreatesNonExpiringBan() {
        // Contract (per docs Rules/Effects/Ban.md): duration "forever" = "No time limit,
        // banned until manually unbanned". This is currently RED on two counts: BanEffect
        // checks the wrong keyword ("permanent" instead of "forever"), and even that branch
        // doesn't return, so "forever" falls through to Long.parseLong("forever") and throws.
        // This asserts the documented behaviour and must stay failing until BanEffect is fixed;
        // do not weaken it to match the bug.
        effect("dead", "forever").onDeath(TestContexts.death(player));
        server.getScheduler().performTicks(1);

        BanEntry<?> entry = banList().getBanEntry(player.getName());
        assertNotNull(entry, "a 'forever' duration should still ban the player");
        assertNull(entry.getExpiration(), "a 'forever' ban must not have an expiry");
    }

    /**
     * Asserts the stored ban expiry is roughly {@code now + expected}. A 30s tolerance absorbs
     * the small amount of wall-clock time between test start and the scheduler firing the task.
     */
    private void assertExpiryApprox(Duration expected) {
        BanEntry<?> entry = banList().getBanEntry(player.getName());
        assertNotNull(entry, "a ban entry should exist for the player");

        Date expiration = entry.getExpiration();
        assertNotNull(expiration, "ban should have an expiration date");

        Instant expectedInstant = Instant.now().plus(expected);
        long deltaSeconds = Math.abs(Duration.between(expiration.toInstant(), expectedInstant).getSeconds());
        assertTrue(deltaSeconds <= 30,
                "expiry should be about " + expected + " out, but was off by " + deltaSeconds + "s");
    }
}
