package com.beepsterr.betterkeepinventory.api;

import com.beepsterr.betterkeepinventory.api.Registries.ConditionRegistry;
import com.beepsterr.betterkeepinventory.api.Registries.EffectRegistry;

public interface BetterKeepInventoryAPI {

    /**
     * The API version this addon was <em>compiled</em> against.
     * <p>
     * This is a compile-time constant, so the value is inlined into your addon's bytecode
     * when you build it. That is deliberate: comparing it against {@link #apiVersion()},Ma
     * which is resolved at runtime from the plugin actually installed on the server, is how
     * an addon detects that it is running against an API it does not understand.
     * <pre>{@code
     * if (api.apiVersion() != BetterKeepInventoryAPI.API_VERSION) {
     *     getLogger().severe("Built for BKI API v" + BetterKeepInventoryAPI.API_VERSION
     *                      + " but the server is running v" + api.apiVersion() + ".");
     *     getServer().getPluginManager().disablePlugin(this);
     *     return;
     * }
     * }</pre>
     * Incremented whenever a breaking change is made to this API. It tracks the API, not the
     * plugin: it does not change for a release that leaves these interfaces alone.
     */
    int API_VERSION = 3;

    /**
     * The API version provided by the plugin currently running on this server.
     * <p>
     * Unlike {@link #API_VERSION} this is resolved at runtime, so it reflects the installed
     * plugin rather than whatever your addon was built against. See {@link #API_VERSION} for
     * the intended comparison.
     */
    int apiVersion();

    /**
     * Access the condition registry to register or retrieve custom conditions.
     */
    ConditionRegistry conditionRegistry();

    /**
     * Access the effect registry to register or retrieve custom effects.
     */
    EffectRegistry effectRegistry();

}
