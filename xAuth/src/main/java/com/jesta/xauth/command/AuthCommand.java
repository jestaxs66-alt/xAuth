package com.jesta.xauth.command;

import com.jesta.xauth.xAuth;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthCommand implements CommandExecutor {
    private final xAuth plugin;
    private final Map<UUID, Integer> attempts = new HashMap<>();

    public AuthCommand(xAuth plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (plugin.getDataManager().isAuthenticated(uuid)) {
            player.sendMessage(plugin.getConfigManager().getMessage("already-logged-in"));
            return true;
        }

        if (command.getName().equalsIgnoreCase("register")) {
            if (plugin.getDataManager().isRegistered(player.getName())) {
                return true;
            }
            if (args.length != 2) {
                player.sendMessage(plugin.getConfigManager().getMessage("register-prompt"));
                return true;
            }

            String pass1 = args[0];
            String pass2 = args[1];

            if (!pass1.equals(pass2)) {
                player.sendMessage(plugin.getConfigManager().getMessage("passwords-not-match"));
                return true;
            }

            int min = plugin.getConfig().getInt("settings.password.min-length", 6);
            int max = plugin.getConfig().getInt("settings.password.max-length", 16);
            if (pass1.length() < min || pass1.length() > max) {
                player.sendMessage(plugin.getConfigManager().getMessage("invalid-length")
                        .replace("%min%", String.valueOf(min))
                        .replace("%max%", String.valueOf(max)));
                return true;
            }

            if (plugin.getConfig().getStringList("settings.password.blacklist").contains(pass1)) {
                player.sendMessage(plugin.getConfigManager().getMessage("blacklisted-password"));
                return true;
            }

            plugin.getDataManager().registerPlayer(player.getName(), pass1);
            authenticatePlayer(player);
            player.sendMessage(plugin.getConfigManager().getMessage("success-register"));
            return true;
        }

        if (command.getName().equalsIgnoreCase("login")) {
            if (!plugin.getDataManager().isRegistered(player.getName())) {
                return true;
            }
            if (args.length != 1) {
                player.sendMessage(plugin.getConfigManager().getMessage("login-prompt"));
                return true;
            }

            if (plugin.getDataManager().checkPassword(player.getName(), args[0])) {
                authenticatePlayer(player);
                attempts.remove(uuid);
                player.sendMessage(plugin.getConfigManager().getMessage("success-login"));
            } else {
                int maxAttempts = plugin.getConfig().getInt("settings.auth.max-wrong-attempts", 3);
                int current = attempts.getOrDefault(uuid, 0) + 1;
                attempts.put(uuid, current);

                if (current >= maxAttempts) {
                    player.kickPlayer("Max wrong attempts!");
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("wrong-password")
                            .replace("%attempts%", String.valueOf(maxAttempts - current)));
                }
            }
            return true;
        }
        return true;
    }

    private void authenticatePlayer(Player player) {
        plugin.getDataManager().setAuthenticated(player.getUniqueId(), true);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
    }
}