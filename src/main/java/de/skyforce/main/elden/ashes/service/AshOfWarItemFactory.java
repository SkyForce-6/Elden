package de.skyforce.main.elden.ashes.service;

import de.skyforce.main.elden.ashes.model.AshOfWarDefinition;
import de.skyforce.main.elden.item.CustomModelDataRegistry;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class AshOfWarItemFactory {

    private final NamespacedKey ashIdKey;
    private final NamespacedKey ashAffinityKey;
    private final CustomModelDataRegistry customModelDataRegistry;

    public AshOfWarItemFactory(JavaPlugin plugin, CustomModelDataRegistry customModelDataRegistry) {
        this.ashIdKey = new NamespacedKey(plugin, "ash-id");
        this.ashAffinityKey = new NamespacedKey(plugin, "ash-affinity");
        this.customModelDataRegistry = customModelDataRegistry;
    }

    public ItemStack createAshOfWarItem(AshOfWarDefinition ash) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(ash.displayName(), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("✦ Ash of War", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(" "));
        lore.add(Component.text("Affinity: " + ash.affinity(), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Weapon Type: " + ash.weaponType(), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(" "));

        // Wrap description
        String[] words = ash.description().split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if ((line.length() + word.length() + 1) > 40) {
                lore.add(Component.text(line.toString(), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                line = new StringBuilder();
            }
            if (line.length() > 0) {
                line.append(" ");
            }
            line.append(word);
        }
        if (line.length() > 0) {
            lore.add(Component.text(line.toString(), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.text(" "));
        lore.add(Component.text("Location: " + ash.location(), NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);

        // Store data
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ashIdKey, PersistentDataType.STRING, ash.id());
        pdc.set(ashAffinityKey, PersistentDataType.STRING, ash.affinity());
        Integer customModelData = customModelDataRegistry.ash(ash.id());
        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }

        item.setItemMeta(meta);
        return item;
    }

    public String getAshId(ItemStack item) {
        if (!item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .get(ashIdKey, PersistentDataType.STRING);
    }

    public String getAshAffinity(ItemStack item) {
        if (!item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .get(ashAffinityKey, PersistentDataType.STRING);
    }

    public boolean isAshOfWar(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .has(ashIdKey, PersistentDataType.STRING);
    }
}

