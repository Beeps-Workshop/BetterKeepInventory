package com.beepsterr.betterkeepinventory.api;

import com.beepsterr.betterkeepinventory.api.Registries.ConditionRegistry;
import com.beepsterr.betterkeepinventory.api.Registries.EffectRegistry;

public record BetterKeepInventoryAPIImpl(
        ConditionRegistry conditionRegistry,
        EffectRegistry effectRegistry
) implements BetterKeepInventoryAPI {

    /**
     * The plugin ships the API module it implements, so the constant it was compiled against
     * is by definition the version it provides.
     */
    @Override
    public int apiVersion() {
        return BetterKeepInventoryAPI.API_VERSION;
    }
}
