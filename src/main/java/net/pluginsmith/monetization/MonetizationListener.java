package net.pluginsmith.monetization;

import org.bukkit.event.Listener;

public final class MonetizationListener implements Listener {

    private final MonetizationPlugin plugin;
    private final MonetizationManager monetizationManager;

    public MonetizationListener(MonetizationPlugin plugin, MonetizationManager monetizationManager) {
        this.plugin = plugin;
        this.monetizationManager = monetizationManager;
    }
}
