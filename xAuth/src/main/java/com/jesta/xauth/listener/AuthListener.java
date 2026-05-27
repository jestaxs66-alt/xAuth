package com.jesta.xauth.listener;

import com.jesta.xauth.xAuth;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthListener implements Listener {
    private final xAuth plugin;
    private final Map<UUID, BukkitRunnable> authTasks = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<String, Long> lastJoinTimes = new HashMap<>();

    public AuthListener(xAuth plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (plugin.getConfig().getBoolean("settings.bot-protection", true)) {
            String ip = event.getAddress().getHostAddress();
            long now = System.currentTimeMillis();
            if (lastJoinTimes.containsKey(ip) && (now - lastJoinTimes.get(ip)) < 1000) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Bot protection enabled. Please wait.");
                return;
            }
            lastJoinTimes.put(ip, now);
        }

        if (plugin.getConfig().getStringList("blacklist").contains(event.getName())) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, plugin.getConfigManager().getMessage("blacklisted-player"));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        plugin.getDataManager().setAuthenticated(uuid, false);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 999999, 1, false, false));
        
        if (plugin.getConfig().contains("spawn") && plugin.getConfig().get("spawn") != null) {
            player.teleport((Location) plugin.getConfig().get("spawn"));
        }

        startAuthTask(player);
    }

    private void startAuthTask(Player player) {
        UUID uuid = player.getUniqueId();
        int timeout = plugin.getConfig().getInt("settings.auth.timeout-seconds", 60);
        boolean useBossBar = plugin.getConfig().getBoolean("settings.auth.use-bossbar", true);
        BossBar bar = null;

        if (useBossBar) {
            bar = Bukkit.createBossBar("", BarColor.RED, BarStyle.SOLID);
            bar.addPlayer(player);
            bossBars.put(uuid, bar);
        }

        BossBar finalBar = bar;
        BukkitRunnable task = new BukkitRunnable() {
            int time = timeout;
            @Override
            public void run() {
                if (!player.isOnline() || plugin.getDataManager().isAuthenticated(uuid)) {
                    if (finalBar != null) finalBar.removeAll();
                    this.cancel();
                    return;
                }

                if (time <= 0) {
                    Bukkit.getScheduler().runTask(plugin, () -> player.kickPlayer(plugin.getConfigManager().getMessage("timeout-kick")));
                    if (finalBar != null) finalBar.removeAll();
                    this.cancel();
                    return;
                }

                if (time % 3 == 0) {
                    if (plugin.getDataManager().isRegistered(player.getName())) {
                        player.sendMessage(plugin.getConfigManager().getMessage("login-prompt"));
                    } else {
                        player.sendMessage(plugin.getConfigManager().getMessage("register-prompt"));
                    }
                }

                if (finalBar != null) {
                    finalBar.setTitle(plugin.getConfigManager().getMessage("bossbar-title").replace("%time%", String.valueOf(time)));
                    finalBar.setProgress((double) time / timeout);
                }
                time--;
            }
        };
        task.runTaskTimerAsynchronously(plugin, 0, 20);
        authTasks.put(uuid, task);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.getDataManager().setAuthenticated(uuid, false);
        if (authTasks.containsKey(uuid)) {
            authTasks.get(uuid).cancel();
            authTasks.remove(uuid);
        }
        if (bossBars.containsKey(uuid)) {
            bossBars.get(uuid).removeAll();
            bossBars.remove(uuid);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        cancelIfUnauth(event, event.getPlayer());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            cancelIfUnauth(event, (Player) event.getEntity());
        }
    }

    @EventHandler
    public void onDealDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            cancelIfUnauth(event, (Player) event.getDamager());
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        cancelIfUnauth(event, event.getPlayer());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN) {
            cancelIfUnauth(event, event.getPlayer());
        }
    }

    private void cancelIfUnauth(org.bukkit.event.Cancellable event, Player player) {
        if (!plugin.getDataManager().isAuthenticated(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}