package de.skyforce.main.elden.ashes.service;

import de.skyforce.main.elden.ashes.model.AshOfWarDefinition;
import de.skyforce.main.elden.weapon.model.WeaponDefinition;
import de.skyforce.main.elden.weapon.registry.WeaponRegistry;
import de.skyforce.main.elden.weapon.service.WeaponItemFactory;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class AshOfWarBindingService {

    private final WeaponRegistry weaponRegistry;
    private final WeaponItemFactory weaponItemFactory;
    private final NamespacedKey boundAshIdKey;
    private final NamespacedKey boundAffinityKey;

    public AshOfWarBindingService(JavaPlugin plugin, WeaponRegistry weaponRegistry, WeaponItemFactory weaponItemFactory) {
        this.weaponRegistry = weaponRegistry;
        this.weaponItemFactory = weaponItemFactory;
        this.boundAshIdKey = new NamespacedKey(plugin, "weapon-ash-id");
        this.boundAffinityKey = new NamespacedKey(plugin, "weapon-ash-affinity");
    }

    public boolean isBindableWeapon(ItemStack item) {
        return getWeaponDefinition(item) != null;
    }

    public boolean canApply(ItemStack weaponItem, AshOfWarDefinition ash) {
        WeaponDefinition weapon = getWeaponDefinition(weaponItem);
        if (weapon == null || ash == null) {
            return false;
        }

        return ash.compatibleWeaponTypes().contains(weapon.weaponType());
    }

    public boolean applyAsh(ItemStack weaponItem, AshOfWarDefinition ash) {
        if (weaponItem == null || ash == null || !weaponItem.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = weaponItem.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(boundAshIdKey, PersistentDataType.STRING, ash.id());
        pdc.set(boundAffinityKey, PersistentDataType.STRING, ash.affinity());

        List<Component> lore = meta.hasLore() && meta.lore() != null
                ? new ArrayList<>(meta.lore())
                : new ArrayList<>();
        lore.removeIf(this::isAshLoreLine);
        lore.add(Component.empty());
        lore.add(text("Ash of War: " + ash.displayName(), NamedTextColor.GOLD));
        lore.add(text("Affinity: " + ash.affinity(), NamedTextColor.GRAY));
        meta.lore(lore);

        weaponItem.setItemMeta(meta);
        return true;
    }

    public String getBoundAshId(ItemStack weaponItem) {
        if (weaponItem == null || !weaponItem.hasItemMeta()) {
            return null;
        }
        return weaponItem.getItemMeta().getPersistentDataContainer().get(boundAshIdKey, PersistentDataType.STRING);
    }

    private WeaponDefinition getWeaponDefinition(ItemStack item) {
        String weaponId = weaponItemFactory.getWeaponId(item);
        if (weaponId == null) {
            return null;
        }
        return weaponRegistry.getById(weaponId).orElse(null);
    }

    private boolean isAshLoreLine(Component line) {
        String plain = PlainTextComponentSerializer.plainText().serialize(line);
        return plain.isBlank() || plain.startsWith("Ash of War:") || plain.startsWith("Affinity:");
    }

    private Component text(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
