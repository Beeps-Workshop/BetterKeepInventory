package com.beepsterr.betterkeepinventory.api;

import java.util.Map;
import java.util.logging.Level;

/**
 * Structured logging for a death.
 * <p>
 * A line is a message plus fields. The fields describing <em>which</em> death this is -- the
 * player, the phase, an id for the death itself -- are bound once by the context, so a call site
 * only supplies what it is actually saying. Both halves reach the console as a rendered line and
 * the debug report as JSON.
 *
 * <h2>Why fields rather than indentation</h2>
 * This replaces a logger that nested by depth. Indentation cannot survive two deaths being
 * processed at once: each had its own depth counter but they shared one console stream, so
 * simultaneous deaths interleaved into a single tree that looked corrupted rather than into two
 * that could be told apart. A field is carried by the line it belongs to, so it survives being
 * interleaved -- and {@code /bki debug upload} can be grouped by {@code death_id} afterwards.
 *
 * <h2>Levels</h2>
 * {@link #debug} is the narration level: recorded to the debug report always, and shown in the
 * console only when the server owner has turned {@code debug} on. {@link #info} and
 * {@link #warn} are always shown. Prefer {@code debug} for anything said per death -- a busy
 * server dies a lot.
 */
public interface BkiLogger {

    /**
     * @param fields extra fields for this line only. Merged over whatever the logger already
     *               carries, so a call can override a bound field but usually just adds to it.
     */
    void log(Level level, String message, Map<String, Object> fields);

    default void info(String message)                              { log(Level.INFO,    message, Map.of()); }
    default void info(String message, Map<String, Object> fields)  { log(Level.INFO,    message, fields); }
    default void warn(String message)                              { log(Level.WARNING, message, Map.of()); }
    default void warn(String message, Map<String, Object> fields)  { log(Level.WARNING, message, fields); }
    default void debug(String message)                             { log(Level.FINE,    message, Map.of()); }
    default void debug(String message, Map<String, Object> fields) { log(Level.FINE,    message, fields); }

    /**
     * A logger carrying these fields on every line it writes, in addition to the ones already
     * bound.
     * <p>
     * This is what narrowing to a rule or an effect looks like: the rule name is attached once
     * and every line written through the returned logger carries it. The receiver is unchanged.
     */
    BkiLogger with(Map<String, Object> fields);
}
