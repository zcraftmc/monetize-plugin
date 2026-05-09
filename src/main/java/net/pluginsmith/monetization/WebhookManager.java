package net.pluginsmith.monetization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.Color;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class WebhookManager {

    private final JavaPlugin plugin;
    private final ConfigManager.PluginConfig config;
    private final HttpClient httpClient;
    private final Gson gson;
    private final MiniMessage miniMessage;

    public WebhookManager(JavaPlugin plugin, ConfigManager.PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new GsonBuilder().disableHtmlEscaping().create();
        this.miniMessage = MiniMessage.miniMessage();
    }

    public void sendPurchaseWebhook(ConfigManager.Purchase purchase, Player player) {
        if (!config.features.discordWebhooksEnabled) return;

        String webhookUrl = config.discord.webhookUrls.getOrDefault(purchase.storeName.toLowerCase(), config.discord.webhookUrls.get("default"));
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("YOUR_DEFAULT_DISCORD_WEBHOOK_URL_HERE")) {
            plugin.getLogger().warning("No Discord webhook URL configured for store '" + purchase.storeName + "' or default.");
            return;
        }

        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            try {
                DiscordWebhook webhook = new DiscordWebhook();
                webhook.username = config.branding.serverName;
                webhook.avatar_url = config.branding.serverIconUrl;

                DiscordWebhook.Embed embed = new DiscordWebhook.Embed();
                embed.title = "New Purchase!";
                embed.description = config.messages.thankYou
                    .replace("{player}", purchase.playerName)
                    .replace("{product}", purchase.productId)
                    .replace("{amount}", String.format("%.2f", purchase.amount));
                embed.color = parseColor(config.embedColors.purchase);

                embed.fields.add(new DiscordWebhook.Embed.Field("Player", purchase.playerName, true));
                embed.fields.add(new DiscordWebhook.Embed.Field("Product", purchase.productId, true));
                embed.fields.add(new DiscordWebhook.Embed.Field("Amount", String.format("$%.2f", purchase.amount), true));
                embed.fields.add(new DiscordWebhook.Embed.Field("Store", purchase.storeName, true));

                String avatarUrl = config.avatarApi.replace("{uuid}", purchase.playerUuid.toString());
                String skinRenderUrl = config.skinRenderApi.replace("{uuid}", purchase.playerUuid.toString());

                embed.thumbnail = new DiscordWebhook.Embed.Thumbnail(avatarUrl);
                embed.image = new DiscordWebhook.Embed.Image(skinRenderUrl);

                embed.timestamp = Instant.ofEpochSecond(purchase.timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                embed.footer = new DiscordWebhook.Embed.Footer(config.branding.serverName, config.branding.serverIconUrl);

                webhook.embeds.add(embed);

                sendWebhook(webhookUrl, webhook);

                // Send thank you message to player
                if (player != null && player.isOnline()) {
                    player.getScheduler().run(plugin, pTask -> {
                        Component thankYouMessage = miniMessage.deserialize(config.messages.thankYou
                            .replace("{player}", player.getName())
                            .replace("{product}", purchase.productId)
                            .replace("{amount}", String.format("%.2f", purchase.amount)));
                        player.sendMessage(thankYouMessage);
                    }, null);
                }

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to send purchase webhook: " + e.getMessage());
            }
        });
    }

    public void sendGoalUpdateWebhook(ConfigManager.DonationGoal goal, String progressBar, double progressPercent) {
        if (!config.features.discordWebhooksEnabled) return;

        String webhookUrl = config.discord.webhookUrls.getOrDefault("default", "");
        if (webhookUrl.isEmpty() || webhookUrl.equals("YOUR_DEFAULT_DISCORD_WEBHOOK_URL_HERE")) {
            plugin.getLogger().warning("No default Discord webhook URL configured for goal updates.");
            return;
        }

        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            try {
                DiscordWebhook webhook = new DiscordWebhook();
                webhook.username = config.branding.serverName;
                webhook.avatar_url = config.branding.serverIconUrl;

                String messageContent = config.messages.goalUpdate
                    .replace("{goalName}", goal.name)
                    .replace("{progressBar}", progressBar)
                    .replace("{progressPercent}", String.format("%.0f", progressPercent))
                    .replace("{current}", String.format("$%.2f", goal.currentAmount))
                    .replace("{target}", String.format("$%.2f", goal.targetAmount));

                String rolePing = config.rolePings.getOrDefault(goal.id, config.rolePings.get("defaultGoal"));
                if (rolePing != null && !rolePing.isEmpty()) {
                    messageContent = rolePing + " " + messageContent;
                }
                webhook.content = messageContent;

                DiscordWebhook.Embed embed = new DiscordWebhook.Embed();
                embed.title = "Donation Goal Update: " + goal.name;
                embed.description = messageContent;
                embed.color = parseColor(config.embedColors.goalUpdate);

                embed.fields.add(new DiscordWebhook.Embed.Field("Progress", progressBar + " " + String.format("%.0f", progressPercent) + "%", false));
                embed.fields.add(new DiscordWebhook.Embed.Field("Current", String.format("$%.2f", goal.currentAmount), true));
                embed.fields.add(new DiscordWebhook.Embed.Field("Target", String.format("$%.2f", goal.targetAmount), true));

                if (goal.expiryTimestamp > 0) {
                    embed.fields.add(new DiscordWebhook.Embed.Field("Expires", Instant.ofEpochSecond(goal.expiryTimestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE), true));
                }

                embed.timestamp = Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                embed.footer = new DiscordWebhook.Embed.Footer(config.branding.serverName, config.branding.serverIconUrl);

                webhook.embeds.add(embed);

                sendWebhook(webhookUrl, webhook);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to send goal update webhook: " + e.getMessage());
            }
        });
    }

    public void sendGoalCompletionWebhook(ConfigManager.DonationGoal goal) {
        if (!config.features.discordWebhooksEnabled) return;

        String webhookUrl = config.discord.webhookUrls.getOrDefault("default", "");
        if (webhookUrl.isEmpty() || webhookUrl.equals("YOUR_DEFAULT_DISCORD_WEBHOOK_URL_HERE")) {
            plugin.getLogger().warning("No default Discord webhook URL configured for goal completion.");
            return;
        }

        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            try {
                DiscordWebhook webhook = new DiscordWebhook();
                webhook.username = config.branding.serverName;
                webhook.avatar_url = config.branding.serverIconUrl;

                String messageContent = config.messages.goalComplete.replace("{goalName}", goal.name);
                String rolePing = config.rolePings.getOrDefault(goal.id, config.rolePings.get("defaultGoal"));
                if (rolePing != null && !rolePing.isEmpty()) {
                    messageContent = rolePing + " " + messageContent;
                }
                webhook.content = messageContent;

                DiscordWebhook.Embed embed = new DiscordWebhook.Embed();
                embed.title = "Goal Completed: " + goal.name + "!";
                embed.description = messageContent;
                embed.color = parseColor(config.embedColors.goalComplete);

                embed.fields.add(new DiscordWebhook.Embed.Field("Target Reached", String.format("$%.2f", goal.targetAmount), false));
                embed.timestamp = Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                embed.footer = new DiscordWebhook.Embed.Footer(config.branding.serverName, config.branding.serverIconUrl);

                webhook.embeds.add(embed);

                sendWebhook(webhookUrl, webhook);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to send goal completion webhook: " + e.getMessage());
            }
        });
    }

    private void sendWebhook(String url, DiscordWebhook webhook) {
        String jsonPayload = gson.toJson(webhook);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> {
                if (response.statusCode() != 204) { // 204 No Content is success for Discord webhooks
                    plugin.getLogger().warning("Failed to send webhook. Status: " + response.statusCode() + ", Body: " + response.body());
                }
            })
            .exceptionally(e -> {
                plugin.getLogger().severe("Error sending webhook: " + e.getMessage());
                return null;
            });
    }

    private int parseColor(String hexColor) {
        try {
            return Color.decode(hexColor).getRGB() & 0xFFFFFF;
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid hex color in config: " + hexColor + ". Using default.");
            return Color.GRAY.getRGB() & 0xFFFFFF;
        }
    }

    // --- Inner Class for Discord Webhook Payload ---
    public static final class DiscordWebhook {
        public String content;
        public String username;
        public String avatar_url;
        public boolean tts;
        public List<Embed> embeds = new ArrayList<>();

        public static final class Embed {
            public String title;
            public String description;
            public String url;
            public int color;
            public Footer footer;
            public Image image;
            public Thumbnail thumbnail;
            public Author author;
            public List<Field> fields = new ArrayList<>();
            public String timestamp;

            public Embed() {}

            public static final class Footer {
                public String text;
                public String icon_url;
                public Footer(String text, String icon_url) { this.text = text; this.icon_url = icon_url; }
            }

            public static final class Image {
                public String url;
                public Image(String url) { this.url = url; }
            }

            public static final class Thumbnail {
                public String url;
                public Thumbnail(String url) { this.url = url; }
            }

            public static final class Author {
                public String name;
                public String url;
                public String icon_url;
                public Author(String name, String url, String icon_url) { this.name = name; this.url = url; this.icon_url = icon_url; }
            }

            public static final class Field {
                public String name;
                public String value;
                public boolean inline;
                public Field(String name, String value, boolean inline) { this.name = name; this.value = value; this.inline = inline; }
            }
        }
    }
}