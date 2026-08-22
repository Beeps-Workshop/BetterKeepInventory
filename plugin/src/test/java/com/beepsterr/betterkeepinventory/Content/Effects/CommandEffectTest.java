package com.beepsterr.betterkeepinventory.Content.Effects;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.support.NoopLogger;
import com.beepsterr.betterkeepinventory.support.TestContexts;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CommandEffect dispatches configured commands (with %placeholder% substitution) a tick after
 * death. We register a capturing command to assert both that it ran and that placeholders were
 * substituted, and advance ticks since dispatch is scheduled.
 */
class CommandEffectTest {

    private ServerMock server;
    private PlayerMock player;
    private final List<String[]> captured = new ArrayList<>();

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        MockBukkit.load(BetterKeepInventory.class);
        player = server.addPlayer();

        captured.clear();
        server.getCommandMap().register("bkitest", new Command("capture") {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                captured.add(args);
                return true;
            }
        });
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static CommandEffect effect(String command, String executor) {
        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("on_death", List.of(command));
        cfg.set("executor", executor);
        return new CommandEffect(cfg);
    }

    private static CommandEffect respawnEffect(String command, String executor) {
        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("on_respawn", List.of(command));
        cfg.set("executor", executor);
        return new CommandEffect(cfg);
    }

    @Test
    void consoleCommandRunsWithPlaceholdersSubstituted() {
        effect("capture %player%", "CONSOLE").onDeath(TestContexts.death(player));
        server.getScheduler().performTicks(3); // dispatch is scheduled one tick out

        assertEquals(1, captured.size(), "the configured command should have been dispatched once");
        assertEquals(player.getName(), captured.get(0)[0], "%player% should be replaced with the player's name");
    }

    @Test
    void playerExecutorRunsCommandWithPlaceholdersSubstituted() {
        effect("capture %player%", "PLAYER").onDeath(TestContexts.death(player));
        server.getScheduler().performTicks(3); // dispatch is scheduled one tick out

        assertEquals(1, captured.size(), "the configured command should have been dispatched once as the player");
        assertEquals(player.getName(), captured.get(0)[0], "%player% should be replaced with the player's name");
    }

    @Test
    void playerExecutorRunsOnRespawnCommands() {
        respawnEffect("capture %player% %world%", "PLAYER").onRespawn(TestContexts.respawn(player, null));
        server.getScheduler().performTicks(3);

        assertEquals(1, captured.size(), "on_respawn command should have been dispatched once");
        String[] args = captured.get(0);
        assertEquals(player.getName(), args[0], "%player% should be substituted on respawn");
        assertEquals("world", args[1], "%world% should be substituted on respawn");
    }

    @Test
    void supportsMultipleCoordinatePlaceholders() {
        effect("capture %world% %x% %y% %z%", "CONSOLE").onDeath(TestContexts.death(player));
        server.getScheduler().performTicks(3);

        assertEquals(1, captured.size());
        String[] args = captured.get(0);
        assertEquals("world", args[0]);
        assertTrue(args.length == 4, "world + x/y/z should all be substituted into separate args");
    }
}
