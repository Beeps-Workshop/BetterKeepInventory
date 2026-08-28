package com.beepsterr.betterkeepinventory.Registries;

import com.beepsterr.betterkeepinventory.api.Factory.ConditionFactory;
import com.beepsterr.betterkeepinventory.api.Registries.ConditionRegistry;
import org.bukkit.plugin.Plugin;

public class PluginConditionRegistry extends PluginRegistry<ConditionFactory> implements ConditionRegistry {

    public PluginConditionRegistry(Plugin owner) {
        super(owner);
    }
}
