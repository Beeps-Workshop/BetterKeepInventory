package com.beepsterr.betterkeepinventory.Content.Effects;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.Content.Effects.Vault.VaultEffect;
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
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The {@code vault} effect, which takes money on death.
 * <p>
 * Every case here sets {@code min} equal to {@code max}, which cancels the random term in
 * {@code calculateLoss} and makes the amount exact. The randomness itself is not what is worth
 * pinning; the clamping and the two modes are.
 */
class VaultEffectTest {

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

    private static VaultEffect effect(String mode, double min, double max, boolean allowNegative) {
        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("mode", mode);
        cfg.set("min", min);
        cfg.set("max", max);
        cfg.set("allow_negative_balance", allowNegative);
        return new VaultEffect(cfg);
    }

    private DeathContextImpl context() {
        return TestContexts.death(player);
    }

    @Test
    void simpleModeTakesAFlatAmount() {
        economy.setBalance(player, 500.0);

        effect("SIMPLE", 50.0, 50.0, false).onDeath(context());

        assertEquals(450.0, economy.getBalance(player));
    }

    @Test
    void percentageModeTakesAShareOfTheBalance() {
        economy.setBalance(player, 200.0);

        effect("PERCENTAGE", 25.0, 25.0, false).onDeath(context());

        assertEquals(150.0, economy.getBalance(player));
    }

    @Test
    void aPlayerCannotLoseMoreThanTheyHave() {
        economy.setBalance(player, 30.0);

        effect("SIMPLE", 100.0, 100.0, false).onDeath(context());

        assertEquals(0.0, economy.getBalance(player), "the loss should be clamped to the balance");
    }

    @Test
    void allowNegativeBalanceLetsThemGoIntoDebt() {
        economy.setBalance(player, 30.0);

        effect("SIMPLE", 100.0, 100.0, true).onDeath(context());

        assertEquals(-70.0, economy.getBalance(player));
    }

    @Test
    void takingNothingLeavesTheBalanceAlone() {
        economy.setBalance(player, 75.0);

        effect("SIMPLE", 0.0, 0.0, false).onDeath(context());

        assertEquals(75.0, economy.getBalance(player));
    }

    @Test
    void percentageOfNothingIsNothing() {
        economy.setBalance(player, 0.0);

        effect("PERCENTAGE", 50.0, 50.0, false).onDeath(context());

        assertEquals(0.0, economy.getBalance(player));
    }

    @Test
    void theEffectDoesNothingOnRespawn() {
        economy.setBalance(player, 100.0);

        DeathContextImpl ctx = context();
        VaultEffect effect = effect("SIMPLE", 25.0, 25.0, false);
        effect.onDeath(ctx);
        ctx.enterRespawnPhase(player, null);
        effect.onRespawn(ctx);

        assertEquals(75.0, economy.getBalance(player), "the money should be taken once, not twice");
    }

    @Test
    void aServerWithVaultButNoEconomyPluginStillProcessesTheDeath() {
        // Both the vault condition and effect are registered whenever the Vault *plugin* is
        // present, which does not mean any economy plugin has registered a provider. A death is
        // not allowed to fail because the money could not be counted.
        server.getServicesManager().unregister(Economy.class, economy);

        assertDoesNotThrow(() -> effect("SIMPLE", 10.0, 10.0, false).onDeath(context()));
    }
}
