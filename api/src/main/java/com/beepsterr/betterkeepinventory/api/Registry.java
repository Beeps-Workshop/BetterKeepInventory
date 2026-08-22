package com.beepsterr.betterkeepinventory.api;

import org.bukkit.plugin.Plugin;

import java.util.Map;

/**
 * <h2>How keys resolve</h2>
 * Every entry is stored under exactly one key: {@code pluginname.key}. The bare {@code key}
 * is not stored at all -- it is resolved at lookup time against whatever is registered at
 * that moment, in a fixed order:
 * <ol>
 *   <li>BetterKeepInventory's own entry, if it provides that key;</li>
 *   <li>otherwise the addon whose plugin name sorts first alphabetically.</li>
 * </ol>
 * This means a bare key means the same thing on two servers with the same plugins installed,
 * regardless of the order those plugins happened to enable in -- which is not something the
 * server owner controls or can easily observe. It also means an addon can never take a bare
 * key away from the core plugin, so {@code drop} and {@code damage} keep meaning what every
 * existing configuration expects.
 * <p>
 * Collisions between two addons are resolved rather than rejected, but they are reported once
 * to the console. If two addons genuinely provide the same key, configurations should say
 * which one they mean by using the namespaced form.
 */
public interface Registry<T> {

    /**
     * Register an entry under {@code key}, addressable as {@code pluginname.key} and
     * potentially as the bare {@code key} -- see the class documentation for how that
     * resolves.
     * <p>
     * Registering the same key twice from the same plugin replaces the earlier entry.
     */
    void register(Plugin plugin, String key, T entry);

    /**
     * Remove an entry previously registered by {@code plugin}.
     * <p>
     * Entries registered by a different plugin are never removed, whatever key is passed. If
     * this entry was the one the bare {@code key} resolved to, the bare key now resolves to
     * the next candidate, or to nothing.
     *
     * @return true if an entry was removed.
     */
    boolean unregister(Plugin plugin, String key);

    /**
     * Remove every entry registered by {@code plugin}.
     * <p>
     * Addons should call this from {@code onDisable}. An entry left behind by a disabled
     * plugin points at a class from a classloader that may no longer be live, and will fail
     * the next time the configuration is parsed.
     *
     * @return the number of entries removed.
     */
    int unregisterAll(Plugin plugin);

    /**
     * Whether {@code key} resolves to anything. Accepts both bare and namespaced keys.
     */
    boolean has(String key);

    /**
     * @return the registered entry, or null if {@code key} resolves to nothing.
     */
    T get(String key);

    /**
     * @return the entry along with the plugin that registered it, or null if {@code key}
     *         resolves to nothing.
     */
    RegistryEntry<T> getFull(String key);

    /**
     * Every registration, keyed by its namespaced name, in registration order.
     * <p>
     * Bare keys do not appear here because they are not stored; use {@link #getFull} to find
     * out what one currently resolves to.
     */
    Map<String, RegistryEntry<T>> getAll();
}
