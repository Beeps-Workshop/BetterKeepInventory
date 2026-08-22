package com.beepsterr.betterkeepinventory.Events;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.Exceptions.UnloadableConfiguration;
import com.beepsterr.betterkeepinventory.Library.Config;
import com.beepsterr.betterkeepinventory.api.Phase;
import com.beepsterr.betterkeepinventory.api.Events.BKIPlayerDeathAppliedEvent;
import com.beepsterr.betterkeepinventory.api.Events.BKIPlayerDeathProcessedEvent;
import com.beepsterr.betterkeepinventory.api.Events.BKIPlayerRespawnProcessedEvent;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The events are the integration path for plugins that should work the moment they are installed,
 * without anyone editing a rules tree. These tests stand in for the two addons that motivated
 * them -- a buyback shop and a "purchased life" plugin -- because what matters is not that the
 * events fire but that listening to them is actually sufficient to change the outcome.
 */
class BKIEventsTest {

    private ServerMock server;
    private WorldMock world;
    private PlayerMock player;
    private BetterKeepInventory plugin;

    @BeforeEach
    void setUp() throws UnloadableConfiguration {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        plugin = MockBukkit.load(BetterKeepInventory.class);
        player = server.addPlayer();
        player.teleport(world.getSpawnLocation());

        // DROP so the drop bucket starts full and a listener has something to take.
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("version", "3.0.0");
        cfg.set("default_behavior", "DROP");
        plugin.config = new Config(cfg, null);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private void register(Listener listener) {
        server.getPluginManager().registerEvents(listener, plugin);
    }

    private void die() {
        DamageSource source = DamageSource.builder(DamageType.GENERIC).build();
        server.getPluginManager().callEvent(
                new PlayerDeathEvent(player, source, new ArrayList<>(), 0, "died"));
    }

    private List<ItemStack> groundItems() {
        return world.getEntitiesByClass(Item.class).stream().map(Item::getItemStack).toList();
    }

    // --- ordering -----------------------------------------------------------------------------

    @Test
    void processedFiresBeforeAppliedAndBothFireOnce() {
        List<String> seen = new ArrayList<>();
        register(new Listener() {
            @EventHandler public void processed(BKIPlayerDeathProcessedEvent e) { seen.add("processed"); }
            @EventHandler public void applied(BKIPlayerDeathAppliedEvent e) { seen.add("applied"); }
        });

        die();

        assertEquals(List.of("processed", "applied"), seen);
    }

    @Test
    void firesEvenWhenNoRuleMatched() {
        List<String> seen = new ArrayList<>();
        register(new Listener() {
            @EventHandler public void processed(BKIPlayerDeathProcessedEvent e) { seen.add("processed"); }
        });

        die(); // config has no rules at all

        assertEquals(1, seen.size(), "listeners are unconditional; they do their own filtering");
    }

    // --- the buyback shop ---------------------------------------------------------------------

    /**
     * Stands in for a buyback shop: take everything that was going to hit the ground, record it,
     * and leave nothing behind. No effect registered, no config edited.
     */
    @Test
    void aListenerCanTakeTheDropsSoNothingReachesTheGround() {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 64));

        List<ItemStack> shop = new ArrayList<>();
        register(new Listener() {
            @EventHandler public void onProcessed(BKIPlayerDeathProcessedEvent e) {
                shop.addAll(e.context().drops());
                e.context().drops().clear();
            }
        });

        die();

