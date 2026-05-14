package de.skyforce.main.elden.talisman.gui;

import de.skyforce.main.elden.talisman.model.TalismanDefinition;
import de.skyforce.main.elden.talisman.registry.TalismanRegistry;
import de.skyforce.main.elden.talisman.service.TalismanItemFactory;
import de.skyforce.main.elden.talisman.service.TalismanManager;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class TalismanMenu {

    private static final int SIZE = 54;
    private static final int[] EQUIPPED_SLOTS = {10, 12, 14, 16};

    private final TalismanRegistry talismanRegistry;
    private final TalismanItemFactory talismanItemFactory;
    private final TalismanManager talismanManager;
    private final NamespacedKey actionKey;
    private final NamespacedKey talismanIdKey;
    private final NamespacedKey slotKey;

    public TalismanMenu(JavaPlugin plugin, TalismanRegistry talismanRegistry, TalismanItemFactory talismanItemFactory,
                        TalismanManager talismanManager) {
        this.talismanRegistry = talismanRegistry;
        this.talismanItemFactory = talismanItemFactory;
        this.talismanManager = talismanManager;
        this.actionKey = new NamespacedKey(plugin, "talisman-menu-action");
        this.talismanIdKey = new NamespacedKey(plugin, "talisman-menu-id");
        this.slotKey = new NamespacedKey(plugin, "talisman-menu-slot");
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(new Holder(), SIZE, Component.text("Talisman Pouch", NamedTextColor.GOLD));
        fillBackground(inventory);

        for (int i = 0; i < EQUIPPED_SLOTS.length; i++) {
            inventory.setItem(EQUIPPED_SLOTS[i], createEquippedSlot(player, i));
        }

        int slot = 28;
        for (TalismanDefinition talisman : talismanRegistry.all()) {
            if (slot >= 44) {
                break;
            }
            inventory.setItem(slot++, createPreview(talisman));
        }
        inventory.setItem(49, createAction(Material.BARRIER, "Close", "close", -1, null));
        player.openInventory(inventory);
    }

    public boolean isTalismanMenu(Inventory inventory) {
        return inventory.getHolder() instanceof Holder;
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!isTalismanMenu(top)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() >= top.getSize()) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !clicked.hasItemMeta()) {
            return;
        }
        ItemMeta meta = clicked.getItemMeta();
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action == null) {
            return;
        }
        if (action.equals("close")) {
            player.closeInventory();
            return;
        }
        Integer slot = meta.getPersistentDataContainer().get(slotKey, PersistentDataType.INTEGER);
        if (slot == null) {
            return;
        }
        switch (action) {
            case "unequip" -> {
                talismanManager.unequip(player, slot);
                open(player);
            }
            case "equip-held" -> {
                String heldTalismanId = talismanItemFactory.getTalismanId(player.getInventory().getItemInMainHand());
                if (heldTalismanId != null && talismanManager.equip(player, slot, heldTalismanId)) {
                    open(player);
                }
            }
            default -> {
            }
        }
    }

    private ItemStack createEquippedSlot(Player player, int slot) {
        TalismanDefinition talisman = talismanManager.equipped(player, slot).orElse(null);
        if (talisman == null) {
            return createAction(Material.GRAY_DYE, "Empty Slot " + (slot + 1), "equip-held", slot,
                    List.of("Hold a talisman and click to equip it here."));
        }
        ItemStack item = talismanItemFactory.createTalismanItem(talisman);
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Click to unequip.", NamedTextColor.YELLOW));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "unequip");
        meta.getPersistentDataContainer().set(slotKey, PersistentDataType.INTEGER, slot);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPreview(TalismanDefinition talisman) {
        ItemStack item = talismanItemFactory.createTalismanItem(talisman);
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Use /talisman give " + talisman.id(), NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(talismanIdKey, PersistentDataType.STRING, talisman.id());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAction(Material material, String label, String action, int slot, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(label, NamedTextColor.YELLOW));
        List<Component> lore = new ArrayList<>();
        if (loreLines != null) {
            for (String line : loreLines) {
                lore.add(Component.text(line, NamedTextColor.GRAY));
            }
        }
        meta.lore(lore);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        if (slot >= 0) {
            meta.getPersistentDataContainer().set(slotKey, PersistentDataType.INTEGER, slot);
        }
        item.setItemMeta(meta);
        return item;
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.text(" ", NamedTextColor.DARK_GRAY));
        filler.setItemMeta(meta);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    private record Holder() implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
