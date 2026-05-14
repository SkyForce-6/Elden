package de.skyforce.main.elden.level;

import de.skyforce.main.elden.equipment.EquipmentWeightService;
import de.skyforce.main.elden.equipment.EquipmentWeightSnapshot;
import de.skyforce.main.elden.runes.RuneManager;
import de.skyforce.main.elden.visual.VisualEffectService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class LevelGuiService {

    private static final int MENU_SIZE = 27;
    private static final int CONFIRM_SIZE = 27;
    private static final int[] ATTRIBUTE_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19};

    private final LevelManager levelManager;
    private final RuneManager runeManager;
    private final EquipmentWeightService equipmentWeightService;
    private final VisualEffectService visualEffectService;
    private final NamespacedKey actionKey;
    private final NamespacedKey attributeKey;

    public LevelGuiService(JavaPlugin plugin, LevelManager levelManager, RuneManager runeManager,
                           EquipmentWeightService equipmentWeightService, VisualEffectService visualEffectService) {
        this.levelManager = levelManager;
        this.runeManager = runeManager;
        this.equipmentWeightService = equipmentWeightService;
        this.visualEffectService = visualEffectService;
        this.actionKey = new NamespacedKey(plugin, "level-action");
        this.attributeKey = new NamespacedKey(plugin, "level-attribute");
    }

    public void openLevelMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new LevelMenuHolder(), MENU_SIZE,
                Component.text("Level Up", NamedTextColor.GOLD));

        fillBackground(inventory);

        PlayerProgress progress = levelManager.getOrCreate(player);
        int nextCost = levelManager.getNextCost(player);
        int runes = runeManager.getRunes(player);
        double maxStamina = levelManager.getDerivedMaxStamina(player);
        double maxFocus = levelManager.getDerivedMaxFocus(player);
        double equipLoad = levelManager.getDerivedEquipLoad(player);
        EquipmentWeightSnapshot weightSnapshot = equipmentWeightService.snapshot(player);
        double dexCastSpeedMultiplier = levelManager.getDexterityCastSpeedMultiplier(player);
        double intCastSpeedMultiplier = levelManager.getIntelligenceCastSpeedMultiplier(player);
        double intAttackMultiplier = levelManager.getIntelligenceAttackMultiplier(player);
        double faithCastSpeedMultiplier = levelManager.getFaithCastSpeedMultiplier(player);
        double faithAttackMultiplier = levelManager.getFaithAttackMultiplier(player);
        double arcaneCastSpeedMultiplier = levelManager.getArcaneCastSpeedMultiplier(player);
        double arcaneStatusMultiplier = levelManager.getArcaneStatusBuildupMultiplier(player);
        int arcaneItemDiscovery = levelManager.getArcaneItemDiscovery(player);

        inventory.setItem(4, createStatusItem(progress, runes, nextCost, maxStamina, maxFocus, equipLoad, weightSnapshot,
                dexCastSpeedMultiplier, intCastSpeedMultiplier, intAttackMultiplier,
                faithCastSpeedMultiplier, faithAttackMultiplier,
                arcaneCastSpeedMultiplier, arcaneStatusMultiplier, arcaneItemDiscovery));
        inventory.setItem(22, createActionItem(Material.BARRIER, "Close", "close"));

        AttributeType[] attributes = AttributeType.values();
        for (int i = 0; i < attributes.length && i < ATTRIBUTE_SLOTS.length; i++) {
            inventory.setItem(ATTRIBUTE_SLOTS[i], createAttributeItem(progress, attributes[i], nextCost, runes));
        }

        player.openInventory(inventory);
    }

    private void openConfirmMenu(Player player, AttributeType attributeType) {
        PlayerProgress progress = levelManager.getOrCreate(player);
        int runes = runeManager.getRunes(player);
        int nextCost = levelManager.getNextCost(player);

        Inventory inventory = Bukkit.createInventory(new LevelConfirmHolder(attributeType.key()), CONFIRM_SIZE,
                Component.text("Confirm Level Up", NamedTextColor.GOLD));

        fillBackground(inventory);
        inventory.setItem(11, createActionItem(Material.LIME_CONCRETE, "Confirm", "confirm"));
        inventory.setItem(15, createActionItem(Material.RED_CONCRETE, "Cancel", "cancel"));
        inventory.setItem(13, createAttributeItem(progress, attributeType, nextCost, runes));

        player.openInventory(inventory);
    }

    public boolean isLevelMenu(Inventory inventory) {
        return inventory.getHolder() instanceof LevelMenuHolder
                || inventory.getHolder() instanceof LevelConfirmHolder;
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof LevelMenuHolder)
                && !(top.getHolder() instanceof LevelConfirmHolder)) {
            return;
        }

        if (event.getRawSlot() >= top.getSize()) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        if (!player.hasPermission("elden.level.use")) {
            player.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            player.closeInventory();
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        String action = pdc.get(actionKey, PersistentDataType.STRING);
        if (action != null && !action.isBlank()) {
            handleAction(player, top.getHolder(), action);
            return;
        }

        if (!(top.getHolder() instanceof LevelMenuHolder)) {
            return;
        }

        String attrKey = pdc.get(attributeKey, PersistentDataType.STRING);
        if (attrKey == null || attrKey.isBlank()) {
            return;
        }

        Optional<AttributeType> attributeType = AttributeType.fromInput(attrKey);
        if (attributeType.isEmpty()) {
            return;
        }

        openConfirmMenu(player, attributeType.get());
    }

    private void handleAction(Player player, InventoryHolder holder, String action) {
        switch (action) {
            case "close" -> player.closeInventory();
            case "cancel" -> openLevelMenu(player);
            case "confirm" -> {
                if (!(holder instanceof LevelConfirmHolder confirmHolder)) {
                    return;
                }

                Optional<AttributeType> attributeType = AttributeType.fromInput(confirmHolder.attributeKey());
                if (attributeType.isEmpty()) {
                    openLevelMenu(player);
                    return;
                }

                Optional<String> error = levelManager.levelUp(player, attributeType.get());
                if (error.isPresent()) {
                    player.sendActionBar(Component.text(error.get(), NamedTextColor.RED));
                    openLevelMenu(player);
                    return;
                }

                PlayerProgress progress = levelManager.getOrCreate(player);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.15f);
                visualEffectService.playLevelUp(player);
                player.sendActionBar(Component.text(
                        attributeType.get().displayName() + " -> " + progress.attribute(attributeType.get())
                                + " (Level " + progress.level() + ")",
                        NamedTextColor.GREEN));
                openLevelMenu(player);
            }
        }
    }

    private ItemStack createStatusItem(PlayerProgress progress, int runes, int nextCost,
                                       double maxStamina, double maxFocus, double equipLoad, EquipmentWeightSnapshot weightSnapshot,
                                       double dexCastSpeedMultiplier, double intCastSpeedMultiplier,
                                       double intAttackMultiplier,
                                       double faithCastSpeedMultiplier, double faithAttackMultiplier,
                                       double arcaneCastSpeedMultiplier, double arcaneStatusMultiplier,
                                       int arcaneItemDiscovery) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Tarnished Status", NamedTextColor.YELLOW));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Level: " + progress.level(), NamedTextColor.WHITE));
        lore.add(Component.text("Runes: " + runes, NamedTextColor.WHITE));
        lore.add(Component.text("Next level cost: " + nextCost, NamedTextColor.GOLD));
        lore.add(Component.text("Max HP: " + String.format(java.util.Locale.ROOT, "%.1f", VigorScaling.maxHealthForVigor(progress.attribute(AttributeType.VIGOR))), NamedTextColor.RED));
        lore.add(Component.text("Max FP: " + String.format(java.util.Locale.ROOT, "%.1f", maxFocus), NamedTextColor.BLUE));
        lore.add(Component.text("Max Stamina: " + String.format(java.util.Locale.ROOT, "%.1f", maxStamina), NamedTextColor.AQUA));
        lore.add(Component.text("Equip Load: " + String.format(java.util.Locale.ROOT, "%.1f", equipLoad), NamedTextColor.GRAY));
        lore.add(Component.text(
                "Current Load: "
                        + String.format(java.util.Locale.ROOT, "%.1f", weightSnapshot.currentLoad())
                        + " / "
                        + String.format(java.util.Locale.ROOT, "%.1f", weightSnapshot.maxLoad())
                        + " ("
                        + weightSnapshot.tier().displayName()
                        + ")",
                NamedTextColor.GRAY));
        lore.add(Component.text("Dex Cast Speed: x" + String.format(java.util.Locale.ROOT, "%.2f", dexCastSpeedMultiplier), NamedTextColor.LIGHT_PURPLE));
        lore.add(Component.text("Int Cast Speed: x" + String.format(java.util.Locale.ROOT, "%.2f", intCastSpeedMultiplier), NamedTextColor.BLUE));
        lore.add(Component.text("Sorcery Power: x" + String.format(java.util.Locale.ROOT, "%.2f", intAttackMultiplier), NamedTextColor.AQUA));
        lore.add(Component.text("Faith Cast Speed: x" + String.format(java.util.Locale.ROOT, "%.2f", faithCastSpeedMultiplier), NamedTextColor.YELLOW));
        lore.add(Component.text("Incantation Power: x" + String.format(java.util.Locale.ROOT, "%.2f", faithAttackMultiplier), NamedTextColor.GOLD));
        lore.add(Component.text("Arcane Cast Speed: x" + String.format(java.util.Locale.ROOT, "%.2f", arcaneCastSpeedMultiplier), NamedTextColor.DARK_PURPLE));
        lore.add(Component.text("Status Buildup: x" + String.format(java.util.Locale.ROOT, "%.2f", arcaneStatusMultiplier), NamedTextColor.DARK_GREEN));
        lore.add(Component.text("Item Discovery: " + arcaneItemDiscovery, NamedTextColor.GREEN));
        lore.add(Component.text("Click an attribute to level up.", NamedTextColor.GRAY));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAttributeItem(PlayerProgress progress, AttributeType attributeType, int nextCost, int runes) {
        ItemStack item = new ItemStack(iconFor(attributeType));
        ItemMeta meta = item.getItemMeta();
        int current = progress.attribute(attributeType);

        meta.displayName(Component.text(attributeType.displayName(), NamedTextColor.GOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Current: " + current, NamedTextColor.WHITE));
        lore.add(Component.text("After level up: " + (current + 1), NamedTextColor.GRAY));
        lore.add(Component.text("Cost: " + nextCost + " runes", NamedTextColor.YELLOW));
        lore.add(Component.text(runes >= nextCost ? "Affordable" : "Not enough runes",
                runes >= nextCost ? NamedTextColor.GREEN : NamedTextColor.RED));

        meta.lore(lore);
        meta.getPersistentDataContainer().set(attributeKey, PersistentDataType.STRING, attributeType.key());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createActionItem(Material material, String label, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(label, NamedTextColor.YELLOW));
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private Material iconFor(AttributeType attributeType) {
        return switch (attributeType) {
            case VIGOR -> Material.RED_DYE;
            case MIND -> Material.LAPIS_LAZULI;
            case ENDURANCE -> Material.CHAINMAIL_CHESTPLATE;
            case STRENGTH -> Material.IRON_AXE;
            case DEXTERITY -> Material.BOW;
            case INTELLIGENCE -> Material.ENCHANTED_BOOK;
            case FAITH -> Material.AMETHYST_SHARD;
            case ARCANE -> Material.ENDER_EYE;
        };
    }

    private void fillBackground(Inventory inventory) {
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(Component.text(" "));
        glass.setItemMeta(meta);

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, glass);
        }
    }

    private record LevelMenuHolder() implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private record LevelConfirmHolder(String attributeKey) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
