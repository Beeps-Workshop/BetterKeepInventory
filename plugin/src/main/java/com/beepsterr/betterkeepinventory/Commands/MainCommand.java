package com.beepsterr.betterkeepinventory.Commands;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.Exceptions.UnloadableConfiguration;
import com.beepsterr.betterkeepinventory.Library.Versions.Version;
import com.beepsterr.betterkeepinventory.Library.Versions.VersionChannel;
import com.beepsterr.betterkeepinventory.api.BetterKeepInventoryAPI;
import com.beepsterr.betterkeepinventory.Library.Config;
import com.beepsterr.betterkeepinventory.Library.Versions.VersionChecker;
import com.beepsterr.betterkeepinventory.api.Registry;
import com.beepsterr.betterkeepinventory.api.RegistryEntry;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.*;

public class MainCommand implements CommandExecutor, TabCompleter {

    private boolean checkPerm(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender instanceof ConsoleCommandSender;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if(args.length > 0){
            return switch (args[0]) {
                case "reload" -> OnReloadCommand(sender, command, label, args);
                case "registry" -> OnRegistryCommand(sender, command, label, args);
                case "debug" -> OnDebugCommand(sender, command, label, args);
                default -> {
                    sender.sendMessage(ChatColor.RED + "Unknown command. Use /betterkeepinventory for help.");
                    yield true;
                }
            };
        }else{

            if(!checkPerm(sender, "betterkeepinventory.command.help")){
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }

            BetterKeepInventory plugin = BetterKeepInventory.getInstance();

            String title = "" + ChatColor.BOLD + ChatColor.GREEN + plugin.getDescription().getName();

            if(checkPerm(sender, "betterkeepinventory.version")){
                title += " " + ChatColor.GRAY + "[" + ChatColor.LIGHT_PURPLE + "v" + plugin.version.toString() + ChatColor.GRAY + "]";
            }

            TextComponent helpTitleComponent = new TextComponent(title);
            sender.spigot().sendMessage(helpTitleComponent);

            // only show section to players with version permission
            if(checkPerm(sender, "betterkeepinventory.version")){

                // Update checking
                // Null when update checks are off -- the checker is only constructed when the
                // channel is something other than NONE.
                if(plugin.versionChecker != null && plugin.versionChecker.channel != VersionChannel.NONE) {
                    boolean isUpdateAvailable = plugin.versionChecker.IsUpdateAvailable();
                    if (!isUpdateAvailable) {
                        sender.sendMessage("" + ChatColor.GREEN + ChatColor.ITALIC + "No updates available.");
                    }else{

                        // Modrinth rather than SpigotMC: the checker filters by what this server
                        // can run, and Modrinth is where that per-version compatibility lives.
                        String updateURL = "https://modrinth.com/plugin/betterkeepinventory/versions";

                        TextComponent updateAvailableComponent = new TextComponent(plugin.versionChecker.foundVersion.toString() + " is available for download!");
                        updateAvailableComponent.setColor(ChatColor.GOLD);
                        updateAvailableComponent.setItalic(true);
                        updateAvailableComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, updateURL));
                        updateAvailableComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new ComponentBuilder(ChatColor.GOLD + "Download Update").create()));

                        sender.spigot().sendMessage(updateAvailableComponent);
                    }

                }

                sender.sendMessage("");

                // Beta warning
                if("BETA".equals(plugin.version.flavor)){
                    TextComponent betaWarning = new TextComponent("You are running a BETA version of BetterKeepInventory! If things don't work or you find bugs, please report them on Github or Discord!");
                    betaWarning.setColor(ChatColor.RED);
                    betaWarning.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://leafcat.live/discord"));
                    betaWarning.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder(ChatColor.AQUA + "Join Discord").create()));
                    sender.spigot().sendMessage(betaWarning);
                    sender.sendMessage("");
                }

                // SNAPSHOT warning
                if("SNAPSHOT".equals(plugin.version.flavor)){
                    TextComponent betaWarning = new TextComponent("You are running a SNAPSHOT version of BetterKeepInventory! Expect things to not work and report any bugs you find on GitHub or Discord!");
                    betaWarning.setColor(ChatColor.DARK_RED);
                    betaWarning.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://leafcat.live/discord"));
                    betaWarning.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder(ChatColor.AQUA + "Join Discord").create()));
                    sender.spigot().sendMessage(betaWarning);
                    sender.sendMessage("");
                }
            }

            SendLink(sender, "SpigotMC", "https://www.spigotmc.org/resources/betterkeepinventory.93081/");
            SendLink(sender, "Documentation", "https://bki.beepsterr.com");
            SendLink(sender, "Discord", "https://leafcat.live/discord");

            sender.sendMessage("");

            // command index

            if(checkPerm(sender, "betterkeepinventory.command.reload")){
                TextComponent reloadCommand = new TextComponent("/betterki reload");
                reloadCommand.setColor(ChatColor.AQUA);
                reloadCommand.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/betterkeepinventory reload"));
                sender.spigot().sendMessage(reloadCommand);
            }

            if(checkPerm(sender, "betterkeepinventory.command.registry")){
                TextComponent registryEffectsCommand = new TextComponent("/betterki registry effects");
                registryEffectsCommand.setColor(ChatColor.AQUA);
                registryEffectsCommand.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/betterkeepinventory registry effects"));
                sender.spigot().sendMessage(registryEffectsCommand);

                TextComponent registryConditionsCommand = new TextComponent("/betterki registry conditions");
                registryConditionsCommand.setColor(ChatColor.AQUA);
                registryConditionsCommand.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/betterkeepinventory registry conditions"));
                sender.spigot().sendMessage(registryConditionsCommand);
            }

            if(checkPerm(sender, "betterkeepinventory.command.debug.upload")){
                TextComponent registryEffectsCommand = new TextComponent("/betterki debug upload");
                registryEffectsCommand.setColor(ChatColor.AQUA);
                registryEffectsCommand.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/betterkeepinventory debug upload"));
                sender.spigot().sendMessage(registryEffectsCommand);
            }

            if(checkPerm(sender, "betterkeepinventory.command.debug.verbose")){
                TextComponent registryEffectsCommand = new TextComponent("/betterki debug verbose");
                registryEffectsCommand.setColor(ChatColor.AQUA);
                registryEffectsCommand.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/betterkeepinventory debug verbose"));
                sender.spigot().sendMessage(registryEffectsCommand);
            }

        }

        return true;
    }

    public void SendLink(CommandSender sender, String name, String link){
        if(sender instanceof ConsoleCommandSender){
            sender.sendMessage("\uD83C\uDF0E " + name + ChatColor.DARK_GRAY + " ( " + link + " )");
        }else{
            TextComponent linkComponent = new TextComponent("\uD83C\uDF0E " + name);
            linkComponent.setColor(ChatColor.YELLOW);
            linkComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link));
            sender.spigot().sendMessage(linkComponent);
        }
    }

    public boolean OnRegistryCommand(CommandSender sender, Command command, String label, String[] args) {

        if(!checkPerm(sender, "betterkeepinventory.command.registry")){
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        BetterKeepInventoryAPI api = Bukkit.getServer().getServicesManager().load(BetterKeepInventoryAPI.class);
        if (api == null) {
            sender.sendMessage(ChatColor.RED + "BetterKeepInventory API is not available.");
            return true;
        }


        if(args.length < 2){
            sender.sendMessage(ChatColor.RED + "Usage: /betterkeepinventory registry <type>");
            return true;
        }

        switch(args[1].toLowerCase()){
            case "effects":
                printRegistryEntries(sender, "Registered Effects", api.effectRegistry());
                break;
            case "conditions":
                printRegistryEntries(sender, "Registered Conditions", api.conditionRegistry());
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown registry type: " + args[1]);
                return true;
        }

        return true;

    }

    public static <T> void printRegistryEntries(
            CommandSender sender,
            String title,
            Registry<T> registry
    ) {
        Map<String, RegistryEntry<T>> allEntries = registry.getAll();
        sender.sendMessage(ChatColor.GOLD + title + " (" + allEntries.size() + "):");

        for (Map.Entry<String, RegistryEntry<T>> entry : allEntries.entrySet()) {
            String fullKey = entry.getKey();
            RegistryEntry<T> regEntry = entry.getValue();
            Plugin plugin = regEntry.plugin();

            String pluginPrefix = plugin.getName().toLowerCase() + ".";
            String shortKey = fullKey.startsWith(pluginPrefix)
                    ? fullKey.substring(pluginPrefix.length())
                    : fullKey;

            // Bare keys are resolved rather than stored, so ask the registry which entry this
            // one currently points at instead of looking for it in the map.
            RegistryEntry<T> resolved = registry.getFull(shortKey);
            boolean ownsShortKey = resolved != null && resolved.equals(regEntry);

            String display = ChatColor.GREEN + fullKey;
            if (ownsShortKey) {
                display += ChatColor.LIGHT_PURPLE + " (" + shortKey + ")";
            }

            sender.sendMessage(display);

            TextComponent pluginComponent = new TextComponent(plugin.getDescription().getFullName());
            pluginComponent.setColor(ChatColor.GREEN);
            pluginComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/version " + plugin.getName()));
            pluginComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("/version " + plugin.getName()).create()));

            TextComponent label = new TextComponent("provided by: ");
            label.setColor(ChatColor.GRAY);
            label.addExtra(pluginComponent);

            sender.spigot().sendMessage(label);


            sender.sendMessage("");
        }
    }

    public boolean OnReloadCommand(CommandSender sender, Command command, String label, String[] args) {

        if(!checkPerm(sender, "betterkeepinventory.command.reload")){
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        BetterKeepInventory plugin = BetterKeepInventory.getInstance();
        plugin.reloadConfig();

        try {
            plugin.config = new Config(plugin.getConfig(), null);
        }catch (UnloadableConfiguration e) {

            String alert = "\n"
                    + ChatColor.DARK_RED + ChatColor.BOLD + " RELOAD ERROR!!\n"
                    + ChatColor.RED + "BetterKeepInventory encountered a problem while reloading:\n\n"
                    + ChatColor.YELLOW + e.getMessage() + "\n\n"
                    + ChatColor.RED + "The updated configuration was NOT loaded.\n"
                    + ChatColor.RED + "You should fix the issue, and try again\n";

            sender.sendMessage(alert);
            plugin.getLogger().severe(alert);

            return true;
        }

        // Parse the rules here rather than leaving it to the next death, so a broken rule is
        // reported to whoever ran the reload instead of surfacing hours later in a death log.
        plugin.config.buildRules(null);

        sender.sendMessage(ChatColor.GREEN + "Configuration reloaded successfully.");

        // Cancel version checks
        if(plugin.versionChecker != null){
            plugin.versionChecker.CancelCheck();
        }

        // Create a new VersionChecker, which will start checking for updates again
        plugin.versionChecker = new VersionChecker(plugin.config.getNotifyChannel());

        return true;

    }

    public boolean OnDebugCommand(CommandSender sender, Command command, String label, String[] args){

        if(!checkPerm(sender, "betterkeepinventory.command.debug")){
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if(args.length > 1){
            return switch (args[1]) {
                case "upload" -> OnDebugUploadCommand(sender, command, label, args);
                case "verbose" -> OnDebugVerboseCommand(sender, command, label, args);
                default -> {
                    sender.sendMessage(ChatColor.RED + "Unknown command. Use /betterkeepinventory for help.");
                    yield true;
                }
            };
        }

        sender.sendMessage(ChatColor.RED + "Unknown command. Use /betterkeepinventory for help.");
        return true;

    }

    public boolean OnDebugUploadCommand(CommandSender sender, Command command, String label, String[] args){

        if(!checkPerm(sender, "betterkeepinventory.command.debug.upload")){
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        sender.sendMessage(ChatColor.GRAY + "Generating...");
        try{
            BetterKeepInventory.getInstance().debugger.write();
        }catch (RuntimeException e){
            sender.sendMessage(ChatColor.RED + "Error: " + e.getMessage());
            return true;
        }

        sender.sendMessage(ChatColor.GRAY + "Uploading...");
        try {
            String key = BetterKeepInventory.getInstance().debugger.upload();
            TextComponent textComponent = new TextComponent("Uploaded! https://bin.lunega.dev/" + key + " (click to copy URL)");
            textComponent.setColor(ChatColor.GREEN);
            textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, "https://bin.lunega.dev/" + key ));
            sender.spigot().sendMessage(textComponent);
        }catch (Exception e){
            sender.sendMessage(ChatColor.RED + "Error: " + e.getMessage());
            return true;
        }


        return true;

    }

    public boolean OnDebugVerboseCommand(CommandSender sender, Command command, String label, String[] args){

        if(!(sender instanceof Player)){
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if(!checkPerm(sender, "betterkeepinventory.command.debug.verbose")){
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        sender.sendMessage(ChatColor.GRAY + "Toggling verbose debug mode...");
        BetterKeepInventory plugin = BetterKeepInventory.getInstance();
        plugin.debugger.toggleVerbosePlayer((Player) sender);

        return true;

    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("betterkeepinventory.command.help")) return Collections.emptyList();

        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (sender.hasPermission("betterkeepinventory.command.reload")) options.add("reload");
            if (sender.hasPermission("betterkeepinventory.command.registry")) options.add("registry");
            if (sender.hasPermission("betterkeepinventory.command.debug")) options.add("debug");

            return options.stream()
                    .filter(opt -> opt.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("registry")) {
            if (!sender.hasPermission("betterkeepinventory.command.registry")) return Collections.emptyList();

            return List.of("effects", "conditions").stream()
                    .filter(opt -> opt.startsWith(args[1].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            if (!sender.hasPermission("betterkeepinventory.command.debug")) return Collections.emptyList();

            return List.of("upload", "verbose").stream()
                    .filter(opt -> opt.startsWith(args[1].toLowerCase()) && sender.hasPermission("betterkeepinventory.command.debug." + opt))
                    .toList();
        }

        return Collections.emptyList();
    }
}
