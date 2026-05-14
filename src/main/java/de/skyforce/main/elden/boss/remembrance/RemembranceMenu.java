package de.skyforce.main.elden.boss.remembrance;

import de.skyforce.main.elden.boss.BossManager;
import java.util.ArrayList;
import java.util.List;
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

public final class RemembranceMenu {

    private static final int MENU_SIZE = 27;

    private final BossManager bossManager;

    public RemembranceMenu(BossManager bossManager) {
        this.bossManager = bossManager;
    }

    public void open(Player player) {
        BossManager.RemembrancePreview preview = bossManager.inspectHeldRemembrance(player);
        if (preview == null) {
            player.sendMessage(Component.text("Hold a remembrance in your main hand to use the station.", NamedTextColor.RED));
            return;
        }

        Inventory inventory = Bukkit.createInventory(new Holder(), MENU_SIZE, Component.text("Remembrance Exchange", NamedTextColor.LIGHT_PURPLE));
        fillBackground(inventory);
        inventory.setItem(10, createRemembrancePreview(player));
        inventory.setItem(13, createRewardOption(preview));
        inventory.setItem(16, createRuneOption(preview));
        inventory.setItem(22, createCloseButton());
        player.openInventory(inventory);
    }

    public boolean isRemembranceMenu(Inventory inventory) {
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

        switch (clicked.getType()) {
            case RED_CONCRETE -> player.closeInventory();
            case LIME_CONCRETE -> exchange(player, "reward");
            case GOLD_INGOT -> exchange(player, "runes");
            default -> {
            }
        }
    }

    private void exchange(Player player, String option) {
        BossManager.ExchangeResult result = bossManager.exchangeHeldRemembrance(player, option);
        player.sendActionBar(Component.text(
                result.message(),
                result.success() ? NamedTextColor.GREEN : NamedTextColor.RED
        ));
        if (!result.success()) {
            open(player);
            return;
        }

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, option.equals("reward") ? 1.1f : 0.9f);
        player.closeInventory();
    }

    private ItemStack createRemembrancePreview(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand().clone();
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Consumed on exchange.", NamedTextColor.RED));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRewardOption(BossManager.RemembrancePreview preview) {
        ItemStack item = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Exchange For Reward", NamedTextColor.GREEN));
        meta.lore(List.of(
                Component.text("Boss: " + preview.bossName(), NamedTextColor.YELLOW),
                Component.text("Receive: " + preview.rewardName(), NamedTextColor.AQUA),
                Component.text("Consumes the held remembrance.", NamedTextColor.GRAY)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRuneOption(BossManager.RemembrancePreview preview) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Exchange For Runes", NamedTextColor.GOLD));
        meta.lore(List.of(
                Component.text("Boss: " + preview.bossName(), NamedTextColor.YELLOW),
                Component.text("Receive: " + preview.runeValue() + " runes", NamedTextColor.GOLD),
                Component.text("Consumes the held remembrance.", NamedTextColor.GRAY)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.RED_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Close", NamedTextColor.RED));
        meta.lore(List.of(Component.text("Leave the remembrance station.", NamedTextColor.GRAY)));
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
