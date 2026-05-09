package net.pluginsmith.monetization;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executors;

public final class WebhookServer {

    private final JavaPlugin plugin;
    private final MonetizationManager monetizationManager;
    private final ConfigManager.PluginConfig config;
    private final Gson gson;
    private HttpServer server;

    public WebhookServer(JavaPlugin plugin, MonetizationManager monetizationManager, ConfigManager.PluginConfig config) {
        this.plugin = plugin;
        this.monetizationManager = monetizationManager;
        this.config = config;
        this.gson = new Gson();
    }

    public void start() throws IOException {
        if (!config.webhook.enabled) {
            plugin.getLogger().info("Webhook server is disabled in configuration.");
            return;
        }

        server = HttpServer.create(new InetSocketAddress(config.webhook.serverPort), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        HttpHandler unifiedHandler = new UnifiedWebhookHandler();
        server.createContext("/webhook", unifiedHandler);
        server.createContext("/webhook/tebex", unifiedHandler);
        server.createContext("/webhook/craftingstore", unifiedHandler);
        server.createContext("/webhook/generic", unifiedHandler);

        server.start();
        plugin.getLogger().info("Webhook server started on port " + config.webhook.serverPort);
        plugin.getLogger().info("Unified webhook endpoint:");
        plugin.getLogger().info("  - POST http://your-server:" + config.webhook.serverPort + "/webhook");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("Webhook server stopped.");
        }
    }

    // --- Webhook Handlers ---

    private class UnifiedWebhookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                JsonObject webhookData = gson.fromJson(body, JsonObject.class);

                String playerUuid;
                String playerName;
                String productId;
                double amount;
                String storeName = "generic";

                if (webhookData.has("player") && webhookData.has("packages")) {
                    playerUuid = webhookData.getAsJsonObject("player").get("uuid").getAsString();
                    playerName = webhookData.getAsJsonObject("player").get("name").getAsString();
                    productId = webhookData.getAsJsonArray("packages").get(0).getAsJsonObject().get("id").getAsString();
                    amount = webhookData.getAsJsonObject("price").get("amount").getAsDouble();
                    storeName = "tebex";
                } else if (webhookData.has("uuid") && webhookData.has("username") && webhookData.has("package")) {
                    playerUuid = webhookData.get("uuid").getAsString();
                    playerName = webhookData.get("username").getAsString();
                    productId = webhookData.getAsJsonObject("package").get("id").getAsString();
                    amount = webhookData.get("price").getAsDouble();
                    storeName = "craftingstore";
                } else if (webhookData.has("playerUuid") && webhookData.has("playerName") && webhookData.has("productId") && webhookData.has("amount")) {
                    playerUuid = webhookData.get("playerUuid").getAsString();
                    playerName = webhookData.get("playerName").getAsString();
                    productId = webhookData.get("productId").getAsString();
                    amount = webhookData.get("amount").getAsDouble();
                    if (webhookData.has("storeName")) {
                        storeName = webhookData.get("storeName").getAsString();
                    }
                } else {
                    throw new IllegalArgumentException("Unrecognized webhook payload structure");
                }

                String finalStoreName = storeName;
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    monetizationManager.recordPurchase(UUID.fromString(playerUuid), playerName, productId, amount, finalStoreName);
                });

                sendResponse(exchange, 200, "Purchase recorded successfully");
                plugin.getLogger().info("Webhook processed for " + playerName + " - " + productId + " (store=" + finalStoreName + ")");

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to process webhook: " + e.getMessage());
                sendResponse(exchange, 400, "Invalid webhook data");
            }
        }
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }
}