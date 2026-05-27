package com.jesta.xauth.manager;

import com.jesta.xauth.xAuth;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {
    private final xAuth plugin;
    private FileConfiguration messages;
    private File messagesFile;

    public ConfigManager(xAuth plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        loadMessages();
    }

    public void reload() {
        plugin.reloadConfig();
        loadMessages();
    }

    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String getMessage(String path) {
        String prefix = messages.getString("prefix", "");
        String msg = messages.getString("messages." + path, "");
        return ChatColor.translateAlternateColorCodes('&', prefix + msg);
    }
}