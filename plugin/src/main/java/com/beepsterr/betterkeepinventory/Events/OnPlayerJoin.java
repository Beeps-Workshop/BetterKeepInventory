package com.beepsterr.betterkeepinventory.Events;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import com.beepsterr.betterkeepinventory.Library.Versions.VersionChecker;
import com.beepsterr.betterkeepinventory.Library.ConfigRule;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class OnPlayerJoin implements Listener {

    BetterKeepInventory plugin;


    public OnPlayerJoin(BetterKeepInventory main){
        plugin = main;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player ply = event.getPlayer();
        if(ply.hasPermission("betterkeepinventory.version.notify")){
            BetterKeepInventory.getScheduler().getScheduler().runAsync((consumer) -> {

                try{
                    // Yes, We're using Thread.sleep.
                    // I Don't think FoliaLib has a way to schedule delayed tasks at this point.
                    // I didn't want to spend a lot of time figuring out a way to do it the right way
                    // And since this is a thread that only gets spawned when "admin" players join
                    // It's a non-issue for now.
                    Thread.sleep(1000*5);
                }catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                }


                // One read: the checker thread can clear this between an "is there one?" and a
                // "what is it?".
                VersionChecker.Update available = plugin.versionChecker == null
                        ? null
                        : plugin.versionChecker.getAvailableUpdate();

                if(available != null) {
                    ply.sendMessage(ChatColor.YELLOW + "A new version of BetterKeepInventory is available!");

                    // Clickable, and pointing at this exact version rather than the list. Being
                    // told an update exists without being told where to get it just means a trip
                    // to a search engine.
                    TextComponent download = new TextComponent(
                            available.version() + " (Installed: " + plugin.version + ")");
                    download.setColor(ChatColor.GREEN);
                    download.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, available.downloadUrl()));
                    download.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder(ChatColor.GOLD + "Download " + available.version()).create()));

                    ply.spigot().sendMessage(download);
                }
            });

        }

    }


}
