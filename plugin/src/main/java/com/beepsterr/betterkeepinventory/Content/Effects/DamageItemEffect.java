package com.beepsterr.betterkeepinventory.Content.Effects;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.api.Utilities;
import com.beepsterr.betterkeepinventory.api.LoggerInterface;
import com.beepsterr.betterkeepinventory.api.Types.MaterialList;
import com.beepsterr.betterkeepinventory.api.Types.SlotType;
import com.beepsterr.betterkeepinventory.api.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.*;

public class DamageItemEffect implements Effect {

    public enum Mode {
        PERCENTAGE, PERCENTAGE_REMAINING, SIMPLE
    }

    private final Mode mode;
    private final float min;
    private final float max;
    private final boolean useEnchantments;
    private final boolean dontBreak;
    private List<String> nameFilters = List.of();
    private List<String> loreFilters = List.of();
    private SlotType slots = new SlotType(List.of());
    private MaterialList items = new MaterialList(List.of());

    public DamageItemEffect(ConfigurationSection config) {
        this.mode = Mode.valueOf(config.getString("mode", "PERCENTAGE").toUpperCase());
        this.min = (float) config.getDouble("min", 0.0);
        this.max = (float) config.getDouble("max", 0.0);
        this.useEnchantments = config.getBoolean("use_enchantments", false);
        this.dontBreak = config.getBoolean("dont_break", false);

        ConfigurationSection filters = config.getConfigurationSection("filters");
        if(filters != null) {
            this.slots = new SlotType(Utilities.ConfigList(filters, "slots"));
            this.items = new MaterialList(Utilities.ConfigList(filters, "items"));
            this.nameFilters = Utilities.ConfigList(filters, "name");
            this.loreFilters = Utilities.ConfigList(filters, "lore");
        }

    }

    @Override
    public void onRespawn(Player player, PlayerRespawnEvent event, LoggerInterface logger) {
        // This effect doesn't do anything on respawn (yet)
    }

    @Override
    public void onDeath(Player ply, PlayerDeathEvent event, LoggerInterface logger) {
        BetterKeepInventory plugin = BetterKeepInventory.getInstance();
        Random rng = plugin.rng;

        List<Integer> slots = this.slots.getSlotIds();
        List<Material> items = this.items.getMaterials();

        BetterKeepInventory.instance.debug(ply, "The Items Are " + items);
        for (int i = 0; i < ply.getInventory().getSize(); i++) {

            var item = ply.getInventory().getItem(i);
            if(item == null) continue;

            var meta = item.getItemMeta();

            // Check the filters
            if (!items.isEmpty() && !items.contains(item.getType())){
                plugin.debug(ply, "Damage skipped due to item filter: " + item.getType());
                continue;
            };
            if (!slots.isEmpty() && !slots.contains(i)){
                plugin.debug(ply, "Damage skipped due to slot filter: " + item.getType() + " at slot " + i);
                continue;
            };

            if(meta != null){
                if (!nameFilters.isEmpty() && !Utilities.advancedStringCompare(meta.getDisplayName(), nameFilters)){
                    plugin.debug(ply, "Damage skipped due to name filter: " + item.getType() + " with name " + meta.getDisplayName());
                    continue;
                };
                if(meta.getLore() != null){
                    boolean loreFilterMatched = false;
                    for( String lore : meta.getLore()){
                        if (!loreFilters.isEmpty() && !Utilities.advancedStringCompare(lore, loreFilters)) {
                            loreFilterMatched = true;
                        }
                    }
                    if(loreFilterMatched){
                        plugin.debug(ply, "Damage skipped due to lore filter: " + item.getType());
                        continue;
                    }
                }
            }

            if (!(meta instanceof Damageable damageableMeta)){
                plugin.debug(ply, "Damage skipped due to item not being damageable: " + item.getType());
                continue;
            };

            int currentDamageTaken = damageableMeta.getDamage();
            int maxDurability = item.getType().getMaxDurability();
            if (maxDurability <= 0) continue; // item can't hold durability (e.g. blocks/food) — skip so we don't tag it or spam "0 damage"
            int damageToTake = calculateDamage(rng, currentDamageTaken, maxDurability);

            if (damageToTake <= 0) continue;

            damageToTake = applyUnbreaking(item, damageToTake);
            if (damageToTake <= 0) continue; // Unbreaking can reduce damage to 0 — skip so we don't tag the item or spam "0 damage"
            Map<String, String> replacements = new HashMap<>();
            replacements.put("amount", String.valueOf(damageToTake));
            replacements.put("item", MaterialList.GetName(item));

            plugin.metrics.durabilityPointsLost += damageToTake;

            if (maxDurability - currentDamageTaken - damageToTake < 0) {
                if (dontBreak || item.getType() == Material.ELYTRA) { // elytra is special, it doesn't break
                    damageableMeta.setDamage(maxDurability);
                    item.setItemMeta(damageableMeta);
                    plugin.debug(ply, "Item saved from breaking: " + item.getType());
                    plugin.config.sendMessage(ply, "effects.damage", replacements);
                } else {
                    item.setAmount(item.getAmount() - 1);
                    damageableMeta.setDamage(0);
                    item.setItemMeta(meta);
                    ply.getWorld().playSound(ply.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 0.8f);
                    plugin.debug(ply, "Item broke: " + item.getType());
                    plugin.config.sendMessage(ply, "effects.damage_break", replacements);
                }
            } else {
                damageableMeta.setDamage(currentDamageTaken + damageToTake);
                item.setItemMeta(meta);
                plugin.config.sendMessage(ply, "effects.damage", replacements);
            }
        }
    }

    private int calculateDamage(Random rng, int currentDamageTaken, int maxDurability) {
        return switch (mode) {
            case SIMPLE -> (int) (min + (max - min) * rng.nextDouble());
            case PERCENTAGE -> (int) (maxDurability * ((min + (max - min) * rng.nextDouble()) / 100.0));
            case PERCENTAGE_REMAINING -> (int) ((maxDurability - currentDamageTaken) * ((min + (max - min) * rng.nextDouble()) / 100.0));
        };
    }

    private int applyUnbreaking(ItemStack item, int damageToTake) {
        if (!useEnchantments) return damageToTake;
        if (!item.getEnchantments().containsKey(Enchantment.UNBREAKING)) return damageToTake;

        int level = item.getEnchantmentLevel(Enchantment.UNBREAKING);
        if (level > 9) return 0; // interpreted as unbreakable
        return (int) (damageToTake * (1.0 - (0.33 * level)));
    }

}
