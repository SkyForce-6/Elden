package de.skyforce.main.elden.spirit.service;

import de.skyforce.main.elden.item.CustomModelDataRegistry;
import de.skyforce.main.elden.spirit.model.SpiritAshDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpiritAshItemFactory {

    private final NamespacedKey spiritAshIdKey;
    private final CustomModelDataRegistry customModelDataRegistry;

    public SpiritAshItemFactory(JavaPlugin plugin, CustomModelDataRegistry customModelDataRegistry) {
        this.spiritAshIdKey = new NamespacedKey(plugin, "spirit-ash-id");
        this.customModelDataRegistry = customModelDataRegistry;
    }

    public ItemStack createSpiritAshItem(SpiritAshDefinition spiritAsh) {
        ItemStack item = new ItemStack(spiritAsh.icon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(spiritAsh.displayName(), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Spirit Ash", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("FP Cost: " + formatNumber(spiritAsh.fpCost()), NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Cooldown: " + spiritAsh.cooldownTicks() + "t", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Duration: " + spiritAsh.summonDurationTicks() + "t", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        for (String line : wrap(spiritAsh.description(), 36)) {
            lore.add(Component.text(line, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Component.text("Origin: " + spiritAsh.location(), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Hold in hand and right-click to summon.", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(spiritAshIdKey, PersistentDataType.STRING, spiritAsh.id());
        Integer customModelData = customModelDataRegistry.spiritAsh(spiritAsh.id());
        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }
        item.setItemMeta(meta);
        return item;
    }

    public String getSpiritAshId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(spiritAshIdKey, PersistentDataType.STRING);
    }

    public boolean isSpiritAshItem(ItemStack item) {
        return getSpiritAshId(item) != null;
    }

    private List<String> wrap(String text, int maxLength) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (!line.isEmpty() && line.length() + word.length() + 1 > maxLength) {
                lines.add(line.toString());
                line = new StringBuilder();
            }
            if (!line.isEmpty()) {
                line.append(' ');
            }
            line.append(word);
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private String formatNumber(double value) {
        if (Math.floor(value) == value) {
            return Integer.toString((int) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
