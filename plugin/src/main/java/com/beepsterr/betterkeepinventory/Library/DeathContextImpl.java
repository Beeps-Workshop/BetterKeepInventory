package com.beepsterr.betterkeepinventory.Library;

import com.beepsterr.betterkeepinventory.api.DeathContext;
import com.beepsterr.betterkeepinventory.api.BkiLogger;
import com.beepsterr.betterkeepinventory.api.Phase;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * One death, captured at the moment it happens and carried through to the respawn.
 *
 * @see DeathContext for what the buckets mean and why they exist
 */
public class DeathContextImpl implements DeathContext {

    /**
     * Identifies this death in the logs.
     * <p>
     * Short rather than a full UUID because it is read off a console line, and there only has to
     * be enough of it to tell apart the handful of deaths in flight at any one moment.
     */
    private final String deathId = UUID.randomUUID().toString().substring(0, 8);

    private final UUID playerUuid;
    private final Location deathLocation;
    private final EntityDamageEvent.DamageCause cause;
    private final Entity killer;

    /** What INHERIT actually resolved to, kept so the logs can say which one applied. */
    private final Config.DefaultBehavior resolvedBehavior;

    private final List<ItemStack> originalInventory;
    private final int originalLevels;
    private final float originalProgress;

    private final ItemStack[] inventory;
    private final List<ItemStack> drops = new ArrayList<>();
    private int levels;
    private float progress;
    private int droppedExp;

    private final Map<String, Object> extraData = new HashMap<>();

    /**
     * The rule tree as it stood when this death began, and which of its rules matched.
     * <p>
     * Pinned rather than re-read, because the tree is rebuilt whenever a plugin registers or
     * unregisters something. A rebuild between the two phases would otherwise leave the respawn
     * walking a different set of objects than the death recorded matches against, and nothing
     * would run.
     */
    private final List<ConfigRule> rules;
    private final Set<ConfigRule> matched = new HashSet<>();

    // Mutable across phases. The Player object is replaced when a player rejoins, so it cannot
    // be captured once and reused.
    private Player player;
    private Phase phase;
    private PlayerDeathEvent deathEvent;
    private PlayerRespawnEvent respawnEvent;

    /**
     * Built here rather than handed in, so that every line written during this death carries the
     * fields identifying it without a caller having to remember them. It reads this context at
     * log time, which is what lets one logger stay correct after the phase changes.
     */
    private final BkiLogger baseLogger = StructuredLogger.forDeath(this);

    /**
     * What {@link #logger()} currently hands out: the base logger, narrowed to whichever rule and
     * effect is running. An effect writing through {@code ctx.logger()} is then attributable to
     * the rule that invoked it without having to be told which one that was -- which is the one
     * thing the old nesting did that fields would otherwise lose.
     */
    private BkiLogger logger = baseLogger;

    public DeathContextImpl(Player player, PlayerDeathEvent event, Config.DefaultBehavior behavior,
                            List<ConfigRule> rules) {

        this.rules = List.copyOf(rules);
        this.player = player;
        this.playerUuid = player.getUniqueId();
        this.phase = Phase.DEATH;
        this.deathEvent = event;

        this.deathLocation = player.getLocation().clone();

        EntityDamageEvent lastDamage = player.getLastDamageCause();
        this.cause = lastDamage != null ? lastDamage.getCause() : null;
        this.killer = resolveKiller(player, lastDamage);

        // Vanilla works out how much experience a death drops from the level the player died at,
        // before any plugin gets a say. Captured now so the payout matches what the server would
        // have given -- reading it back after the rules would instead measure whatever an
        // experience effect left behind.
        //
        // Progress is captured alongside the level rather than derived from a point total: this
        // is exactly how the player stores it, so writing it back is lossless. Points only come
        // into it for what gets dropped, where orbs are whole numbers anyway.
        this.originalLevels = player.getLevel();
        this.originalProgress = player.getExp();

        ItemStack[] contents = player.getInventory().getContents();
        this.originalInventory = Collections.unmodifiableList(Arrays.asList(cloneAll(contents)));
        this.inventory = new ItemStack[contents.length];

        this.resolvedBehavior = resolveBehavior(player, behavior);
        initialiseBuckets(this.resolvedBehavior, contents);
    }

    private static Entity resolveKiller(Player player, EntityDamageEvent lastDamage) {
        if (player.getKiller() != null) {
            return player.getKiller();
        }
        if (lastDamage instanceof EntityDamageByEntityEvent byEntity) {
            return byEntity.getDamager();
        }
        return null;
    }

