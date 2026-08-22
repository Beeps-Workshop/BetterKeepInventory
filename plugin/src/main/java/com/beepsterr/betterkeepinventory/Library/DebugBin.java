package com.beepsterr.betterkeepinventory.Library;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * The paste bin a debug report is uploaded to.
 * <p>
 * This is the only place a bin URL is built. {@link #post} hands back the finished address rather
 * than the bin's id, because the id on its own invites every caller to append it to a base of
 * their own -- which is exactly how the console and the in-game message came to advertise two
 * different hosts for the same upload.
 */
public final class DebugBin {

    /** The bin the BetterKeepInventory developers read. Overridable in config.yml. */
    public static final String DEFAULT_UPLOAD_URL = "https://bin.workshop.beepsterr.com/post";
    public static final String DEFAULT_VIEW_URL   = "https://bin.workshop.beepsterr.com/{key}";

    /** Replaced with the id the bin returns. */
    private static final String KEY_PLACEHOLDER = "{key}";

    // Deliberately short. This runs off the main thread, but a server owner who has just typed a
    // command is waiting on it, and an unreachable host should say so rather than hang.
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    /** How much of an error body to quote back. Enough to be useful, not enough to flood chat. */
    private static final int ERROR_BODY_LIMIT = 200;

    private final String uploadUrl;
    private final String viewUrl;

    public DebugBin(String uploadUrl, String viewUrl) {
        this.uploadUrl = uploadUrl;
        this.viewUrl = viewUrl;
    }

    /**
     * The bin as configured, falling back to the defaults when there is no configuration to read
     * -- which is the case in tests, and during a startup that failed before the config loaded.
     */
    public static DebugBin fromConfig() {
        Config config = Config.getInstance();
        return config == null
                ? new DebugBin(DEFAULT_UPLOAD_URL, DEFAULT_VIEW_URL)
                : new DebugBin(config.getDebugBinUploadUrl(), config.getDebugBinViewUrl());
    }

    /**
     * Upload a report.
     * <p>
     * Blocking, and not to be called from a server thread: see {@code /bki debug upload}, which
     * runs this asynchronously.
     *
     * @return the address the report can be read at.
     * @throws IOException if the bin could not be reached, refused the report, or answered with
     *                     something other than an id.
     */
    public String post(String content) throws IOException {

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(uploadUrl))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "text/plain; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(content, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("The upload was interrupted.", e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            // The body is quoted because the status code alone never explains a rejection -- a
            // bin that refuses a report for being too large answers 413 and says so only here.
            throw new IOException("The bin rejected the report (HTTP " + response.statusCode() + ")"
                    + describeBody(response.body()));
        }

        return viewUrlFor(readKey(response.body()));
    }

    private static String readKey(String body) throws IOException {
        try {
            JsonObject object = JsonParser.parseString(body).getAsJsonObject();
            if (!object.has("key")) {
                throw new IOException("The bin accepted the report but did not return an id."
                        + describeBody(body));
            }
            return object.get("key").getAsString();
        } catch (JsonParseException | IllegalStateException e) {
            throw new IOException("The bin answered with something that is not JSON."
                    + describeBody(body), e);
        }
    }

    /**
     * Substitutes the id into the configured view URL, appending it if the placeholder is absent
     * -- an owner who points this at their own bin and writes a bare base URL gets a working link
     * rather than one that silently drops the id.
     */
    String viewUrlFor(String key) {
        return viewUrl.contains(KEY_PLACEHOLDER)
                ? viewUrl.replace(KEY_PLACEHOLDER, key)
                : viewUrl + key;
    }

    private static String describeBody(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String trimmed = body.strip();
        if (trimmed.length() > ERROR_BODY_LIMIT) {
            trimmed = trimmed.substring(0, ERROR_BODY_LIMIT) + "...";
        }
        return " Response: " + trimmed;
    }
}
