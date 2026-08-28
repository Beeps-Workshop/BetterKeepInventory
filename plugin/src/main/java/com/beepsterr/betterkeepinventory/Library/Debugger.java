package com.beepsterr.betterkeepinventory.Library;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Writes the structured log to disk and assembles a report from it for {@code /bki debug upload}.
 *
 * <h2>Why disk rather than memory</h2>
 * The log used to live in a 1000-line ring buffer, which meant the interesting lines were gone by
 * the time anyone asked for them on a busy server, and were held for hours on a quiet one. On
 * disk, a report can be asked for after the fact, the lines survive a restart, and nothing is
 * retained in the heap.
 *
 * <h2>Rotation</h2>
 * Two files, the way a server log works: {@code logs/latest.log} for this run and
 * {@code logs/previous.log} for the one before. Rotation happens on start and only on start, so
 * each file means exactly one session.
 * <p>
 * Size is handled separately, by trimming the front of {@code latest.log} rather than by rotating
 * it. Rotating on size as well would give {@code previous.log} two meanings -- sometimes the last
 * run, sometimes four hours ago in this one -- and since every death is written whether or not
 * debug is on, a busy server would hit a size rotation far more often than a restart.
 *
 * <h2>Threading</h2>
 * Lines arrive from wherever a death is being processed -- a region thread on Folia -- so the
 * writer is guarded. Each line is flushed rather than batched: a flush is a write to the page
 * cache, not to the platter, so it costs a syscall in the order of microseconds and survives the
 * process dying, which is exactly the case a debug log has to survive.
 */
public class Debugger {

    static final String LOG_DIRECTORY = "logs";
    static final String CURRENT_LOG = "latest.log";
    static final String PREVIOUS_LOG = "previous.log";

    /** How large {@code latest.log} may grow before its oldest lines are dropped. */
    static final long MAX_LOG_BYTES = 8L * 1024 * 1024;

    /**
     * How much of {@code latest.log} a trim keeps.
     * <p>
     * Half the cap rather than all but the last line, so a long session pays for the rewrite
     * every four megabytes instead of on nearly every write once it is full.
     */
    static final long TRIM_TO_BYTES = MAX_LOG_BYTES / 2;

    /**
     * How much of the log a report carries.
     * <p>
     * The file on disk is deliberately larger than this: it is a log, and worth keeping. A report
     * is something a paste bin has to accept and a human has to read, so it takes the tail rather
     * than everything.
     */
    static final long REPORT_TAIL_BYTES = 1024 * 1024;

    private final Object writeLock = new Object();

    private BufferedWriter writer;
    private long currentLogBytes;
    private boolean started;

    /**
     * Set when writing fails, to stop a broken data folder from producing one warning per log
     * line for the rest of the server's life. Everything else carries on: a death must not fail
     * because its narration could not be saved.
     */
    private boolean writingDisabled;

    private final Set<UUID> verboseTargets = ConcurrentHashMap.newKeySet();

    // ------------------------------------------------------------------ writing

    /**
     * Store one log line.
     * <p>
     * Two renderings of the same line: the JSON goes to the log, and the human text goes to
     * anyone watching with {@code /bki debug verbose} -- a player with that on wants a sentence
     * in chat, not an object.
     *
     * @param json  the structured record, one object, no newlines.
     * @param human the same line as rendered for the console.
     */
    public void record(String json, String human) {

        synchronized (writeLock) {
            append(json);
        }

        // Outside the lock: this reaches out to the server, and holding the writer across it
        // would stall every death that wanted to record a line.
        Broadcast(human);
    }

    /** Caller holds {@link #writeLock}. */
    private void append(String json) {

        if (writingDisabled) {
            return;
        }

        try {
            if (!started) {
                start();
            }

            // Counted rather than stat'ed: asking the filesystem for a size on every line would
            // cost more than the write does.
            long lineBytes = json.length() + 1L;
            if (currentLogBytes + lineBytes > MAX_LOG_BYTES) {
                trim();
            }

            writer.write(json);
            writer.newLine();
            writer.flush();
            currentLogBytes += lineBytes;

        } catch (IOException e) {
            writingDisabled = true;
            closeQuietly();
            BetterKeepInventory.getInstance().getLogger().warning(
                    "Could not write the debug log, so it is disabled for this session. "
                            + "Deaths are unaffected. Cause: " + e.getMessage());
        }
    }

