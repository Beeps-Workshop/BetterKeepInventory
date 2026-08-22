package com.beepsterr.betterkeepinventory.Library.Versions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The download link has to carry Modrinth's own spelling of the version.
 * <p>
 * Modrinth serves a page per version and 404s on anything else, so building the URL from
 * {@link Version#toString()} would break the moment the two disagreed -- and they can: the
 * parser normalises, while the link needs the literal string the release was published under.
 */
class UpdateTest {

    @Test
    void linksToTheExactVersionRatherThanTheList() {
        VersionChecker.Update update = new VersionChecker.Update(new Version("2.3.4"), "2.3.4");

        assertEquals("https://modrinth.com/plugin/betterkeepinventory/version/2.3.4",
                update.downloadUrl());
    }

    @Test
    void usesTheModrinthStringNotTheParsedForm() {
        // A published version whose string carries more than the parser keeps hold of.
        VersionChecker.Update update = new VersionChecker.Update(
                new Version("2.3.0-SNAPSHOT-2607271"), "2.3.0-SNAPSHOT-2607271");

        assertTrue(update.downloadUrl().endsWith("/version/2.3.0-SNAPSHOT-2607271"),
                "the link must be the string Modrinth published, got: " + update.downloadUrl());
    }

    @Test
    void rendersAsThePlainVersionForMessages() {
        VersionChecker.Update update = new VersionChecker.Update(new Version("3.0.0"), "3.0.0");

        assertEquals("3.0.0", update.toString(),
                "chat messages print this directly, so it should not leak the record shape");
    }
}
