# StorePulse Plugin

A premium Minecraft server store and webhook plugin with unified payment webhooks, Discord integration, and goal management. Built for Paper 1.21.x servers with full Folia compatibility.

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

## 🧩 Plugin JAR Location

After building the plugin, the compiled `.jar` file is located at:

```
build/libs/StorePulse-1.0.0.jar
```

**Absolute path in this workspace:**
```
/workspaces/monetize-plugin/build/libs/StorePulse-1.0.0.jar
```

Use this file to install the plugin on your server.

## 🚀 Installation

### Step 1: Download the Plugin
- Download the latest `StorePulse-1.0.0.jar` from the releases page
- Or compile from source: `./gradlew build` (produces `build/libs/StorePulse-1.0.0.jar`)
- See **Plugin JAR Location** above for the exact file path after building

### Step 2: Install to Server
```bash
# Copy the jar file to your server's plugins directory
cp StorePulse-1.0.0.jar /path/to/server/plugins/

# Restart your server
./start.sh
```

### Step 3: Configuration
On first run, the plugin generates:
- `plugins/StorePulse/config.yml` - Plugin configuration
- `plugins/StorePulse/data.json` - Purchase and goal data
- `plugins/StorePulse/backup-data.json` - Automatic backup

### Step 4: Configure Webhook Server
The plugin includes a built-in webhook server to receive purchase notifications from payment processors like Tebex and CraftingStore.

**Important:** Configure your server's firewall to allow incoming connections on the webhook port.

```yaml
webhook:
  enabled: true
  serverPort: 8080
```

**Webhook Endpoints:**
- `POST http://your-server:8080/webhook` - Unified webhook endpoint for Tebex, CraftingStore, and generic payment processors

If you are using a reverse proxy with HTTPS, expose this endpoint as `https://your-domain:7750/webhook` or the port you choose.

### Step 5: Configure Payment Processor Webhooks
Edit `plugins/StorePulse/config.yml`:

```yaml
discord:
  webhookUrls:
    default: "https://discord.com/api/webhooks/YOUR_WEBHOOK_ID/YOUR_WEBHOOK_TOKEN"
    tebex: "https://discord.com/api/webhooks/YOUR_TEBEX_WEBHOOK_URL"
    craftingstore: "https://discord.com/api/webhooks/YOUR_CRAFTINGSTORE_WEBHOOK_URL"
  apiTokens:
    tebex: "YOUR_TEBEX_SECRET_KEY_HERE"
    craftingstore: "YOUR_CRAFTINGSTORE_SECRET_KEY_HERE"
branding:
  serverName: "xyz.studio.zcraft"
  serverIconUrl: "https://example.com/icon.png"
embedColors:
  purchase: "#00FF00"
  goalUpdate: "#FFFF00"
  goalComplete: "#0000FF"
messages:
  thankYou: "Thank you, {player}, for your generous purchase of {product}!"
  goalComplete: "🎉 {goalName} has been completed! Thank you to all supporters!"
  goalUpdate: "{goalName} progress: {progressBar} {progressPercent}% ({current}/{target})"
rolePings:
  defaultGoal: "<@&123456789012345678>"
webhook:
  enabled: true
  serverPort: 8080
store:
  defaultStoreName: "generic"
features:
  discordWebhooksEnabled: true
  donationGoalsEnabled: true
  analyticsEnabled: true
  autoBackupEnabled: true
``` 

## 📝 Commands

### Player Commands
| Command | Permission | Description |
|---------|-----------|-------------|
| `/topdonors [page]` | `storepulse.view.topdonors` | View top supporters |
| `/recentpurchases [page]` | `storepulse.view.recentpurchases` | View recent purchases |
| `/goalstatus <goalId>` | `storepulse.view.goalstatus` | Check a specific donation goal |

