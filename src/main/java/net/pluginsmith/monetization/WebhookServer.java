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
        if (!config.webhookServerEnabled) {
            plugin.getLogger().info("Webhook server is disabled in configuration.");
            return;
        }

        server = HttpServer.create(new InetSocketAddress(config.webhookServerPort), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        // Register webhook endpoints
        server.createContext("/webhook/tebex", new TebexWebhookHandler());
        server.createContext("/webhook/craftingstore", new CraftingStoreWebhookHandler());
        server.createContext("/webhook/generic", new GenericWebhookHandler());

        server.start();
        plugin.getLogger().info("Webhook server started on port " + config.webhookServerPort);
        plugin.getLogger().info("Webhook endpoints:");
        plugin.getLogger().info("  - POST http://your-server:" + config.webhookServerPort + "/webhook/tebex");
        plugin.getLogger().info("  - POST http://your-server:" + config.webhookServerPort + "/webhook/craftingstore");
        plugin.getLogger().info("  - POST http://your-server:" + config.webhookServerPort + "/webhook/generic");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("Webhook server stopped.");
        }
    }

    // --- Webhook Handlers ---

    private class TebexWebhookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                JsonObject webhookData = gson.fromJson(body, JsonObject.class);

                // Tebex webhook structure
                String playerUuid = webhookData.get("player").getAsJsonObject().get("uuid").getAsString();
                String playerName = webhookData.get("player").getAsJsonObject().get("name").getAsString();
                String productId = webhookData.get("packages").getAsJsonArray().get(0).getAsJsonObject().get("id").getAsString();
                double amount = webhookData.get("price").getAsJsonObject().get("amount").getAsDouble();

                // Record the purchase
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    monetizationManager.recordPurchase(UUID.fromString(playerUuid), playerName, productId, amount, "tebex");
                });

                sendResponse(exchange, 200, "Purchase recorded successfully");
                plugin.getLogger().info("Tebex webhook processed for " + playerName + " - " + productId);

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to process Tebex webhook: " + e.getMessage());
                sendResponse(exchange, 400, "Invalid webhook data");
            }
        }
    }

    private class CraftingStoreWebhookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                JsonObject webhookData = gson.fromJson(body, JsonObject.class);

                // CraftingStore webhook structure (adjust based on their API)
                String playerUuid = webhookData.get("uuid").getAsString();
                String playerName = webhookData.get("username").getAsString();
                String productId = webhookData.get("package").getAsJsonObject().get("id").getAsString();
                double amount = webhookData.get("price").getAsDouble();

                // Record the purchase
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    monetizationManager.recordPurchase(UUID.fromString(playerUuid), playerName, productId, amount, "craftingstore");
                });

                sendResponse(exchange, 200, "Purchase recorded successfully");
                plugin.getLogger().info("CraftingStore webhook processed for " + playerName + " - " + productId);

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to process CraftingStore webhook: " + e.getMessage());
                sendResponse(exchange, 400, "Invalid webhook data");
            }
        }
    }

    private class GenericWebhookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                JsonObject webhookData = gson.fromJson(body, JsonObject.class);

                // Generic webhook structure - expects these fields
                String playerUuid = webhookData.get("playerUuid").getAsString();
                String playerName = webhookData.get("playerName").getAsString();
                String productId = webhookData.get("productId").getAsString();
                double amount = webhookData.get("amount").getAsDouble();
                String storeName = webhookData.has("storeName") ? webhookData.get("storeName").getAsString() : "generic";

                // Record the purchase
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    monetizationManager.recordPurchase(UUID.fromString(playerUuid), playerName, productId, amount, storeName);
                });

                sendResponse(exchange, 200, "Purchase recorded successfully");
                plugin.getLogger().info("Generic webhook processed for " + playerName + " - " + productId);

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to process generic webhook: " + e.getMessage());
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