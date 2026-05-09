# Monetization Plugin

A premium Minecraft server monetization plugin with Discord webhook integration, donation tracking, and goal management. Built for Paper 1.21.x servers with full Folia compatibility.

## 🎯 Features

### Core Functionality
- **Purchase Tracking** - Record and track all player purchases with detailed metadata
- **Donation Goals** - Create, manage, and monitor community donation goals with progress tracking
- **Supporter Leaderboard** - Display top donors and recent purchases in-game
- **Analytics** - Daily revenue tracking and analytics data collection
- **Auto-Backups** - Automatic data backups to prevent data loss

### Integration & Customization
- **Discord Webhooks** - Send purchase confirmations and goal updates to Discord channels
- **PlaceholderAPI** - Extensive placeholder support for your custom GUIs and scoreboards
- **JSON Configuration** - Flexible JSON-based configuration files
- **Colorized Messages** - Full MiniMessage support for rich in-game formatting
- **Role Pings** - Configure Discord role mentions for goal completions
- **Branding** - Customize server name and icons in Discord embeds

### Technical Features
- **Folia Compatible** - Works on both Paper and Folia servers (Async scheduler support)
- **Soft Dependencies** - Optional integration with PlaceholderAPI, DiscordSRV, LuckPerms, and Vault
- **JSON Webhooks** - Custom Discord webhook payload system with embed support
- **Async Operations** - All I/O operations run asynchronously to prevent server lag

## 📋 Requirements

- **Minecraft Version**: 1.21.x (tested on 1.21.11)
- **Server Software**: Paper or Folia
- **Java**: Java 21+
- **Optional**: PlaceholderAPI plugin for placeholder expansion

## � Plugin JAR Location

After building the plugin, the compiled `.jar` file is located at:

```
build/libs/Monetization-1.0.0.jar
```

**Absolute path in this workspace:**
```
/workspaces/monetize-plugin/build/libs/Monetization-1.0.0.jar
```

Use this file to install the plugin on your server.

## 🚀 Installation

### Step 1: Download the Plugin
- Download the latest `Monetization-1.0.0.jar` from the releases page
- Or compile from source: `./gradlew build` (produces `build/libs/Monetization-1.0.0.jar`)
- See **Plugin JAR Location** above for the exact file path after building

### Step 2: Install to Server
```bash
# Copy the jar file to your server's plugins directory
cp Monetization-1.0.0.jar /path/to/server/plugins/

# Restart your server
./start.sh
```

### Step 3: Configuration
On first run, the plugin generates:
- `plugins/Monetization/config.json` - Plugin configuration
- `plugins/Monetization/data.json` - Purchase and goal data
- `plugins/Monetization/backup-data.json` - Automatic backup

### Step 4: Configure Discord Webhooks
Edit `plugins/Monetization/config.json`:

```json
{
  "webhookUrls": {
    "default": "https://discord.com/api/webhooks/YOUR_WEBHOOK_ID/YOUR_WEBHOOK_TOKEN",
    "store-name": "https://discord.com/api/webhooks/..."
  },
  "branding": {
    "serverName": "Your Server Name",
    "serverIconUrl": "https://example.com/icon.png"
  },
  "features": {
    "discordWebhooksEnabled": true,
    "donationGoalsEnabled": true,
    "analyticsEnabled": true,
    "autoBackupEnabled": true
  }
}
```

## 📝 Commands

### Player Commands
| Command | Permission | Description |
|---------|-----------|-------------|
| `/topdonors [page]` | `monetization.view.topdonors` | View top supporters |
| `/recentpurchases [page]` | `monetization.view.recentpurchases` | View recent purchases |
| `/goalstatus <goalId>` | `monetization.view.goalstatus` | Check a specific donation goal |

