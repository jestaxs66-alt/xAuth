package com.jesta.xauth;

import com.jesta.xauth.command.AdminCommand;
import com.jesta.xauth.command.AuthCommand;
import com.jesta.xauth.listener.AuthListener;
import com.jesta.xauth.manager.ConfigManager;
import com.jesta.xauth.manager.DataManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.bukkit.plugin.java.JavaPlugin;

public class xAuth extends JavaPlugin {

    private ConfigManager configManager;
    private DataManager dataManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.dataManager = new DataManager(this);

        getServer().getPluginManager().registerEvents(new AuthListener(this), this);
        getCommand("login").setExecutor(new AuthCommand(this));
        getCommand("register").setExecutor(new AuthCommand(this));
        getCommand("xauth").setExecutor(new AdminCommand(this));

        hideCommandsFromConsole();
    }

    private void hideCommandsFromConsole() {
        ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).addFilter(new AbstractFilter() {
            @Override
            public Filter.Result filter(LogEvent event) {
                String msg = event.getMessage().getFormattedMessage();
                if (msg != null && (msg.toLowerCase().contains("/login ") || msg.toLowerCase().contains("/register "))) {
                    return Filter.Result.DENY;
                }
                return Filter.Result.NEUTRAL;
            }
        });
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }
}