    /**
     * INHERIT means "whatever the world would have done", so it has to be resolved into a real
     * behavior before the buckets can be filled.
     */
    private static Config.DefaultBehavior resolveBehavior(Player player, Config.DefaultBehavior behavior) {
        if (behavior != Config.DefaultBehavior.INHERIT) {
            return behavior;
        }
        Boolean keepInventory = player.getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY);
        return Boolean.TRUE.equals(keepInventory)
                ? Config.DefaultBehavior.KEEP
                : Config.DefaultBehavior.DROP;
    }

    /**
     * Where each item and each experience level starts out. Effects move things between the two
     * buckets from here; nothing is created or destroyed by the initialisation itself.
     */
    private void initialiseBuckets(Config.DefaultBehavior behavior, ItemStack[] contents) {

        if (behavior == Config.DefaultBehavior.KEEP) {
            System.arraycopy(cloneAll(contents), 0, inventory, 0, contents.length);
            this.levels = originalLevels;
            this.progress = originalProgress;
            this.droppedExp = 0;
            return;
        }

        // DROP: the player keeps nothing unless an effect puts it back.
        for (ItemStack item : contents) {
            if (item == null || item.getType().isAir()) continue;

            // A vanishing-cursed item is destroyed by dying, not dropped. The server normally
            // applies that while collecting death loot, but we build both buckets ourselves and
            // pin keepInventory on, so it never gets the chance -- putting the item in drops
            // would resurrect an item vanilla had decided to destroy.
            if (isVanishing(item)) continue;

            drops.add(item.clone());
        }
        this.levels = 0;
        this.progress = 0f;
        // Vanilla caps the experience a death drops; match it so this behaves like a normal death.
        this.droppedExp = Math.min(originalLevels * 7, 100);
    }

    /**
     * Whether dying destroys this item outright.
     * <p>
     * Only relevant to items that would otherwise be dropped: the curse has no effect when the
     * player keeps their inventory, which is why this is only consulted while filling the drop
     * bucket.
     */
    private static boolean isVanishing(ItemStack item) {
        return item.containsEnchantment(Enchantment.VANISHING_CURSE);
    }

    private static ItemStack[] cloneAll(ItemStack[] source) {
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i] == null ? null : source[i].clone();
        }
        return copy;
    }

    /**
     * Move this context into the respawn phase. The player is replaced rather than reused: the
     * {@link Player} object is a different instance after a rejoin. The logger is not replaced --
     * it reads the phase from here, so it starts saying {@code RESPAWN} on its own.
     */
    public void enterRespawnPhase(Player player, PlayerRespawnEvent event) {
        this.player = player;
        this.phase = Phase.RESPAWN;
        this.respawnEvent = event;
        this.deathEvent = null;
    }

    @Override public Player player() { return player; }
    @Override public UUID playerUuid() { return playerUuid; }
    @Override public Phase phase() { return phase; }

    @Override public Location deathLocation() { return deathLocation; }
    @Override public Location respawnLocation() { return respawnEvent == null ? null : respawnEvent.getRespawnLocation(); }
    @Override public EntityDamageEvent.DamageCause cause() { return cause; }
    @Override public Entity killer() { return killer; }
    @Override public List<ItemStack> originalInventory() { return originalInventory; }
    @Override public int originalLevels() { return originalLevels; }
    @Override public float originalProgress() { return originalProgress; }

    @Override public ItemStack[] inventory() { return inventory; }
    @Override public List<ItemStack> drops() { return drops; }
    @Override public int levels() { return levels; }
    @Override public void setLevels(int levels) { this.levels = Math.max(0, levels); }
    @Override public float progress() { return progress; }
    @Override public void setProgress(float progress) { this.progress = Math.min(1f, Math.max(0f, progress)); }
    @Override public int droppedExp() { return droppedExp; }
    @Override public void setDroppedExp(int exp) { this.droppedExp = Math.max(0, exp); }

    @Override public PlayerDeathEvent deathEvent() { return deathEvent; }
    @Override public PlayerRespawnEvent respawnEvent() { return respawnEvent; }

    @Override
    public <T> T getExtraData(String key, Class<T> type) {
        Object value = extraData.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    @Override
    public void setExtraData(String key, Object value) {
        extraData.put(key, value);
    }

    @Override public BkiLogger logger() { return logger; }

    /**
     * Narrow the logger handed to effects for the duration of one rule or effect.
     * <p>
     * Rules nest, so the previous logger is returned rather than assumed: the caller restores it
     * in a finally block. One death is processed on one thread, so there is nothing to guard
     * beyond that.
     *
     * @return the logger that was in place, to be passed back to {@link #popLogger}.
     */
    BkiLogger pushLogger(Map<String, Object> fields) {
        BkiLogger previous = logger;
        this.logger = logger.with(fields);
        return previous;
    }

    void popLogger(BkiLogger previous) {
        this.logger = previous;
    }

    /** Identifies this death across both phases and across concurrent deaths. */
    public String deathId() { return deathId; }

    /** The behavior the buckets were initialised from, with INHERIT already resolved. */
    public Config.DefaultBehavior resolvedBehavior() { return resolvedBehavior; }

    /** The rules this death is being evaluated against, fixed for its whole lifetime. */
    public List<ConfigRule> rules() {
        return rules;
    }

    /**
     * Note that a rule's conditions were satisfied. Conditions are checked once, during the death
     * phase; the respawn phase replays this rather than asking again, so that a rule cannot match
     * one half of a death and not the other.
     */
    void recordMatch(ConfigRule rule) {
        matched.add(rule);
    }

    boolean hasMatched(ConfigRule rule) {
        return matched.contains(rule);
    }
}
