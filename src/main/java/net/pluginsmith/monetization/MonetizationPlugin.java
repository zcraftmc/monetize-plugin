package net.pluginsmith.monetization;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.LuckPerms;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class MonetizationPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private WebhookManager webhookManager;
    private MonetizationManager monetizationManager;
    private WebhookServer webhookServer;
    private LuckPerms luckPerms;
    private Permission vaultPermission;
    private boolean luckPermsConnected = false;
    private boolean vaultConnected = false;
    private boolean placeholderApiConnected = false;

    @Override
    public void onEnable() {
        // Initialize ConfigManager and load configurations
        configManager = new ConfigManager(this);
        try {
            configManager.loadAll();
        } catch (IOException e) {
            getLogger().severe(ChatColor.RED + "[StorePulse] " + ChatColor.WHITE + "Failed to load configuration files: " + e.getMessage());
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
            getLogger().severe(ChatColor.RED + "[StorePulse] " + ChatColor.WHITE + "Failed to start webhook server: " + e.getMessage());
            getLogger().severe(ChatColor.RED + "[StorePulse] " + ChatColor.WHITE + "Make sure port " + configManager.getPluginConfig().webhook.serverPort + " is not in use by another application.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Connect optional soft dependencies
        setupOptionalDependencies();

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
        PluginCommand storePulseCmd = getCommand("storepulse");
        if (storePulseCmd != null) {
            storePulseCmd.setExecutor(commandHandler);
            storePulseCmd.setTabCompleter(commandHandler);
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

    private void setupOptionalDependencies() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderApiConnected = true;
            getLogger().info(colorize("&aPlaceholderAPI is available and will be used for expansion."));
        } else {
            getLogger().info(colorize("&ePlaceholderAPI is not installed. Placeholder expansion will be unavailable."));
        }

        RegisteredServiceProvider<LuckPerms> luckPermsProvider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (luckPermsProvider != null) {
            luckPerms = luckPermsProvider.getProvider();
            luckPermsConnected = true;
            getLogger().info(colorize("&aLuckPerms detected and hooked successfully."));
        } else {
            getLogger().info(colorize("&eLuckPerms is not installed or not available. Permissions fallback will remain unchanged."));
        }

        RegisteredServiceProvider<Permission> vaultProvider = Bukkit.getServicesManager().getRegistration(Permission.class);
        if (vaultProvider != null) {
            vaultPermission = vaultProvider.getProvider();
            vaultConnected = true;
            getLogger().info(colorize("&aVault detected and permission support enabled."));
        } else {
            getLogger().info(colorize("&eVault is not installed or no permissions provider was found."));
        }
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

   private void printBanner(boolean enabled) {
    String state = enabled ? colorize("&aENABLED") : colorize("&cDISABLED");
    String border = colorize("&e========================================");

    getLogger().info(border);
    getLogger().info(colorize("&6StorePulse &7" + state));
    getLogger().info(colorize("&7Made by ZCraft Studios"));
    getLogger().info(border);

    getLogger().info(colorize("&e  _____ _                   _____       _          "));
    getLogger().info(colorize("&e / ____| |                 |  __ \\     | |         "));
    getLogger().info(colorize("&e| (___ | |_ ___  _ __ ___  | |__) |   _| |___  ___ "));
    getLogger().info(colorize("&e \\___ \\| __/ _ \\| '__/ _ \\ |  ___/ | | | / __|/ _ \\"));
    getLogger().info(colorize("&e ____) | || (_) | | |  __/ | |   | |_| | \\__ \\  __/"));
    getLogger().info(colorize("&e|_____/ \\__\\___/|_|  \\___| |_|    \\__,_|_|___/\\___|"));

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
                getLogger().warning(ChatColor.RED + "[StorePulse] " + ChatColor.WHITE + "Invalid UUID for simulated purchase: " + playerUuidStr);
            }
        });
    }
}