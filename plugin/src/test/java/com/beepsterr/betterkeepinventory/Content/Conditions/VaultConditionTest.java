package com.beepsterr.betterkeepinventory.Content.Conditions;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.Content.Conditions.Vault.VaultCondition;
import com.beepsterr.betterkeepinventory.Library.DeathContextImpl;
import com.beepsterr.betterkeepinventory.support.StubEconomy;
import com.beepsterr.betterkeepinventory.support.TestContexts;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code vault} condition, which passes when a player is at or above a balance.
 * <p>
 * Driven through a {@link StubEconomy} registered as a Bukkit service, which is how
 * {@code Depends.Vault} finds a provider in production.
 */
class VaultConditionTest {

    private ServerMock server;
    private Plugin plugin;
    private PlayerMock player;
    private StubEconomy economy;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        plugin = MockBukkit.load(BetterKeepInventory.class);
        player = server.addPlayer();

        economy = new StubEconomy();
        server.getServicesManager().register(Economy.class, economy, plugin, ServicePriority.Normal);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static VaultCondition condition(double minBalance) {
        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("min_balance", minBalance);
        return new VaultCondition(cfg);
    }

    private DeathContextImpl context() {
        return TestContexts.death(player);
    }

    @Test
    void passesWhenTheBalanceIsAboveTheMinimum() {
        economy.setBalance(player, 150.0);
        assertTrue(condition(100.0).check(context()));
    }

    @Test
    void failsWhenTheBalanceIsBelowTheMinimum() {
        economy.setBalance(player, 50.0);
        assertFalse(condition(100.0).check(context()));
    }

    @Test
    void theMinimumIsInclusive() {
        // "min_balance" reads as "at least this much", so the boundary belongs to the player.
        economy.setBalance(player, 100.0);
        assertTrue(condition(100.0).check(context()));
    }

    @Test
    void aPlayerWithNoAccountHasNothing() {
        assertFalse(condition(1.0).check(context()));
    }

    @Test
    void aMinimumOfZeroPassesForEveryone() {
        // The default when min_balance is not configured.
        assertTrue(condition(0.0).check(context()));
    }

    @Test
    void aServerWithVaultButNoEconomyPluginStillProcessesTheDeath() {
        // The condition is registered whenever the Vault *plugin* is present, which does not mean
        // any economy plugin has registered a provider. Evaluating a rule is not allowed to throw
        // out of the death handler.
        server.getServicesManager().unregister(Economy.class, economy);

        assertDoesNotThrow(() -> condition(100.0).check(context()));
    }

    @Test
    void aNegativeBalanceFailsAMinimumOfZero() {
        economy.setBalance(player, -5.0);
        assertFalse(condition(0.0).check(context()));
    }
}
