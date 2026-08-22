package com.beepsterr.betterkeepinventory.Library;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Conditions are checked once, during the death, and the answer is remembered for the respawn.
 * <p>
 * Several conditions describe the circumstances of the death -- which world, which light level,
 * which height. Asking them again about a player who has since respawned somewhere else gives a
 * different answer to the same question, so a rule could fire its death-phase effects and then
 * decline to run the respawn half of the same death.
 */
class ConditionPhaseTest {

    private ServerMock server;
    private WorldMock overworld;
    private WorldMock nether;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        overworld = server.addSimpleWorld("world");
        nether = server.addSimpleWorld("world_nether");
        MockBukkit.load(BetterKeepInventory.class);
        player = server.addPlayer();
        player.teleport(overworld.getSpawnLocation());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** A rule gated on the overworld, whose only effect does its work at respawn. */
    private ConfigRule worldGatedHungerRule() {
        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("name", "world-gated");
        cfg.set("enabled", true);
        cfg.set("conditions.worlds.nodes", List.of("world"));
        cfg.set("effects.hunger.min", 0);
        cfg.set("effects.hunger.amount", 6);
        return new ConfigRule(cfg, null, null);
    }

    @Test
    void aRuleThatMatchedAtDeathStillRunsAtRespawnFromAnotherWorld() {
        player.setFoodLevel(20);
        ConfigRule rule = worldGatedHungerRule();
        DeathContextImpl ctx = TestContexts.death(player, null);

        rule.trigger(ctx);

        // The server puts them somewhere else entirely -- a nether death respawning in the
        // overworld, or the other way about.
        player.teleport(new Location(nether, 0, 64, 0));
        ctx.enterRespawnPhase(player, null, new NoopLogger());
        rule.trigger(ctx);

        server.getScheduler().performTicks(6);
        assertEquals(14, player.getFoodLevel(),
                "the rule matched this death, so both halves of it should have run");
    }

    @Test
    void aRuleThatDidNotMatchAtDeathDoesNotRunAtRespawnEither() {
        player.setFoodLevel(20);
        player.teleport(new Location(nether, 0, 64, 0));   // died in the nether

        ConfigRule rule = worldGatedHungerRule();           // gated on the overworld
        DeathContextImpl ctx = TestContexts.death(player, null);

        rule.trigger(ctx);

        player.teleport(overworld.getSpawnLocation());      // respawns in the overworld
        ctx.enterRespawnPhase(player, null, new NoopLogger());
        rule.trigger(ctx);

        server.getScheduler().performTicks(6);
        assertEquals(20, player.getFoodLevel(),
                "the rule did not match this death, so respawning where it would have matched "
                + "must not bring it back");
    }

    @Test
    void anUnconditionalRuleRunsInBothPhases() {
        player.setFoodLevel(20);
        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("name", "always");
        cfg.set("enabled", true);
        cfg.set("effects.hunger.min", 0);
        cfg.set("effects.hunger.amount", 6);
        ConfigRule rule = new ConfigRule(cfg, null, null);

        DeathContextImpl ctx = TestContexts.death(player, null);
        rule.trigger(ctx);
        ctx.enterRespawnPhase(player, null, new NoopLogger());
        rule.trigger(ctx);

        server.getScheduler().performTicks(6);
        assertEquals(14, player.getFoodLevel());
    }
}
