package com.beepsterr.betterkeepinventory.Library;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.api.BkiLogger;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The one implementation of {@link BkiLogger}: renders a line for the console and records the
 * same line as JSON for {@code /bki debug upload}.
 *
 * <h2>Console and storage are decided separately</h2>
 * Storage is unconditional. That is the whole reason the two are not the same call: a bug report
 * arrives from someone who has never touched the {@code debug} setting, and the narration of the
 * death they are complaining about has to already be in the report. The console is where the
 * {@code debug} flag applies, by promoting the narration level to something the default handler
 * will print.
 *
 * <h2>Bound fields</h2>
 * Fields come from three places and are merged in this order, so the most specific wins: the
 * death context (which death this is), the fields bound by {@link #with} (which rule or effect
 * is speaking), and the fields passed to the call itself.
 * <p>
 * The context is read at log time rather than copied at construction. {@code phase} changes
 * underneath a context that outlives the death phase, and reading late means one logger stays
 * correct across both phases instead of having to be rebuilt for the second.
 */
public class StructuredLogger implements BkiLogger {

    /** Reserved: written by this class, never taken from the field maps. */
    private static final String FIELD_TIMESTAMP = "ts";
    private static final String FIELD_LEVEL     = "level";
    private static final String FIELD_MESSAGE   = "message";

    /** Rendered into the console prefix as {@code [player=<uuid>] [<death_id>]}. */
    private static final String FIELD_UUID     = "uuid";
    private static final String FIELD_DEATH_ID = "death_id";

    /** The death being narrated, or null for plugin-level logging (startup, config, commands). */
    private final DeathContextImpl context;

    private final Map<String, Object> bound;

    private StructuredLogger(DeathContextImpl context, Map<String, Object> bound) {
        this.context = context;
        this.bound = bound;
    }

    /** For anything that is not a death: startup, configuration, rule parsing, commands. */
    public static StructuredLogger plugin() {
        return new StructuredLogger(null, Map.of());
    }

    /**
     * For one death, with its identifying fields bound.
     * <p>
     * Called by {@link DeathContextImpl} on itself, which is why it takes the context rather than
     * a field map: everything it needs is already there, and taking it live keeps the phase
     * honest.
     */
    public static StructuredLogger forDeath(DeathContextImpl context) {
        return new StructuredLogger(context, Map.of());
    }

    @Override
    public BkiLogger with(Map<String, Object> fields) {
        Map<String, Object> merged = new LinkedHashMap<>(bound);
        merged.putAll(fields);
        return new StructuredLogger(context, Map.copyOf(merged));
    }

    @Override
    public void log(Level level, String message, Map<String, Object> fields) {

        Map<String, Object> merged = merge(fields);
        String rendered = render(message, merged);

        // Unconditional, and at the level as given. This is what keeps `/bki debug upload` worth
        // asking for on a server running with debug off -- the narration is in the report even
        // though nobody watched it scroll past.
        BetterKeepInventory plugin = BetterKeepInventory.getInstance();
        if (plugin != null && plugin.debugger != null) {
            plugin.debugger.record(toJson(level, message, merged), rendered);
        }

        // Console only. FINE is the narration level: dropped by the default handler, and promoted
        // when the owner has actually asked to see it. Read live rather than fixed when the logger
        // is built, so `/bki reload` flipping the flag takes effect on the next death.
        console(plugin).log(level == Level.FINE && isDebug() ? Level.INFO : level, rendered);
    }

    private static boolean isDebug() {
        Config config = Config.getInstance();
        return config != null && config.isDebug();
    }

    /** Falls back to a bare logger so a context built in a test does not need the plugin loaded. */
    private static Logger console(BetterKeepInventory plugin) {
        return plugin != null ? plugin.getLogger() : Logger.getLogger("BetterKeepInventory");
    }

    private Map<String, Object> merge(Map<String, Object> callFields) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (context != null) {
            putContextFields(merged);
        }
        merged.putAll(bound);
        merged.putAll(callFields);
        return merged;
    }

    private void putContextFields(Map<String, Object> into) {

        into.put(FIELD_DEATH_ID, context.deathId());
        into.put(FIELD_UUID, context.playerUuid().toString());
        into.put("phase", context.phase().toString());
        into.put("behavior", context.resolvedBehavior().toString());

        // The player object is replaced on a rejoin and can be gone entirely by the time a late
        // line is written; the uuid above is the field that is always there.
        if (context.player() != null) {
            into.put("player", context.player().getName());
        }

        // The world the death happened in, not wherever the player currently is. Both phases of
        // one death then agree, which is what makes the field worth grouping on.
        if (context.deathLocation() != null && context.deathLocation().getWorld() != null) {
            into.put("world", context.deathLocation().getWorld().getName());
        }
    }

    /**
     * {@code [player=<uuid>] [<death_id>] message}, with either bracket omitted when the field is
     * not bound -- startup and configuration lines carry neither and render as the bare message.
     */
    private static String render(String message, Map<String, Object> fields) {

        StringBuilder line = new StringBuilder();

        Object uuid = fields.get(FIELD_UUID);
        if (uuid != null) {
            line.append("[player=").append(uuid).append("] ");
        }

        Object deathId = fields.get(FIELD_DEATH_ID);
        if (deathId != null) {
            line.append('[').append(deathId).append("] ");
        }

        return line.append(message).toString();
    }

    /**
     * One JSON object per line. {@code message} is included rather than left to the console
     * rendering, so an uploaded report is still readable by eye without being run through a tool.
     */
    private static String toJson(Level level, String message, Map<String, Object> fields) {

        JsonObject object = new JsonObject();
        object.addProperty(FIELD_TIMESTAMP, Instant.now().toString());
        object.addProperty(FIELD_LEVEL, level.getName());

        for (Map.Entry<String, Object> field : fields.entrySet()) {
            String key = field.getKey();
            if (FIELD_TIMESTAMP.equals(key) || FIELD_LEVEL.equals(key) || FIELD_MESSAGE.equals(key)) {
                continue;
            }
            put(object, key, field.getValue());
        }

        object.addProperty(FIELD_MESSAGE, message);
        return object.toString();
    }

    private static void put(JsonObject object, String key, Object value) {
        switch (value) {
            case null           -> object.add(key, com.google.gson.JsonNull.INSTANCE);
            case String string  -> object.addProperty(key, string);
            case Number number  -> object.addProperty(key, number);
            case Boolean bool   -> object.addProperty(key, bool);
            // Enums, UUIDs, Locations and the like. Rendering them rather than reflecting over
            // them keeps a field that someone adds from an addon from becoming a nested object
            // nobody expected.
            default             -> object.addProperty(key, String.valueOf(value));
        }
    }
}
