package com.jesta.xauth.manager;

import com.jesta.xauth.xAuth;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DataManager {
    private final xAuth plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final Set<UUID> authenticated = new HashSet<>();

    public DataManager(xAuth plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try { 
                dataFile.createNewFile(); 
            } catch (IOException ignored) {}
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private String hashSHA256(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isRegistered(String playerName) {
        return dataConfig.contains(playerName.toLowerCase());
    }

    public boolean checkPassword(String playerName, String password) {
        String hashedPassword = hashSHA256(password);
        return hashedPassword.equals(dataConfig.getString(playerName.toLowerCase()));
    }

    public void registerPlayer(String playerName, String password) {
        dataConfig.set(playerName.toLowerCase(), hashSHA256(password));
        save();
    }

    public void setAuthenticated(UUID uuid, boolean state) {
        if (state) {
            authenticated.add(uuid);
        } else {
            authenticated.remove(uuid);
        }
    }

    public boolean isAuthenticated(UUID uuid) {
        return authenticated.contains(uuid);
    }

    public void deleteData(String playerName) {
        dataConfig.set(playerName.toLowerCase(), null);
        save();
    }

    public void deleteAllData() {
        if (dataFile.exists()) {
            dataFile.delete();
        }
        try { 
            dataFile.createNewFile(); 
            } catch (IOException ignored) {}
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        authenticated.clear();
    }

    private void save() {
        try { 
            dataConfig.save(dataFile); 
        } catch (IOException ignored) {}
    }
}