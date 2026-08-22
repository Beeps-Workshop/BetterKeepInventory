package com.beepsterr.betterkeepinventory.Content.Effects;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.Library.DeathContextImpl;
import com.beepsterr.betterkeepinventory.support.NoopLogger;
import com.beepsterr.betterkeepinventory.support.TestContexts;
import org.bukkit.Location;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.LightningStrike;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Only the decision to strike, not the strike itself.
 * <p>
 * MockBukkit does not implement {@code World#strikeLightning}, so any test that reaches it is
 * skipped rather than run -- and a permanently-skipped test reads like something waiting to be
 * fixed. What is covered here is the part that belongs to this plugin: whether the effect decides
 * to strike at all, in each phase. Whether a bolt actually lands, and where, is the server's job;
 * it would need the end-to-end suite, which is not worth booting a server for a cosmetic effect.
 */
class LightningEffectTest {

    private ServerMock server;
    private WorldMock world;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        MockBukkit.load(BetterKeepInventory.class);
        player = server.addPlayer();
        player.teleport(new Location(world, 100, 64, 100));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static LightningEffect effect(boolean onDeath, boolean onRespawn) {
        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("on_death", onDeath);
        cfg.set("on_respawn", onRespawn);
        return new LightningEffect(cfg);
    }

    private int strikes() {
        return world.getEntitiesByClass(LightningStrike.class).size();
    }

    @Test
    void doesNotStrikeOnDeathWhenTurnedOff() {
        effect(false, false).onDeath(TestContexts.death(player));

        assertEquals(0, strikes());
    }

    @Test
    void doesNotStrikeOnRespawnByDefault() {
        // on_respawn defaults to false, so a rule that only asks for a death strike must not
        // fire a second one when the player comes back.
        DeathContextImpl ctx = TestContexts.death(player);
        ctx.enterRespawnPhase(player, null, new NoopLogger());

        new LightningEffect(new MemoryConfiguration()).onRespawn(ctx);
        server.getScheduler().performTicks(10);

        assertEquals(0, strikes());
    }

    @Test
    void doesNotScheduleAnythingWhenRespawnStrikesAreOff() {
        DeathContextImpl ctx = TestContexts.death(player);
        ctx.enterRespawnPhase(player, null, new NoopLogger());

        effect(true, false).onRespawn(ctx);
        server.getScheduler().performTicks(10);

        assertEquals(0, strikes(), "on_death being true must not leak into the respawn phase");
    }
}
