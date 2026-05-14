package de.skyforce.main.elden.flask;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class FlaskSoulboundListener implements Listener {

    private final FlaskService flaskService;

    public FlaskSoulboundListener(FlaskService flaskService) {
        this.flaskService = flaskService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Item dropped = event.getItemDrop();
        ItemStack item = dropped.getItemStack();

        if (!flaskService.isSoulboundFlask(item)) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendActionBar(text("✖ Soulbound flasks cannot be dropped.", NamedTextColor.RED));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack item = event.getItem().getItemStack();
        if (!flaskService.isSoulboundFlask(item)) {
            return;
        }

        if (flaskService.isOwnedBy(player, item)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        boolean currentIsSoulbound = flaskService.isSoulboundFlask(current);
        boolean cursorIsSoulbound = flaskService.isSoulboundFlask(cursor);

        if (!currentIsSoulbound && !cursorIsSoulbound) {
            return;
        }

        if (currentIsSoulbound && !flaskService.isOwnedBy(player, current)) {
            event.setCancelled(true);
            player.sendActionBar(text("✖ This flask does not belong to you.", NamedTextColor.RED));
            return;
        }

        if (cursorIsSoulbound && !flaskService.isOwnedBy(player, cursor)) {
            event.setCancelled(true);
            player.sendActionBar(text("✖ This flask does not belong to you.", NamedTextColor.RED));
            return;
        }

        Inventory clickedInventory = event.getClickedInventory();
        Inventory playerInventory = player.getInventory();

        if (event.getClick().isKeyboardClick() || event.getHotbarButton() != -1) {
            event.setCancelled(true);
            player.sendActionBar(text("✖ Soulbound flasks cannot be moved.", NamedTextColor.RED));
            return;
        }

        if (event.isShiftClick()) {
            event.setCancelled(true);
            player.sendActionBar(text("✖ Soulbound flasks cannot be moved this way.", NamedTextColor.RED));
            return;
        }

        InventoryAction action = event.getAction();
        switch (action) {
            case HOTBAR_SWAP,
                 HOTBAR_MOVE_AND_READD,
                 MOVE_TO_OTHER_INVENTORY,
                 COLLECT_TO_CURSOR,
                 DROP_ALL_SLOT,
                 DROP_ONE_SLOT,
                 DROP_ALL_CURSOR,
                 DROP_ONE_CURSOR -> {
                event.setCancelled(true);
                player.sendActionBar(text("✖ Soulbound flasks cannot be moved.", NamedTextColor.RED));
                return;
            }
            default -> {
            }
        }

        if ((currentIsSoulbound || cursorIsSoulbound)
                && clickedInventory != null
                && clickedInventory != playerInventory) {
            event.setCancelled(true);
            player.sendActionBar(text("✖ Soulbound flasks cannot be stored.", NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack oldCursor = event.getOldCursor();
        if (!flaskService.isSoulboundFlask(oldCursor)) {
            return;
        }

        if (!flaskService.isOwnedBy(player, oldCursor)) {
            event.setCancelled(true);
            player.sendActionBar(text("✖ This flask does not belong to you.", NamedTextColor.RED));
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                player.sendActionBar(text("✖ Soulbound flasks cannot be stored.", NamedTextColor.RED));
                return;
            }
        }
    }

    private Component text(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}