package net.pluginsmith.monetization;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class MonetizationPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private WebhookManager webhookManager;
    private MonetizationManager monetizationManager;
    private WebhookServer webhookServer;

    @Override
    public void onEnable() {
        // Initialize ConfigManager and load configurations
        configManager = new ConfigManager(this);
        try {
            configManager.loadAll();
        } catch (IOException e) {
            getLogger().severe("Failed to load configuration files: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize managers
        webhookManager = new WebhookManager(this, configManager.getPluginConfig());
        monetizationManager = new MonetizationManager(this, configManager.getPluginData(), webhookManager, configManager.getPluginConfig());
        webhookServer = new WebhookServer(this, monetizationManager, configManager.getPluginConfig());

        // Start webhook server
        try {
            webhookServer.start();
        } catch (IOException e) {
            getLogger().severe("Failed to start webhook server: " + e.getMessage());
            getLogger().severe("Make sure port " + configManager.getPluginConfig().webhook.serverPort + " is not in use by another application.");
        }

        // Register commands
        MonetizationCommand commandHandler = new MonetizationCommand(this, monetizationManager, configManager);
        PluginCommand topDonorsCmd = getCommand("topdonors");
        if (topDonorsCmd != null) {
            topDonorsCmd.setExecutor(commandHandler);
            topDonorsCmd.setTabCompleter(commandHandler);
        }
        PluginCommand recentPurchasesCmd = getCommand("recentpurchases");
        if (recentPurchasesCmd != null) {
            recentPurchasesCmd.setExecutor(commandHandler);
            recentPurchasesCmd.setTabCompleter(commandHandler);
        }
        PluginCommand goalStatusCmd = getCommand("goalstatus");
        if (goalStatusCmd != null) {
            goalStatusCmd.setExecutor(commandHandler);
            goalStatusCmd.setTabCompleter(commandHandler);
        }
        PluginCommand goalCreateCmd = getCommand("goalcreate");
        if (goalCreateCmd != null) {
            goalCreateCmd.setExecutor(commandHandler);
            goalCreateCmd.setTabCompleter(commandHandler);
        }
        PluginCommand goalEditCmd = getCommand("goaledit");
        if (goalEditCmd != null) {
            goalEditCmd.setExecutor(commandHandler);
            goalEditCmd.setTabCompleter(commandHandler);
        }
        PluginCommand reloadMonetizationCmd = getCommand("reloadmonetization");
        if (reloadMonetizationCmd != null) {
            reloadMonetizationCmd.setExecutor(commandHandler);
            reloadMonetizationCmd.setTabCompleter(commandHandler);
        }
        PluginCommand reloadStorePulseCmd = getCommand("reloadstorepulse");
        if (reloadStorePulseCmd != null) {
            reloadStorePulseCmd.setExecutor(commandHandler);
            reloadStorePulseCmd.setTabCompleter(commandHandler);
        }

        // Register listeners
        getServer().getPluginManager().registerEvents(new MonetizationListener(this, monetizationManager), this);

        // Start periodic tasks using Folia-compatible GlobalRegionScheduler
        if (configManager.getPluginConfig().features.donationGoalsEnabled) {
            long goalUpdateInterval = configManager.getPluginConfig().goalUpdateIntervalTicks;
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, task -> monetizationManager.checkGoals(), goalUpdateInterval, goalUpdateInterval);
            getLogger().info("Donation goal updates scheduled every " + (goalUpdateInterval / 20) + " seconds.");
        }
        if (configManager.getPluginConfig().features.autoBackupEnabled) {
            long backupInterval = configManager.getPluginConfig().backupIntervalTicks;
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, task -> {
                configManager.createBackup();
                getLogger().info("Data backup created successfully.");
            }, backupInterval, backupInterval);
            getLogger().info("Automatic data backups scheduled every " + (backupInterval / 20 / 60) + " minutes.");
        }

        printBanner(true);
    }

    @Override
    public void onDisable() {
        // Stop webhook server
        if (webhookServer != null) {
            webhookServer.stop();
        }

        // Save all data on disable
        configManager.saveAll();
        // Cancel all plugin-owned tasks
        Bukkit.getGlobalRegionScheduler().cancelTasks(this);
        Bukkit.getAsyncScheduler().cancelTasks(this);

        printBanner(false);
    }

    private void printBanner(boolean enabled) {
        String title = "StorePulse";
        String byLine = "by ZCraft Studios";
        String state = enabled ? "ENABLED" : "DISABLED";
        String border = "========================================";

        getLogger().info(border);
        getLogger().info("  _____ _               _____       _       ");
        getLogger().info(" / ____| |             |  __ \\     | |      ");
        getLogger().info("| (___ | |_ _ __ _   _ | |__) |   _| | ___  ");
        getLogger().info(" \\___ \\| __| '__| | | ||  ___/ | | | |/ _ \\ ");
        getLogger().info(" ____) | |_| |  | |_| || |   | |_| | |  __/ ");
        getLogger().info("|_____/ \\__|_|   \\__, ||_|    \\__,_|_|\\___| ");
        getLogger().info("                  __/ |                    ");
        getLogger().info("                 |___/                     ");
        getLogger().info(border);
        getLogger().info(" " + title + " " + state + " - " + byLine);
        getLogger().info(border);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public WebhookManager getWebhookManager() {
        return webhookManager;
    }

    public MonetizationManager getMonetizationManager() {
        return monetizationManager;
    }

    /**
     * Example method to simulate a purchase (e.g., from a Tebex webhook listener)
     * This would typically be called by an external system or API handler.
     */
    public void simulatePurchase(String playerUuidStr, String playerName, String productId, double amount, String storeName) {
        Bukkit.getAsyncScheduler().runNow(this, task -> {
            try {
                monetizationManager.recordPurchase(java.util.UUID.fromString(playerUuidStr), playerName, productId, amount, storeName);
                getLogger().info("Simulated purchase recorded for " + playerName + " (" + productId + ")");
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid UUID for simulated purchase: " + playerUuidStr);
            }
        });
    }
}