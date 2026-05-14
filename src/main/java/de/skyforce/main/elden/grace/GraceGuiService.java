package de.skyforce.main.elden.grace;

import de.skyforce.main.elden.combat.StaminaManager;
import de.skyforce.main.elden.flask.FlaskService;
import de.skyforce.main.elden.focus.FocusManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class GraceGuiService {

    private static final int TRAVEL_INVENTORY_SIZE = 54;
    private static final int MAIN_INVENTORY_SIZE = 27;

    private static final int[] TRAVEL_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private static final String ACTION_REST = "rest";
    private static final String ACTION_TRAVEL = "travel";
    private static final String ACTION_LEAVE = "leave";
    private static final String ACTION_PREVIOUS = "previous";
    private static final String ACTION_NEXT = "next";
    private static final String ACTION_BACK = "back";
    private static final String ACTION_LEVEL = "level";

    private final GraceManager graceManager;
    private final FlaskService flaskService;
    private final StaminaManager staminaManager;
    private final FocusManager focusManager;
    private final NamespacedKey graceNameKey;
    private final NamespacedKey actionKey;

    public GraceGuiService(JavaPlugin plugin, GraceManager graceManager, FlaskService flaskService, StaminaManager staminaManager, FocusManager focusManager) {
        this.graceManager = graceManager;
        this.flaskService = flaskService;
        this.staminaManager = staminaManager;
        this.focusManager = focusManager;
        this.graceNameKey = new NamespacedKey(plugin, "grace-name");
        this.actionKey = new NamespacedKey(plugin, "grace-action");
    }

    public void openMainMenu(Player player, String currentGraceKey) {
        Inventory inventory = Bukkit.createInventory(
                new GraceMainMenuHolder(currentGraceKey),
                MAIN_INVENTORY_SIZE,
                Component.text("Site of Grace", NamedTextColor.GOLD)
        );

        fillBackground(inventory, MAIN_INVENTORY_SIZE);

        inventory.setItem(11, createActionItem(
                Material.GOLDEN_APPLE,
                "Rest",
                List.of(
                        "Restore health and recover.",
                        "Attune to this grace."
                ),
                ACTION_REST
        ));

        inventory.setItem(13, createGraceStatusItem(player, currentGraceKey));

        inventory.setItem(15, createActionItem(
                Material.COMPASS,
                "Travel",
                List.of(
                        "Fast travel to discovered graces."
                ),
                ACTION_TRAVEL
        ));

        inventory.setItem(22, createActionItem(
                Material.BARRIER,
                "Leave",
                List.of(
                        "Close the grace menu."
                ),
                ACTION_LEAVE
        ));

        inventory.setItem(24, createActionItem(
                Material.EXPERIENCE_BOTTLE,
                "Level Up",
                List.of(
                        "Open your level interface.",
                        "Spend runes to increase attributes."
                ),
                ACTION_LEVEL
        ));

        player.openInventory(inventory);
    }

    public void openTravelMenu(Player player, int requestedPage) {
        List<GracePoint> gracePoints = new ArrayList<>(graceManager.getDiscoveredGracePoints(player));
        int maxPage = Math.max(0, (int) Math.ceil(gracePoints.size() / (double) TRAVEL_SLOTS.length) - 1);
        int page = Math.max(0, Math.min(requestedPage, maxPage));

        Inventory inventory = Bukkit.createInventory(
                new GraceTravelMenuHolder(page),
                TRAVEL_INVENTORY_SIZE,
                Component.text("Travel", NamedTextColor.GOLD)
        );

        fillBackground(inventory, TRAVEL_INVENTORY_SIZE);

        inventory.setItem(4, createTravelStatusItem(player));

        if (page > 0) {
            inventory.setItem(45, createActionButton(Material.ARROW, "Previous", ACTION_PREVIOUS));
        }

        inventory.setItem(49, createActionButton(Material.BARRIER, "Back", ACTION_BACK));

        if (page < maxPage) {
            inventory.setItem(53, createActionButton(Material.ARROW, "Next", ACTION_NEXT));
        }

        int start = page * TRAVEL_SLOTS.length;
        for (int index = 0; index < TRAVEL_SLOTS.length; index++) {
            int sourceIndex = start + index;
            if (sourceIndex >= gracePoints.size()) {
                break;
            }

            GracePoint point = gracePoints.get(sourceIndex);
            boolean isActive = graceManager.getActiveGraceName(player)
                    .map(active -> active.equalsIgnoreCase(point.getKey()))
                    .orElse(false);

            inventory.setItem(TRAVEL_SLOTS[index], createGraceItem(point, isActive));
        }

        player.openInventory(inventory);
    }

    public boolean isGraceMenu(Inventory inventory) {
        return inventory.getHolder() instanceof GraceMainMenuHolder
                || inventory.getHolder() instanceof GraceTravelMenuHolder;
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();
        if (!(holder instanceof GraceMainMenuHolder) && !(holder instanceof GraceTravelMenuHolder)) {
            return;
        }

        if (event.getRawSlot() >= topInventory.getSize()) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        String action = container.get(actionKey, PersistentDataType.STRING);
        if (action != null && !action.isBlank()) {
            handleActionClick(player, holder, action);
            return;
        }

        String graceKey = container.get(graceNameKey, PersistentDataType.STRING);
        if (graceKey == null || graceKey.isBlank()) {
            return;
        }

        if (!(holder instanceof GraceTravelMenuHolder travelHolder)) {
            return;
        }

        Optional<GracePoint> gracePoint = graceManager.getGracePoint(graceKey);
        if (gracePoint.isEmpty()) {
            player.sendMessage(Component.text("This grace no longer exists.", NamedTextColor.RED));
            openTravelMenu(player, travelHolder.page());
            return;
        }

        if (!graceManager.hasDiscoveredGrace(player, graceKey)) {
            player.sendMessage(Component.text("You have not discovered this grace yet.", NamedTextColor.RED));
            return;
        }

        if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT) {
            player.playSound(player.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1.0f, 1.0f);
            player.teleport(gracePoint.get().getLocation());
            player.sendMessage(Component.text("Traveled to: " + gracePoint.get().getDisplayName(), NamedTextColor.GOLD));
            player.closeInventory();
        }
    }

    private void handleActionClick(Player player, InventoryHolder holder, String action) {
        switch (action) {
            case ACTION_REST -> {
                if (!(holder instanceof GraceMainMenuHolder mainHolder)) {
                    return;
                }

                String currentGraceKey = mainHolder.currentGraceKey();
                graceManager.activateGrace(player, currentGraceKey);
                graceManager.saveAll();

                restAtGrace(player);

                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.2f);
                player.sendMessage(Component.text("You rest at the Site of Grace.", NamedTextColor.GOLD));
                openMainMenu(player, currentGraceKey);
            }

            case ACTION_TRAVEL -> openTravelMenu(player, 0);

            case ACTION_LEAVE -> player.closeInventory();

            case ACTION_LEVEL -> {
                player.closeInventory();
                player.performCommand("level");
            }

            case ACTION_PREVIOUS -> {
                if (holder instanceof GraceTravelMenuHolder travelHolder) {
                    openTravelMenu(player, travelHolder.page() - 1);
                }
            }

            case ACTION_NEXT -> {
                if (holder instanceof GraceTravelMenuHolder travelHolder) {
                    openTravelMenu(player, travelHolder.page() + 1);
                }
            }

            case ACTION_BACK -> {
                String activeKey = graceManager.getActiveGraceName(player).orElse("unknown");
                openMainMenu(player, activeKey);
            }
        }
    }

    private void restAtGrace(Player player) {
        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            player.setHealth(maxHealthAttribute.getValue());
        }

        staminaManager.reset(player);
        focusManager.reset(player);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setExhaustion(0.0f);
        player.setFireTicks(0);

        flaskService.refillFlasks(player);

        removeNegativeEffect(player, PotionEffectType.POISON);
        removeNegativeEffect(player, PotionEffectType.WEAKNESS);
        removeNegativeEffect(player, PotionEffectType.SLOWNESS);
        removeNegativeEffect(player, PotionEffectType.BLINDNESS);
        removeNegativeEffect(player, PotionEffectType.HUNGER);
        removeNegativeEffect(player, PotionEffectType.WITHER);
        removeNegativeEffect(player, PotionEffectType.LEVITATION);
        removeNegativeEffect(player, PotionEffectType.DARKNESS);
        removeNegativeEffect(player, PotionEffectType.MINING_FATIGUE);
        removeNegativeEffect(player, PotionEffectType.NAUSEA);
    }

    private void removeNegativeEffect(Player player, PotionEffectType type) {
        PotionEffect effect = player.getPotionEffect(type);
        if (effect != null) {
            player.removePotionEffect(type);
        }
    }

    private ItemStack createGraceStatusItem(Player player, String currentGraceKey) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        String currentDisplay = graceManager.getGraceDisplayName(currentGraceKey).orElse(currentGraceKey);
        String activeDisplay = graceManager.getActiveGraceDisplayName(player).orElse("None");

        meta.displayName(Component.text("Current Grace", NamedTextColor.YELLOW));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(currentDisplay, NamedTextColor.WHITE));
        lore.add(Component.text("Active: " + activeDisplay, NamedTextColor.GRAY));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTravelStatusItem(Player player) {
        ItemStack item = new ItemStack(Material.MAP);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Discovered Graces", NamedTextColor.YELLOW));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Unlocked: " + graceManager.getDiscoveredGraces(player).size(), NamedTextColor.WHITE));
        lore.add(Component.text("Select a destination to travel.", NamedTextColor.GRAY));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGraceItem(GracePoint point, boolean active) {
        ItemStack item = new ItemStack(active ? Material.SOUL_CAMPFIRE : Material.CAMPFIRE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(point.getDisplayName(), active ? NamedTextColor.AQUA : NamedTextColor.GOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Key: " + point.getKey(), NamedTextColor.DARK_GRAY));
        lore.add(Component.text(active ? "Currently active" : "Discovered", NamedTextColor.GRAY));
        lore.add(Component.text("Click to travel.", NamedTextColor.WHITE));
        meta.lore(lore);

        if (active) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        meta.getPersistentDataContainer().set(graceNameKey, PersistentDataType.STRING, point.getKey());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createActionItem(Material material, String name, List<String> loreLines, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(name, NamedTextColor.GOLD));

        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(Component.text(line, NamedTextColor.GRAY));
        }
        meta.lore(lore);

        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createActionButton(Material material, String label, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(label, NamedTextColor.YELLOW));
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private void fillBackground(Inventory inventory, int size) {
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(Component.text(" "));
        glass.setItemMeta(meta);

        for (int slot = 0; slot < size; slot++) {
            inventory.setItem(slot, glass);
        }
    }

    private record GraceMainMenuHolder(String currentGraceKey) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private record GraceTravelMenuHolder(int page) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}





