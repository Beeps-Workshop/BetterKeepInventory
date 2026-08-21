package com.beepsterr.betterkeepinventory.Library;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/**
 * The active rule tree: built from configuration, then held and reused.
 * <p>
 * Rules used to be re-parsed from YAML on every death <em>and</em> every respawn. Building them
 * once is the point of this class, but the reason it is a class rather than a cached field is
 * the invalidation: plugins can register and unregister conditions and effects at any moment,
 * so "parsed once" has to mean "parsed once per set of registrations", not "parsed once at
 * startup".
 * <p>
 * Deciding <em>when</em> a build happens is {@link Config}'s job. This class only knows how to
 * build, how to hand out what it has, and that what it has may be out of date.
 *
 * <h2>Why the tree is swapped rather than mutated</h2>
 * {@link #rules()} hands out an immutable list. A rebuild replaces the reference instead of
 * editing in place, so a death already walking the previous list finishes against a consistent
 * tree rather than one being rewritten underneath it. That also keeps this safe on Folia
 * without locking readers.
 */
public class Ruleset {

    private final ConfigurationSection rulesSection;

    private volatile List<ConfigRule> rules = List.of();
    private volatile boolean stale = true;

    public Ruleset(ConfigurationSection rulesSection) {
        this.rulesSection = rulesSection;
    }

    /**
     * Mark the tree as needing a rebuild. Cheap and safe to call repeatedly -- several
     * registrations in a row cost one rebuild, not one each.
     */
    public void invalidate() {
        this.stale = true;
    }

    public boolean isStale() {
        return stale;
    }

    /**
     * The rules to evaluate.
     * <p>
     * Rebuilds synchronously if the tree is stale. In normal operation it will not be: the
     * scheduled rebuild runs first and this just returns what is already there. The fallback
     * exists so that a death occurring between an invalidation and its scheduled rebuild sees
     * current rules rather than stale ones.
     */
    public List<ConfigRule> rules() {
        if (stale) {
            build(null);
        }
        return rules;
    }

    /**
     * Parse the configured rules and swap them in.
     * <p>
     * Synchronized so two callers racing -- a scheduled rebuild and a death taking the fallback
     * above -- do the work once rather than both parsing the whole tree.
     *
     * @param nlb where to narrate the parse, or null for a fresh log.
     * @return the rules now active.
     */
    public synchronized List<ConfigRule> build(NestedLogBuilder nlb) {

        NestedLogBuilder log = nlb != null ? nlb : new NestedLogBuilder();

        List<ConfigRule> built = new ArrayList<>();

        if (rulesSection != null) {
            for (String ruleKey : rulesSection.getKeys(false)) {
                ConfigurationSection ruleSection = rulesSection.getConfigurationSection(ruleKey);
                if (ruleSection != null) {
                    built.add(new ConfigRule(ruleSection, null, log));
                }
            }
        }

        this.rules = List.copyOf(built);
        this.stale = false;

        if (nlb == null) {
            log.end();
        }

        return this.rules;
    }

    /**
     * How many top-level rules are currently active. Does not count children.
     */
    public int size() {
        return rules.size();
    }
}