    /**
     * First write of the run: make the directory, retire the previous run's log, and open a fresh
     * one. Done lazily rather than in {@code onEnable} because the very first line the plugin
     * logs is written before anything has had a chance to initialise us.
     * <p>
     * This is the only thing that ever writes {@code previous.log}, which is what makes that file
     * mean "the run before this one" and nothing else.
     */
    private void start() throws IOException {

        Files.createDirectories(logDirectory());
        closeQuietly();

        Path current = logDirectory().resolve(CURRENT_LOG);
        if (Files.exists(current)) {
            Files.move(current, logDirectory().resolve(PREVIOUS_LOG), StandardCopyOption.REPLACE_EXISTING);
        }

        writer = open(current, false);
        currentLogBytes = 0;
        started = true;
    }

    /**
     * Drop the oldest lines from {@code latest.log}, keeping the most recent
     * {@link #TRIM_TO_BYTES}.
     * <p>
     * Caller holds {@link #writeLock}. Rewrites the file rather than rotating it, so that this
     * session's log stays one file and {@code previous.log} keeps belonging to the previous
     * session. The rewrite is O(size) but happens once every {@link #TRIM_TO_BYTES} written.
     */
    private void trim() throws IOException {

        Path current = logDirectory().resolve(CURRENT_LOG);

        writer.flush();
        String kept = tailOf(current, TRIM_TO_BYTES);
        closeQuietly();

        writer = open(current, false);
        writer.write(kept);
        writer.flush();
        currentLogBytes = kept.length();
    }

