package de.skyforce.main.elden.ashes.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class AshApplyMenuListener implements Listener {

    private final AshApplyMenu ashApplyMenu;

    public AshApplyMenuListener(AshApplyMenu ashApplyMenu) {
        this.ashApplyMenu = ashApplyMenu;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ashApplyMenu.handleClick(event);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (ashApplyMenu.isAshMenu(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }
}
