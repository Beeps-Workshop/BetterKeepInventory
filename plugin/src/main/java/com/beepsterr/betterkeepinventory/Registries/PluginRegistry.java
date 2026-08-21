package com.beepsterr.betterkeepinventory.Registries;

import com.beepsterr.betterkeepinventory.api.Registry;
import com.beepsterr.betterkeepinventory.api.RegistryEntry;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared storage for the condition and effect registries, which differ only in what they hold.
 * <p>
 * Only namespaced keys are stored. Bare keys are resolved on lookup, which is what makes them
 * mean the same thing regardless of the order plugins happened to enable in -- storing the
 * bare key at registration time would hand it to whoever got there first, and enable order is
 * neither stable nor visible to the server owner.
 */
public abstract class PluginRegistry<T> implements Registry<T> {

    /** BetterKeepInventory itself, which always wins a bare key it provides. */
    private final Plugin owner;

    private final Map<String, RegistryEntry<T>> entries = new LinkedHashMap<>();

    /** Bare keys already reported as ambiguous, so the warning is printed once and not per lookup. */
    private final Set<String> reportedCollisions = new HashSet<>();

    /** Notified whenever registrations change, so the rule tree can be rebuilt. */
    private Runnable changeListener = () -> {};

    protected PluginRegistry(Plugin owner) {
        this.owner = owner;
    }

    public void setChangeListener(Runnable listener) {
        this.changeListener = listener != null ? listener : () -> {};
    }

    private static String namespaced(Plugin plugin, String key) {
        return (plugin.getName() + "." + key).toLowerCase();
    }

    /** The key an entry was registered under, recovered from its namespaced form. */
    private static <T> String bareKeyOf(Map.Entry<String, RegistryEntry<T>> entry) {
        return entry.getKey().substring(entry.getValue().plugin().getName().length() + 1);
    }

    @Override
    public void register(Plugin plugin, String key, T entry) {
        entries.put(namespaced(plugin, key), new RegistryEntry<>(plugin, entry));
        changeListener.run();
    }

    @Override
    public boolean unregister(Plugin plugin, String key) {
        boolean removed = entries.remove(namespaced(plugin, key)) != null;
        if (removed) {
            changeListener.run();
        }
        return removed;
    }

    @Override
    public int unregisterAll(Plugin plugin) {
        int before = entries.size();
        entries.values().removeIf(entry -> entry.plugin().equals(plugin));
        int removed = before - entries.size();
        if (removed > 0) {
            changeListener.run();
        }
        return removed;
    }

    @Override
    public boolean has(String key) {
        return getFull(key) != null;
    }

    @Override
    public T get(String key) {
        RegistryEntry<T> entry = getFull(key);
        return entry == null ? null : entry.entry();
    }

    @Override
    public RegistryEntry<T> getFull(String key) {

        String lookup = key.toLowerCase();

        // A namespaced key addresses exactly one entry. Checked first rather than by looking
        // for a dot, so that a key which legitimately contains a dot still works.
        RegistryEntry<T> exact = entries.get(lookup);
        if (exact != null) {
            return exact;
        }

        List<Map.Entry<String, RegistryEntry<T>>> candidates = entries.entrySet().stream()
                .filter(entry -> bareKeyOf(entry).equals(lookup))
                .sorted(Comparator
                        // The core plugin's own entry wins, so an addon can never take over
                        // 'drop' or 'damage' and silently change what existing configs mean.
                        .comparingInt((Map.Entry<String, RegistryEntry<T>> entry) ->
                                entry.getValue().plugin().equals(owner) ? 0 : 1)
                        .thenComparing(entry -> entry.getValue().plugin().getName().toLowerCase()))
                .toList();

        if (candidates.isEmpty()) {
            return null;
        }

        if (candidates.size() > 1) {
            reportCollision(lookup, candidates);
        }

        return candidates.get(0).getValue();
    }

    private void reportCollision(String bareKey, List<Map.Entry<String, RegistryEntry<T>>> candidates) {

        if (!reportedCollisions.add(bareKey)) {
            return;
        }

        String winner = candidates.get(0).getKey();
        String others = candidates.stream()
                .skip(1)
                .map(Map.Entry::getKey)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        owner.getLogger().warning(
                "'" + bareKey + "' is provided by more than one plugin. Using '" + winner
                        + "'; also available as " + others + ". Name the one you want in your "
                        + "configuration to be explicit.");
    }

    @Override
    public Map<String, RegistryEntry<T>> getAll() {
        return Collections.unmodifiableMap(entries);
    }
}
