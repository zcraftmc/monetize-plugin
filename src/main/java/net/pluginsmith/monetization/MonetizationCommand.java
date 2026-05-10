package net.pluginsmith.monetization;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class MonetizationCommand implements CommandExecutor, TabCompleter {

    private final MonetizationPlugin plugin;
    private final MonetizationManager monetizationManager;
    private final ConfigManager configManager;

    public MonetizationCommand(MonetizationPlugin plugin, MonetizationManager monetizationManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.monetizationManager = monetizationManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String name = command.getName().toLowerCase();
        switch (name) {
            case "storepulse":
                return handleStorePulse(sender, args);
            case "topdonors":
                return handleTopDonors(sender, args);
            case "recentpurchases":
                return handleRecentPurchases(sender, args);
            case "goalstatus":
                return handleGoalStatus(sender, args);
            case "goalcreate":
                return handleGoalCreate(sender, args);
            case "goaledit":
                return handleGoalEdit(sender, args);
            case "reloadmonetization":
            case "reloadstorepulse":
                return handleReload(sender);
            default:
                return false;
        }
    }

    private boolean handleStorePulse(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendStorePulseHelp(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subcommand) {
            case "top":
            case "topdonors":
                return handleTopDonors(sender, subArgs);
            case "recent":
            case "recentpurchases":
                return handleRecentPurchases(sender, subArgs);
            case "goal":
                return handleStorePulseGoal(sender, subArgs);
            case "reload":
                return handleReload(sender);
            default:
                sendStorePulseHelp(sender);
                return true;
        }
    }

    private void sendStorePulseHelp(CommandSender sender) {
        sender.sendMessage("&6StorePulse Commands:");
        sender.sendMessage("&e/storepulse top [limit] &7- Show top donors");
        sender.sendMessage("&e/storepulse recent [limit] &7- Show recent purchases");
        sender.sendMessage("&e/storepulse goal status <goalId> &7- Show goal status");
        sender.sendMessage("&e/storepulse goal create <id> <name> <targetAmount> [expiryDays] &7- Create a donation goal");
        sender.sendMessage("&e/storepulse goal edit <id> <field> <value> &7- Edit a donation goal");
        sender.sendMessage("&e/storepulse reload &7- Reload StorePulse configuration");
    }

    private boolean handleStorePulseGoal(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("&cUsage: /storepulse goal <status|create|edit> ...");
            return true;
        }

        String action = args[0].toLowerCase();
        String[] goalArgs = Arrays.copyOfRange(args, 1, args.length);
        switch (action) {
            case "status":
                return handleGoalStatus(sender, goalArgs);
            case "create":
                return handleGoalCreate(sender, goalArgs);
            case "edit":
                return handleGoalEdit(sender, goalArgs);
            default:
                sender.sendMessage("&cUnknown goal action: " + action);
                return true;
        }
    }

    private boolean handleTopDonors(CommandSender sender, String[] args) {
        int limit = 10;
        if (args.length > 0) {
            try {
                limit = Math.max(1, Integer.parseInt(args[0]));
            } catch (NumberFormatException ignored) {
                sender.sendMessage("&cInvalid page number. Showing top 10 donors.");
            }
        }
        sender.sendMessage("&aTop " + limit + " donors:");
        List<ConfigManager.SupporterData> donors = monetizationManager.getTopDonors(limit);
        if (donors.isEmpty()) {
            sender.sendMessage("&eNo donor data available.");
            return true;
        }
        int position = 1;
        for (ConfigManager.SupporterData donor : donors) {
            sender.sendMessage("&6" + position++ + ". " + donor.playerName + " - " + String.format("$%.2f", donor.totalAmount));
        }
        return true;
    }

    private boolean handleRecentPurchases(CommandSender sender, String[] args) {
        int limit = 10;
        if (args.length > 0) {
            try {
                limit = Math.max(1, Integer.parseInt(args[0]));
            } catch (NumberFormatException ignored) {
                sender.sendMessage("&cInvalid page number. Showing last 10 purchases.");
            }
        }
        sender.sendMessage("&aRecent purchases:");
        List<ConfigManager.Purchase> purchases = monetizationManager.getRecentPurchases(limit);
        if (purchases.isEmpty()) {
            sender.sendMessage("&eNo purchase data available.");
            return true;
        }
        for (ConfigManager.Purchase purchase : purchases) {
            sender.sendMessage("&6" + purchase.playerName + " &f- " + purchase.productId + " &7($" + String.format("%.2f", purchase.amount) + ")");
        }
        return true;
    }

    private boolean handleGoalStatus(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("&cUsage: /goalstatus <goalId>");
            return false;
        }
        ConfigManager.DonationGoal goal = monetizationManager.getGoal(args[0]);
        if (goal == null) {
            sender.sendMessage("&cGoal not found: " + args[0]);
            return true;
        }
        sender.sendMessage("&aGoal " + goal.id + ": " + goal.name);
        sender.sendMessage("&7Current: $" + String.format("%.2f", goal.currentAmount) + " / $" + String.format("%.2f", goal.targetAmount));
        sender.sendMessage("&7Completed: " + goal.completed);
        sender.sendMessage("&7Expiry: " + (goal.expiryTimestamp == 0 ? "Never" : String.valueOf(goal.expiryTimestamp)));
        return true;
    }

    private boolean handleGoalCreate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("&cUsage: /goalcreate <id> <name> <targetAmount> [expiryDays]");
            return false;
        }
        String id = args[0];
        String name = args[1];
        double targetAmount;
        try {
            targetAmount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("&cInvalid target amount: " + args[2]);
            return true;
        }
        long expiryTimestamp = 0;
        if (args.length >= 4) {
            try {
                long expiryDays = Long.parseLong(args[3]);
                expiryTimestamp = System.currentTimeMillis() / 1000 + expiryDays * 86400;
            } catch (NumberFormatException e) {
                sender.sendMessage("&cInvalid expiry days: " + args[3]);
                return true;
            }
        }
        if (monetizationManager.createGoal(id, name, targetAmount, expiryTimestamp)) {
            sender.sendMessage("&aCreated goal " + id + ": " + name);
        } else {
            sender.sendMessage("&cA goal with that ID already exists.");
        }
        return true;
    }

    private boolean handleGoalEdit(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("&cUsage: /goaledit <id> <name|target|current|expiry|complete> <value>");
            return false;
        }
        String id = args[0];
        String field = args[1];
        String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (monetizationManager.editGoal(id, field, value)) {
            sender.sendMessage("&aUpdated goal " + id + ".");
        } else {
            sender.sendMessage("&cFailed to update goal. Check the goal ID and field.");
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        try {
            configManager.loadAll();
            sender.sendMessage("&aStorePulse configuration reloaded.");
        } catch (IOException e) {
            sender.sendMessage("&cFailed to reload configuration: " + e.getMessage());
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        String name = command.getName().toLowerCase();
        if (name.equals("goalcreate") && args.length == 2) {
            return Collections.singletonList("<goal-name>");
        }
        if (name.equals("goaledit") && args.length == 2) {
            return Arrays.asList("name", "target", "current", "expiry", "complete");
        }
        if (name.equals("storepulse") && args.length == 1) {
            return Arrays.asList("top", "recent", "goal", "reload");
        }
        if (name.equals("storepulse") && args.length == 2 && args[0].equalsIgnoreCase("goal")) {
            return Arrays.asList("status", "create", "edit");
        }
        if (name.equals("storepulse") && args.length == 3 && args[0].equalsIgnoreCase("goal") && args[1].equalsIgnoreCase("edit")) {
            return Arrays.asList("name", "target", "current", "expiry", "complete");
        }
        return Collections.emptyList();
    }
}
