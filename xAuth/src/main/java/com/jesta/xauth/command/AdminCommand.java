package com.jesta.xauth.command;

import com.jesta.xauth.xAuth;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class AdminCommand implements CommandExecutor {
    private final xAuth plugin;

    public AdminCommand(xAuth plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return true;
        }

        if (args[0].equalsIgnoreCase("version")) {
            sender.sendMessage("§axAuth Version: §f" + plugin.getDescription().getVersion());
            return true;
        }

        if (!sender.hasPermission("xauth.admin")) {
            sender.sendMessage("§cBu komutu kullanmak icin yetkiniz yok.");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.getConfigManager().reload();
            sender.sendMessage("§axAuth yapilandirmasi yenilendi.");
            return true;
        }

        if (args[0].equalsIgnoreCase("logout") && args.length == 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null) {
                plugin.getDataManager().setAuthenticated(target.getUniqueId(), false);
                target.kickPlayer(plugin.getConfigManager().getMessage("kick-logout"));
                sender.sendMessage("§a" + target.getName() + " adli oyuncunun cikisi yapildi.");
            } else {
                sender.sendMessage("§cOyuncu bulunamadi.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("deldataall")) {
            plugin.getDataManager().deleteAllData();
            sender.sendMessage(plugin.getConfigManager().getMessage("data-all-deleted"));
            return true;
        }

        if (args[0].equalsIgnoreCase("deldata") && args.length == 2) {
            plugin.getDataManager().deleteData(args[1]);
            sender.sendMessage(plugin.getConfigManager().getMessage("data-deleted"));
            return true;
        }

        if (args[0].equalsIgnoreCase("setspawn") && sender instanceof Player) {
            Player player = (Player) sender;
            plugin.getConfig().set("spawn", player.getLocation());
            plugin.saveConfig();
            player.sendMessage(plugin.getConfigManager().getMessage("spawn-set"));
            return true;
        }

        if (args[0].equalsIgnoreCase("blacklist") && args.length == 2) {
            List<String> blacklist = plugin.getConfig().getStringList("blacklist");
            String targetName = args[1];
            if (!blacklist.contains(targetName)) {
                blacklist.add(targetName);
                plugin.getConfig().set("blacklist", blacklist);
                plugin.saveConfig();
            }
            sender.sendMessage(plugin.getConfigManager().getMessage("player-blacklisted"));
            return true;
        }

        return true;
    }
}