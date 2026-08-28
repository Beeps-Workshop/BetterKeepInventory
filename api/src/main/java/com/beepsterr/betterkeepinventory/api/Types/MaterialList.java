package com.beepsterr.betterkeepinventory.api.Types;

import com.beepsterr.betterkeepinventory.api.Exceptions.TypeError;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MaterialList {

    List<Material> allowedMaterials = new ArrayList<>();

    public MaterialList(List<String> materials){
        this.parse(materials);
    }

    public void parse(List<String> materials){
        for(String matStr : materials){
            this.parse(matStr);
        }
    }

    /**
     * Parse a single material string and add or remove it from the allowed materials list.
     * <p>
     * Supported input formats:
     * <ul>
     *     <li>{@code *} — include all non-legacy materials. This clears the current list and
     *     adds every non-legacy {@link Material}.</li>
     *     <li>{@code !<material>} — exclude a specific material or material group from the
     *     current list.</li>
     *     <li>{@code G:<group>} — include (or, when prefixed with {@code !}, exclude) a named
     *     material group as understood by {@link MaterialGroups#parse(String)}.</li>
     *     <li>Plain material names such as {@code OAK_LOG}, matching the enum constants in
     *     {@link Material} (case-insensitive).</li>
     *     <li>Minecraft ID format such as {@code minecraft:oak_log}, as supported by
     *     {@link Material#matchMaterial}.</li>
     * </ul>
     * <p>
     * Behavior on invalid input:
     * <ul>
     *     <li>If {@code material} is {@code null} or blank after trimming, this method does
     *     nothing.</li>
     *     <li>If the string does not correspond to a valid {@link Material}, a recognized
     *     Minecraft ID, or a valid group name, it is silently ignored; no exception is thrown
     *     and {@link #allowedMaterials} is left unchanged.</li>
     *     <li>If a material or group name is unknown or cannot be parsed, it is skipped and
     *     the method returns without modifying the list.</li>
     * </ul>
     *
     * @param material the material definition string to parse
     */
    public void parse(String material){

        if (material == null) return;
        String s = material.trim();
        if (s.isEmpty()) return;

        if (s.equals("*")) {
            allowedMaterials.clear();
            allowedMaterials.addAll(
                    Arrays.stream(Material.values())
                            .filter(m -> !m.isLegacy())
                            .toList()
            );
            return;
        }

        boolean exclude = s.startsWith("!");
        if (exclude) {
            s = s.substring(1).trim();
            if (s.isEmpty()) return;
        }

        if(s.startsWith("G:")){
            try {
                s = s.substring(2).trim();
                List<Material> group = MaterialGroups.parse(s);
                if (exclude) {
                    allowedMaterials.removeAll(group);
                } else {
                    for (Material m : group) {
                        if (!allowedMaterials.contains(m)) {
                            allowedMaterials.add(m);
                        }
                    }
                }
                return;
            } catch (TypeError ignored) {
                // invalid group - cry about it
            }
        }

        Material mat = Material.matchMaterial(s);
        if (mat == null) {
            // unknown material - sucks to suck
            return;
        }

        if (exclude) {
            allowedMaterials.remove(mat);
        } else {
            if (!allowedMaterials.contains(mat)) {
                allowedMaterials.add(mat);
            }
        }

    }

    public void add(Material material){
        if (!allowedMaterials.contains(material)) {
            allowedMaterials.add(material);
        }
    }

    public void remove(Material material){
        allowedMaterials.remove(material);
    }

    public List<Material> getMaterials(){
        return allowedMaterials;
    }

    /**
     * The name an item carries itself: the custom_name component first, then the
     * item_name component, which is what most custom item plugins set.
     * Returns null when the item only has its material name.
     */
    public static String GetCustomName(ItemMeta meta){
        if (meta == null) return null;
        if (meta.hasDisplayName()) return meta.getDisplayName();
        if (meta.hasItemName()) return meta.getItemName();
        return null;
    }

    // TODO: When dropping 1.19, migrate to use Material#GetTranslationKey()
    public static String GetName(ItemStack item){
        String customName = GetCustomName(item.getItemMeta());
        if (customName != null) return customName;

        // Fallback to a readable name from the enum
        return Arrays.stream(item.getType().toString().split("_"))
                .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

}
