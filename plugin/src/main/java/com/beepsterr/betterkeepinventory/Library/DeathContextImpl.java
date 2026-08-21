package com.beepsterr.betterkeepinventory.Library;

import com.beepsterr.betterkeepinventory.api.DeathContext;
import com.beepsterr.betterkeepinventory.api.LoggerInterface;
import com.beepsterr.betterkeepinventory.api.Phase;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One death, captured at the moment it happens and carried through to the respawn.
 *
 * @see DeathContext for what the buckets mean and why they exist
 */
public class DeathContextImpl implements DeathContext {

    private final UUID playerUuid;
    private final Location deathLocation;
    private final EntityDamageEvent.DamageCause cause;
    private final Entity killer;

    private final List<ItemStack> originalInventory;
    private final int originalLevels;
    private final float originalProgress;

    private final ItemStack[] inventory;
    private final List<ItemStack> drops = new ArrayList<>();
    private int levels;
    private float progress;
    private int droppedExp;

    private final Map<String, Object> extraData = new HashMap<>();

    // Mutable across phases. The Player object is replaced when a player rejoins, so it cannot
    // be captured once and reused.
    private Player player;
    private Phase phase;
    private PlayerDeathEvent deathEvent;
    private PlayerRespawnEvent respawnEvent;
    private LoggerInterface logger;

    public DeathContextImpl(Player player, PlayerDeathEvent event, Config.DefaultBehavior behavior, LoggerInterface logger) {

        this.player = player;
        this.playerUuid = player.getUniqueId();
        this.phase = Phase.DEATH;
        this.deathEvent = event;
        this.logger = logger;

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

        initialiseBuckets(resolveBehavior(player, behavior), contents);
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
            if (item != null && !item.getType().isAir()) {
                drops.add(item.clone());
            }
        }
        this.levels = 0;
        this.progress = 0f;
        // Vanilla caps the experience a death drops; match it so this behaves like a normal death.
        this.droppedExp = Math.min(originalLevels * 7, 100);
    }

    private static ItemStack[] cloneAll(ItemStack[] source) {
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i] == null ? null : source[i].clone();
        }
        return copy;
    }

    /**
     * Move this context into the respawn phase. The player and logger are replaced rather than
     * reused: the {@link Player} object is a different instance after a rejoin, and the log
     * belongs to the respawn being processed.
     */
    public void enterRespawnPhase(Player player, PlayerRespawnEvent event, LoggerInterface logger) {
        this.player = player;
        this.phase = Phase.RESPAWN;
        this.respawnEvent = event;
        this.deathEvent = null;
        this.logger = logger;
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

    @Override public LoggerInterface logger() { return logger; }
}
