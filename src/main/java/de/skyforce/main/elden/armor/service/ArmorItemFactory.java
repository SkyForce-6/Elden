package de.skyforce.main.elden.armor.service;

import de.skyforce.main.elden.armor.model.ArmorDefinition;
import de.skyforce.main.elden.item.CustomModelDataRegistry;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class ArmorItemFactory {

    private final NamespacedKey armorIdKey;
    private final CustomModelDataRegistry customModelDataRegistry;

    public ArmorItemFactory(JavaPlugin plugin, CustomModelDataRegistry customModelDataRegistry) {
        this.armorIdKey = new NamespacedKey(plugin, "armor-id");
        this.customModelDataRegistry = customModelDataRegistry;
    }

    public ItemStack createArmorItem(ArmorDefinition armor) {
        ItemStack item = new ItemStack(armor.material());
        ItemMeta meta = item.getItemMeta();

        meta.displayName(text(armor.displayName(), NamedTextColor.WHITE));

        List<Component> lore = new ArrayList<>();
        lore.add(text(armor.slot().displayName(), NamedTextColor.DARK_GRAY));
        lore.add(Component.empty());
        lore.add(text("Defense", NamedTextColor.GOLD));
        lore.add(text("  Physical: " + armor.physicalDefense(), NamedTextColor.GRAY));
        lore.add(text("  Magic: " + armor.magicDefense(), NamedTextColor.GRAY));
        lore.add(text("  Fire: " + armor.fireDefense(), NamedTextColor.GRAY));
        lore.add(text("  Lightning: " + armor.lightningDefense(), NamedTextColor.GRAY));
        lore.add(text("  Holy: " + armor.holyDefense(), NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(text("Poise: " + armor.poise(), NamedTextColor.DARK_AQUA));
        lore.add(text("Weight: " + armor.weight(), NamedTextColor.DARK_GRAY));

        meta.lore(lore);
        meta.getPersistentDataContainer().set(armorIdKey, PersistentDataType.STRING, armor.id());
        Integer customModelData = customModelDataRegistry.armor(armor.id());
        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }
        item.setItemMeta(meta);
        return item;
    }

    public String getArmorId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(armorIdKey, PersistentDataType.STRING);
    }

    private Component text(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
