package de.skyforce.main.elden.talisman.service;

import de.skyforce.main.elden.item.CustomModelDataRegistry;
import de.skyforce.main.elden.talisman.model.TalismanDefinition;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class TalismanItemFactory {

    private final NamespacedKey talismanIdKey;
    private final CustomModelDataRegistry customModelDataRegistry;

    public TalismanItemFactory(JavaPlugin plugin, CustomModelDataRegistry customModelDataRegistry) {
        this.talismanIdKey = new NamespacedKey(plugin, "talisman-id");
        this.customModelDataRegistry = customModelDataRegistry;
    }

    public ItemStack createTalismanItem(TalismanDefinition talisman) {
        ItemStack item = new ItemStack(talisman.material());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(text(talisman.displayName(), NamedTextColor.GOLD));
        meta.lore(List.of(
                text("Talisman", NamedTextColor.LIGHT_PURPLE),
                Component.empty(),
                text(talisman.description(), NamedTextColor.GRAY),
                text("Bonus: " + formatBonus(talisman.value()), NamedTextColor.DARK_AQUA),
                Component.empty(),
                text("Use /talisman equip <slot> while holding this item.", NamedTextColor.DARK_GRAY)
        ));
        meta.getPersistentDataContainer().set(talismanIdKey, PersistentDataType.STRING, talisman.id());
        Integer customModelData = customModelDataRegistry.talisman(talisman.id());
        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }
        item.setItemMeta(meta);
        return item;
    }

    public String getTalismanId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(talismanIdKey, PersistentDataType.STRING);
    }

    private String formatBonus(double value) {
        return "+" + Math.round(value * 100.0D) + "%";
    }

    private Component text(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
