package com.beepsterr.betterkeepinventory.Depends;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.jetbrains.annotations.NotNull;

public class BetterKeepInventoryPlaceholderExpansion extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return BetterKeepInventory.getInstance().getDescription().getName();
    }

    @Override
    public @NotNull String getAuthor() {
        return BetterKeepInventory.getInstance().getDescription().getAuthors().get(0);
    }

    @Override
    public @NotNull String getVersion() {
        return BetterKeepInventory.getInstance().getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(org.bukkit.entity.Player player, @NotNull String identifier) {
        return null;
    }
}
