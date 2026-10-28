package com.beepsterr.betterkeepinventory.Library;

import com.beepsterr.betterkeepinventory.BetterKeepInventory;
import org.bukkit.Bukkit;

import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

public class Debugger {

    private final java.util.List<String> verboseTargets = new java.util.ArrayList<>();
    private static final int MAX_MESSAGES = 1000;
    private String[] messages = new String[MAX_MESSAGES];
    private int messageCount = 0;
    private int nextIndex = 0;

    public Debugger() {
        reset();
    }

    private String getHeader(){

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());

        String header = "=== SYSTEM INFO";
        header += "\nServer Implementation: " + Bukkit.getServer().getClass().getPackage().getName() + " (" + BetterKeepInventory.getScheduler().getImplType().toString() + ")";
        header += "\nServer Version: " + Bukkit.getVersion();
        header += "\nPlugin Version: " + BetterKeepInventory.getInstance().getDescription().getVersion();
        header += "\nJava Version: " + System.getProperty("java.version");
        header += "\nOS: " + System.getProperty("os.name") + " "+ System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")";
        header += "\nCPU Cores: " + Runtime.getRuntime().availableProcessors();
        header += "\nTime: " + nowAsISO;
        return header;
    }

    private String getConfig() {
        String prefix = "=== CONFIGURATION\n";
        File configFile = new File(BetterKeepInventory.instance.getDataFolder(), "config.yml");
        if(!configFile.exists()){
            return prefix + "No config file found.";
        }
        try {
            return prefix + java.nio.file.Files.readString(configFile.toPath());
        } catch (IOException e) {
            return prefix + "Error reading config file: " + e.getMessage();
        }
    }

    public void reset() {
        this.messages = new String[MAX_MESSAGES];
    }

    private String getContent(){
        String log = "BetterKeepInventory DDF 1.0\n";
        log += getHeader() + "\n\n";
        log += getConfig() + "\n\n";
        log += "=== LOGS\n";

        int start = (messageCount == MAX_MESSAGES) ? nextIndex : 0;
        int count = messageCount;
        for (int i = 0; i < count; i++) {
            int idx = (start + i) % MAX_MESSAGES;
            log += messages[idx] + "\n";
        }
        return log;
    }

    public void AddLine(String line) {
        if (nextIndex >= MAX_MESSAGES) {
            nextIndex = 0; // safety reset :3c
        }

        messages[nextIndex] = line;

        nextIndex = (nextIndex + 1) % MAX_MESSAGES;
        if (messageCount < MAX_MESSAGES) {
            messageCount++;
        }

        Broadcast(line);
    }

    private void Broadcast(String message) {
        for(String target : verboseTargets){
            Player player = Bukkit.getPlayer(UUID.fromString(target));
            if(player != null && player.isOnline()){
                player.sendMessage(message);
            }
        }
    }

    public void toggleVerbosePlayer(Player player){
        this.toggleVerbosePlayer(player.getUniqueId());
    }

    public void toggleVerbosePlayer(UUID playerUUID){
        if(!isVerbose(playerUUID)){
            verboseTargets.add(playerUUID.toString());
            BetterKeepInventory.getInstance().getLogger().info("Enabled verbose debugging for player " + playerUUID);
        } else {
            verboseTargets.remove(playerUUID.toString());
            BetterKeepInventory.getInstance().getLogger().info("Disabling verbose debugging for player " + playerUUID);
        }
    }

    private boolean isVerbose(UUID playerUUID){
        for(String target : verboseTargets){
            if(target.equals(playerUUID.toString())){
                return true;
            }
        }
        return false;
    }

    public File write() {

        String log = getContent();

        File dumpFolder = new File(BetterKeepInventory.instance.getDataFolder(), "dumps");
        if (!dumpFolder.exists()) {
            dumpFolder.mkdirs();
        }
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File file = new File(dumpFolder, timestamp + ".log");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(log);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write debugger log to file: " + e.getMessage());
        }
        return file;
    }

}
