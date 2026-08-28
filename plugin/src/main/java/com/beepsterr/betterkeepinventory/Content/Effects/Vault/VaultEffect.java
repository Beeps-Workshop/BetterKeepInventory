package com.beepsterr.betterkeepinventory.Content.Effects.Vault;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.Depends.Vault;
import com.beepsterr.betterkeepinventory.api.DeathContext;
import com.beepsterr.betterkeepinventory.api.Effect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class VaultEffect implements Effect {

    public enum Mode {
        SIMPLE, PERCENTAGE
    }

    private final Mode mode;
    private final double min;
    private final double max;
    private final boolean allowNegativeBalance;

    public VaultEffect(ConfigurationSection config) {
        this.mode = Mode.valueOf(config.getString("mode", "PERCENTAGE").toUpperCase());
        this.min = config.getDouble("min", 0.0);
        this.max = config.getDouble("max", 0.0);
        this.allowNegativeBalance = config.getBoolean("allow_negative_balance", false);
    }

    @Override
    public void onRespawn(DeathContext ctx) {
        // noop
    }

    @Override
    public void onDeath(DeathContext ctx) {
        Player ply = ctx.player();

        Vault vault = new Vault(BetterKeepInventory.getInstance());
        BetterKeepInventory plugin = BetterKeepInventory.getInstance();
        Random rng = plugin.rng;

        double playerBalance = vault.getPlayerBalance(ply);
        double balanceLoss = calculateLoss(playerBalance, rng);

        if (playerBalance < balanceLoss && !allowNegativeBalance) {
            balanceLoss = playerBalance;
        }

        vault.takeMoney(ply, balanceLoss);

        Map<String, String> replacements = new HashMap<>();
        replacements.put("amount", vault.format(balanceLoss));
        plugin.config.sendMessage(ply, "effects.economy", replacements);

        plugin.debug(ply, "EconomyLoss: Took " + balanceLoss + " from player " + ply.getName());
    }

    private double calculateLoss(double balance, Random rng) {
        return switch (mode) {
            case SIMPLE -> min + (max - min) * rng.nextDouble();
            case PERCENTAGE -> balance * ((min + (max - min) * rng.nextDouble()) / 100.0);
        };
    }
}
