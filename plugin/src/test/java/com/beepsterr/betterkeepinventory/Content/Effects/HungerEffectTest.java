package com.beepsterr.betterkeepinventory.Content.Effects;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.Library.DeathContextImpl;
import com.beepsterr.betterkeepinventory.support.NoopLogger;
import com.beepsterr.betterkeepinventory.support.TestContexts;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HungerEffect stores a reduced hunger on death and re-applies it a few ticks after respawn,
 * so this loads the plugin (for the scheduler) and advances ticks.
 */
class HungerEffectTest {

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

    private static HungerEffect effect(int min, int amount) {
        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("min", min);
        cfg.set("amount", amount);
        return new HungerEffect(cfg);
    }

    @Test
    void restoresReducedHungerAfterRespawn() {
        player.setFoodLevel(20);
        HungerEffect effect = effect(0, 6);

        DeathContextImpl ctx = TestContexts.death(player);
        effect.onDeath(ctx);
        ctx.enterRespawnPhase(player, null, new NoopLogger());
        effect.onRespawn(ctx);
        server.getScheduler().performTicks(6); // respawn re-applies on a delayed task

        assertEquals(14, player.getFoodLevel());
    }

    @Test
    void neverDropsBelowConfiguredMinimum() {
        player.setFoodLevel(5);
        HungerEffect effect = effect(2, 6); // 5 - 6 = -1, floored to min 2

        DeathContextImpl ctx = TestContexts.death(player);
        effect.onDeath(ctx);
        ctx.enterRespawnPhase(player, null, new NoopLogger());
        effect.onRespawn(ctx);
        server.getScheduler().performTicks(6);

        assertEquals(2, player.getFoodLevel());
    }

    /**
     * The reason the saved value lives on the context rather than in a static map: an effect that
     * saves on death but never reaches its respawn phase -- a rule paired with `kick` or `ban`,
     * or one whose conditions stop matching -- used to leave the entry behind, and the next death
     * that did reach respawn would pick up that stale value.
     */
    @Test
    void aDeathThatNeverRespawnsDoesNotLeakIntoTheNextOne() {
        HungerEffect effect = effect(0, 6);

        player.setFoodLevel(20);
        DeathContextImpl abandoned = TestContexts.death(player);
        effect.onDeath(abandoned);          // kicked, banned, or the rule stopped matching

        player.setFoodLevel(10);
        DeathContextImpl later = TestContexts.death(player);
        effect.onDeath(later);
        later.enterRespawnPhase(player, null, new NoopLogger());
        effect.onRespawn(later);
        server.getScheduler().performTicks(6);

        assertEquals(4, player.getFoodLevel(), "should reflect the second death (10 - 6), not the first");
    }

    @Test
    void respawnWithoutAMatchingDeathDoesNothing() {
        player.setFoodLevel(20);

        HungerEffect effect = effect(0, 6);
        effect.onRespawn(TestContexts.respawn(player, null));
        server.getScheduler().performTicks(6);

        assertEquals(20, player.getFoodLevel(), "nothing was saved, so nothing should be applied");
    }
}
