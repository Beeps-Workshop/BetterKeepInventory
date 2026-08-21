package com.beepsterr.betterkeepinventory.Library;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.api.BetterKeepInventoryAPI;
import com.beepsterr.betterkeepinventory.api.Condition;
import com.beepsterr.betterkeepinventory.api.Effect;
import com.beepsterr.betterkeepinventory.api.Exceptions.ConditionParseError;
import com.beepsterr.betterkeepinventory.api.LoggerInterface;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ConfigRule {

    private final String name;
    private final boolean enabled;
    private final ConfigRule parent;

    private List<Condition> conditions = new ArrayList<>();
    private final List<Effect> effects = new ArrayList<>();

    private final List<ConfigRule> children = new ArrayList<>();

    public ConfigRule(ConfigurationSection config, ConfigRule parent, NestedLogBuilder nlb) {

        this.parent = parent;
        this.name = config.getString("name", "Unnamed Rule");
        this.enabled = config.getBoolean("enabled", false);

        // Parse-time only. The rule tree outlives this logger -- see trigger(), which takes the
        // logger for the death being processed rather than reusing this one.
        if (nlb == null) {
            nlb = new NestedLogBuilder(Level.FINE);
        }
        nlb.child("Parsing Rule '" + name + "'");

        var api = Bukkit.getServer().getServicesManager().load(BetterKeepInventoryAPI.class);
        if(api == null){
            throw new RuntimeException("BetterKeepInventory API not loaded (?)");
        }

        // Parse conditions
        if (config.isConfigurationSection("conditions")) {
            var condSection = config.getConfigurationSection("conditions");
            assert condSection != null;

            nlb.child("Conditions (" + condSection.getKeys(false).size() + ")");
            for (String key : condSection.getKeys(false))
            {

                nlb.log("Parsing condition '" + key + "'");
                if (!api.conditionRegistry().has(key)) {
                    nlb.cont(Level.WARNING, "'" + key + "' is not a registered condition");
                    nlb.cont(Level.WARNING, "Either you need a plugin to provide it, or it does not exist");
                    nlb.cont("This condition is being treated as if it does not exist (skipping)");
                    continue;
                }

                ConfigurationSection section = condSection.getConfigurationSection(key);
                if (section == null) {
                    nlb.cont(Level.WARNING, "'" + key + "' is not configured properly.");
                    nlb.cont(Level.WARNING, "Either you did not provide a configuration section, or it is malformed.");
                    nlb.cont("This condition is being treated as if it does not exist (skipping)");
                    continue;
                }

                try{
                    Condition cond = api.conditionRegistry().get(key).create(section);
                    conditions.add(cond);
                }catch(ConditionParseError e){
                    nlb.cont(Level.WARNING, "'" + key + "' could not be parsed.");
                    nlb.cont(Level.WARNING, "The configuration is malformed.");
                    nlb.cont(e.getMessage());
                }
            }
            nlb.parent();
        }else{
            nlb.log("No conditions defined in this rule.");
        }


        // Parse effects
        ConfigurationSection effectSection = config.getConfigurationSection("effects");
        if (effectSection != null) {
            nlb.child("Effects (" + effectSection.getKeys(false).size() + ")");
            for (String key : effectSection.getKeys(false)) {

                nlb.log("Parsing effect '" + key + "'");
                ConfigurationSection effConfig = effectSection.getConfigurationSection(key);
                if (effConfig == null) continue;

                Effect effect = api.effectRegistry().create(key, effConfig);
                if (effect == null){
                    nlb.cont(Level.WARNING, "'" + key + "' is not a registered effect");
                    nlb.cont(Level.WARNING, "Either you need a plugin to provide it, or it does not exist");
                    nlb.cont("This effect is being treated as if it does not exist (skipping)");
                    continue;
                }

                effects.add(effect);
            }
            nlb.parent();
        }else{
            nlb.log("No effects defined in this rule.");
        }

        nlb.spacer();

        // Parse children
        ConfigurationSection childrenSection = config.getConfigurationSection("children");
        if (childrenSection != null) {
            for (String childKey : childrenSection.getKeys(false)) {
                ConfigurationSection childConfig = childrenSection.getConfigurationSection(childKey);
                if (childConfig != null) {
                    children.add(new ConfigRule(childConfig, this, nlb));
                }
            }
        }

        nlb.parent();

    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }


    /**
     * @param logger the log for the death currently being processed. Passed in rather than held
     *               on the rule, because one rule instance now serves every death.
     */
    public void trigger(Player ply, PlayerDeathEvent deathEvent, PlayerRespawnEvent respawnEvent, LoggerInterface logger) {

        logger.child("Executing Rule '" + name + "'");
        BetterKeepInventory plugin = BetterKeepInventory.getInstance();

        if (!isEnabled()) {
            logger.log("Skipped execution of rule '" + name + "' (enabled: false)");
            return;
        }

        // log all conditions that are to be checked

        if (conditions.isEmpty() || conditions.stream().allMatch(c -> c.check(ply, deathEvent, respawnEvent, logger))) {
            logger.log("All conditions met for rule '" + name + "'");

            if (deathEvent != null) {
                for (Effect effect : effects) {
                    logger.child("Effect: " + effect.getClass());
                    effect.onDeath(ply, deathEvent, logger);
                    logger.parent();
                }
            }

            if (respawnEvent != null) {
                for (Effect effect : effects) {
                    logger.child("Effect: " + effect.getClass());
                    effect.onRespawn(ply, respawnEvent, logger);
                    logger.parent();
                }
            }

            for (ConfigRule child : children) {
                child.trigger(ply, deathEvent, respawnEvent, logger);
            }

        } else {
            logger.log("Not all conditions were met, skipping effects.");
        }

        logger.parent();

    }

    @Override
    public String toString() {
        return parent != null ? "ConfigRule{" + parent.getName() + " > " + name + "}" : "ConfigRule{" + name + "}";
    }
}
