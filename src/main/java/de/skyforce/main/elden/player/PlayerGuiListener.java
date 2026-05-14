package de.skyforce.main.elden.player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class PlayerGuiListener implements Listener {

    private final PlayerGuiService playerGuiService;

    public PlayerGuiListener(PlayerGuiService playerGuiService) {
        this.playerGuiService = playerGuiService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (playerGuiService.isPlayerMenu(event.getView().getTopInventory())) {
            playerGuiService.handleClick(event);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (playerGuiService.isPlayerMenu(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }
}
