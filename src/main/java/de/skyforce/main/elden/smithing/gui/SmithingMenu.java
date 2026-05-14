package de.skyforce.main.elden.smithing.gui;

import de.skyforce.main.elden.runes.RuneManager;
import de.skyforce.main.elden.smithing.model.SmithingRequirement;
import de.skyforce.main.elden.smithing.model.SmithingTrack;
import de.skyforce.main.elden.smithing.service.SmithingService;
import de.skyforce.main.elden.smithing.service.SmithingStoneService;
import de.skyforce.main.elden.weapon.model.WeaponDefinition;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class SmithingMenu {

    private static final int MENU_SIZE = 27;

    private final SmithingService smithingService;
    private final SmithingStoneService smithingStoneService;
    private final WeaponItemFactory weaponItemFactory;
    private final RuneManager runeManager;

    public SmithingMenu(JavaPlugin plugin,
                        SmithingService smithingService,
                        SmithingStoneService smithingStoneService,
                        WeaponItemFactory weaponItemFactory,
                        RuneManager runeManager) {
        this.smithingService = smithingService;
        this.smithingStoneService = smithingStoneService;
        this.weaponItemFactory = weaponItemFactory;
        this.runeManager = runeManager;
    }

    public void open(Player player) {
        Optional<WeaponDefinition> weaponOptional = smithingService.getEquippedWeapon(player);
        if (weaponOptional.isEmpty()) {
            player.sendMessage(Component.text("Hold a custom weapon in your main hand to smith it.", NamedTextColor.RED));
            return;
        }

        WeaponDefinition weapon = weaponOptional.get();
        Inventory inventory = Bukkit.createInventory(new Holder(), MENU_SIZE, Component.text("Smithing", NamedTextColor.GOLD));
        fillBackground(inventory);
        inventory.setItem(10, createWeaponPreview(player, weapon));
        inventory.setItem(13, createRequirementPreview(player, weapon));
        inventory.setItem(16, createStatusPreview(player, weapon));
        inventory.setItem(21, createActionItem(Material.LIME_CONCRETE, "Upgrade", true));
        inventory.setItem(23, createActionItem(Material.RED_CONCRETE, "Close", false));
        player.openInventory(inventory);
    }

    public boolean isSmithingMenu(Inventory inventory) {
        return inventory.getHolder() instanceof Holder;
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof Holder)) {
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

        SmithingService.SmithingUpgradeResult result = smithingService.upgradeMainHand(player);
        if (!result.success()) {
            player.sendActionBar(Component.text(result.message(), NamedTextColor.RED));
            open(player);
            return;
        }

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.1f);
        player.sendActionBar(Component.text(
                result.track().displayName() + " weapon upgraded to +" + result.newLevel(),
                NamedTextColor.GREEN));
        open(player);
    }

    private ItemStack createWeaponPreview(Player player, WeaponDefinition weapon) {
        ItemStack item = player.getInventory().getItemInMainHand().clone();
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        int level = weaponItemFactory.getSmithingLevel(player.getInventory().getItemInMainHand());
        SmithingTrack track = smithingService.trackFor(weapon);
        lore.add(Component.empty());
        lore.add(Component.text("Track: " + track.displayName(), NamedTextColor.YELLOW));
        lore.add(Component.text("Current Level: +" + level + " / +" + track.maxLevel(), NamedTextColor.GREEN));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRequirementPreview(Player player, WeaponDefinition weapon) {
        SmithingTrack track = smithingService.trackFor(weapon);
        int level = weaponItemFactory.getSmithingLevel(player.getInventory().getItemInMainHand());
        SmithingRequirement requirement = smithingService.nextRequirement(weapon, level);

        ItemStack item = new ItemStack(track == SmithingTrack.STANDARD ? Material.AMETHYST_SHARD : Material.PRISMARINE_CRYSTALS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Upgrade Cost", NamedTextColor.GOLD));

        List<Component> lore = new ArrayList<>();
        if (requirement == null) {
            lore.add(Component.text("This weapon is already fully upgraded.", NamedTextColor.GREEN));
        } else {
            int available = smithingStoneService.countMatching(player, requirement.track(), requirement.stoneTier());
            int runes = runeManager.getRunes(player);
            lore.add(Component.text("Next Level: +" + requirement.targetLevel(), NamedTextColor.WHITE));
            lore.add(Component.text("Runes: " + requirement.runeCost() + " (" + runes + ")", runes >= requirement.runeCost()
                    ? NamedTextColor.GREEN : NamedTextColor.RED));
            lore.add(Component.text(
                    smithingStoneService.displayName(requirement.track(), requirement.stoneTier())
                            + ": " + requirement.stoneAmount() + " (" + available + ")",
                    available >= requirement.stoneAmount() ? NamedTextColor.GREEN : NamedTextColor.RED));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStatusPreview(Player player, WeaponDefinition weapon) {
        SmithingTrack track = smithingService.trackFor(weapon);
        int level = weaponItemFactory.getSmithingLevel(player.getInventory().getItemInMainHand());
        SmithingRequirement requirement = smithingService.nextRequirement(weapon, level);
        boolean ready = requirement != null
                && runeManager.getRunes(player) >= requirement.runeCost()
                && smithingStoneService.countMatching(player, requirement.track(), requirement.stoneTier()) >= requirement.stoneAmount();

        ItemStack item = new ItemStack(requirement == null ? Material.NETHER_STAR : ready ? Material.LIME_DYE : Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Smithing Status", requirement == null ? NamedTextColor.GOLD
                : ready ? NamedTextColor.GREEN : NamedTextColor.RED));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Track: " + track.displayName(), NamedTextColor.YELLOW));
        lore.add(Component.text("Damage Bonus: x" + String.format(java.util.Locale.ROOT, "%.2f",
                smithingService.damageMultiplier(weapon, level)), NamedTextColor.AQUA));
        if (requirement == null) {
            lore.add(Component.text("Max level reached.", NamedTextColor.GREEN));
        } else {
            lore.add(Component.text(ready ? "Ready to upgrade." : "Missing runes or stones.",
                    ready ? NamedTextColor.GREEN : NamedTextColor.RED));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createActionItem(Material material, String name, boolean confirm) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, confirm ? NamedTextColor.GREEN : NamedTextColor.RED));
        meta.lore(List.of(Component.text(
                confirm ? "Consume the displayed cost and reinforce the held weapon." : "Close the smithing menu.",
                NamedTextColor.GRAY)));
        item.setItemMeta(meta);
        return item;
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.text(" "));
        filler.setItemMeta(meta);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private record Holder() implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
