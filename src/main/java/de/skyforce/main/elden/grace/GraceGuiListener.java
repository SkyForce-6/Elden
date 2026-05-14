package de.skyforce.main.elden.grace;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class GraceGuiListener implements Listener {

    private final GraceGuiService graceGuiService;

    public GraceGuiListener(GraceGuiService graceGuiService) {
        this.graceGuiService = graceGuiService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        graceGuiService.handleClick(event);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (graceGuiService.isGraceMenu(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }
}