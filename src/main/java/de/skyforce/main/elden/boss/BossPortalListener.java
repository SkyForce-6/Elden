package de.skyforce.main.elden.boss;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class BossPortalListener implements Listener {

    private final BossPortalManager bossPortalManager;

    public BossPortalListener(BossPortalManager bossPortalManager) {
        this.bossPortalManager = bossPortalManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().getWorld().equals(event.getTo().getWorld())
                && event.getFrom().distanceSquared(event.getTo()) < 0.0001D) {
            return;
        }
        bossPortalManager.handlePlayerMove(event.getPlayer(), event.getFrom(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        bossPortalManager.handlePlayerDeath(event.getEntity());
    }
}
