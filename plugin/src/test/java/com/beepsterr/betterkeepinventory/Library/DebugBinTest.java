package com.beepsterr.betterkeepinventory.Library;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * How a bin id becomes a link. This is the whole reason {@link DebugBin} exists: the console and
 * the in-game message used to build this string separately, and drifted onto different hosts.
 */
class DebugBinTest {

    @Test
    void thePlaceholderIsSubstituted() {
        DebugBin bin = new DebugBin("https://example.test/post", "https://example.test/{key}");
        assertEquals("https://example.test/abc123", bin.viewUrlFor("abc123"));
    }

    @Test
    void aBaseUrlWithoutThePlaceholderStillGetsTheKey() {
        // An owner pointing this at their own bin is likely to write the base and stop. Appending
        // beats handing them a link to the bin's front page.
        DebugBin bin = new DebugBin("https://example.test/post", "https://example.test/");
        assertEquals("https://example.test/abc123", bin.viewUrlFor("abc123"));
    }

    @Test
    void theShippedDefaultsAgreeWithEachOther() {
        DebugBin bin = new DebugBin(DebugBin.DEFAULT_UPLOAD_URL, DebugBin.DEFAULT_VIEW_URL);
        assertEquals("https://bin.workshop.beepsterr.com/abc123", bin.viewUrlFor("abc123"));
    }
}
