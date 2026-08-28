package com.beepsterr.betterkeepinventory.Library;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.api.BetterKeepInventoryAPI;
import com.beepsterr.betterkeepinventory.api.Condition;
import com.beepsterr.betterkeepinventory.api.Effect;
import com.beepsterr.betterkeepinventory.api.Exceptions.ConditionParseError;
import com.beepsterr.betterkeepinventory.api.BkiLogger;
import com.beepsterr.betterkeepinventory.api.Phase;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigRule {

    private final String name;
    private final boolean enabled;
    private final ConfigRule parent;

    private List<Condition> conditions = new ArrayList<>();
    private final List<Effect> effects = new ArrayList<>();

    private final List<ConfigRule> children = new ArrayList<>();

    public ConfigRule(ConfigurationSection config, ConfigRule parent, BkiLogger log) {

        this.parent = parent;
        this.name = config.getString("name", "Unnamed Rule");
        this.enabled = config.getBoolean("enabled", false);

        // Parse-time only. The rule tree outlives this logger -- see trigger(), which takes the
        // logger for the death being processed rather than reusing this one.
        if (log == null) {
            log = StructuredLogger.plugin();
        }
        log = log.with(Map.of("rule", name));
        log.debug("parsing rule");

        var api = Bukkit.getServer().getServicesManager().load(BetterKeepInventoryAPI.class);
        if(api == null){
            throw new RuntimeException("BetterKeepInventory API not loaded (?)");
        }

        // Parse conditions
        if (config.isConfigurationSection("conditions")) {
            var condSection = config.getConfigurationSection("conditions");
            assert condSection != null;

            log.debug("parsing conditions", Map.of("count", condSection.getKeys(false).size()));
            for (String key : condSection.getKeys(false))
            {

                if (!api.conditionRegistry().has(key)) {
                    log.warn("skipping rule condition: not a registered condition. Either a plugin"
                            + " needs to provide it, or it does not exist.", Map.of("condition", key));
                    continue;
                }

                ConfigurationSection section = condSection.getConfigurationSection(key);
                if (section == null) {
                    log.warn("skipping rule condition: no configuration section, or it is malformed.",
                            Map.of("condition", key));
                    continue;
                }

                try{
                    Condition cond = api.conditionRegistry().get(key).create(section);
                    conditions.add(cond);
                }catch(ConditionParseError e){
                    log.warn("skipping rule condition: the configuration could not be parsed.",
                            Map.of("condition", key, "error", e.getMessage()));
                }
            }
        }else{
            log.debug("rule has no conditions");
        }


        // Parse effects
        ConfigurationSection effectSection = config.getConfigurationSection("effects");
        if (effectSection != null) {
            log.debug("parsing effects", Map.of("count", effectSection.getKeys(false).size()));
            for (String key : effectSection.getKeys(false)) {

                ConfigurationSection effConfig = effectSection.getConfigurationSection(key);
                if (effConfig == null) continue;

                Effect effect = api.effectRegistry().create(key, effConfig);
                if (effect == null){
                    log.warn("skipping rule effect: not a registered effect. Either a plugin needs"
                            + " to provide it, or it does not exist.", Map.of("effect", key));
                    continue;
                }

                effects.add(effect);
            }
        }else{
            log.debug("rule has no effects");
        }

        // Parse children
        ConfigurationSection childrenSection = config.getConfigurationSection("children");
        if (childrenSection != null) {
            for (String childKey : childrenSection.getKeys(false)) {
                ConfigurationSection childConfig = childrenSection.getConfigurationSection(childKey);
                if (childConfig != null) {
                    children.add(new ConfigRule(childConfig, this, log));
                }
            }
        }

    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }


    /**
     * Evaluate this rule against one death.
     * <p>
     * The context carries the log for the death being processed, rather than the rule holding
     * one: a single rule instance now serves every death.
     */
    public void trigger(DeathContextImpl ctx) {

        // Narrow the context's logger for the whole rule, so that anything an effect writes
        // through ctx.logger() names the rule that invoked it. Restored in the finally below --
        // rules nest, and a child must not leave its own name bound for its siblings.
        BkiLogger outer = ctx.pushLogger(Map.of("rule", name));
        try {

            BkiLogger logger = ctx.logger();

            if (!isEnabled()) {
                logger.debug("rule skipped (enabled: false)");
                return;
            }

            if (!applies(ctx)) {
                logger.debug("rule skipped: not all conditions were met");
                return;
            }

            logger.debug("rule matched: all conditions met");

            for (Effect effect : effects) {
                BkiLogger outerEffect = ctx.pushLogger(Map.of("effect", effect.getClass().getSimpleName()));
                try {
                    if (ctx.phase() == Phase.DEATH) {
                        effect.onDeath(ctx);
                    } else {
                        effect.onRespawn(ctx);
                    }
                } finally {
                    ctx.popLogger(outerEffect);
                }
            }

            for (ConfigRule child : children) {
                child.trigger(ctx);
            }

        } finally {
            ctx.popLogger(outer);
        }

    }

    /**
     * Whether this rule applies to the death in progress.
     * <p>
     * Conditions are evaluated once, in the death phase, and the answer is remembered. The
     * respawn phase replays it rather than asking again -- several conditions describe the
     * circumstances of the death, and re-checking them against a player who has since respawned
     * somewhere else gives a different answer to the same question. A rule gated on `worlds`
     * would fire its `hunger` at death and then decline to restore it at respawn.
     */
    private boolean applies(DeathContextImpl ctx) {

        if (ctx.phase() == Phase.RESPAWN) {
            return ctx.hasMatched(this);
        }

        if (!conditions.isEmpty() && !conditions.stream().allMatch(c -> c.check(ctx))) {
            return false;
        }

        ctx.recordMatch(this);
        return true;
    }

    @Override
    public String toString() {
        return parent != null ? "ConfigRule{" + parent.getName() + " > " + name + "}" : "ConfigRule{" + name + "}";
    }
}
