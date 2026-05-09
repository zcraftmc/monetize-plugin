package net.pluginsmith.monetization;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public final class MonetizationManager {

    private final JavaPlugin plugin;
    private final ConfigManager.PluginData pluginData;
    private final WebhookManager webhookManager;
    private final ConfigManager.PluginConfig pluginConfig;
    private final MiniMessage miniMessage;
    private final DecimalFormat currencyFormat = new DecimalFormat("$#,##0.00");
    private final DecimalFormat percentFormat = new DecimalFormat("0");

    public MonetizationManager(JavaPlugin plugin, ConfigManager.PluginData pluginData, WebhookManager webhookManager, ConfigManager.PluginConfig pluginConfig) {
        this.plugin = plugin;
        this.pluginData = pluginData;
        this.webhookManager = webhookManager;
        this.pluginConfig = pluginConfig;
        this.miniMessage = MiniMessage.miniMessage();

        // Register PlaceholderAPI expansion if PlaceholderAPI is enabled
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MonetizationPlaceholderExpansion(plugin, this).register();
            plugin.getLogger().info("PlaceholderAPI expansion registered.");
        }
    }

    public void recordPurchase(UUID playerUuid, String playerName, String productId, double amount, String storeName) {
        ConfigManager.Purchase purchase = new ConfigManager.Purchase(playerUuid, playerName, productId, amount, storeName);
        pluginData.purchases.add(0, purchase); // Add to top for recent purchases

        // Update supporter data
        pluginData.supporters.compute(playerUuid, (uuid, data) -> {
            if (data == null) {
                return new ConfigManager.SupporterData(playerName, amount, purchase.timestamp);
            } else {
                return new ConfigManager.SupporterData(playerName, data.totalAmount + amount, purchase.timestamp);
            }
        });

        // Update analytics
        if (pluginConfig.features.analyticsEnabled) {
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            pluginData.analytics.merge(today, amount, Double::sum);
        }

        // Check donation goals
        if (pluginConfig.features.donationGoalsEnabled) {
            pluginData.goals.forEach(goal -> {
                if (!goal.completed) {
                    goal.currentAmount += amount;
                }
            });
            checkGoals(); // Re-check goals immediately after a purchase
        }

        // Send webhook
        Player player = Bukkit.getPlayer(playerUuid);
        webhookManager.sendPurchaseWebhook(purchase, player);

        // Save data asynchronously
        plugin.getConfigManager().saveData();
    }

    public List<ConfigManager.SupporterData> getTopDonors(int limit) {
        return pluginData.supporters.values().stream()
            .sorted(Comparator.comparingDouble(ConfigManager.SupporterData::totalAmount).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    public List<ConfigManager.Purchase> getRecentPurchases(int limit) {
        return pluginData.purchases.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    public ConfigManager.DonationGoal getGoal(String goalId) {
        return pluginData.goals.stream()
            .filter(g -> g.id.equalsIgnoreCase(goalId))
            .findFirst()
            .orElse(null);
    }

    public List<ConfigManager.DonationGoal> getAllGoals() {
        return pluginData.goals;
    }

    public boolean createGoal(String id, String name, double targetAmount, long expiryTimestamp) {
        if (getGoal(id) != null) return false;
        pluginData.goals.add(new ConfigManager.DonationGoal(id, name, targetAmount, expiryTimestamp));
        plugin.getConfigManager().saveData();
        return true;
    }

    public boolean editGoal(String id, String field, String value) {
        ConfigManager.DonationGoal goal = getGoal(id);
        if (goal == null) return false;

        boolean changed = false;
        switch (field.toLowerCase()) {
            case "name":
                goal.name = value;
                changed = true;
                break;
            case "target":
                try {
                    goal.targetAmount = Double.parseDouble(value);
                    changed = true;
                } catch (NumberFormatException ignored) {}
                break;
            case "current":
                try {
                    goal.currentAmount = Double.parseDouble(value);
                    changed = true;
                } catch (NumberFormatException ignored) {}
                break;
            case "expiry":
                if (value.equalsIgnoreCase("none")) {
                    goal.expiryTimestamp = 0;
                    changed = true;
                } else {
                    try {
                        LocalDate date = LocalDate.parse(value); // YYYY-MM-DD
                        goal.expiryTimestamp = date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
                        changed = true;
                    } catch (Exception ignored) {}
                }
                break;
            case "complete":
                goal.completed = Boolean.parseBoolean(value);
                goal.webhookSent = goal.completed; // Reset webhook status if manually completed/uncompleted
                changed = true;
                break;
        }
        if (changed) {
            plugin.getConfigManager().saveData();
            checkGoals(); // Re-check goals after edit
        }
        return changed;
    }

    public void checkGoals() {
        if (!pluginConfig.features.donationGoalsEnabled) return;

        long now = Instant.now().getEpochSecond();
        for (ConfigManager.DonationGoal goal : pluginData.goals) {
            if (goal.completed && goal.webhookSent) continue; // Already completed and webhook sent

            boolean expired = goal.expiryTimestamp > 0 && now >= goal.expiryTimestamp;
            boolean reached = goal.currentAmount >= goal.targetAmount;

            if (reached && !goal.completed) {
                goal.completed = true;
                if (pluginConfig.features.discordWebhooksEnabled && !goal.webhookSent) {
                    webhookManager.sendGoalCompletionWebhook(goal);
                    goal.webhookSent = true;
                }
                plugin.getLogger().info("Donation goal '" + goal.name + "' completed!");
            } else if (expired && !goal.completed) {
                plugin.getLogger().info("Donation goal '" + goal.name + "' expired without completion.");
                goal.completed = true; // Mark as completed (expired) to stop tracking
            }

            // Send periodic update webhook if not completed and not expired
            if (!goal.completed && pluginConfig.features.discordWebhooksEnabled) {
                double progressPercent = (goal.currentAmount / goal.targetAmount) * 100;
                String progressBar = formatProgressBar(goal.currentAmount, goal.targetAmount, pluginConfig.progressBarSegments);
                webhookManager.sendGoalUpdateWebhook(goal, progressBar, progressPercent);
            }
        }
        plugin.getConfigManager().saveData();
    }

    public String formatProgressBar(double current, double target, int segments) {
        if (target <= 0) return "";
        int filledSegments = (int) Math.round((current / target) * segments);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < segments; i++) {
            if (i < filledSegments) {
                bar.append(pluginConfig.progressBarFullChar);
            } else {
                bar.append(pluginConfig.progressBarEmptyChar);
            }
        }
        return bar.toString();
    }

    // --- PlaceholderAPI Expansion (Inner Class) ---
    private static final class MonetizationPlaceholderExpansion extends PlaceholderExpansion {

        private final JavaPlugin plugin;
        private final MonetizationManager manager;
        private final DecimalFormat currencyFormat = new DecimalFormat("$#,##0.00");
        private final DecimalFormat percentFormat = new DecimalFormat("0");

        public MonetizationPlaceholderExpansion(JavaPlugin plugin, MonetizationManager manager) {
            this.plugin = plugin;
            this.manager = manager;
        }

        @Override
        public @NotNull String getIdentifier() {
            return "monetization";
        }

        @Override
        public @NotNull String getAuthor() {
            return plugin.getDescription().getAuthors().getFirst();
        }

        @Override
        public @NotNull String getVersion() {
            return plugin.getDescription().getVersion();
        }

        @Override
        public boolean persist() {
            return true; // This is a persistent expansion
        }

        @Override
        public @Nullable String onPlaceholderRequest(Player player, @NotNull String identifier) {
            if (player == null) {
                return "";
            }

            // Player-specific placeholders
            if (identifier.startsWith("total_donated_")) {
                UUID targetUuid = player.getUniqueId();
                if (identifier.length() > "total_donated_".length()) {
                    String targetName = identifier.substring("total_donated_".length());
                    OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(targetName);
                    if (target != null) targetUuid = target.getUniqueId();
                }
                ConfigManager.SupporterData data = manager.pluginData.supporters.get(targetUuid);
                return currencyFormat.format(data != null ? data.totalAmount : 0.0);
            }

            // Global placeholders
            switch (identifier.toLowerCase()) {
                case "total_revenue":
                    AtomicReference<Double> total = new AtomicReference<>(0.0);
                    manager.pluginData.analytics.values().forEach(total::updateAndGet);
                    return currencyFormat.format(total.get());
                case "top_donor_name":
                    return manager.getTopDonors(1).stream()
                        .findFirst()
                        .map(d -> d.playerName)
                        .orElse("N/A");
                case "top_donor_amount":
                    return manager.getTopDonors(1).stream()
                        .findFirst()
                        .map(d -> currencyFormat.format(d.totalAmount))
                        .orElse(currencyFormat.format(0.0));
            }

            // Goal-specific placeholders
            if (identifier.startsWith("goal_")) {
                String[] parts = identifier.split("_", 3); // goal_id_field
                if (parts.length < 3) return null;

                String goalId = parts[1];
                String field = parts[2];
                ConfigManager.DonationGoal goal = manager.getGoal(goalId);
                if (goal == null) return null;

                switch (field.toLowerCase()) {
                    case "name": return goal.name;
                    case "current": return currencyFormat.format(goal.currentAmount);
                    case "target": return currencyFormat.format(goal.targetAmount);
                    case "progress_bar":
                        return manager.formatProgressBar(goal.currentAmount, goal.targetAmount, manager.pluginConfig.progressBarSegments);
                    case "progress_percent":
                        return percentFormat.format((goal.currentAmount / goal.targetAmount) * 100);
                    case "remaining": return currencyFormat.format(Math.max(0, goal.targetAmount - goal.currentAmount));
                    case "expires":
                        if (goal.expiryTimestamp == 0) return "Never";
                        return Instant.ofEpochSecond(goal.expiryTimestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE);
                    case "completed": return String.valueOf(goal.completed);
                }
            }

            return null;
        }
    }
}