package com.beepsterr.betterkeepinventory.Registries;

import com.beepsterr.betterkeepinventory.api.Factory.ConditionFactory;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Only namespaced keys are stored; bare keys are worked out at lookup time. These tests pin
 * that resolution order, because it is what stops a bare key from meaning different things on
 * two servers with the same plugins installed -- and, more importantly, what stops an addon
 * from quietly taking 'drop' or 'damage' away from the core plugin.
 * <p>
 * "Aaa" and "Zzz" are named so that alphabetical order is obvious at a glance, and so that
 * the core-wins cases are not accidentally also alphabetical wins.
 */
class PluginRegistryTest {

    private Plugin core;
    private Plugin aaaAddon;
    private Plugin zzzAddon;
    private PluginConditionRegistry registry;

    private static final ConditionFactory CORE_FACTORY = section -> null;
    private static final ConditionFactory AAA_FACTORY = section -> null;
    private static final ConditionFactory ZZZ_FACTORY = section -> null;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        // Deliberately not alphabetically first, so "core wins" cannot pass by accident.
        core = MockBukkit.createMockPlugin("MiddleCore");
        aaaAddon = MockBukkit.createMockPlugin("AaaAddon");
        zzzAddon = MockBukkit.createMockPlugin("ZzzAddon");
        registry = new PluginConditionRegistry(core);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void resolvesBareAndNamespacedKeys() {
        registry.register(aaaAddon, "example", AAA_FACTORY);

        assertTrue(registry.has("example"));
        assertTrue(registry.has("aaaaddon.example"));
        assertSame(AAA_FACTORY, registry.get("example"));
        assertSame(AAA_FACTORY, registry.get("aaaaddon.example"));
    }

    @Test
    void keysAreCaseInsensitive() {
        registry.register(aaaAddon, "Example", AAA_FACTORY);

        assertTrue(registry.has("EXAMPLE"));
        assertSame(AAA_FACTORY, registry.get("AaaAddon.Example"));
    }

    @Test
    void onlyNamespacedKeysAreStored() {
        registry.register(aaaAddon, "example", AAA_FACTORY);

        assertEquals(List.of("aaaaddon.example"), List.copyOf(registry.getAll().keySet()));
    }

    @Test
    void coreWinsTheBareKeyRegardlessOfAlphabeticalOrder() {
        registry.register(aaaAddon, "drop", AAA_FACTORY);   // sorts first alphabetically
        registry.register(core, "drop", CORE_FACTORY);

        assertSame(CORE_FACTORY, registry.get("drop"),
                "an addon must not be able to take a bare key away from the core plugin");
        assertSame(AAA_FACTORY, registry.get("aaaaddon.drop"), "still reachable by namespace");
    }

    @Test
    void coreWinsEvenWhenItRegistersFirst() {
        registry.register(core, "drop", CORE_FACTORY);
        registry.register(aaaAddon, "drop", AAA_FACTORY);

        assertSame(CORE_FACTORY, registry.get("drop"));
    }

    @Test
    void addonsTieBreakAlphabeticallyNotByRegistrationOrder() {
        registry.register(zzzAddon, "example", ZZZ_FACTORY);
        registry.register(aaaAddon, "example", AAA_FACTORY);

        assertSame(AAA_FACTORY, registry.get("example"),
                "registration order must not decide this -- plugin enable order is not stable");
    }

    @Test
    void resolutionIsIndependentOfRegistrationOrder() {
        registry.register(aaaAddon, "example", AAA_FACTORY);
        registry.register(zzzAddon, "example", ZZZ_FACTORY);

        PluginConditionRegistry reversed = new PluginConditionRegistry(core);
        reversed.register(zzzAddon, "example", ZZZ_FACTORY);
        reversed.register(aaaAddon, "example", AAA_FACTORY);

        assertSame(registry.get("example"), reversed.get("example"));
    }

    @Test
    void unregisterRemovesOnlyThatPluginsEntry() {
        registry.register(aaaAddon, "example", AAA_FACTORY);
        registry.register(zzzAddon, "example", ZZZ_FACTORY);

        assertTrue(registry.unregister(aaaAddon, "example"));

        assertFalse(registry.has("aaaaddon.example"));
        assertSame(ZZZ_FACTORY, registry.get("example"), "the bare key falls through to the survivor");
    }

    @Test
    void aPluginCannotUnregisterAnothersEntry() {
        registry.register(aaaAddon, "example", AAA_FACTORY);

        assertFalse(registry.unregister(zzzAddon, "example"));
        assertSame(AAA_FACTORY, registry.get("example"));
    }

    @Test
    void unregisterOfSomethingUnregisteredReportsNothingRemoved() {
        assertFalse(registry.unregister(aaaAddon, "nope"));
    }

    @Test
    void unregisteringTheLastProviderLeavesTheBareKeyUnresolvable() {
        registry.register(aaaAddon, "example", AAA_FACTORY);
        registry.unregister(aaaAddon, "example");

        assertFalse(registry.has("example"));
        assertNull(registry.get("example"));
    }

    @Test
    void unregisterAllRemovesOnlyThatPluginsEntries() {
        registry.register(aaaAddon, "one", AAA_FACTORY);
        registry.register(aaaAddon, "two", AAA_FACTORY);
        registry.register(zzzAddon, "three", ZZZ_FACTORY);

        assertEquals(2, registry.unregisterAll(aaaAddon));

        assertFalse(registry.has("one"));
        assertFalse(registry.has("two"));
        assertTrue(registry.has("three"));
    }

    @Test
    void registeringTheSameKeyTwiceReplacesTheEarlierEntry() {
        registry.register(aaaAddon, "example", AAA_FACTORY);
        registry.register(aaaAddon, "example", ZZZ_FACTORY);

        assertEquals(1, registry.getAll().size());
        assertSame(ZZZ_FACTORY, registry.get("example"));
    }

    @Test
    void missingKeysReturnNullRatherThanThrowing() {
        assertNull(registry.get("absent"));
        assertNull(registry.getFull("absent"));
        assertNull(registry.get("aaaaddon.absent"));
    }

    @Test
    void aKeyContainingADotStillResolves() {
        registry.register(aaaAddon, "my.key", AAA_FACTORY);

        assertSame(AAA_FACTORY, registry.get("my.key"), "must not be mistaken for a namespaced lookup");
        assertSame(AAA_FACTORY, registry.get("aaaaddon.my.key"));
    }

    @Test
    void getAllIsInsertionOrdered() {
        registry.register(aaaAddon, "zebra", AAA_FACTORY);
        registry.register(aaaAddon, "apple", AAA_FACTORY);

        assertEquals(
                List.of("aaaaddon.zebra", "aaaaddon.apple"),
                List.copyOf(registry.getAll().keySet())
        );
    }
}
