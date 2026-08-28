package com.beepsterr.betterkeepinventory.support;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal Vault {@link Economy} holding a balance per player, so the {@code vault} condition
 * and effect can be driven without an economy plugin.
 * <p>
 * Registered as a Bukkit service rather than injected: that is how
 * {@link com.beepsterr.betterkeepinventory.Depends.Vault} finds a provider in production, so a
 * test that registers one exercises the same lookup rather than working around it.
 * <p>
 * Only the player-balance half of the interface does anything. Banks are not something
 * BetterKeepInventory touches, and stubbing them to succeed would invent behaviour no test reads.
 */
public class StubEconomy implements Economy {

    private final Map<String, Double> balances = new HashMap<>();

    public void setBalance(OfflinePlayer player, double amount) {
        balances.put(player.getName(), amount);
    }

    @Override public double getBalance(OfflinePlayer player) {
        return balances.getOrDefault(player.getName(), 0.0);
    }

    @Override public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        double now = getBalance(player) - amount;
        balances.put(player.getName(), now);
        return new EconomyResponse(amount, now, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        double now = getBalance(player) + amount;
        balances.put(player.getName(), now);
        return new EconomyResponse(amount, now, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override public String format(double amount) { return "$" + amount; }

    @Override public boolean isEnabled() { return true; }
    @Override public String getName() { return "StubEconomy"; }
    @Override public boolean hasBankSupport() { return false; }
    @Override public int fractionalDigits() { return 2; }
    @Override public String currencyNamePlural() { return "dollars"; }
    @Override public String currencyNameSingular() { return "dollar"; }

    @Override public boolean hasAccount(OfflinePlayer player) { return balances.containsKey(player.getName()); }
    @Override public boolean hasAccount(OfflinePlayer player, String world) { return hasAccount(player); }
    @Override public double getBalance(OfflinePlayer player, String world) { return getBalance(player); }
    @Override public boolean has(OfflinePlayer player, double amount) { return getBalance(player) >= amount; }
    @Override public boolean has(OfflinePlayer player, String world, double amount) { return has(player, amount); }
    @Override public EconomyResponse withdrawPlayer(OfflinePlayer player, String world, double amount) { return withdrawPlayer(player, amount); }
    @Override public EconomyResponse depositPlayer(OfflinePlayer player, String world, double amount) { return depositPlayer(player, amount); }
    @Override public boolean createPlayerAccount(OfflinePlayer player) { return true; }
    @Override public boolean createPlayerAccount(OfflinePlayer player, String world) { return true; }

    // ---- name-keyed overloads: deprecated in Vault, and nothing here calls them ----
    @Override public boolean hasAccount(String name) { return unsupported(); }
    @Override public boolean hasAccount(String name, String world) { return unsupported(); }
    @Override public double getBalance(String name) { return unsupported(); }
    @Override public double getBalance(String name, String world) { return unsupported(); }
    @Override public boolean has(String name, double amount) { return unsupported(); }
    @Override public boolean has(String name, String world, double amount) { return unsupported(); }
    @Override public EconomyResponse withdrawPlayer(String name, double amount) { return unsupported(); }
    @Override public EconomyResponse withdrawPlayer(String name, String world, double amount) { return unsupported(); }
    @Override public EconomyResponse depositPlayer(String name, double amount) { return unsupported(); }
    @Override public EconomyResponse depositPlayer(String name, String world, double amount) { return unsupported(); }
    @Override public boolean createPlayerAccount(String name) { return unsupported(); }
    @Override public boolean createPlayerAccount(String name, String world) { return unsupported(); }

    // ---- banks: BetterKeepInventory does not use them ----
    @Override public EconomyResponse createBank(String name, String player) { return unsupported(); }
    @Override public EconomyResponse createBank(String name, OfflinePlayer player) { return unsupported(); }
    @Override public EconomyResponse deleteBank(String name) { return unsupported(); }
    @Override public EconomyResponse bankBalance(String name) { return unsupported(); }
    @Override public EconomyResponse bankHas(String name, double amount) { return unsupported(); }
    @Override public EconomyResponse bankWithdraw(String name, double amount) { return unsupported(); }
    @Override public EconomyResponse bankDeposit(String name, double amount) { return unsupported(); }
    @Override public EconomyResponse isBankOwner(String name, String player) { return unsupported(); }
    @Override public EconomyResponse isBankOwner(String name, OfflinePlayer player) { return unsupported(); }
    @Override public EconomyResponse isBankMember(String name, String player) { return unsupported(); }
    @Override public EconomyResponse isBankMember(String name, OfflinePlayer player) { return unsupported(); }
    @Override public List<String> getBanks() { return unsupported(); }

    /** Fails loudly rather than returning a plausible answer nobody meant to rely on. */
    private static <T> T unsupported() {
        throw new UnsupportedOperationException("StubEconomy does not implement this");
    }
}
