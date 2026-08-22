package com.beepsterr.betterkeepinventory.Library.Versions;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tcoded.folialib.enums.ImplementationType;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tells the server about versions it can actually run.
 * <p>
 * Modrinth already knows which Minecraft versions and server software each release supports, so
 * the check asks it for compatible versions rather than comparing version numbers and hoping.
 * The previous implementation compared numbers alone, which meant a 1.18 server was told that
 * the newest release -- needing Minecraft 1.21 and Java 21 -- was available to it. Following
 * that advice broke the server.
 * <p>
 * That failure mode is the reason this has to be right in any release that ships: the update
 * checker is the only thing that tells anyone a fix exists, so a broken one cannot deliver its
 * own repair. Everybody left on that version keeps getting the wrong advice indefinitely.
 */
public class VersionChecker {

    private static final String PROJECT = "betterkeepinventory";
    private static final String VERSIONS_API = "https://api.modrinth.com/v2/project/" + PROJECT + "/version";

    public static final String URL_RECOMMENDED =
            "https://raw.githubusercontent.com/Beeps-Workshop/BetterKeepInventory/refs/heads/master/versions/recommended.txt";

    /** Modrinth asks for a contact address so it can reach maintainers about API misuse. */
    private static final String USER_AGENT = "Beeps-Workshop/BetterKeepInventory (hello@beeps.email)";

    private static final long CHECK_INTERVAL_TICKS = 20L * 14400; // every 4 hours

    /** Written on the checker thread, read from the command thread. */
    public volatile Version foundVersion;

    public final VersionChannel channel;

    private WrappedTask task;

    public VersionChecker(VersionChannel channel) {
        this.channel = channel;

        if (channel == VersionChannel.NONE) {
            return;
        }

        this.task = BetterKeepInventory.getScheduler().getScheduler().runTimerAsync(() -> {
            try {
                foundVersion = getLatestVersion(channel);
            } catch (IOException e) {
                BetterKeepInventory.getInstance().log("Failed to check for updates: " + e.getMessage());
            }
        }, 0L, CHECK_INTERVAL_TICKS);
    }

    public boolean IsUpdateAvailable() {
        Version found = foundVersion;
        if (found == null) return false;
        return found.compareTo(BetterKeepInventory.getInstance().version) > 0;
    }

    /**
     * Stop checking.
     * <p>
     * Cancels this timer specifically. It used to call {@code cancelAllTasks()}, which took down
     * every scheduled task the plugin owned -- including the delayed work effects rely on, so a
     * {@code /bki reload} during a death cancelled the hunger, command and lightning follow-ups
     * that were still pending.
     */
    public void CancelCheck() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /**
     * The newest version this server could actually install on the given channel, or null.
     */
    public static Version getLatestVersion(VersionChannel channel) throws IOException {

        if (channel == VersionChannel.NONE) {
            return null;
        }

        List<Candidate> candidates = fetchCompatibleVersions(allowedReleaseTypes(channel));
        if (candidates.isEmpty()) {
            return null;
        }

        if (channel == VersionChannel.STABLE) {
            Set<String> recommended = fetchRecommended();
            candidates.removeIf(candidate -> !recommended.contains(candidate.versionNumber));
            if (candidates.isEmpty()) {
                // Deliberately no fallback to LATEST -- see VersionChannel.STABLE.
                return null;
            }
        }

        Version best = null;
        for (Candidate candidate : candidates) {
            if (best == null || candidate.version.compareTo(best) > 0) {
                best = candidate.version;
            }
        }

        if (best != null) {
            BetterKeepInventory.getInstance().log("Update checker found version: " + best);
        }
        return best;
    }

    private static Set<String> allowedReleaseTypes(VersionChannel channel) {
        return switch (channel) {
            case BETA -> Set.of("release", "beta");
            case LATEST, STABLE -> Set.of("release");
            case NONE -> Set.of();
        };
    }

    private record Candidate(String versionNumber, Version version) {}

    /**
     * Ask Modrinth for versions matching this server's Minecraft version and software.
     * <p>
     * Filtering here rather than locally is the whole point: Modrinth is the thing that knows
     * what each release supports.
     */
    private static List<Candidate> fetchCompatibleVersions(Set<String> releaseTypes) throws IOException {

        String url = VERSIONS_API
                + "?loaders=" + encodeJsonArray(serverLoader())
                + "&game_versions=" + encodeJsonArray(minecraftVersion());

        List<Candidate> candidates = new ArrayList<>();

        try (BufferedReader reader = open(url)) {
            JsonArray versions = JsonParser.parseReader(reader).getAsJsonArray();

            for (JsonElement element : versions) {
                JsonObject version = element.getAsJsonObject();

                String type = version.get("version_type").getAsString();
                if (!releaseTypes.contains(type)) continue;

                String number = version.get("version_number").getAsString();
                try {
                    candidates.add(new Candidate(number, new Version(number)));
                } catch (IllegalArgumentException e) {
                    // A version numbered in some shape this plugin cannot parse is not one it
                    // can sensibly recommend.
                    BetterKeepInventory.getInstance().log("Ignoring unparseable version: " + number);
                }
            }
        }

        return candidates;
    }

    /** The version numbers currently considered known-good, one per line. */
    private static Set<String> fetchRecommended() throws IOException {
        Set<String> recommended = new HashSet<>();

        try (BufferedReader reader = open(URL_RECOMMENDED)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    recommended.add(trimmed);
                }
            }
        }

        return recommended;
    }

    private static BufferedReader open(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        return new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
    }

    private static String encodeJsonArray(String value) {
        return URLEncoder.encode("[\"" + value + "\"]", StandardCharsets.UTF_8);
    }

    /**
     * This server's Minecraft version, as Modrinth spells it.
     * <p>
     * {@code getBukkitVersion()} looks like {@code 1.21.8-R0.1-SNAPSHOT} on every fork, which is
     * why it is preferred over the Paper-only accessor.
     */
    static String minecraftVersion() {
        return Bukkit.getBukkitVersion().split("-")[0];
    }

    static String serverLoader() {
        ImplementationType type = BetterKeepInventory.getScheduler().getImplType();
        if (type == null) return "spigot";

        return switch (type) {
            case FOLIA -> "folia";
            case PAPER, LEGACY_PAPER -> "paper";
            default -> "spigot";
        };
    }
}
