package de.skyforce.main.elden.ashes.gui;

import de.skyforce.main.elden.ashes.model.AshOfWarDefinition;
import de.skyforce.main.elden.ashes.registry.AshOfWarRegistry;
import de.skyforce.main.elden.ashes.service.AshOfWarBindingService;
import de.skyforce.main.elden.ashes.service.AshOfWarItemFactory;
import de.skyforce.main.elden.weapon.model.WeaponDefinition;
import de.skyforce.main.elden.weapon.registry.WeaponRegistry;
import de.skyforce.main.elden.weapon.service.WeaponItemFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class AshApplyMenu {

    private static final int MENU_SIZE = 27;

    private final AshOfWarRegistry ashRegistry;
    private final AshOfWarItemFactory ashItemFactory;
    private final AshOfWarBindingService bindingService;
    private final WeaponRegistry weaponRegistry;
    private final WeaponItemFactory weaponItemFactory;

    public AshApplyMenu(JavaPlugin plugin, AshOfWarRegistry ashRegistry, AshOfWarItemFactory ashItemFactory,
                        AshOfWarBindingService bindingService, WeaponRegistry weaponRegistry,
                        WeaponItemFactory weaponItemFactory) {
        this.ashRegistry = ashRegistry;
        this.ashItemFactory = ashItemFactory;
        this.bindingService = bindingService;
        this.weaponRegistry = weaponRegistry;
        this.weaponItemFactory = weaponItemFactory;
    }

    public void open(Player player, EquipmentSlot usedHand, ItemStack ashItem) {
        String ashId = ashItemFactory.getAshId(ashItem);
        AshOfWarDefinition ash = ashRegistry.getById(ashId).orElse(null);
        ItemStack targetWeapon = getTargetWeapon(player, usedHand);

        if (ash == null) {
            player.sendMessage(Component.text("This Ash of War is not registered.", NamedTextColor.RED));
            return;
        }
        if (!bindingService.isBindableWeapon(targetWeapon)) {
            player.sendMessage(Component.text("Hold an Elden weapon in your other hand to apply this Ash of War.", NamedTextColor.RED));
            return;
        }

        Inventory inventory = Bukkit.createInventory(
                new Holder(ash.id(), usedHand == EquipmentSlot.HAND ? "HAND" : "OFF_HAND"),
                MENU_SIZE,
                Component.text("Ash of War", NamedTextColor.GOLD)
        );

        fillBackground(inventory);
        inventory.setItem(11, createAshPreview(ash));
        inventory.setItem(13, createWeaponPreview(targetWeapon, ash));
        inventory.setItem(15, createStatusItem(targetWeapon, ash));
        inventory.setItem(21, createActionItem(Material.LIME_CONCRETE, "Apply", true));
        inventory.setItem(23, createActionItem(Material.RED_CONCRETE, "Cancel", false));
        player.openInventory(inventory);
    }

    public boolean isAshMenu(Inventory inventory) {
        return inventory.getHolder() instanceof Holder;
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof Holder holder)) {
            return;
        }

        if (event.getRawSlot() >= top.getSize()) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        if (clicked.getType() == Material.RED_CONCRETE) {
            player.closeInventory();
            return;
        }

        if (clicked.getType() != Material.LIME_CONCRETE) {
            return;
        }

        EquipmentSlot usedHand = "HAND".equals(holder.usedHand()) ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND;
        ItemStack ashItem = usedHand == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();

        if (!ashItemFactory.isAshOfWar(ashItem) || !holder.ashId().equalsIgnoreCase(ashItemFactory.getAshId(ashItem))) {
            player.sendActionBar(Component.text("The ash is no longer in your hand.", NamedTextColor.RED));
            player.closeInventory();
            return;
        }

        AshOfWarDefinition ash = ashRegistry.getById(holder.ashId()).orElse(null);
        ItemStack targetWeapon = getTargetWeapon(player, usedHand);
        if (ash == null || !bindingService.isBindableWeapon(targetWeapon)) {
            player.sendActionBar(Component.text("No valid target weapon was found.", NamedTextColor.RED));
            player.closeInventory();
            return;
        }
        if (!bindingService.canApply(targetWeapon, ash)) {
            player.sendActionBar(Component.text("This ash is not compatible with that weapon.", NamedTextColor.RED));
            player.closeInventory();
            return;
        }

        bindingService.applyAsh(targetWeapon, ash);
        consumeOne(player, usedHand, ashItem);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.1f);
        player.sendMessage(Component.text("Ash of War bound to weapon: " + ash.displayName(), NamedTextColor.GREEN));
        player.closeInventory();
    }

    private ItemStack getTargetWeapon(Player player, EquipmentSlot usedHand) {
        return usedHand == EquipmentSlot.HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
    }

    private void consumeOne(Player player, EquipmentSlot usedHand, ItemStack item) {
        int newAmount = item.getAmount() - 1;
        if (usedHand == EquipmentSlot.HAND) {
            player.getInventory().setItemInMainHand(newAmount > 0 ? item.asQuantity(newAmount) : null);
            return;
        }
        player.getInventory().setItemInOffHand(newAmount > 0 ? item.asQuantity(newAmount) : null);
    }

    private ItemStack createAshPreview(AshOfWarDefinition ash) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(ash.displayName(), NamedTextColor.GOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Affinity: " + ash.affinity(), NamedTextColor.GRAY));
        lore.add(Component.text("Type: " + ash.weaponType(), NamedTextColor.AQUA));
        lore.add(Component.text("FP Cost: " + formatNumber(ash.fpCost()), NamedTextColor.BLUE));
        lore.add(Component.text("Cooldown: " + ash.cooldownTicks() + "t", NamedTextColor.GRAY));
        lore.add(Component.text("Location: " + ash.location(), NamedTextColor.DARK_GREEN));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createWeaponPreview(ItemStack weapon, AshOfWarDefinition ash) {
        ItemStack item = weapon.clone();
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Will receive:", NamedTextColor.YELLOW));
        lore.add(Component.text("Ash of War: " + ash.displayName(), NamedTextColor.GOLD));
        lore.add(Component.text("Affinity: " + ash.affinity(), NamedTextColor.GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStatusItem(ItemStack weapon, AshOfWarDefinition ash) {
        boolean compatible = bindingService.canApply(weapon, ash);
        ItemStack item = new ItemStack(compatible ? Material.NETHER_STAR : Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Apply Status", compatible ? NamedTextColor.GREEN : NamedTextColor.RED));
        List<Component> lore = new ArrayList<>();
        Optional<WeaponDefinition> weaponDefinition = resolveWeaponDefinition(weapon);
        lore.add(Component.text("Weapon: " + weapon.getType(), NamedTextColor.WHITE));
        weaponDefinition.ifPresent(def -> lore.add(Component.text("Class: " + def.weaponType().displayName(), NamedTextColor.GRAY)));
        lore.add(Component.text("Ash FP Cost: " + formatNumber(ash.fpCost()), NamedTextColor.BLUE));
        lore.add(Component.text("Ash Cooldown: " + ash.cooldownTicks() + "t", NamedTextColor.GRAY));
        lore.add(Component.text(compatible ? "Compatible" : "Not compatible", compatible ? NamedTextColor.GREEN : NamedTextColor.RED));
        lore.add(Component.text("Use Sneak + Right Click after applying.", NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Optional<WeaponDefinition> resolveWeaponDefinition(ItemStack weapon) {
        String weaponId = weaponItemFactory.getWeaponId(weapon);
        if (weaponId == null) {
            return Optional.empty();
        }
        return weaponRegistry.getById(weaponId);
    }

    private ItemStack createActionItem(Material material, String label, boolean confirm) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(label, confirm ? NamedTextColor.GREEN : NamedTextColor.RED));
        List<Component> lore = List.of(Component.text(confirm ? "Bind this Ash to the shown weapon." : "Close this menu without applying.", NamedTextColor.GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.text(" ", NamedTextColor.BLACK));
        filler.setItemMeta(meta);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private String formatNumber(double value) {
        if (Math.floor(value) == value) {
            return Integer.toString((int) value);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private record Holder(String ashId, String usedHand) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}

