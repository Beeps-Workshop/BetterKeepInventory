package com.beepsterr.betterkeepinventory.Registries;

import com.beepsterr.betterkeepinventory.api.Effect;
import com.beepsterr.betterkeepinventory.api.Factory.EffectFactory;
import com.beepsterr.betterkeepinventory.api.Registries.EffectRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public class PluginEffectRegistry extends PluginRegistry<EffectFactory> implements EffectRegistry {

    public PluginEffectRegistry(Plugin owner) {
        super(owner);
    }

    @Override
    public Effect create(String key, ConfigurationSection config) {
        EffectFactory factory = get(key);
        return factory == null ? null : factory.create(config);
    }
}
