package com.beepsterr.betterkeepinventory.api;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Everything a condition or effect needs to know about one death, captured once and carried
 * through both phases.
 *
 * <h2>The two buckets</h2>
 * The model is vanilla's own -- an inventory and a drops list -- made explicit and mutable.
 * Effects do not spawn items into the world or clear slots themselves; they move things
 * between {@link #inventory()} and {@link #drops()}, and one application step at the end of
 * the death phase hands the result to the server. That single exit point is what keeps items
 * from being moved by several uncoordinated code paths at once.
 * <p>
 * Where the buckets start depends on the configured default behavior:
 * <ul>
 *   <li>{@code KEEP} -- everything in {@link #inventory()}, {@link #drops()} empty</li>
 *   <li>{@code DROP} -- everything in {@link #drops()}, {@link #inventory()} empty</li>
 *   <li>{@code INHERIT} -- resolve the world's keepInventory gamerule, then as above</li>
 * </ul>
 * So dropping is a move from inventory to drops, keeping is the move back, and deleting is a
 * removal from inventory that adds to nothing.
 *
 * <h2>Effects are shared</h2>
 * The rule tree is parsed once, not per death, so a single {@code Effect} instance serves
 * every death concurrently. Treat effect fields as immutable configuration and put anything
 * per-death in {@link #setExtraData}.
 *
 * @see Phase
 */
public interface DeathContext {

    // ------------------------------------------------------------------ identity

    Player player();

    /**
     * The player's UUID, which is what identifies them across a disconnect -- the
     * {@link Player} object is replaced when they rejoin.
     */
    UUID playerUuid();

    Phase phase();

    // ------------------------------------------------------------------ circumstance

    /**
     * Where the player died. Captured at death and unchanged in the respawn phase, so it means
     * the same thing in both -- unlike reading the player's live location, which by respawn is
     * wherever the server has since put them.
     */
    Location deathLocation();

    /**
     * Where the player is about to respawn, or null during {@link Phase#DEATH}.
     */
    Location respawnLocation();

    /**
     * What killed the player, or null if it could not be determined.
     */
    EntityDamageEvent.DamageCause cause();

    /**
     * Whatever dealt the killing blow -- not necessarily a {@link Player}, and null for
     * environmental deaths.
     */
    Entity killer();

    // ------------------------------------------------------------------ the buckets

    /**
     * The inventory exactly as it was at the moment of death, for comparison. Structurally
     * unmodifiable; the stacks themselves should be treated as read-only too.
     * <p>
     * Useful for working out what a death actually cost, without having to thread state
     * through the effects that caused it.
     */
    List<ItemStack> originalInventory();

    /**
     * The working inventory -- what the player ends up with. Slot-indexed to match
     * {@code PlayerInventory}, and mutable: writing null to a slot empties it.
     * <p>
     * Only meaningful during {@link Phase#DEATH}. By the respawn phase this has already been
     * applied and changes to it do nothing.
     */
    ItemStack[] inventory();

    /**
     * What will hit the ground. Mutable, and ordered as the drops were added.
     * <p>
     * Only meaningful during {@link Phase#DEATH}, as with {@link #inventory()}.
     */
    List<ItemStack> drops();

    /**
     * Experience levels as they were at the moment of death, before any effect ran.
     * <p>
     * Vanilla decides how much experience a death drops from this value, so it is captured
     * before the rules get a turn. Reading {@link #levels()} back afterwards would instead
     * measure whatever an experience effect left behind.
     */
    int originalLevels();

    /**
     * Progress toward the next level as it was at the moment of death, from 0.0 to 1.0.
     */
    float originalProgress();

    /**
     * The experience levels the player ends up with. The counterpart of {@link #inventory()}.
     */
    int levels();

    void setLevels(int levels);

    /**
     * Progress toward the next level the player ends up with, from 0.0 to 1.0.
     * <p>
     * Tracked separately from {@link #levels()} because that is how the player stores it, so
     * writing it back is exact. An effect that reduces levels should decide deliberately what
     * happens to the partial level rather than dropping it by accident.
     */
    float progress();

    void setProgress(float progress);

    /**
     * Raw experience points dropped on the ground. The counterpart of {@link #drops()}.
     * Spawned as a single orb, and only if greater than zero.
     * <p>
     * Points rather than levels because that is the unit orbs are measured in. This is the one
     * place the level/point conversion has to happen, and the only place rounding is unavoidable.
     */
    int droppedExp();

    void setDroppedExp(int exp);

    // ------------------------------------------------------------------ escape hatches

    /**
     * The underlying event, or null during {@link Phase#RESPAWN}.
     * <p>
     * For the things the buckets do not model -- the death message above all. Note that
     * {@code PlayerDeathEvent} is <strong>not cancellable</strong>: it does not implement
     * {@code Cancellable}, and by the time it fires the death has already happened. There is
     * no way to call the death off from here.
     * <p>
     * Adding to {@code getDrops()} does nothing. The application step pins keepInventory on
     * and distributes {@link #drops()} itself, and the server does not spawn its own drop list
     * when keepInventory is set. Use {@link #drops()}.
     */
    PlayerDeathEvent deathEvent();

    /**
     * The underlying event, or null during {@link Phase#DEATH}.
     */
    PlayerRespawnEvent respawnEvent();

    // ------------------------------------------------------------------ extra data

    /**
     * Read a value stashed earlier in this same death.
     * <p>
     * This is how an effect carries something from the death phase to the respawn phase.
     * Doing it with a static map keyed by UUID -- which is what the built-in effects used to
     * do -- leaks whenever the respawn phase does not arrive, and hands the stale value to an
     * unrelated later death.
     *
     * @return the stored value, or null if absent or of a different type.
     */
    <T> T getExtraData(String key, Class<T> type);

    /**
     * Stash a value for later in this same death. In-memory only: it does not survive a
     * server restart.
     */
    void setExtraData(String key, Object value);

    // ------------------------------------------------------------------ logging

    /**
     * The logger for the rule evaluation currently in progress, already nested at the right
     * depth.
     */
    LoggerInterface logger();
}
