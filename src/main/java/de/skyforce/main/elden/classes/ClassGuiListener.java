package de.skyforce.main.elden.classes;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class ClassGuiListener implements Listener {

    private final ClassGuiService classGuiService;

    public ClassGuiListener(ClassGuiService classGuiService) {
        this.classGuiService = classGuiService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!classGuiService.isClassMenu(event.getView().getTopInventory())) {
            return;
        }

        classGuiService.handleClick(event);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!classGuiService.isClassMenu(event.getView().getTopInventory())) {
            return;
        }

        event.setCancelled(true);
    }
}

