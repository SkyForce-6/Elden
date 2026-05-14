package de.skyforce.main.elden.talisman.gui;

import de.skyforce.main.elden.talisman.service.TalismanManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class TalismanMenuListener implements Listener {

    private final TalismanMenu talismanMenu;
    private final TalismanManager talismanManager;
    private final JavaPlugin plugin;

    public TalismanMenuListener(JavaPlugin plugin, TalismanMenu talismanMenu, TalismanManager talismanManager) {
        this.plugin = plugin;
        this.talismanMenu = talismanMenu;
        this.talismanManager = talismanManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (talismanMenu.isTalismanMenu(event.getView().getTopInventory())) {
            talismanMenu.handleClick(event);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (talismanMenu.isTalismanMenu(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        talismanManager.applyPassiveStats(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        player.getServer().getScheduler().runTaskLater(plugin, () -> talismanManager.applyPassiveStats(player), 1L);
    }
}
