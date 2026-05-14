package de.skyforce.main.elden.player;

import de.skyforce.main.elden.classes.ClassManager;
import de.skyforce.main.elden.combat.StaminaManager;
import de.skyforce.main.elden.equipment.EquipmentWeightService;
import de.skyforce.main.elden.equipment.EquipmentWeightSnapshot;
import de.skyforce.main.elden.focus.FocusManager;
import de.skyforce.main.elden.level.AttributeType;
import de.skyforce.main.elden.level.LevelManager;
import de.skyforce.main.elden.level.PlayerProgress;
import de.skyforce.main.elden.level.VigorScaling;
import de.skyforce.main.elden.runes.RuneManager;
import de.skyforce.main.elden.spell.model.SpellDefinition;
import de.skyforce.main.elden.spell.registry.SpellRegistry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerGuiService {

    private static final int MENU_SIZE = 54;
    private static final int DETAIL_SIZE = 27;

    private final LevelManager levelManager;
    private final RuneManager runeManager;
    private final ClassManager classManager;
    private final EquipmentWeightService equipmentWeightService;
    private final StaminaManager staminaManager;
    private final FocusManager focusManager;
    private final SpellRegistry spellRegistry;
    private final NamespacedKey actionKey;
    private final NamespacedKey tabKey;

    public PlayerGuiService(JavaPlugin plugin, LevelManager levelManager, RuneManager runeManager,
                            ClassManager classManager, EquipmentWeightService equipmentWeightService,
                            StaminaManager staminaManager, FocusManager focusManager, SpellRegistry spellRegistry) {
        this.levelManager = levelManager;
        this.runeManager = runeManager;
        this.classManager = classManager;
        this.equipmentWeightService = equipmentWeightService;
        this.staminaManager = staminaManager;
        this.focusManager = focusManager;
        this.spellRegistry = spellRegistry;
        this.actionKey = new NamespacedKey(plugin, "profile-action");
        this.tabKey = new NamespacedKey(plugin, "profile-tab");
    }

    public void openProfileMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new ProfileMenuHolder(), MENU_SIZE,
                Component.text("Tarnished Profile", NamedTextColor.GOLD));

        fillBackground(inventory, Material.GRAY_STAINED_GLASS_PANE);

        inventory.setItem(4, createHeadItem(player));
        inventory.setItem(19, createResourceItem("Runes", Material.GOLD_INGOT, Integer.toString(runeManager.getRunes(player)), NamedTextColor.GOLD,
                List.of("Current rune balance.", "Spend runes through the level menu."), null));
        inventory.setItem(20, createResourceItem("Level", Material.EXPERIENCE_BOTTLE,
                Integer.toString(levelManager.getOrCreate(player).level()), NamedTextColor.GREEN,
                List.of("Next level cost: " + levelManager.getNextCost(player), "Click to open the level menu."), "open-level"));
        inventory.setItem(21, createResourceItem("Health", Material.RED_STAINED_GLASS,
                formatDouble(player.getHealth()) + " / " + formatDouble(VigorScaling.maxHealthForVigor(levelManager.getOrCreate(player).attribute(AttributeType.VIGOR))),
                NamedTextColor.RED,
                List.of("Based on Vigor.", "Higher Vigor raises max HP."), null));
        inventory.setItem(22, createResourceItem("Stamina", Material.LIGHT_BLUE_STAINED_GLASS,
                formatDouble(staminaManager.getStamina(player)) + " / " + formatDouble(staminaManager.getMaxStamina(player)),
                NamedTextColor.AQUA,
                List.of("Used for attacks, dodges, and movement.", "Regenerates over time."), null));
        inventory.setItem(23, createResourceItem("Focus", Material.PURPLE_STAINED_GLASS,
                formatDouble(focusManager.getFocus(player)) + " / " + formatDouble(focusManager.getMaxFocus(player)),
                NamedTextColor.LIGHT_PURPLE,
                List.of("Used for Ashes of War and spell-like effects.", "Restore it with Cerulean flasks."), null));
        inventory.setItem(24, createClassItem(player));
        inventory.setItem(25, createOverviewItem(player));

        inventory.setItem(37, createTabButton(Material.NETHER_STAR, "Overview", "overview"));
        inventory.setItem(39, createTabButton(Material.RED_DYE, "Attributes", "attributes"));
        inventory.setItem(41, createTabButton(Material.IRON_CHESTPLATE, "Equipment", "equipment"));
        inventory.setItem(43, createTabButton(Material.ENCHANTED_BOOK, "Magic", "magic"));
        inventory.setItem(49, createActionButton(Material.BARRIER, "Close", "close"));

        inventory.setItem(46, createQuickAttributeItem(player, AttributeType.VIGOR));
        inventory.setItem(47, createQuickAttributeItem(player, AttributeType.MIND));
        inventory.setItem(48, createQuickAttributeItem(player, AttributeType.ENDURANCE));
        inventory.setItem(50, createQuickAttributeItem(player, AttributeType.STRENGTH));
        inventory.setItem(51, createQuickAttributeItem(player, AttributeType.DEXTERITY));
        inventory.setItem(52, createQuickAttributeItem(player, AttributeType.INTELLIGENCE));

        player.openInventory(inventory);
    }

    public void openAttributesMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new ProfileTabHolder("attributes"), MENU_SIZE,
                Component.text("Profile: Attributes", NamedTextColor.GOLD));

        fillBackground(inventory, Material.RED_STAINED_GLASS_PANE);
        inventory.setItem(49, createActionButton(Material.ARROW, "Back", "back"));

        PlayerProgress progress = levelManager.getOrCreate(player);
        AttributeType[] attributes = AttributeType.values();
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 28};
        for (int i = 0; i < attributes.length && i < slots.length; i++) {
            inventory.setItem(slots[i], createDetailedAttributeItem(progress, attributes[i]));
        }

        inventory.setItem(31, createStatusSummaryItem(player));
        inventory.setItem(32, createGrowthSummaryItem(player));
        inventory.setItem(34, createActionButton(Material.EXPERIENCE_BOTTLE, "Open Level Menu", "open-level"));
        player.openInventory(inventory);
    }

    public void openEquipmentMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new ProfileTabHolder("equipment"), MENU_SIZE,
                Component.text("Profile: Equipment", NamedTextColor.AQUA));

        fillBackground(inventory, Material.BLUE_STAINED_GLASS_PANE);
        inventory.setItem(49, createActionButton(Material.ARROW, "Back", "back"));

        EquipmentWeightSnapshot snapshot = equipmentWeightService.snapshot(player);
        double maxLoad = levelManager.getDerivedEquipLoad(player);

        inventory.setItem(10, createEquipmentLoadItem(snapshot, maxLoad));
        inventory.setItem(12, createEquippedItem("Main Hand", player.getInventory().getItemInMainHand(), Material.IRON_SWORD, "detail-main-hand"));
        inventory.setItem(13, createEquippedItem("Off Hand", player.getInventory().getItemInOffHand(), Material.SHIELD, "detail-off-hand"));
        inventory.setItem(15, createEquippedItem("Helmet", player.getInventory().getHelmet(), Material.IRON_HELMET, "detail-helmet"));
        inventory.setItem(16, createEquippedItem("Chestplate", player.getInventory().getChestplate(), Material.IRON_CHESTPLATE, "detail-chestplate"));
        inventory.setItem(24, createEquippedItem("Leggings", player.getInventory().getLeggings(), Material.IRON_LEGGINGS, "detail-leggings"));
        inventory.setItem(25, createEquippedItem("Boots", player.getInventory().getBoots(), Material.IRON_BOOTS, "detail-boots"));

        PlayerProgress progress = levelManager.getOrCreate(player);
        inventory.setItem(30, createResistanceItem("Poison", Material.POISONOUS_POTATO, progress.attribute(AttributeType.ARCANE), NamedTextColor.GREEN));
        inventory.setItem(31, createResistanceItem("Hemorrhage", Material.REDSTONE, progress.attribute(AttributeType.ARCANE), NamedTextColor.DARK_RED));
        inventory.setItem(32, createResistanceItem("Frost", Material.PACKED_ICE, progress.attribute(AttributeType.ENDURANCE), NamedTextColor.AQUA));
        inventory.setItem(33, createResistanceItem("Madness", Material.AMETHYST_BLOCK, progress.attribute(AttributeType.MIND), NamedTextColor.LIGHT_PURPLE));

        player.openInventory(inventory);
    }

    public void openMagicMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new ProfileTabHolder("magic"), MENU_SIZE,
                Component.text("Profile: Magic", NamedTextColor.LIGHT_PURPLE));

        fillBackground(inventory, Material.PURPLE_STAINED_GLASS_PANE);
        inventory.setItem(49, createActionButton(Material.ARROW, "Back", "back"));

        PlayerProgress progress = levelManager.getOrCreate(player);
        inventory.setItem(10, createMagicStatItem("Sorcery Scaling", Material.ENCHANTED_BOOK,
                levelManager.getIntelligenceAttackMultiplier(player), "Driven by Intelligence."));
        inventory.setItem(11, createMagicStatItem("Incantation Scaling", Material.BOOK,
                levelManager.getFaithAttackMultiplier(player), "Driven by Faith."));
        inventory.setItem(12, createMagicStatItem("Status Buildup", Material.ENDER_EYE,
                levelManager.getArcaneStatusBuildupMultiplier(player), "Driven by Arcane."));

        inventory.setItem(20, createMagicStatItem("Dex Cast Speed", Material.FEATHER,
                levelManager.getDexterityCastSpeedMultiplier(player), "Dexterity reduces cast delay."));
        inventory.setItem(21, createMagicStatItem("Int Cast Speed", Material.LAPIS_LAZULI,
                levelManager.getIntelligenceCastSpeedMultiplier(player), "Intelligence boosts sorcery flow."));
        inventory.setItem(22, createMagicStatItem("Faith Cast Speed", Material.GLOWSTONE_DUST,
                levelManager.getFaithCastSpeedMultiplier(player), "Faith improves incantation flow."));
        inventory.setItem(23, createMagicStatItem("Arcane Cast Speed", Material.AMETHYST_SHARD,
                levelManager.getArcaneCastSpeedMultiplier(player), "Arcane affects occult techniques."));

        inventory.setItem(31, createResourceItem("Mind", Material.PURPLE_DYE, Integer.toString(progress.attribute(AttributeType.MIND)),
                NamedTextColor.LIGHT_PURPLE, List.of("Primary Focus stat.", "Raises max FP."), null));
        inventory.setItem(32, createResourceItem("Intelligence", Material.BLUE_DYE, Integer.toString(progress.attribute(AttributeType.INTELLIGENCE)),
                NamedTextColor.BLUE, List.of("Improves sorcery power.", "Boosts sorcery cast speed."), null));
        inventory.setItem(33, createResourceItem("Faith", Material.YELLOW_DYE, Integer.toString(progress.attribute(AttributeType.FAITH)),
                NamedTextColor.YELLOW, List.of("Improves incantations.", "Boosts incantation cast speed."), null));
        inventory.setItem(34, createResourceItem("Arcane", Material.MAGENTA_DYE, Integer.toString(progress.attribute(AttributeType.ARCANE)),
                NamedTextColor.LIGHT_PURPLE, List.of("Improves status buildup.", "Raises item discovery to " + levelManager.getArcaneItemDiscovery(player) + "."), null));
        inventory.setItem(39, createResourceItem("Spellcasting", Material.BLAZE_POWDER, Integer.toString(spellRegistry.getAll().size()) + " tomes",
                NamedTextColor.AQUA, List.of("Use /spell list and /spell give <id>.", "Hold a tome and right-click to cast."), null));

        int slot = 41;
        for (SpellDefinition spell : firstSpellPreview(spellRegistry.getAll(), 4)) {
            inventory.setItem(slot++, createSpellPreviewItem(spell));
        }

        player.openInventory(inventory);
    }

    private void openEquipmentDetail(Player player, String title, ItemStack equipped, Material fallback) {
        Inventory inventory = Bukkit.createInventory(new ProfileTabHolder("equipment-detail"), DETAIL_SIZE,
                Component.text(title, NamedTextColor.YELLOW));
        fillBackground(inventory, Material.BLACK_STAINED_GLASS_PANE);
        inventory.setItem(18, createActionButton(Material.ARROW, "Back", "equipment-back"));

        ItemStack display = equipped != null && equipped.getType() != Material.AIR ? equipped.clone() : new ItemStack(fallback);
        ItemMeta meta = display.getItemMeta();
        List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        if (equipped == null || equipped.getType() == Material.AIR) {
            meta.displayName(Component.text(title, NamedTextColor.RED));
            lore.add(Component.text("No item equipped in this slot.", NamedTextColor.GRAY));
        } else {
            meta.displayName(Component.text(title + " Details", NamedTextColor.GOLD));
            lore.add(0, Component.text("Material: " + equipped.getType(), NamedTextColor.GRAY));
        }
        meta.lore(lore);
        display.setItemMeta(meta);
        inventory.setItem(13, display);
        player.openInventory(inventory);
    }

    public boolean isPlayerMenu(Inventory inventory) {
        return inventory.getHolder() instanceof ProfileMenuHolder || inventory.getHolder() instanceof ProfileTabHolder;
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (!isPlayerMenu(top)) {
            return;
        }

        if (event.getRawSlot() >= top.getSize()) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        if (!player.hasPermission("elden.player.gui")) {
            player.sendMessage(Component.text("You do not have permission to open this menu.", NamedTextColor.RED));
            player.closeInventory();
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) {
            return;
        }

        PersistentDataContainer pdc = clicked.getItemMeta().getPersistentDataContainer();
        String action = pdc.get(actionKey, PersistentDataType.STRING);
        if (action != null && !action.isBlank()) {
            handleAction(player, action);
            return;
        }

        String tab = pdc.get(tabKey, PersistentDataType.STRING);
        if (tab != null && !tab.isBlank()) {
            handleTabClick(player, tab);
        }
    }

    private void handleAction(Player player, String action) {
        switch (action) {
            case "close" -> player.closeInventory();
            case "back" -> openProfileMenu(player);
            case "equipment-back" -> openEquipmentMenu(player);
            case "open-level" -> {
                player.closeInventory();
                player.performCommand("level");
            }
            case "open-class" -> {
                player.closeInventory();
                player.performCommand("eldenclass");
            }
            case "detail-main-hand" -> openEquipmentDetail(player, "Main Hand", player.getInventory().getItemInMainHand(), Material.IRON_SWORD);
            case "detail-off-hand" -> openEquipmentDetail(player, "Off Hand", player.getInventory().getItemInOffHand(), Material.SHIELD);
            case "detail-helmet" -> openEquipmentDetail(player, "Helmet", player.getInventory().getHelmet(), Material.IRON_HELMET);
            case "detail-chestplate" -> openEquipmentDetail(player, "Chestplate", player.getInventory().getChestplate(), Material.IRON_CHESTPLATE);
            case "detail-leggings" -> openEquipmentDetail(player, "Leggings", player.getInventory().getLeggings(), Material.IRON_LEGGINGS);
            case "detail-boots" -> openEquipmentDetail(player, "Boots", player.getInventory().getBoots(), Material.IRON_BOOTS);
        }
    }

    private void handleTabClick(Player player, String tab) {
        switch (tab) {
            case "overview" -> openProfileMenu(player);
            case "attributes" -> openAttributesMenu(player);
            case "equipment" -> openEquipmentMenu(player);
            case "magic" -> openMagicMenu(player);
        }
    }

    private ItemStack createHeadItem(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        meta.displayName(Component.text(player.getName(), NamedTextColor.YELLOW));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Class: " + classManager.getPlayerClass(player).map(c -> c.displayName()).orElse("Unbound"), NamedTextColor.GRAY));
        lore.add(Component.text("World: " + player.getWorld().getName(), NamedTextColor.GRAY));
        lore.add(Component.text("Playtime: " + formatTime(player.getStatistic(Statistic.PLAY_ONE_MINUTE) * 50L), NamedTextColor.GRAY));
        lore.add(Component.text("UUID: " + player.getUniqueId(), NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createClassItem(Player player) {
        String playerClass = classManager.getPlayerClass(player).map(c -> c.displayName()).orElse("Unbound");
        return createResourceItem("Origin", Material.NAME_TAG, playerClass, NamedTextColor.YELLOW,
                List.of("Your starting path and identity.", "Click to open the class menu."), "open-class");
    }

    private ItemStack createOverviewItem(Player player) {
        PlayerProgress progress = levelManager.getOrCreate(player);
        EquipmentWeightSnapshot snapshot = equipmentWeightService.snapshot(player);
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Overview", NamedTextColor.GOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Equip Load Tier: " + snapshot.tier().displayName(), NamedTextColor.GRAY));
        lore.add(Component.text("Current Load: " + formatDouble(snapshot.currentLoad()) + " / " + formatDouble(snapshot.maxLoad()), NamedTextColor.GRAY));
        lore.add(Component.text("Vigor / Mind / Endurance: "
                + progress.attribute(AttributeType.VIGOR) + " / "
                + progress.attribute(AttributeType.MIND) + " / "
                + progress.attribute(AttributeType.ENDURANCE), NamedTextColor.GRAY));
        lore.add(Component.text("Use the tabs below to inspect more detail.", NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createQuickAttributeItem(Player player, AttributeType type) {
        PlayerProgress progress = levelManager.getOrCreate(player);
        ItemStack item = new ItemStack(iconFor(type));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(type.displayName() + ": " + progress.attribute(type), NamedTextColor.WHITE));
        meta.lore(List.of(Component.text(attributeDescription(type), NamedTextColor.GRAY)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDetailedAttributeItem(PlayerProgress progress, AttributeType type) {
        ItemStack item = new ItemStack(iconFor(type));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(type.displayName(), NamedTextColor.GOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Current Value: " + progress.attribute(type), NamedTextColor.WHITE));
        lore.add(Component.text(attributeDescription(type), NamedTextColor.GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStatusSummaryItem(Player player) {
        PlayerProgress progress = levelManager.getOrCreate(player);
        return createResourceItem("Status Summary", Material.COMPASS,
                "Lv " + progress.level(), NamedTextColor.GREEN,
                List.of(
                        "Runes: " + runeManager.getRunes(player),
                        "Next level cost: " + levelManager.getNextCost(player),
                        "Max HP: " + formatDouble(VigorScaling.maxHealthForVigor(progress.attribute(AttributeType.VIGOR)))
                ), null);
    }

    private ItemStack createGrowthSummaryItem(Player player) {
        return createResourceItem("Derived Growth", Material.NETHER_STAR,
                "Combat Scaling", NamedTextColor.AQUA,
                List.of(
                        "Max Stamina: " + formatDouble(levelManager.getDerivedMaxStamina(player)),
                        "Max Focus: " + formatDouble(levelManager.getDerivedMaxFocus(player)),
                        "Equip Load: " + formatDouble(levelManager.getDerivedEquipLoad(player))
                ), null);
    }

    private ItemStack createEquipmentLoadItem(EquipmentWeightSnapshot snapshot, double maxLoad) {
        return createResourceItem("Equip Load", Material.IRON_CHESTPLATE,
                formatDouble(snapshot.currentLoad()) + " / " + formatDouble(maxLoad), NamedTextColor.GRAY,
                List.of(
                        "Tier: " + snapshot.tier().displayName(),
                        "Heavier loads affect dodge quality.",
                        "Keep weight under control for better mobility."
                ), null);
    }

    private ItemStack createEquippedItem(String label, ItemStack equipped, Material fallback, String action) {
        ItemStack item = equipped != null && equipped.getType() != Material.AIR ? equipped.clone() : new ItemStack(fallback);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(label, NamedTextColor.YELLOW));
        List<Component> lore = new ArrayList<>();
        if (equipped != null && equipped.getType() != Material.AIR) {
            lore.add(Component.text("Equipped: " + equipped.getType(), NamedTextColor.GREEN));
            lore.add(Component.text("Click to inspect this slot.", NamedTextColor.GRAY));
        } else {
            lore.add(Component.text("Empty slot", NamedTextColor.RED));
            lore.add(Component.text("Click to inspect this slot.", NamedTextColor.GRAY));
        }
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createResistanceItem(String label, Material material, int sourceValue, NamedTextColor color) {
        return createResourceItem(label, material, Integer.toString(sourceValue), color,
                List.of("This resistance is currently derived from one of your core stats."), null);
    }

    private ItemStack createMagicStatItem(String label, Material material, double multiplier, String description) {
        return createResourceItem(label, material, "x" + formatDouble(multiplier), NamedTextColor.LIGHT_PURPLE,
                List.of(description), null);
    }

    private ItemStack createSpellPreviewItem(SpellDefinition spell) {
        return createResourceItem(spell.displayName(), spell.icon(),
                spell.school().displayName(), NamedTextColor.AQUA,
                List.of(
                        requirementLine(spell),
                        "FP: " + formatDouble(spell.fpCost()) + " | CD: " + spell.cooldownTicks() + "t"
                ), null);
    }

    private String requirementLine(SpellDefinition spell) {
        String line = "Requires " + spell.primaryRequirementAttribute().displayName() + " " + spell.primaryRequirementLevel();
        if (spell.hasSecondaryRequirement()) {
            line += ", " + spell.secondaryRequirementAttribute().displayName() + " " + spell.secondaryRequirementLevel();
        }
        return line;
    }

    private ItemStack createResourceItem(String label, Material material, String value, NamedTextColor color, List<String> description, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(label + ": " + value, color));
        List<Component> lore = new ArrayList<>();
        for (String line : description) {
            lore.add(Component.text(line, NamedTextColor.GRAY));
        }
        if (action != null) {
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTabButton(Material material, String label, String tab) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(label, NamedTextColor.YELLOW));
        meta.lore(List.of(Component.text("Open the " + label.toLowerCase(Locale.ROOT) + " panel.", NamedTextColor.GRAY)));
        meta.getPersistentDataContainer().set(tabKey, PersistentDataType.STRING, tab);
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

    private void fillBackground(Inventory inventory, Material material) {
        ItemStack filler = new ItemStack(material);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.text(" ", NamedTextColor.DARK_GRAY));
        filler.setItemMeta(meta);

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null || inventory.getItem(slot).getType() == Material.AIR) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private Material iconFor(AttributeType attributeType) {
        return switch (attributeType) {
            case VIGOR -> Material.RED_DYE;
            case MIND -> Material.LAPIS_LAZULI;
            case ENDURANCE -> Material.CHAINMAIL_CHESTPLATE;
            case STRENGTH -> Material.IRON_AXE;
            case DEXTERITY -> Material.BOW;
            case INTELLIGENCE -> Material.ENCHANTED_BOOK;
            case FAITH -> Material.GLOWSTONE_DUST;
            case ARCANE -> Material.ENDER_EYE;
        };
    }

    private String attributeDescription(AttributeType type) {
        return switch (type) {
            case VIGOR -> "Raises maximum HP.";
            case MIND -> "Raises maximum Focus / FP.";
            case ENDURANCE -> "Raises stamina and equip load.";
            case STRENGTH -> "Improves physical weapon scaling.";
            case DEXTERITY -> "Improves finesse scaling and cast tempo.";
            case INTELLIGENCE -> "Improves sorcery damage and sorcery cast speed.";
            case FAITH -> "Improves incantation damage and cast speed.";
            case ARCANE -> "Improves status buildup and item discovery.";
        };
    }

    private String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d";
        }
        if (hours > 0) {
            return hours + "h";
        }
        if (minutes > 0) {
            return minutes + "m";
        }
        return seconds + "s";
    }

    private List<SpellDefinition> firstSpellPreview(Collection<SpellDefinition> spells, int limit) {
        return spells.stream().limit(limit).toList();
    }

    private record ProfileMenuHolder() implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private record ProfileTabHolder(String tab) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}



