package com.beepsterr.betterkeepinventory.Library;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The on-disk log and the report built from it.
 * <p>
 * The plugin is loaded rather than a bare {@link Debugger} constructed, because the log directory
 * hangs off the plugin's data folder -- and MockBukkit gives each test its own, which is what
 * keeps rotation assertions from seeing another test's file.
 */
class DebuggerTest {

    private BetterKeepInventory plugin;
    private Debugger debugger;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = MockBukkit.load(BetterKeepInventory.class);
        debugger = plugin.debugger;
    }

    @AfterEach
    void tearDown() {
        debugger.close();
        MockBukkit.unmock();
    }

    private Path logDir() {
        return plugin.getDataFolder().toPath().resolve(Debugger.LOG_DIRECTORY);
    }

    private String read(String name) throws Exception {
        Path path = logDir().resolve(name);
        return Files.isRegularFile(path) ? Files.readString(path, StandardCharsets.UTF_8) : "";
    }

    /** Enough lines to push latest.log past MAX_LOG_BYTES and force at least one trim. */
    private static final int CAP_FILLING_LINES = 5000;

    private void writePastTheCap() {
        String padding = "x".repeat(2500);
        for (int i = 0; i < CAP_FILLING_LINES; i++) {
            debugger.record("{\"seq\":" + i + ",\"pad\":\"" + padding + "\"}", "line");
        }
    }

    /** How many lines this test wrote, ignoring the plugin's own startup narration. */
    private static long countMarked(String content, String marker) {
        return content.lines().filter(line -> line.contains("\"marker\":\"" + marker + "\"")).count();
    }

    /** Just the JSONL section of a report. */
    private String reportedLines() {
        String content = debugger.getContent();
        return content.substring(content.indexOf("=== LOGS"));
    }

    // ------------------------------------------------------------------ writing

    @Test
    void linesAreWrittenToLatestLog() throws Exception {
        debugger.record("{\"message\":\"first\"}", "first");
        debugger.record("{\"message\":\"second\"}", "second");

        String latest = read(Debugger.CURRENT_LOG);
        assertTrue(latest.contains("{\"message\":\"first\"}"));
        assertTrue(latest.contains("{\"message\":\"second\"}"));
    }

    @Test
    void everyLineIsFlushedRatherThanBuffered() throws Exception {
        // The point of flushing per line: a report asked for immediately afterwards, or a look at
        // the file after a crash, has to see the line that was just written.
        debugger.record("{\"message\":\"unbuffered\"}", "unbuffered");
        assertTrue(read(Debugger.CURRENT_LOG).contains("unbuffered"));
    }

    @Test
    void oneLinePerRecord() throws Exception {
        for (int i = 0; i < 5; i++) {
            debugger.record("{\"marker\":\"count\",\"i\":" + i + "}", "line");
        }

        // Only this test's lines: the plugin narrates its own startup into the same log, which is
        // the point of the log rather than noise to be worked around.
        assertEquals(5, countMarked(read(Debugger.CURRENT_LOG), "count"));
    }

    // ------------------------------------------------------------------ rotation

    @Test
    void theRunStartsOnAFreshLatestLogAndRetiresTheOldOne() throws Exception {
        debugger.record("{\"message\":\"first run\"}", "first run");
        debugger.close();

        // A second start, as a restart would do.
        debugger.record("{\"message\":\"second run\"}", "second run");

        assertTrue(read(Debugger.PREVIOUS_LOG).contains("first run"),
                "the previous run's log should be retired, not overwritten");
        assertTrue(read(Debugger.CURRENT_LOG).contains("second run"));
        assertFalse(read(Debugger.CURRENT_LOG).contains("first run"),
                "a new run must not append to the old log");
    }

    @Test
    void aReportCoversThisSessionOnly() throws Exception {
        debugger.record("{\"message\":\"the previous session\"}", "previous");
        debugger.close();
        debugger.record("{\"message\":\"this session\"}", "this");

        // previous.log belongs to a different run of the server, with its own configuration and
        // possibly its own plugin version. It stays on disk; it does not go in the report.
        String reported = reportedLines();
        assertTrue(reported.contains("this session"));
        assertFalse(reported.contains("the previous session"));
    }

    @Test
    void aReportReadsForwardsInTime() throws Exception {
        debugger.record("{\"message\":\"older\"}", "older");
        debugger.record("{\"message\":\"newer\"}", "newer");

        String reported = reportedLines();
        assertTrue(reported.indexOf("older") < reported.indexOf("newer"),
                "a report should read forwards in time");
    }

    // ------------------------------------------------------------------ size

    @Test
    void aLongSessionStaysInOneFileAndDoesNotTouchPreviousLog() throws Exception {
        debugger.record("{\"message\":\"the previous session\"}", "previous");
        debugger.close();

        writePastTheCap();

        // Trimming must not be a second thing that writes previous.log: that file still holds the
        // run before this one, however long this one has been going.
        assertTrue(read(Debugger.PREVIOUS_LOG).contains("the previous session"),
                "growing past the cap must not overwrite the previous session's log");
    }

    @Test
    void theSessionLogIsBoundedByTrimmingItsOldestLines() throws Exception {
        writePastTheCap();

        long size = Files.size(logDir().resolve(Debugger.CURRENT_LOG));
        assertTrue(size <= Debugger.MAX_LOG_BYTES,
                "latest.log grew to " + size + ", past the " + Debugger.MAX_LOG_BYTES + " cap");
    }

    @Test
    void trimmingLeavesWholeLinesBehind() throws Exception {
        writePastTheCap();

        for (String line : read(Debugger.CURRENT_LOG).lines().toArray(String[]::new)) {
            if (line.isBlank()) continue;
            assertTrue(line.startsWith("{") && line.endsWith("}"),
                    "a trim must cut at a line boundary, got: "
                            + line.substring(0, Math.min(60, line.length())));
        }
    }

    @Test
    void theNewestLinesAreTheOnesKept() throws Exception {
        writePastTheCap();

        String latest = read(Debugger.CURRENT_LOG);
        assertTrue(latest.contains("\"seq\":" + (CAP_FILLING_LINES - 1)),
                "the most recent line should survive a trim");
        assertFalse(latest.contains("\"seq\":0,"),
                "the oldest line should be the one dropped");
    }

    // ------------------------------------------------------------------ report assembly

    @Test
    void aReportNeverCarriesAPartialLine() throws Exception {
        // Fill past the tail budget so the report has to seek into the middle of the file.
        String padding = "x".repeat(2000);
        for (int i = 0; i < 800; i++) {
            debugger.record("{\"i\":" + i + ",\"pad\":\"" + padding + "\"}", "line");
        }

        String[] lines = reportedLines().lines().toArray(String[]::new);
        for (String line : lines) {
            if (line.startsWith("=== LOGS") || line.isBlank()) continue;
            assertTrue(line.startsWith("{") && line.endsWith("}"),
                    "a seek into the log must resume at a line boundary, got: "
                            + line.substring(0, Math.min(60, line.length())));
        }
    }

    @Test
    void theReportIsBounded() throws Exception {
        String padding = "x".repeat(2000);
        for (int i = 0; i < 2000; i++) {
            debugger.record("{\"i\":" + i + ",\"pad\":\"" + padding + "\"}", "line");
        }

        // Roughly: the tail budget for latest.log plus whatever previous.log contributes, not the
        // four megabytes that were written.
        assertTrue(reportedLines().length() <= Debugger.REPORT_TAIL_BYTES + 8192,
                "a report must not grow with the log");
    }

    // ------------------------------------------------------------------ threading

    @Test
    void concurrentWritersDoNotLoseLines() throws Exception {
        int writers = 8;
        int perWriter = 100;

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(writers);

        for (int w = 0; w < writers; w++) {
            final int id = w;
            new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perWriter; i++) {
                        debugger.record("{\"marker\":\"concurrent\",\"w\":" + id + ",\"i\":" + i + "}", "line");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }

        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "writers did not finish");

        assertEquals(writers * perWriter, countMarked(read(Debugger.CURRENT_LOG), "concurrent"));
    }
}
