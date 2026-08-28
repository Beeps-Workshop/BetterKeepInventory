package com.beepsterr.betterkeepinventory.Content.Conditions.Vault;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.Depends.Vault;
import com.beepsterr.betterkeepinventory.api.DeathContext;
import com.beepsterr.betterkeepinventory.api.Condition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class VaultCondition implements Condition {

    private final double minBalance;

    public VaultCondition(ConfigurationSection config) {
        this.minBalance = config.getDouble("min_balance", 0.0);
    }

    @Override
    public boolean check(DeathContext ctx) {
        Player ply = ctx.player();

        BetterKeepInventory.getInstance().debug(ply, "Checking economy condition: Minimum balance required = " + minBalance);
        Vault vault = new Vault(BetterKeepInventory.getInstance());
        return vault.getPlayerBalance(ply) >= minBalance;
    }
}
