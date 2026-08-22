package com.beepsterr.betterkeepinventory.Library.Versions;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.Exceptions.UnloadableConfiguration;
import com.beepsterr.betterkeepinventory.Library.Config;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The SNAPSHOT channel was removed in 3.0, and {@code VersionChannel.valueOf} throws rather than
 * defaulting. Without a migration the plugin refuses to start for anyone who had selected it --
 * which would be a poor way to ship a fix to the update checker, since the servers running
 * SNAPSHOT are the ones following releases most closely.
 */
class VersionChannelMigrationTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        MockBukkit.load(BetterKeepInventory.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** A config as it would be on disk from an older install. */
    private YamlConfiguration configWith(String channel) {
        YamlConfiguration cfg = new YamlConfiguration();
        // Anything other than "default", which would send Config off to rewrite from disk.
        cfg.set("version", "2.3.4");
        cfg.set("notify_channel", channel);
        return cfg;
    }

    @Test
    void snapshotBecomesBeta() throws UnloadableConfiguration {
        Config config = new Config(configWith("SNAPSHOT"), null);

        assertEquals(VersionChannel.BETA, config.getNotifyChannel(),
                "someone on SNAPSHOT was asking for pre-release builds; BETA is the closest thing left");
    }

    @Test
    void aSnapshotConfigStillLoads() {
        assertDoesNotThrow(() -> new Config(configWith("SNAPSHOT"), null),
                "a removed channel name must not stop the plugin starting");
    }

    @Test
    void lowercaseIsMigratedToo() throws UnloadableConfiguration {
        // The readme has always told people to write it lowercase.
        Config config = new Config(configWith("snapshot"), null);

        assertEquals(VersionChannel.BETA, config.getNotifyChannel());
    }

    @Test
    void survivingChannelsAreLeftAlone() throws UnloadableConfiguration {
        assertEquals(VersionChannel.STABLE, new Config(configWith("STABLE"), null).getNotifyChannel());
        assertEquals(VersionChannel.LATEST, new Config(configWith("LATEST"), null).getNotifyChannel());
        assertEquals(VersionChannel.NONE, new Config(configWith("NONE"), null).getNotifyChannel());
        assertEquals(VersionChannel.BETA, new Config(configWith("BETA"), null).getNotifyChannel());
    }

    @Test
    void anAbsentChannelDefaultsToStable() throws UnloadableConfiguration {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("version", "2.3.4");

        assertEquals(VersionChannel.STABLE, new Config(cfg, null).getNotifyChannel(),
                "the recommended list is the safe default");
    }

    /**
     * The other path into Config: a file whose version is still the placeholder, which sends the
     * constructor off to reload from disk and swap the object it reads from.
     * <p>
     * Every other test here uses a stamped version, where the parameter and the reloaded config
     * happen to be the same object -- so a migration writing to one and the parse reading the
     * other still worked by accident.
     */
    @Test
    void snapshotIsMigratedEvenOnAFreshlyStampedConfig() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("version", "default");
        cfg.set("notify_channel", "SNAPSHOT");

        assertDoesNotThrow(() -> new Config(cfg, null),
                "the reload path must not leave the parse reading a config the migration did not touch");
    }
}
