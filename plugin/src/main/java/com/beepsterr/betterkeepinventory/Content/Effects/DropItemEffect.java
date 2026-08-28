package com.beepsterr.betterkeepinventory.Content.Effects;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.api.DeathContext;
import com.beepsterr.betterkeepinventory.api.Effect;
import com.beepsterr.betterkeepinventory.api.Types.MaterialList;
import com.beepsterr.betterkeepinventory.api.Types.SlotType;
import com.beepsterr.betterkeepinventory.api.Utilities;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Moves items from what the player keeps to what hits the ground.
 * <p>
 * Nothing is spawned here. The effect only rearranges the two buckets on the context; the
 * application step at the end of the death is what hands the result to the world.
 */
public class DropItemEffect implements Effect {

    public enum Mode {
        SIMPLE, PERCENTAGE, ALL
    }

    private final Mode mode;
    private final float min;
    private final float max;
    private List<String> nameFilters = List.of();
    private List<String> loreFilters = List.of();
    private SlotType slots = new SlotType(List.of());
    private MaterialList items = new MaterialList(List.of());

    public DropItemEffect(ConfigurationSection config) {
        this.mode = Mode.valueOf(config.getString("mode", "SIMPLE").toUpperCase());
        this.min = (float) config.getDouble("min", 0.0);
        this.max = (float) config.getDouble("max", 0.0);

        ConfigurationSection filters = config.getConfigurationSection("filters");
        if(filters != null) {
            this.slots = new SlotType(Utilities.ConfigList(filters, "slots"));
            this.items = new MaterialList(Utilities.ConfigList(filters, "items"));
            this.nameFilters = Utilities.ConfigList(filters, "name");
            this.loreFilters = Utilities.ConfigList(filters, "lore");
        }
    }

    @Override
    public void onRespawn(DeathContext ctx) {
        // Nothing on respawn
    }

    @Override
    public void onDeath(DeathContext ctx) {

        BetterKeepInventory plugin = BetterKeepInventory.getInstance();
        Player ply = ctx.player();
        Random rng = plugin.rng;

        List<Integer> dropSlots = this.slots.getSlotIds();
        List<Material> dropItems = items.getMaterials();

        ItemStack[] inventory = ctx.inventory();

        for (int i = 0; i < inventory.length; i++) {

            ItemStack item = inventory[i];
            if (item == null || item.getType().isAir()) continue;

            if (!matchesFilters(plugin, ply, item, i, dropItems, dropSlots)) continue;

            if (mode == Mode.ALL) {
                ctx.drops().add(item);
                inventory[i] = null;
                continue;
            }

            int inventoryCount = item.getAmount();
            int removalCount = switch (mode) {
                case SIMPLE -> (int) (min + (max - min) * rng.nextDouble());
                case PERCENTAGE -> (int) (inventoryCount * ((min + (max - min) * rng.nextDouble()) / 100.0));
                default -> 0;
            };

            if (removalCount <= 0) continue;
            if (removalCount > inventoryCount) removalCount = inventoryCount;

            Map<String, String> replacements = new HashMap<>();
            replacements.put("amount", String.valueOf(removalCount));
            replacements.put("item", MaterialList.GetName(item));
            plugin.config.sendMessage(ply, "effects.drop", replacements);

            plugin.debug(ply, "DropItemEffect: Dropping " + removalCount + " items from slot " + i + " (" + item.getType() + ")");

            ItemStack moved = item.clone();
            moved.setAmount(removalCount);
            ctx.drops().add(moved);

            if (inventoryCount - removalCount == 0) {
                inventory[i] = null;
            } else {
                item.setAmount(inventoryCount - removalCount);
            }
        }
    }

    private boolean matchesFilters(BetterKeepInventory plugin, Player ply, ItemStack item, int slot,
                                   List<Material> dropItems, List<Integer> dropSlots) {

        if (!dropItems.isEmpty() && !dropItems.contains(item.getType())) {
            plugin.debug(ply, "Drop skipped due to item filter: " + item.getType());
            return false;
        }
        if (!dropSlots.isEmpty() && !dropSlots.contains(slot)) {
            plugin.debug(ply, "Drop skipped due to slot filter: " + item.getType() + " at slot " + slot);
            return false;
        }

        var meta = item.getItemMeta();
        if (meta == null) return true;

        if (!nameFilters.isEmpty() && !Utilities.advancedStringCompare(meta.getDisplayName(), nameFilters)) {
            plugin.debug(ply, "Drop skipped due to name filter: " + item.getType() + " with name " + meta.getDisplayName());
            return false;
        }

        if (meta.getLore() != null && !loreFilters.isEmpty()) {
            for (String lore : meta.getLore()) {
                if (!Utilities.advancedStringCompare(lore, loreFilters)) {
                    plugin.debug(ply, "Drop skipped due to lore filter: " + item.getType() + " with lore " + lore);
                    return false;
                }
            }
        }

        return true;
    }
}
