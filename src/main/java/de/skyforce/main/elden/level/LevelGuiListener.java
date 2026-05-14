package de.skyforce.main.elden.level;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class LevelGuiListener implements Listener {

    private final LevelGuiService levelGuiService;

    public LevelGuiListener(LevelGuiService levelGuiService) {
        this.levelGuiService = levelGuiService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!levelGuiService.isLevelMenu(event.getView().getTopInventory())) {
            return;
        }

        levelGuiService.handleClick(event);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!levelGuiService.isLevelMenu(event.getView().getTopInventory())) {
            return;
        }

        event.setCancelled(true);
    }
}

