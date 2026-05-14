package de.skyforce.main.elden.level;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class LevelListener implements Listener {

    private final LevelManager levelManager;

    public LevelListener(LevelManager levelManager) {
        this.levelManager = levelManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        levelManager.applyDerivedStats(event.getPlayer());
    }
}