### Admin Commands
| Command | Permission | Description |
|---------|-----------|-------------|
| `/goalcreate <id> <name> <amount> [days]` | `monetization.admin.goal` | Create a donation goal |
| `/goaledit <id> <field> <value>` | `monetization.admin.goal` | Edit a donation goal |
| `/reloadmonetization` | `monetization.admin.reload` | Reload configuration |

**Permission Hierarchy:**
- `monetization.admin` - Grants all admin permissions (default: OP)
- `monetization.admin.goal` - Goal creation/editing (default: OP)
- `monetization.admin.reload` - Configuration reload (default: OP)

## 🔌 PlaceholderAPI Placeholders

### Global Placeholders
- `%monetization_total_revenue%` - Total server revenue (all-time)
- `%monetization_top_donor_name%` - Name of the top donor
- `%monetization_top_donor_amount%` - Top donor's total contribution

### Player Placeholders
- `%monetization_total_donated_<player>%` - Total donated by player
- `%monetization_total_donated_%player_name%%` - Donated by current player

### Goal Placeholders
- `%monetization_goal_<goalId>_name%` - Goal name
- `%monetization_goal_<goalId>_current%` - Current progress amount
- `%monetization_goal_<goalId>_target%` - Target amount
- `%monetization_goal_<goalId>_progress_bar%` - Visual progress bar
- `%monetization_goal_<goalId>_progress_percent%` - Progress percentage
- `%monetization_goal_<goalId>_remaining%` - Remaining amount needed
- `%monetization_goal_<goalId>_expires%` - Goal expiration date
- `%monetization_goal_<goalId>_completed%` - Whether goal is completed

**Example:**
```
Top Donor: %monetization_top_donor_name% ($%monetization_top_donor_amount%)
Goal Progress: %monetization_goal_summer_progress_bar% %monetization_goal_summer_progress_percent%%%
```

## ⚙️ Configuration Reference

### config.json Structure

```json
{
  "webhookUrls": {
    "default": "webhook-url"
  },
  "apiTokens": {
    "tebex": "your-api-key"
  },
  "branding": {
    "serverName": "Server Name",
    "serverIconUrl": "https://example.com/icon.png"
  },
  "embedColors": {
    "purchase": "#00FF00",
    "goalUpdate": "#FFFF00",
    "goalComplete": "#0000FF"
  },
  "messages": {
    "thankYou": "Thank you, {player}, for your purchase!",
    "goalComplete": "🎉 {goalName} completed!",
    "goalUpdate": "{goalName} progress: {progressBar}"
  },
  "features": {
    "discordWebhooksEnabled": true,
    "donationGoalsEnabled": true,
    "analyticsEnabled": true,
    "autoBackupEnabled": true
  },
  "goalUpdateIntervalTicks": 1200,
  "backupIntervalTicks": 72000,
  "leaderboardEntriesPerPage": 10,
  "progressBarSegments": 10,
  "progressBarFullChar": "🟩",
  "progressBarEmptyChar": "⬜"
}
```

### Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `webhookUrls` | Map | {} | Discord webhook URLs mapped by store name |
| `branding.serverName` | String | "My Awesome Server" | Server name displayed in embeds |
| `branding.serverIconUrl` | String | "" | Server icon URL for embeds |
| `embedColors.*` | String | Hex | Colors for different webhook types |
| `features.discordWebhooksEnabled` | Boolean | true | Enable Discord webhook notifications |
| `features.donationGoalsEnabled` | Boolean | true | Enable donation goal tracking |
| `features.analyticsEnabled` | Boolean | true | Enable daily analytics |
| `features.autoBackupEnabled` | Boolean | true | Enable automatic backups |
| `goalUpdateIntervalTicks` | Long | 1200 | Goal check interval in ticks (60s) |
| `backupIntervalTicks` | Long | 72000 | Backup interval in ticks (1 hour) |
| `progressBarSegments` | Int | 10 | Number of segments in progress bar |

## 🔗 Discord Webhook Integration

### Automatic Webhook Events

The plugin sends webhook messages for:

1. **Purchase Notifications** - Player purchase confirmations
   - Includes player name, product, amount, store
   - Player avatar and skin preview

2. **Goal Updates** - Periodic goal progress updates
   - Shows progress bar, percentage, current/target amounts
   - Only sent if goal not completed

3. **Goal Completion** - When a goal is completed
   - Confirmation message with final amounts
   - Optional role ping

### Setting Up Discord Webhooks

1. Go to your Discord server settings
2. Navigate to **Channels** → Select a channel → **Integrations** → **Webhooks**
3. Click **Create Webhook**
4. Give it a name (e.g., "Monetization Bot")
5. Copy the webhook URL
6. Paste into `config.json` → `webhookUrls.default`

## 💾 Data Files

### data.json Structure
```json
{
  "purchases": [
    {
      "playerUuid": "uuid",
      "playerName": "PlayerName",
      "productId": "product-123",
      "amount": 99.99,
      "storeName": "Tebex",
      "timestamp": 1234567890
    }
  ],
  "goals": [
    {
      "id": "summer-goal",
      "name": "Summer Server Upgrade",
      "targetAmount": 5000.00,
      "currentAmount": 2500.00,
      "expiryTimestamp": 1234567890,
      "completed": false,
      "webhookSent": false
    }
  ],
  "supporters": {
    "uuid": {
      "playerName": "PlayerName",
      "totalAmount": 299.97,
      "lastPurchaseTimestamp": 1234567890
    }
  },
  "analytics": {
    "2026-05-09": 500.00
  }
}
```

## 🛠️ Building from Source

### Requirements
- Git
- Java 21+
- Gradle (included via wrapper)

### Build Steps
```bash
# Clone the repository
git clone https://github.com/zraxgaming/monetize-plugin.git
cd monetize-plugin

# Build the plugin
./gradlew clean build

# Find the compiled jar
ls build/libs/Monetization-1.0.0.jar
```

## 🔧 Advanced Configuration

### Custom Embed Colors

Discord uses HEX color codes. Common colors:
- `#00FF00` - Green
- `#FF0000` - Red
- `#FFFF00` - Yellow
- `#0000FF` - Blue
- `#FFA500` - Orange

### Player Avatar & Skin URLs

The plugin uses [Crafatar](https://crafatar.com) by default. Customize in config:
```json
{
  "skinRenderApi": "https://crafatar.com/renders/body/{uuid}?scale=2&overlay",
  "avatarApi": "https://crafatar.com/avatars/{uuid}?size=64&overlay"
}
```

## 📊 Monitoring

### Check Plugin Status
```
/pl
# Output shows: [✓] Monetization
```

### View Recent Purchases
```
/recentpurchases 5
```

### Monitor Goals
```
/goalstatus summer-goal
```

## 🐛 Troubleshooting

### Plugin Not Loading
- Check server log for errors: `grep -i monetization logs/latest.log`
- Ensure Java 21+ is installed
- Verify `plugins/Monetization/` folder exists

### Webhooks Not Sending
- Verify Discord webhook URL is valid and accessible
- Check Discord server logs for rejected webhooks
- Ensure `discordWebhooksEnabled: true` in config

### Data Not Saving
- Check file permissions on `plugins/Monetization/`
- Verify disk space available
- Check server logs for I/O errors

### Commands Not Working
- Verify player has correct permissions
- Check permission node spelling (case-sensitive)
- Use `/perms` or permission plugin to debug

## 📄 License

This plugin is maintained by PluginSmith. See LICENSE file for details.

## 🤝 Support

- Report issues on [GitHub Issues](https://github.com/zraxgaming/monetize-plugin/issues)
- Check [Discussions](https://github.com/zraxgaming/monetize-plugin/discussions) for help

## 📦 Version History

### v1.0.0
- Initial release
- Purchase tracking and leaderboards
- Donation goal system
- Discord webhook integration
- PlaceholderAPI expansion
- Folia compatibility