    private static BufferedWriter open(Path path, boolean append) throws IOException {
        return Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING);
    }

    /** Flush and release the log file. Called from {@code onDisable}. */
    public void close() {
        synchronized (writeLock) {
            closeQuietly();
            started = false;
        }
    }

    private void closeQuietly() {
        if (writer == null) {
            return;
        }
        try {
            writer.close();
        } catch (IOException ignored) {
            // Already on the way out, and there is nowhere useful left to report this.
        }
        writer = null;
    }

    private static Path logDirectory() {
        return BetterKeepInventory.getInstance().getDataFolder().toPath().resolve(LOG_DIRECTORY);
    }

    // ------------------------------------------------------------------ verbose players

    private void Broadcast(String message) {
        for(UUID target : verboseTargets){
            Player player = Bukkit.getPlayer(target);
            if(player != null && player.isOnline()){
                player.sendMessage(message);
            }
        }
    }

    public void toggleVerbosePlayer(Player player){
        this.toggleVerbosePlayer(player.getUniqueId());
    }

    public void toggleVerbosePlayer(UUID playerUUID){
        if(verboseTargets.add(playerUUID)){
            BetterKeepInventory.getInstance().getLogger().info("Enabled verbose debugging for player " + playerUUID);
        } else {
            verboseTargets.remove(playerUUID);
            BetterKeepInventory.getInstance().getLogger().info("Disabling verbose debugging for player " + playerUUID);
        }
    }

    // ------------------------------------------------------------------ the report

    private String getHeader(){

        String header = "=== SYSTEM INFO";
        header += "\nServer Implementation: " + Bukkit.getServer().getClass().getPackage().getName() + " (" + schedulerImpl() + ")";
        header += "\nServer Version: " + Bukkit.getVersion();
        header += "\nPlugin Version: " + BetterKeepInventory.getInstance().getDescription().getVersion();
        header += "\nJava Version: " + System.getProperty("java.version");
        header += "\nOS: " + System.getProperty("os.name") + " "+ System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")";
        header += "\nCPU Cores: " + Runtime.getRuntime().availableProcessors();
        header += "\nTime: " + Instant.now().truncatedTo(ChronoUnit.SECONDS);
        return header;
    }

    /**
     * Which scheduler FoliaLib picked. Guarded because a report is worth having even when the
     * plugin failed on the way up and never got as far as building one.
     */
    private static String schedulerImpl() {
        return BetterKeepInventory.getScheduler() == null
                ? "unknown"
                : BetterKeepInventory.getScheduler().getImplType().toString();
    }

    private String getConfig() {
        String prefix = "=== CONFIGURATION\n";
        File configFile = new File(BetterKeepInventory.instance.getDataFolder(), "config.yml");
        if(!configFile.exists()){
            return prefix + "No config file found.";
        }
        try {
            return prefix + Files.readString(configFile.toPath());
        } catch (IOException e) {
            return prefix + "Error reading config file: " + e.getMessage();
        }
    }

    /**
     * Assemble the report: system information, the configuration, and the tail of this session's
     * log.
     * <p>
     * {@code latest.log} only. {@code previous.log} belongs to a different run of the server,
     * with its own configuration and possibly its own plugin version, so folding it into a report
     * about this one would be answering a question nobody asked. It is still on disk if it turns
     * out to be the interesting one.
     */
    public String getContent(){

        StringBuilder log = new StringBuilder("BetterKeepInventory DDF 2.0\n");
        log.append(getHeader()).append("\n\n");
        log.append(getConfig()).append("\n\n");

        // One JSON object per line (JSONL). The rendered console text is in each object's
        // "message", so this stays readable by eye as well as parseable.
        log.append("=== LOGS (jsonl)\n");
        log.append(logTail());

        return log.toString();
    }

    /** The last {@link #REPORT_TAIL_BYTES} of this session's log, oldest line first. */
    private String logTail() {

        synchronized (writeLock) {
            if (writer != null) {
                try {
                    writer.flush();
                } catch (IOException ignored) {
                    // Reported already by append(); a partial tail beats no report at all.
                }
            }
        }

        return tailOf(logDirectory().resolve(CURRENT_LOG), REPORT_TAIL_BYTES);
    }

    /**
     * Read at most {@code maxBytes} from the end of a file, starting at a line boundary.
     * <p>
     * Seeks rather than reading the whole file: it is capped at {@link #MAX_LOG_BYTES}, and
     * pulling eight megabytes into the heap to keep the last one would be a poor trade on a
     * server that is already having a bad day.
     */
    private static String tailOf(Path path, long maxBytes) {

        if (!Files.isRegularFile(path)) {
            return "";
        }

        try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ)) {

            long size = channel.size();
            long from = Math.max(0, size - maxBytes);
            channel.position(from);

            ByteBuffer buffer = ByteBuffer.allocate((int) Math.min(size - from, Integer.MAX_VALUE));
            while (buffer.hasRemaining() && channel.read(buffer) > 0) {
                // Reading until the buffer is full or the file ends.
            }

            String text = new String(buffer.array(), 0, buffer.position(), StandardCharsets.UTF_8);

            // Seeking lands mid-line unless we started at the beginning; a half a JSON object at
            // the top of a report reads like corruption rather than like a truncation.
            if (from > 0) {
                int firstBreak = text.indexOf('\n');
                text = firstBreak < 0 ? "" : text.substring(firstBreak + 1);
            }

            return text;

        } catch (IOException e) {
            return "Could not read " + path.getFileName() + ": " + e.getMessage() + "\n";
        }
    }

    /** Save a report of the current state next to the config, for someone who cannot upload. */
    public void write() throws IOException {
        write(getContent());
    }

    private void write(String content) throws IOException {

        File dataFolder = BetterKeepInventory.instance.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new IOException("Could not create " + dataFolder);
        }

        Files.writeString(new File(dataFolder, "debugger.log").toPath(), content, StandardCharsets.UTF_8);
    }

    /**
     * Save a local copy of the report and send it to the configured bin.
     * <p>
     * Blocking on the network, so this must not be called from a server thread --
     * {@code /bki debug upload} runs it asynchronously. The report is assembled once and used for
     * both, so the file on disk is exactly what was uploaded.
     *
     * @return the address the report can be read at.
     * @throws IOException if the report could not be written or the bin would not take it.
     */
    public String upload() throws IOException {
        String content = getContent();

        // Written first, and deliberately not swallowed: if the local copy cannot be saved there
        // is something wrong with the data folder, and uploading would hide it.
        write(content);

        return DebugBin.fromConfig().post(content);
    }
}
