package de.skyforce.main.elden.ashes.service;

import de.skyforce.main.elden.ashes.model.AffinityType;
import de.skyforce.main.elden.ashes.model.AshOfWar;
import de.skyforce.main.elden.ashes.model.AshSkillType;
import de.skyforce.main.elden.ashes.model.WeaponAshData;
import de.skyforce.main.elden.ashes.model.WeaponCategory;
import de.skyforce.main.elden.ashes.registry.AshRegistry;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class AshBindingService {

    private final NamespacedKey boundAshIdKey;
    private final NamespacedKey boundSkillKey;
    private final NamespacedKey boundAffinityKey;
    private final AshRegistry ashRegistry;

    public AshBindingService(JavaPlugin plugin, AshRegistry ashRegistry) {
        this.ashRegistry = ashRegistry;
        this.boundAshIdKey = new NamespacedKey(plugin, "weapon-ash-id");
        this.boundSkillKey = new NamespacedKey(plugin, "weapon-ash-skill");
        this.boundAffinityKey = new NamespacedKey(plugin, "weapon-ash-affinity");
    }

    public boolean canApply(ItemStack weapon, AshOfWar ash, WeaponCategory category) {
        if (weapon == null || ash == null || category == null) {
            return false;
        }
        return ash.targetCategory() == category;
    }

    public boolean applyAsh(ItemStack weapon, AshOfWar ash) {
        if (weapon == null || ash == null || !weapon.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = weapon.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        pdc.set(boundAshIdKey, PersistentDataType.STRING, ash.id());
        pdc.set(boundSkillKey, PersistentDataType.STRING, ash.skillType().name());
        pdc.set(boundAffinityKey, PersistentDataType.STRING, ash.affinity().name());

        List<Component> lore = meta.hasLore() && meta.lore() != null
                ? new ArrayList<>(meta.lore())
                : new ArrayList<>();

        lore.removeIf(line -> {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line);
            return plain.startsWith("Ash of War:") || plain.startsWith("Affinity:");
        });

        lore.add(text("Ash of War: " + ash.displayName(), NamedTextColor.GOLD));
        lore.add(text("Affinity: " + formatEnumName(ash.affinity().name()), NamedTextColor.GRAY));
        meta.lore(lore);

        weapon.setItemMeta(meta);
        return true;
    }

    public WeaponAshData getBoundData(ItemStack weapon) {
        if (weapon == null || !weapon.hasItemMeta()) {
            return null;
        }

        PersistentDataContainer pdc = weapon.getItemMeta().getPersistentDataContainer();
        String ashId = pdc.get(boundAshIdKey, PersistentDataType.STRING);
        String skillName = pdc.get(boundSkillKey, PersistentDataType.STRING);
        String affinityName = pdc.get(boundAffinityKey, PersistentDataType.STRING);

        if (ashId == null || skillName == null || affinityName == null) {
            return null;
        }

        try {
            AshSkillType skillType = AshSkillType.valueOf(skillName);
            AffinityType affinity = AffinityType.valueOf(affinityName);
            return new WeaponAshData(ashId, skillType, affinity);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public void removeAsh(ItemStack weapon) {
        if (weapon == null || !weapon.hasItemMeta()) {
            return;
        }

        ItemMeta meta = weapon.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        pdc.remove(boundAshIdKey);
        pdc.remove(boundSkillKey);
        pdc.remove(boundAffinityKey);

        if (meta.hasLore() && meta.lore() != null) {
            List<Component> lore = new ArrayList<>(meta.lore());
            lore.removeIf(line -> {
                String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line);
                return plain.startsWith("Ash of War:") || plain.startsWith("Affinity:");
            });
            meta.lore(lore);
        }

        weapon.setItemMeta(meta);
    }

    private Component text(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private String formatEnumName(String value) {
        String lower = value.toLowerCase().replace('_', ' ');
        String[] parts = lower.split(" ");
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) {
                builder.append(parts[i].substring(1));
            }
        }

        return builder.toString();
    }
}