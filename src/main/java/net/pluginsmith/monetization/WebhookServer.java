package net.pluginsmith.monetization;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.ChatColor;
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
        server.createContext("/", unifiedHandler);

        server.start();
        plugin.getLogger().info("Webhook server started on port " + config.webhook.serverPort);
        plugin.getLogger().info("StorePulse webhook listener is ready.");
        plugin.getLogger().info("  - Visit http://your-server:" + config.webhook.serverPort + " to verify the listener is active.");
        plugin.getLogger().info("  - POST any webhook payload to http://your-server:" + config.webhook.serverPort + " or any path.");
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
            String method = exchange.getRequestMethod();
            if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
                sendStatusPage(exchange);
                return;
            }
            if (!"POST".equalsIgnoreCase(method)) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                plugin.getLogger().info(ChatColor.YELLOW + "[StorePulse] " + ChatColor.WHITE + "Received webhook payload of size " + body.length() + " bytes");
                plugin.getLogger().info(ChatColor.GRAY + "Payload: " + body);
                
                JsonObject webhookData = gson.fromJson(body, JsonObject.class);

                String playerUuid;
                String playerName;
                String productId;
                double amount;
                String storeName = "generic";

                // Tebex format: {player: {uuid, name}, packages: [{id}], price: {amount}}
                if (webhookData.has("player") && webhookData.has("packages")) {
                    try {
                        playerUuid = webhookData.getAsJsonObject("player").get("uuid").getAsString();
                        playerName = webhookData.getAsJsonObject("player").get("name").getAsString();
                        productId = webhookData.getAsJsonArray("packages").get(0).getAsJsonObject().get("id").getAsString();
                        amount = webhookData.getAsJsonObject("price").get("amount").getAsDouble();
                        storeName = "tebex";
                        plugin.getLogger().info(ChatColor.GREEN + "[StorePulse] " + ChatColor.WHITE + "Detected Tebex webhook format");
                    } catch (Exception e) {
                        plugin.getLogger().warning(ChatColor.RED + "[StorePulse] " + ChatColor.WHITE + "Failed to parse Tebex payload: " + e.getMessage());
                        throw new IllegalArgumentException("Invalid Tebex payload structure: " + e.getMessage());
                    }
                } 
                // CraftingStore format: {uuid, username, package: {id}, price}
                else if (webhookData.has("uuid") && webhookData.has("username") && webhookData.has("package")) {
                    try {
                        playerUuid = webhookData.get("uuid").getAsString();
                        playerName = webhookData.get("username").getAsString();
                        productId = webhookData.getAsJsonObject("package").get("id").getAsString();
                        amount = webhookData.get("price").getAsDouble();
                        storeName = "craftingstore";
                        plugin.getLogger().info(ChatColor.GREEN + "[StorePulse] " + ChatColor.WHITE + "Detected CraftingStore webhook format");
                    } catch (Exception e) {
                        plugin.getLogger().warning(ChatColor.RED + "[StorePulse] " + ChatColor.WHITE + "Failed to parse CraftingStore payload: " + e.getMessage());
                        throw new IllegalArgumentException("Invalid CraftingStore payload structure: " + e.getMessage());
                    }
                } 
                // Generic format: {playerUuid, playerName, productId, amount, [storeName]}
                else if (webhookData.has("playerUuid") && webhookData.has("playerName") && webhookData.has("productId") && webhookData.has("amount")) {
                    try {
                        playerUuid = webhookData.get("playerUuid").getAsString();
                        playerName = webhookData.get("playerName").getAsString();
                        productId = webhookData.get("productId").getAsString();
                        amount = webhookData.get("amount").getAsDouble();
                        if (webhookData.has("storeName")) {
                            storeName = webhookData.get("storeName").getAsString();
                        }
                        plugin.getLogger().info(ChatColor.GREEN + "[StorePulse] " + ChatColor.WHITE + "Detected Generic webhook format (store=" + storeName + ")");
                    } catch (Exception e) {
                        plugin.getLogger().warning(ChatColor.RED + "[StorePulse] " + ChatColor.WHITE + "Failed to parse Generic payload: " + e.getMessage());
                        throw new IllegalArgumentException("Invalid Generic payload structure: " + e.getMessage());
                    }
                } else {
                    plugin.getLogger().warning(ChatColor.RED + "[StorePulse] " + ChatColor.WHITE + "Unrecognized webhook payload structure. Supported formats:");
                    plugin.getLogger().warning(ChatColor.YELLOW + "  - Tebex: " + ChatColor.WHITE + "{player:{uuid,name}, packages:[{id}], price:{amount}}");
                    plugin.getLogger().warning(ChatColor.YELLOW + "  - CraftingStore: " + ChatColor.WHITE + "{uuid, username, package:{id}, price}");
                    plugin.getLogger().warning(ChatColor.YELLOW + "  - Generic: " + ChatColor.WHITE + "{playerUuid, playerName, productId, amount[, storeName]}");
                    throw new IllegalArgumentException("Unrecognized webhook payload structure");
                }

                // Validate UUID format
                if (playerUuid.equals("player-uuid") || playerUuid.equals("uuid")) {
                    plugin.getLogger().warning(ChatColor.RED + "[StorePulse] " + ChatColor.WHITE + "Received placeholder UUID '" + playerUuid + "'. Please use a real player UUID in webhook payloads.");
                    throw new IllegalArgumentException("Invalid UUID: placeholder value received");
                }

                try {
                    UUID.fromString(playerUuid); // Validate UUID format
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning(ChatColor.RED + "[StorePulse] " + ChatColor.WHITE + "Invalid UUID format: " + playerUuid);
                    throw new IllegalArgumentException("Invalid UUID format: " + playerUuid);
                }

                String finalStoreName = storeName;
                String finalPlayerUuid = playerUuid;
                String finalPlayerName = playerName;
                String finalProductId = productId;
                double finalAmount = amount;

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    try {
                        monetizationManager.recordPurchase(UUID.fromString(finalPlayerUuid), finalPlayerName, finalProductId, finalAmount, finalStoreName);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Failed to record purchase in main thread: " + e.getMessage());
                        e.printStackTrace();
                    }
                });

                sendResponse(exchange, 200, "Purchase recorded successfully");
                plugin.getLogger().info("Webhook processed for " + playerName + " - " + productId + " (store=" + finalStoreName + ")");

            } catch (Exception e) {
                plugin.getLogger().warning(ChatColor.RED + "[StorePulse] " + ChatColor.WHITE + "Failed to process webhook: " + e.getMessage());
                sendResponse(exchange, 400, "Invalid webhook data: " + e.getMessage());
            }
        }
    }

    private void sendStatusPage(HttpExchange exchange) throws IOException {
        String html = "<html><head><meta charset='UTF-8'><title>StorePulse Status</title></head>"
            + "<body style='font-family:Segoe UI,Arial,sans-serif;background:#111;color:#eee;text-align:center;padding:40px;'>"
            + "<div style='display:inline-block;text-align:left;max-width:540px;'>"
            + "<h1 style='font-size:3rem;margin:0;color:#f2b632;'>STOREPULSe</h1>"
            + "<p style='margin:0 0 24px 0;color:#bbb;font-size:1rem;'>Made by ZCraft Studios</p>"
            + "<div style='background:#1b1b1b;border:1px solid #333;border-radius:16px;padding:20px;'>"
            + "<p style='margin:0 0 8px 0;font-size:1.1rem;'>Listening for webhooks</p>"
            + "<p style='margin:0;color:#999;'>Send POST requests to any path, for example <code>POST /</code></p>"
            + "</div></div></body></html>";
        sendResponse(exchange, 200, html, "text/html; charset=utf-8");
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        sendResponse(exchange, statusCode, response, "text/plain; charset=utf-8");
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}