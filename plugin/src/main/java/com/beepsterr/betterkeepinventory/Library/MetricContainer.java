package com.beepsterr.betterkeepinventory.Library;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.Library.Versions.VersionChannel;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bstats.bukkit.Metrics;

import java.util.concurrent.Callable;

public class MetricContainer {

    public int deathsProcessed = 0;
    public int durabilityPointsLost = 0;

    Metrics metrics;
    public MetricContainer(){

        try {
            metrics = new Metrics(BetterKeepInventory.getInstance(), 11596);

            metrics.addCustomChart(new SingleLineChart("deaths_processed", new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    int amount = deathsProcessed;
                    deathsProcessed = 0;
                    return amount;
                }
            }));

            metrics.addCustomChart(new SingleLineChart("durability_points_lost", new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    int amount = durabilityPointsLost;
                    durabilityPointsLost = 0;
                    return amount;
                }
            }));

            metrics.addCustomChart(new SimplePie("version_checker_channel", new Callable<String>() {
                @Override
                public String call() throws Exception {
                    Config config = Config.getInstance();
                    if (config == null) {
                        return null;
                    }
                    VersionChannel channel = config.getNotifyChannel();
                    return channel == null ? null : channel.name().toLowerCase();
                }
            }));

            metrics.addCustomChart(new SimplePie("rules_count", new Callable<String>() {
                @Override
                public String call() throws Exception {
                    Config config = Config.getInstance();
                    return config == null ? null : ruleCountScale(config.countRules());
                }
            }));
        } catch (Throwable t) {
            BetterKeepInventory.getInstance().getLogger().warning("bStats metrics disabled: " + t.getMessage());
        }

    }

    static String ruleCountScale(int count) {
        if (count <= 0)  return "None";
        if (count == 1)  return "Single";
        if (count <= 3)  return "Light";
        if (count <= 9)  return "Medium";
        if (count <= 20) return "Heavy";
        return "Extreme";
    }
}
