package com.beepsterr.betterkeepinventory.Library;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.api.BkiLogger;
import com.beepsterr.betterkeepinventory.support.TestContexts;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the two things {@link StructuredLogger} is for: what a console line looks like, and what
 * ends up in the debug report.
 * <p>
 * The console side is read off a {@link Handler} attached to the plugin's own logger, and the
 * stored side off the report the {@code Debugger} writes -- both the real paths, rather than the
 * private rendering methods, because the thing worth pinning is that the two disagree in exactly
 * one way: storage happens whatever the level, and the console does not.
 */
class StructuredLoggerTest {

    private ServerMock server;
    private BetterKeepInventory plugin;
    private WorldMock world;
    private PlayerMock player;

    private final List<LogRecord> console = new ArrayList<>();
    private Handler capture;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        plugin = MockBukkit.load(BetterKeepInventory.class);
        player = server.addPlayer();
        player.teleport(world.getSpawnLocation());

        capture = new Handler() {
            @Override public void publish(LogRecord record) { console.add(record); }
            @Override public void flush() { }
            @Override public void close() { }
        };
        capture.setLevel(Level.ALL);
        plugin.getLogger().addHandler(capture);
        plugin.getLogger().setLevel(Level.ALL);

        console.clear();
    }

    @AfterEach
    void tearDown() {
        plugin.getLogger().removeHandler(capture);
        MockBukkit.unmock();
    }

    /** The most recent line the plugin logger was asked to print. */
    private String lastConsoleLine() {
        assertFalse(console.isEmpty(), "expected something to reach the console");
        return console.get(console.size() - 1).getMessage();
    }

    /** The most recent record the debugger stored, parsed back out of the written report. */
    private JsonObject lastStoredRecord() throws Exception {
        plugin.debugger.write();
        String report = Files.readString(new java.io.File(plugin.getDataFolder(), "debugger.log").toPath());

        String logs = report.substring(report.indexOf("=== LOGS"));
        String last = logs.lines()
                .filter(line -> line.startsWith("{"))
                .reduce((first, second) -> second)
                .orElse(null);

        assertNotNull(last, "expected a stored JSON record");
        return JsonParser.parseString(last).getAsJsonObject();
    }

    // ------------------------------------------------------------------ console rendering

    @Test
    void pluginLinesCarryNoPrefix() {
        StructuredLogger.plugin().info("starting up");
        assertEquals("starting up", lastConsoleLine());
    }

    @Test
    void deathLinesArePrefixedWithThePlayerAndTheDeathId() {
        DeathContextImpl ctx = TestContexts.death(player);

        ctx.logger().info("conditions met");

        assertEquals("[player=" + player.getUniqueId() + "] [" + ctx.deathId() + "] conditions met",
                lastConsoleLine());
    }

    @Test
    void everyDeathGetsItsOwnId() {
        assertFalse(TestContexts.death(player).deathId()
                .equals(TestContexts.death(player).deathId()));
    }

    // ------------------------------------------------------------------ level handling

    @Test
    void debugLinesAreNotPrintedWhenDebugIsOff() {
        // The default test configuration has debug off, so narration stays at FINE -- which the
        // console handler on a real server drops.
        StructuredLogger.plugin().debug("narration");
        assertEquals(Level.FINE, console.get(console.size() - 1).getLevel());
    }

    @Test
    void warningsArePrintedWhateverTheDebugSetting() {
        StructuredLogger.plugin().warn("something is wrong");
        assertEquals(Level.WARNING, console.get(console.size() - 1).getLevel());
    }

    // ------------------------------------------------------------------ storage

    @Test
    void debugLinesAreStoredEvenThoughTheConsoleHidesThem() throws Exception {
        // The point of the whole split: a report from someone who never turned debug on still
        // carries the narration of the death they are complaining about.
        StructuredLogger.plugin().debug("narration nobody saw");

        JsonObject stored = lastStoredRecord();
        assertEquals("narration nobody saw", stored.get("message").getAsString());
        assertEquals("FINE", stored.get("level").getAsString());
    }

    @Test
    void storedRecordsCarryTheContextFields() throws Exception {
        DeathContextImpl ctx = TestContexts.death(player);

        ctx.logger().debug("moved stacks to drops", Map.of("count", 12));

        JsonObject stored = lastStoredRecord();
        assertEquals(ctx.deathId(), stored.get("death_id").getAsString());
        assertEquals(player.getUniqueId().toString(), stored.get("uuid").getAsString());
        assertEquals(player.getName(), stored.get("player").getAsString());
        assertEquals("DEATH", stored.get("phase").getAsString());
        assertEquals("world", stored.get("world").getAsString());
        assertEquals(12, stored.get("count").getAsInt());
        assertEquals("moved stacks to drops", stored.get("message").getAsString());
    }

    @Test
    void thePhaseFieldFollowsTheContextRatherThanTheLogger() throws Exception {
        // The logger is built once and never rebuilt, so this only holds because it reads the
        // context at log time rather than copying its fields at construction.
        DeathContextImpl ctx = TestContexts.death(player);
        ctx.enterRespawnPhase(player, null);

        ctx.logger().debug("restoring hunger");

        assertEquals("RESPAWN", lastStoredRecord().get("phase").getAsString());
    }

    // ------------------------------------------------------------------ bound fields

    @Test
    void withAddsFieldsWithoutTouchingTheLoggerItCameFrom() throws Exception {
        DeathContextImpl ctx = TestContexts.death(player);
        BkiLogger scoped = ctx.logger().with(Map.of("rule", "Nether deaths"));

        scoped.debug("matched");
        assertEquals("Nether deaths", lastStoredRecord().get("rule").getAsString());

        ctx.logger().debug("unscoped");
        assertFalse(lastStoredRecord().has("rule"), "with() must not mutate the logger it came from");
    }

    @Test
    void callFieldsWinOverBoundFields() throws Exception {
        BkiLogger scoped = StructuredLogger.plugin().with(Map.of("stage", "parse"));

        scoped.debug("late", Map.of("stage", "apply"));

        assertEquals("apply", lastStoredRecord().get("stage").getAsString());
    }

    @Test
    void reservedKeysCannotBeOverwrittenByAField() throws Exception {
        StructuredLogger.plugin().debug("the real message", Map.of("message", "an impostor"));

        JsonObject stored = lastStoredRecord();
        assertEquals("the real message", stored.get("message").getAsString());
        assertTrue(stored.has("ts"));
    }
}
