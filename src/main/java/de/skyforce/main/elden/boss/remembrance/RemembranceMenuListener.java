package de.skyforce.main.elden.boss.remembrance;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class RemembranceMenuListener implements Listener {

    private final RemembranceMenu remembranceMenu;

    public RemembranceMenuListener(RemembranceMenu remembranceMenu) {
        this.remembranceMenu = remembranceMenu;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        remembranceMenu.handleClick(event);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (remembranceMenu.isRemembranceMenu(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }
}
