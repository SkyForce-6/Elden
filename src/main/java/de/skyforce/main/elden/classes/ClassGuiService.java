package de.skyforce.main.elden.classes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class ClassGuiService {

    private static final int INVENTORY_SIZE = 54;
    private static final int CONFIRM_SIZE = 27;

    // 12 Klassen -> alles auf eine Seite
    private static final int[] CLASS_SLOTS = {
            10, 12, 14, 16,
            19, 21, 23, 25,
            28, 30, 32, 34
    };

    private final ClassManager classManager;
    private final NamespacedKey classKey;
    private final NamespacedKey actionKey;

    public ClassGuiService(JavaPlugin plugin, ClassManager classManager) {
        this.classManager = classManager;
        this.classKey = new NamespacedKey(plugin, "elden-class-key");
        this.actionKey = new NamespacedKey(plugin, "elden-class-action");
    }

    public void openClassMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(
                new ClassMenuHolder(),
                INVENTORY_SIZE,
                plain("Choose Your Origin", NamedTextColor.GOLD)
        );

        fillBackground(inventory);

        Optional<EldenClass> current = classManager.getPlayerClass(player);
        boolean canChoose = classManager.canChooseClass(player);

        inventory.setItem(4, createStatusItem(current.orElse(null), canChoose));
        inventory.setItem(49, createActionItem(Material.BARRIER, "✖ Leave", "close"));

        EldenClass[] classes = EldenClass.values();
        for (int i = 0; i < Math.min(classes.length, CLASS_SLOTS.length); i++) {
            inventory.setItem(CLASS_SLOTS[i], createClassItem(classes[i], current.orElse(null), canChoose));
        }

        player.openInventory(inventory);
    }

    private void openConfirmMenu(Player player, EldenClass selected) {
        Inventory inventory = Bukkit.createInventory(
                new ConfirmMenuHolder(selected.key()),
                CONFIRM_SIZE,
                plain("Confirm Your Origin", NamedTextColor.GOLD)
        );

        fillBackground(inventory);

        EldenClass current = classManager.getPlayerClass(player).orElse(null);

        inventory.setItem(13, createClassPreviewItem(selected, current));
        inventory.setItem(11, createActionItem(Material.LIME_CONCRETE, "✔ Bind Origin", "confirm"));
        inventory.setItem(15, createActionItem(Material.RED_CONCRETE, "✖ Turn Back", "cancel"));

        player.openInventory(inventory);
    }

    public boolean isClassMenu(Inventory inventory) {
        return inventory.getHolder() instanceof ClassMenuHolder
                || inventory.getHolder() instanceof ConfirmMenuHolder;
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();

        if (!(holder instanceof ClassMenuHolder) && !(holder instanceof ConfirmMenuHolder)) {
            return;
        }

        if (event.getRawSlot() >= top.getSize()) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) {
            return;
        }

        PersistentDataContainer pdc = clicked.getItemMeta().getPersistentDataContainer();

        String action = pdc.get(actionKey, PersistentDataType.STRING);
        if (action != null && !action.isBlank()) {
            handleAction(player, holder, action);
            return;
        }

        if (!(holder instanceof ClassMenuHolder)) {
            return;
        }

        String selectedKey = pdc.get(classKey, PersistentDataType.STRING);
        if (selectedKey == null || selectedKey.isBlank()) {
            return;
        }

        Optional<EldenClass> parsed = EldenClass.byKey(selectedKey);
        if (parsed.isEmpty()) {
            return;
        }

        EldenClass selected = parsed.get();
        Optional<EldenClass> current = classManager.getPlayerClass(player);

        if (current.isPresent() && current.get() == selected) {
            player.sendActionBar(plain("This origin is already bound to you.", NamedTextColor.GRAY));
            return;
        }

        if (!classManager.canChooseClass(player)) {
            player.sendMessage(plain("You cannot forsake the path already chosen.", NamedTextColor.RED));
            return;
        }

        openConfirmMenu(player, selected);
    }

    private void handleAction(Player player, InventoryHolder holder, String action) {
        switch (action) {
            case "close" -> player.closeInventory();

            case "cancel" -> openClassMenu(player);

            case "confirm" -> {
                if (!(holder instanceof ConfirmMenuHolder confirmHolder)) {
                    return;
                }

                EldenClass.byKey(confirmHolder.classKey()).ifPresentOrElse(
                        selected -> {
                            if (!classManager.chooseClass(player, selected)) {
                                player.sendMessage(plain("You cannot forsake the path already chosen.", NamedTextColor.RED));
                                openClassMenu(player);
                                return;
                            }

                            classManager.saveAll();
                            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.05f);
                            player.sendMessage(plain("Your origin is now bound: " + selected.displayName(), NamedTextColor.GREEN));
                            openClassMenu(player);
                        },
                        () -> openClassMenu(player)
                );
            }

            default -> {
            }
        }
    }

    private ItemStack createStatusItem(EldenClass current, boolean canChoose) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(plain("✦ Path of the Tarnished ✦", NamedTextColor.YELLOW));

        List<Component> lore = new ArrayList<>();

        if (current != null) {
            lore.add(plain("❖ Bound Origin: " + current.displayName(), NamedTextColor.WHITE));
        } else {
            lore.add(plain("❖ Bound Origin: None", NamedTextColor.WHITE));
        }

        lore.add(Component.empty());

        if (canChoose) {
            lore.add(plain("✧ A new path may still be chosen.", NamedTextColor.GREEN));
        } else {
            lore.add(plain("✖ Your fate is already sealed.", NamedTextColor.RED));
        }

        lore.add(plain("➤ Select an origin to continue.", NamedTextColor.GRAY));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createClassItem(EldenClass eldenClass, EldenClass current, boolean canChoose) {
        boolean isCurrent = current != null && current == eldenClass;

        ItemStack item = new ItemStack(iconFor(eldenClass));
        ItemMeta meta = item.getItemMeta();

        meta.displayName(plain("✦ " + eldenClass.displayName() + " ✦",
                isCurrent ? NamedTextColor.AQUA : NamedTextColor.GOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(plain("❖ " + originDescription(eldenClass), NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(plain("✦ Level: " + eldenClass.level(), NamedTextColor.YELLOW));
        lore.add(plain("❤ VIG " + eldenClass.vig() + "   ✦ MND " + eldenClass.mnd() + "   ❈ END " + eldenClass.end(), NamedTextColor.DARK_GRAY));
        lore.add(plain("⚔ STR " + eldenClass.str() + "   ➹ DEX " + eldenClass.dex() + "   ✧ INT " + eldenClass.intl(), NamedTextColor.DARK_GRAY));
        lore.add(plain("✞ FTH " + eldenClass.fth() + "   ☾ ARC " + eldenClass.arc(), NamedTextColor.DARK_GRAY));
        lore.add(Component.empty());

        if (isCurrent) {
            lore.add(plain("✔ This origin is bound to you.", NamedTextColor.AQUA));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else if (!canChoose) {
            lore.add(plain("✖ You cannot abandon the path already taken.", NamedTextColor.RED));
        } else {
            lore.add(plain("➤ Click to choose this origin.", NamedTextColor.GREEN));
        }

        meta.lore(lore);
        meta.getPersistentDataContainer().set(classKey, PersistentDataType.STRING, eldenClass.key());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createClassPreviewItem(EldenClass eldenClass, EldenClass current) {
        boolean isCurrent = current != null && current == eldenClass;

        ItemStack item = new ItemStack(iconFor(eldenClass));
        ItemMeta meta = item.getItemMeta();

        meta.displayName(plain("✦ " + eldenClass.displayName() + " ✦",
                isCurrent ? NamedTextColor.AQUA : NamedTextColor.GOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(plain("❖ " + originDescription(eldenClass), NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(plain("✦ Level: " + eldenClass.level(), NamedTextColor.YELLOW));
        lore.add(plain("❤ VIG " + eldenClass.vig() + "   ✦ MND " + eldenClass.mnd() + "   ❈ END " + eldenClass.end(), NamedTextColor.DARK_GRAY));
        lore.add(plain("⚔ STR " + eldenClass.str() + "   ➹ DEX " + eldenClass.dex() + "   ✧ INT " + eldenClass.intl(), NamedTextColor.DARK_GRAY));
        lore.add(plain("✞ FTH " + eldenClass.fth() + "   ☾ ARC " + eldenClass.arc(), NamedTextColor.DARK_GRAY));
        lore.add(Component.empty());
        lore.add(plain("➤ Will you bind yourself to this origin?", NamedTextColor.YELLOW));

        if (isCurrent) {
            lore.add(plain("✔ This path is already yours.", NamedTextColor.AQUA));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createActionItem(Material material, String label, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(plain(label, NamedTextColor.YELLOW));
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);

        item.setItemMeta(meta);
        return item;
    }

    private Material iconFor(EldenClass eldenClass) {
        return switch (eldenClass) {
            case ASTROLOGER -> Material.ENCHANTING_TABLE;
            case BANDIT -> Material.IRON_SWORD;
            case CONFESSOR -> Material.WRITTEN_BOOK;
            case HEAVY_KNIGHT -> Material.NETHERITE_CHESTPLATE;
            case HERO -> Material.DIAMOND_AXE;
            case IDUS_KNIGHT -> Material.SHIELD;
            case PRISONER -> Material.IRON_BARS;
            case PROPHET -> Material.BLAZE_ROD;
            case SAMURAI -> Material.BOW;
            case VAGABOND -> Material.IRON_HELMET;
            case WARRIOR -> Material.IRON_AXE;
            case WRETCH -> Material.WOODEN_HOE;
        };
    }

    private String originDescription(EldenClass eldenClass) {
        return switch (eldenClass) {
            case ASTROLOGER -> "A scholar of the stars, heir to glintstone wisdom.";
            case BANDIT -> "A ruthless wanderer who strikes from shadow and silence.";
            case CONFESSOR -> "A veiled servant of faith, deadly with blade and prayer.";
            case HEAVY_KNIGHT -> "A towering warrior clad in iron, built to endure.";
            case HERO -> "A fierce champion of raw strength and savage resolve.";
            case IDUS_KNIGHT -> "A sworn knight of disciplined steel and noble purpose.";
            case PRISONER -> "A disgraced soul, once bound in iron, gifted in hidden arts.";
            case PROPHET -> "A blind believer who walks by faith and sacred flame.";
            case SAMURAI -> "A distant warrior, swift of blade and steady of aim.";
            case VAGABOND -> "A hardened exile, balanced in defense and steel.";
            case WARRIOR -> "A nimble fighter who triumphs through speed and precision.";
            case WRETCH -> "A soul with nothing, cast naked into a cruel world.";
        };
    }

    private Component plain(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(plain(" ", NamedTextColor.BLACK));
        filler.setItemMeta(meta);

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private record ClassMenuHolder() implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private record ConfirmMenuHolder(String classKey) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}