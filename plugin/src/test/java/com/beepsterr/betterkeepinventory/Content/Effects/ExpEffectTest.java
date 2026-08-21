package com.beepsterr.betterkeepinventory.Content.Effects;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.support.NoopLogger;
import com.beepsterr.betterkeepinventory.support.TestContexts;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.bukkit.entity.ExperienceOrb;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ExpEffect removes XP levels on death. These cover the DELETE path (deterministic level math)
 * across the SIMPLE / PERCENTAGE / ALL modes; min==max makes the RNG term drop out.
 */
class ExpEffectTest {

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

    private static ExpEffect effect(String mode, String how, double min, double max) {
        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("mode", mode);
        cfg.set("how", how);
        cfg.set("min", min);
        cfg.set("max", max);
        return new ExpEffect(cfg);
    }

    @Test
    void allModeDeletesEveryLevel() {
        player.setLevel(30);
        effect("ALL", "DELETE", 0, 0).onDeath(TestContexts.death(player));
        assertEquals(0, player.getLevel());
    }

    @Test
    void percentageModeDeletesShare() {
        player.setLevel(30);
        effect("PERCENTAGE", "DELETE", 50, 50).onDeath(TestContexts.death(player)); // lose 50% of 30 = 15
        assertEquals(15, player.getLevel());
    }

    @Test
    void simpleModeDeletesFixedAmount() {
        player.setLevel(10);
        effect("SIMPLE", "DELETE", 5, 5).onDeath(TestContexts.death(player)); // lose exactly 5
        assertEquals(5, player.getLevel());
    }

    @Test
    void noLevelsToLoseLeavesPlayerUnchanged() {
        player.setLevel(0);
        effect("ALL", "DELETE", 0, 0).onDeath(TestContexts.death(player)); // 0 levels -> early return
        assertEquals(0, player.getLevel());
    }

    private Collection<ExperienceOrb> orbsInWorld() {
        return player.getWorld().getEntitiesByClass(ExperienceOrb.class);
    }

    @Test
    void allModeDropSpawnsOrbAndZeroesLevel() {
        player.setLevel(30);
        // ALL -> loses every level; DROP branch takes playerExpLevel <= levelsToLose -> setLevel(0).
        effect("ALL", "DROP", 0, 0).onDeath(TestContexts.death(player));

        assertEquals(0, player.getLevel());
        Collection<ExperienceOrb> orbs = orbsInWorld();
        assertEquals(1, orbs.size(), "DROP should spawn exactly one ExperienceOrb");
        assertTrue(orbs.iterator().next().getExperience() > 0, "orb should carry the dropped experience");
    }

    @Test
    void percentageModeDropSpawnsOrbAndLowersLevel() {
        player.setLevel(30);
        // PERCENTAGE 50% of 30 = 15 levels lost; 30 > 15 -> partial DROP branch, setLevel(15).
        effect("PERCENTAGE", "DROP", 50, 50).onDeath(TestContexts.death(player));

        assertEquals(15, player.getLevel());
        Collection<ExperienceOrb> orbs = orbsInWorld();
        assertEquals(1, orbs.size(), "DROP should spawn exactly one ExperienceOrb");
        assertTrue(orbs.iterator().next().getExperience() > 0, "orb should carry the dropped experience");
    }

    @Test
    void simpleModeDropSpawnsOrbAndLowersLevel() {
        player.setLevel(10);
        // SIMPLE min==max -> lose exactly 5 levels; 10 > 5 -> partial DROP branch, setLevel(5).
        effect("SIMPLE", "DROP", 5, 5).onDeath(TestContexts.death(player));

        assertEquals(5, player.getLevel());
        Collection<ExperienceOrb> orbs = orbsInWorld();
        assertEquals(1, orbs.size(), "DROP should spawn exactly one ExperienceOrb");
        assertTrue(orbs.iterator().next().getExperience() > 0, "orb should carry the dropped experience");
    }

    @Test
    void dropWithNoLevelsSpawnsNoOrb() {
        player.setLevel(0);
        // 0 levels -> levelsToLose < 1 -> early return, no orb spawned.
        effect("ALL", "DROP", 0, 0).onDeath(TestContexts.death(player));

        assertEquals(0, player.getLevel());
        assertFalse(orbsInWorld().iterator().hasNext(), "no orb should be spawned when there is nothing to drop");
    }

    @Test
    void dropOfAnEmptyLevelSpawnsNoOrb() {
        player.setLevel(0);
        player.setExp(0);
        // SIMPLE still wants a level gone even though the player has none, so the effect reaches the
        // DROP branch with nothing to hand out. An orb worth 0 would linger in the world forever.
        effect("SIMPLE", "DROP", 1, 1).onDeath(TestContexts.death(player));

        assertEquals(0, player.getLevel());
        assertFalse(orbsInWorld().iterator().hasNext(), "an empty drop should not spawn a 0 experience orb");
    }
}
