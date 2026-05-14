package de.skyforce.main.elden.compass;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class CompassListener implements Listener {

    private final CompassBarService compassBarService;

    public CompassListener(CompassBarService compassBarService) {
        this.compassBarService = compassBarService;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        compassBarService.removePlayer(event.getPlayer());
    }
}