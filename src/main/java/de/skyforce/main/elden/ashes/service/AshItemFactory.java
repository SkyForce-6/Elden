package de.skyforce.main.elden.ashes.service;

import de.skyforce.main.elden.ashes.model.AshOfWar;
import de.skyforce.main.elden.item.CustomModelDataRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class AshItemFactory {

    private final NamespacedKey ashIdKey;
    private final CustomModelDataRegistry customModelDataRegistry;

    public AshItemFactory(JavaPlugin plugin, CustomModelDataRegistry customModelDataRegistry) {
        this.ashIdKey = new NamespacedKey(plugin, "ash-of-war-id");
        this.customModelDataRegistry = customModelDataRegistry;
    }

    public ItemStack createAshItem(AshOfWar ash) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Ash of War: " + ash.displayName(), NamedTextColor.GOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Skill: " + ash.skillType().name(), NamedTextColor.GRAY));
        lore.add(Component.text("Affinity: " + ash.affinity().name(), NamedTextColor.DARK_GRAY));
        lore.add(Component.text("Weapon Type: " + ash.targetCategory().name(), NamedTextColor.DARK_GRAY));

        meta.lore(lore);
        meta.getPersistentDataContainer().set(ashIdKey, PersistentDataType.STRING, ash.id());
        Integer customModelData = customModelDataRegistry.ash(ash.id());
        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }

        item.setItemMeta(meta);
        return item;
    }

    public String getAshId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(ashIdKey, PersistentDataType.STRING);
    }
}