### Admin Commands
| Command | Permission | Description |
|---------|-----------|-------------|
| `/goalcreate <id> <name> <amount> [days]` | `storepulse.admin.goal` | Create a donation goal |
| `/goaledit <id> <field> <value>` | `storepulse.admin.goal` | Edit a donation goal |
| `/reloadmonetization` | `storepulse.admin.reload` | Reload configuration |
| `/reloadstorepulse` | `storepulse.admin.reload` | Reload configuration |

**Permission Hierarchy:**
- `storepulse.admin` - Grants all admin permissions (default: OP)
- `storepulse.admin.goal` - Goal creation/editing (default: OP)
- `storepulse.admin.reload` - Configuration reload (default: OP)

## 🔌 PlaceholderAPI Placeholders

### Global Placeholders
- `%storepulse_total_revenue%` - Total server revenue (all-time)
- `%storepulse_top_donor_name%` - Name of the top donor
- `%storepulse_top_donor_amount%` - Top donor's total contribution

### Player Placeholders
- `%storepulse_total_donated_<player>%` - Total donated by player
- `%storepulse_total_donated_%player_name%%` - Donated by current player

### Goal Placeholders
- `%storepulse_goal_<goalId>_name%` - Goal name
- `%storepulse_goal_<goalId>_current%` - Current progress amount
- `%storepulse_goal_<goalId>_target%` - Target amount
- `%storepulse_goal_<goalId>_progress_bar%` - Visual progress bar
- `%storepulse_goal_<goalId>_progress_percent%` - Progress percentage
- `%storepulse_goal_<goalId>_remaining%` - Remaining amount needed
- `%storepulse_goal_<goalId>_expires%` - Goal expiration date
- `%storepulse_goal_<goalId>_completed%` - Whether goal is completed

**Example:**
```
Top Donor: %storepulse_top_donor_name% ($%storepulse_top_donor_amount%)
Goal Progress: %storepulse_goal_summer_progress_bar% %storepulse_goal_summer_progress_percent%%%
```

## ⚙️ Configuration Reference

### config.yml Structure

Placeholders use `storepulse` as the expansion identifier, e.g. `%storepulse_total_revenue%`.

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
| `webhookServerPort` | Int | 8080 | Port for incoming payment webhooks |
| `webhookServerEnabled` | Boolean | true | Enable/disable webhook server |

## � Payment Processor Webhook Setup

### Tebex Webhook Configuration
1. Log into your Tebex control panel
2. Go to **Webhooks** → **Create Webhook**
3. Set the **URL** to: `http://your-server:8080/webhook/tebex`
4. Select events: **Payment Completed**
5. Save the webhook

### CraftingStore Webhook Configuration
1. Log into your CraftingStore dashboard
2. Go to **Settings** → **Webhooks**
3. Add webhook URL: `http://your-server:8080/webhook/craftingstore`
4. Select events: **Purchase Completed**
5. Save settings

### Generic Webhook Format
For custom payment processors, send POST requests to `http://your-server:8080/webhook/generic` with this JSON format:

```json
{
  "playerUuid": "uuid-string",
  "playerName": "PlayerName",
  "productId": "product-123",
  "amount": 9.99,
  "storeName": "your-store-name"
}
```

### Security Notes
- Ensure your webhook server port (default 8080) is properly firewalled
- Consider using HTTPS in production (requires reverse proxy like nginx)
- The webhook server runs on the same machine as your Minecraft server

## �🔗 Discord Webhook Integration

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
4. Give it a name (e.g., "StorePulse Bot")
5. Copy the webhook URL
6. Paste into `config.yml` → `discord.webhookUrls.default`

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
ls build/libs/StorePulse-1.0.0.jar
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
# Output shows: [✓] StorePulse
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
- Check server log for errors: `grep -i storepulse logs/latest.log`
- Ensure Java 21+ is installed
- Verify `plugins/StorePulse/` folder exists

### Webhooks Not Sending
- Verify Discord webhook URL is valid and accessible
- Check Discord server logs for rejected webhooks
- Ensure `discordWebhooksEnabled: true` in config

### Data Not Saving
- Check file permissions on `plugins/StorePulse/`
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
