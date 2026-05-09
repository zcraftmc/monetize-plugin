package net.pluginsmith.monetization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ConfigManager {

    private final JavaPlugin plugin;
    private final Gson gson;

    private File configFile;
    private File dataFile;
    private File backupDataFile;

    private PluginConfig pluginConfig;
    private PluginData pluginData;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        this.configFile = new File(plugin.getDataFolder(), "config.json");
        this.dataFile = new File(plugin.getDataFolder(), "data.json");
        this.backupDataFile = new File(plugin.getDataFolder(), "backup-data.json");
    }

    public void loadAll() throws IOException {
        plugin.getDataFolder().mkdirs(); // Ensure plugin data folder exists

        // Load config.json
        if (!configFile.exists()) {
            plugin.saveResource("config.json", false);
        }
        try (FileReader reader = new FileReader(configFile)) {
            pluginConfig = gson.fromJson(reader, PluginConfig.class);
            if (pluginConfig == null) pluginConfig = new PluginConfig(); // Fallback if empty/malformed
        }

        // Load data.json
        if (!dataFile.exists()) {
            try (FileWriter writer = new FileWriter(dataFile)) {
                gson.toJson(new PluginData(), writer); // Create empty data.json
            }
        }
        try (FileReader reader = new FileReader(dataFile)) {
            pluginData = gson.fromJson(reader, PluginData.class);
            if (pluginData == null) pluginData = new PluginData(); // Fallback if empty/malformed
        }
    }

    public CompletableFuture<Void> saveAll() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            try {
                saveConfig();
                saveData();
                future.complete(null);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save all configuration files: " + e.getMessage());
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private void saveConfig() throws IOException {
        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(pluginConfig, writer);
        }
    }

    public CompletableFuture<Void> saveData() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            try (FileWriter writer = new FileWriter(dataFile)) {
                gson.toJson(pluginData, writer);
                future.complete(null);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save data.json: " + e.getMessage());
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> createBackup() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            try {
                Files.copy(dataFile.toPath(), backupDataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                future.complete(null);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create backup-data.json: " + e.getMessage());
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> restoreBackup() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            if (!backupDataFile.exists()) {
                plugin.getLogger().warning("No backup-data.json found to restore.");
                future.complete(null);
                return;
            }
            try {
                Files.copy(backupDataFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                loadAll(); // Reload all data after restoring
                future.complete(null);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to restore from backup-data.json: " + e.getMessage());
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public PluginData getPluginData() {
        return pluginData;
    }

    // --- Inner Classes for JSON Data Structures ---

    public static final class PluginConfig {
        public Map<String, String> webhookUrls = new HashMap<>();
        public Map<String, String> apiTokens = new HashMap<>();
        public Branding branding = new Branding();
        public EmbedColors embedColors = new EmbedColors();
        public Messages messages = new Messages();
        public Map<String, String> rolePings = new HashMap<>();
        public Features features = new Features();
        public String skinRenderApi = "https://crafatar.com/renders/body/{uuid}?scale=2&overlay";
        public String avatarApi = "https://crafatar.com/avatars/{uuid}?size=64&overlay";
        public long goalUpdateIntervalTicks = 1200; // 60 seconds
        public long backupIntervalTicks = 72000; // 1 hour
        public int leaderboardEntriesPerPage = 10;
        public int progressBarSegments = 10;
        public String progressBarFullChar = "🟩";
        public String progressBarEmptyChar = "⬜";
        public int webhookServerPort = 8080; // Port for incoming webhooks from payment processors
        public boolean webhookServerEnabled = true; // Enable/disable webhook server

        public PluginConfig() {
            webhookUrls.put("default", "YOUR_DEFAULT_DISCORD_WEBHOOK_URL_HERE");
            apiTokens.put("tebex", "YOUR_TEBEX_SECRET_KEY_HERE");
            branding.serverName = "My Awesome Server";
            embedColors.purchase = "#00FF00";
            embedColors.goalUpdate = "#FFFF00";
            embedColors.goalComplete = "#0000FF";
            messages.thankYou = "Thank you, {player}, for your generous purchase of {product}!";
            messages.goalComplete = "🎉 {goalName} has been completed! Thank you to all supporters!";
            messages.goalUpdate = "{goalName} progress: {progressBar} {progressPercent}% ({current}/{target})";
            rolePings.put("defaultGoal", "<@&123456789012345678>");
            features.discordWebhooksEnabled = true;
            features.donationGoalsEnabled = true;
            features.analyticsEnabled = true;
            features.autoBackupEnabled = true;
        }
    }

    public static final class Branding {
        public String serverName = "My Awesome Server";
        public String serverIconUrl = "";
    }

    public static final class EmbedColors {
        public String purchase = "#00FF00";
        public String goalUpdate = "#FFFF00";
        public String goalComplete = "#0000FF";
    }

    public static final class Messages {
        public String thankYou = "";
        public String goalComplete = "";
        public String goalUpdate = "";
    }

    public static final class Features {
        public boolean discordWebhooksEnabled = true;
        public boolean donationGoalsEnabled = true;
        public boolean analyticsEnabled = true;
        public boolean autoBackupEnabled = true;
    }

    public static final class PluginData {
        public List<Purchase> purchases = new ArrayList<>();
        public List<DonationGoal> goals = new ArrayList<>();
        public Map<UUID, SupporterData> supporters = new HashMap<>();
        public Map<String, Double> analytics = new HashMap<>(); // Date (YYYY-MM-DD) -> total revenue

        public PluginData() {}
    }

    public static final class Purchase {
        public UUID playerUuid;
        public String playerName;
        public String productId;
        public double amount;
        public String storeName;
        public long timestamp;

        public Purchase(UUID playerUuid, String playerName, String productId, double amount, String storeName) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.productId = productId;
            this.amount = amount;
            this.storeName = storeName;
            this.timestamp = Instant.now().getEpochSecond();
        }
    }

    public static final class DonationGoal {
        public String id;
        public String name;
        public double targetAmount;
        public double currentAmount;
        public long expiryTimestamp; // 0 for no expiry
        public boolean completed;
        public boolean webhookSent; // To prevent duplicate completion webhooks

        public DonationGoal(String id, String name, double targetAmount, long expiryTimestamp) {
            this.id = id;
            this.name = name;
            this.targetAmount = targetAmount;
            this.currentAmount = 0.0;
            this.expiryTimestamp = expiryTimestamp;
            this.completed = false;
            this.webhookSent = false;
        }
    }

    public static final class SupporterData {
        public String playerName;
        public double totalAmount;
        public long lastPurchaseTimestamp;

        public SupporterData(String playerName, double totalAmount, long lastPurchaseTimestamp) {
            this.playerName = playerName;
            this.totalAmount = totalAmount;
            this.lastPurchaseTimestamp = lastPurchaseTimestamp;
        }
    }
}