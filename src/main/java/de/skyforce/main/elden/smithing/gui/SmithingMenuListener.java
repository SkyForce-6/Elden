package de.skyforce.main.elden.smithing.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class SmithingMenuListener implements Listener {

    private final SmithingMenu smithingMenu;

    public SmithingMenuListener(SmithingMenu smithingMenu) {
        this.smithingMenu = smithingMenu;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        smithingMenu.handleClick(event);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (smithingMenu.isSmithingMenu(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }
}
