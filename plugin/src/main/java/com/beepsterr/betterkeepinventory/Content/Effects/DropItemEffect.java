package com.beepsterr.betterkeepinventory.Content.Effects;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.api.Utilities;
import com.beepsterr.betterkeepinventory.api.LoggerInterface;
import com.beepsterr.betterkeepinventory.api.Types.MaterialList;
import com.beepsterr.betterkeepinventory.api.Effect;
import com.beepsterr.betterkeepinventory.api.Types.SlotType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
    public void onRespawn(Player ply, PlayerRespawnEvent event, LoggerInterface logger) {
        // Nothing on respawn
    }

    @Override
    public void onDeath(Player ply, PlayerDeathEvent event, LoggerInterface logger) {
        BetterKeepInventory plugin = BetterKeepInventory.getInstance();
        Random rng = plugin.rng;

        List<Integer> dropSlots = this.slots.getSlotIds();
        List<Material> dropItems = items.getMaterials();

        for (int i = 0; i < ply.getInventory().getSize(); i++) {

            var item = ply.getInventory().getItem(i);
            if(item == null) continue;

            var meta = item.getItemMeta();

            // Check the filters
            if (!dropItems.isEmpty() && !dropItems.contains(item.getType())){
                plugin.debug(ply, "Drop skipped due to item filter: " + item.getType());
                continue;
            };
            if (!dropSlots.isEmpty() && !dropSlots.contains(i)){
                plugin.debug(ply, "Drop skipped due to slot filter: " + item.getType() + " at slot " + i);
                continue;
            };

            if(meta != null){
                if (!nameFilters.isEmpty() && !Utilities.advancedStringCompare(meta.getDisplayName(), nameFilters)){
                    plugin.debug(ply, "Drop skipped due to name filter: " + item.getType() + " with name " + meta.getDisplayName());
                    continue;
                };
                if(meta.getLore() != null){
                    boolean loreFilterMatched = false;
                    for( String lore : meta.getLore()){
                        if (!loreFilters.isEmpty() && !Utilities.advancedStringCompare(lore, loreFilters)) {
                            plugin.debug(ply, "Drop skipped due to lore filter: " + item.getType() + " with lore " + lore);
                            loreFilterMatched = true;
                        }
                    }
                    if(loreFilterMatched){
                        continue;
                    }
                }
            }


            // Drop all
            if (mode == Mode.ALL) {
                ply.getWorld().dropItemNaturally(ply.getLocation(), item);
                ply.getInventory().setItem(i, null);
                continue;
            }

            int inventoryCount = item.getAmount();
            int removalCount = 0;

            switch (mode) {
                case SIMPLE -> removalCount = (int) (min + (max - min) * rng.nextDouble());
                case PERCENTAGE -> {
                    double percentage = min + (max - min) * rng.nextDouble();
                    removalCount = (int) (inventoryCount * (percentage / 100.0));
                }
            }

            if (removalCount <= 0) continue;

            Map<String, String> replacements = new HashMap<>();
            replacements.put("amount", String.valueOf(removalCount));
            replacements.put("item", MaterialList.GetName(item));
            plugin.config.sendMessage(ply, "effects.drop", replacements);

            plugin.debug(ply, "DropItemEffect: Dropping " + removalCount + " items from slot " + i + " (" + item.getType() + ")");

            if (inventoryCount - removalCount < 0) {
                removalCount = inventoryCount;
            }

            if (removalCount == 0) continue;

            var itemClone = item.clone();
            itemClone.setAmount(removalCount);
            ply.getWorld().dropItemNaturally(ply.getLocation(), itemClone);

            if (inventoryCount - removalCount == 0) {
                ply.getInventory().setItem(i, null);
            } else {
                item.setAmount(inventoryCount - removalCount);
                ply.getInventory().setItem(i, item);
            }
        }
    }
}
