package com.beepsterr.betterkeepinventory.Depends;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class Vault {

    BetterKeepInventory plugin;
    RegisteredServiceProvider<Economy> economyProvider;

    public Vault(BetterKeepInventory main){
        plugin = main;
        economyProvider = plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
    }

    /**
     * The Vault plugin being installed does not mean an economy plugin registered a provider
     * behind it, so every call here has to tolerate there being nothing to ask.
     */
    public boolean hasEconomy(){
        return economyProvider != null && economyProvider.getProvider() != null;
    }

    public double getPlayerBalance(Player ply){
        if (!hasEconomy()) return 0.0;
        return economyProvider.getProvider().getBalance(ply);
    }

    public String format(double amount){
        if (!hasEconomy()) return String.valueOf(amount);
        return economyProvider.getProvider().format(amount);
    }

    public boolean takeMoney(Player ply, Double amount){
        if (!hasEconomy()) return false;
        EconomyResponse r = economyProvider.getProvider().withdrawPlayer(ply, amount);
        return r.transactionSuccess();
    }

    public boolean grantMoney(Player ply, Double amount){
        if (!hasEconomy()) return false;
        EconomyResponse r = economyProvider.getProvider().depositPlayer(ply, amount);
        return r.transactionSuccess();
    }


}