        assertEquals(2, shop.size(), "the shop should have received both stacks");
        assertTrue(groundItems().isEmpty(), "and nothing should have reached the ground");
        assertTrue(player.getInventory().isEmpty(), "the player still keeps nothing under DROP");
    }

    // --- the purchased life -------------------------------------------------------------------

    /**
     * A listener can override the outcome entirely, whatever the rules decided.
     * <p>
     * Worth having as a capability, but note this is a large hammer and usually the wrong tool: a
     * plugin that only contributes a fact about the player -- combat tagged, holds a purchased
     * life -- should register a condition instead, so the server owner decides what that fact
     * protects. Overriding here ignores their configuration.
     */
    @Test
    void aListenerCanOverrideTheOutcomeEntirely() {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));
        player.setLevel(10);

        register(new Listener() {
            @EventHandler public void onProcessed(BKIPlayerDeathProcessedEvent e) {
                var ctx = e.context();
                for (ItemStack dropped : ctx.drops()) {
                    for (int i = 0; i < ctx.inventory().length; i++) {
                        if (ctx.inventory()[i] == null) { ctx.inventory()[i] = dropped; break; }
                    }
                }
                ctx.drops().clear();
                ctx.setLevels(ctx.originalLevels());
                ctx.setDroppedExp(0);
            }
        });

        die();

        assertTrue(groundItems().isEmpty(), "nothing should drop for a player who bought a life");
        assertEquals(5, player.getInventory().getItem(0).getAmount(), "the diamonds come back");
        assertEquals(10, player.getLevel(), "and so do the levels");
    }

    // --- priority -----------------------------------------------------------------------------

    @Test
    void listenersAreOrderedByEventPriority() {
        List<String> order = new ArrayList<>();
        register(new Listener() {
            @EventHandler(priority = EventPriority.LOWEST)
            public void first(BKIPlayerDeathProcessedEvent e) { order.add("lowest"); }
        });
        register(new Listener() {
            @EventHandler(priority = EventPriority.HIGHEST)
            public void last(BKIPlayerDeathProcessedEvent e) { order.add("highest"); }
        });

        die();

        assertEquals(List.of("lowest", "highest"), order,
                "EventPriority is what orders addons against each other");
    }

    // --- applied ------------------------------------------------------------------------------

    @Test
    void appliedSeesTheSettledOutcome() {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));

        List<Integer> droppedStacks = new ArrayList<>();
        register(new Listener() {
            @EventHandler public void onApplied(BKIPlayerDeathAppliedEvent e) {
                droppedStacks.add(e.context().drops().size());
            }
        });

        die();

        assertEquals(List.of(1), droppedStacks);
        assertFalse(groundItems().isEmpty(), "by this point the drops are already in the world");
    }

    @Test
    void appliedCarriesWhatTheDeathCost() {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));

        List<Integer> costs = new ArrayList<>();
        register(new Listener() {
            @EventHandler public void onApplied(BKIPlayerDeathAppliedEvent e) {
                long before = e.context().originalInventory().stream().filter(java.util.Objects::nonNull).count();
                long after = java.util.Arrays.stream(e.context().inventory()).filter(java.util.Objects::nonNull).count();
                costs.add((int) (before - after));
            }
        });

        die();

        assertEquals(List.of(1), costs,
                "originalInventory vs inventory is what a death cost, without following the rules");
    }

    // --- respawn ------------------------------------------------------------------------------

    @Test
    void respawnEventCarriesTheSameContextInTheRespawnPhase() {
        List<Phase> phases = new ArrayList<>();
        List<String> extras = new ArrayList<>();

        register(new Listener() {
            @EventHandler public void onProcessed(BKIPlayerDeathProcessedEvent e) {
                e.context().setExtraData("test:marker", "set-at-death");
            }
            @EventHandler public void onRespawn(BKIPlayerRespawnProcessedEvent e) {
                phases.add(e.context().phase());
                extras.add(e.context().getExtraData("test:marker", String.class));
            }
        });

        die();
        server.getPluginManager().callEvent(
                new PlayerRespawnEvent(player, world.getSpawnLocation(), false));

        assertEquals(List.of(Phase.RESPAWN), phases);
        assertEquals(List.of("set-at-death"), extras,
                "the respawn event carries the same context the death produced");
    }

    @Test
    void respawnEventDoesNotFireWithoutAKnownDeath() {
        List<String> seen = new ArrayList<>();
        register(new Listener() {
            @EventHandler public void onRespawn(BKIPlayerRespawnProcessedEvent e) { seen.add("respawn"); }
        });

        server.getPluginManager().callEvent(
                new PlayerRespawnEvent(player, world.getSpawnLocation(), false));

        assertTrue(seen.isEmpty(), "no pending death means there is nothing to report");
    }

    @Test
    void eventsExposeTheHandlerListBukkitNeeds() {
        assertNotNull(BKIPlayerDeathProcessedEvent.getHandlerList());
        assertNotNull(BKIPlayerDeathAppliedEvent.getHandlerList());
        assertNotNull(BKIPlayerRespawnProcessedEvent.getHandlerList());
    }
}
