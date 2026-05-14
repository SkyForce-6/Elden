package de.skyforce.main.elden.weapon.service;

import de.skyforce.main.elden.item.CustomModelDataRegistry;
import de.skyforce.main.elden.weapon.model.WeaponAttackStats;
import de.skyforce.main.elden.weapon.model.WeaponDefinition;
import de.skyforce.main.elden.weapon.model.WeaponGuardStats;
import de.skyforce.main.elden.weapon.model.WeaponRequirements;
import de.skyforce.main.elden.weapon.model.WeaponScaling;
import de.skyforce.main.elden.weapon.model.WeaponScalingGrade;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class WeaponItemFactory {

    private final NamespacedKey weaponIdKey;
    private final NamespacedKey smithingLevelKey;
    private final CustomModelDataRegistry customModelDataRegistry;

    public WeaponItemFactory(JavaPlugin plugin, CustomModelDataRegistry customModelDataRegistry) {
        this.weaponIdKey = new NamespacedKey(plugin, "weapon-id");
        this.smithingLevelKey = new NamespacedKey(plugin, "weapon-smithing-level");
        this.customModelDataRegistry = customModelDataRegistry;
    }

    public ItemStack createWeaponItem(WeaponDefinition weapon) {
        return createWeaponItem(weapon, 0);
    }

    public ItemStack createWeaponItem(WeaponDefinition weapon, int smithingLevel) {
        ItemStack item = new ItemStack(weapon.material());
        updateWeaponItem(item, weapon, smithingLevel);
        return item;
    }

    public void updateWeaponItem(ItemStack item, WeaponDefinition weapon, int smithingLevel) {
        ItemMeta meta = item.getItemMeta();
        applyWeaponMeta(meta, weapon, smithingLevel);
        item.setItemMeta(meta);
    }

    public String getWeaponId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(weaponIdKey, PersistentDataType.STRING);
    }

    public int getSmithingLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        Integer level = item.getItemMeta().getPersistentDataContainer().get(smithingLevelKey, PersistentDataType.INTEGER);
        return level == null ? 0 : Math.max(level, 0);
    }

    private void applyWeaponMeta(ItemMeta meta, WeaponDefinition weapon, int smithingLevel) {
        meta.displayName(text(formatDisplayName(weapon, smithingLevel), NamedTextColor.WHITE));

        List<Component> lore = new ArrayList<>();

        lore.add(text(weapon.weaponType().displayName() + "  |  " + weapon.attackTypeLabel(), NamedTextColor.DARK_GRAY));
        lore.add(text("Smithing Level: +" + smithingLevel, NamedTextColor.GREEN));
        lore.add(Component.empty());

        WeaponAttackStats attack = weapon.attackStats();
        lore.add(text("Attack Power", NamedTextColor.GOLD));
        appendAttackLine(lore, "Physical", attack.physical());
        appendAttackLine(lore, "Magic", attack.magic());
        appendAttackLine(lore, "Fire", attack.fire());
        appendAttackLine(lore, "Lightning", attack.lightning());
        appendAttackLine(lore, "Holy", attack.holy());
        lore.add(text("  Critical: " + attack.critical(), NamedTextColor.GRAY));
        lore.add(Component.empty());

        WeaponGuardStats guard = weapon.guardStats();
        lore.add(text("Guard", NamedTextColor.AQUA));
        lore.add(text("  Physical:  " + guard.physical(), NamedTextColor.GRAY));
        lore.add(text("  Magic:     " + guard.magic(), NamedTextColor.GRAY));
        lore.add(text("  Fire:      " + guard.fire(), NamedTextColor.GRAY));
        lore.add(text("  Lightning: " + guard.lightning(), NamedTextColor.GRAY));
        lore.add(text("  Holy:      " + guard.holy(), NamedTextColor.GRAY));
        lore.add(text("  Boost:     " + guard.boost(), NamedTextColor.GRAY));
        lore.add(Component.empty());

        WeaponScaling scaling = weapon.scaling();
        lore.add(text("Scaling", NamedTextColor.YELLOW));
        appendScalingLine(lore, "Strength", scaling.strength());
        appendScalingLine(lore, "Dexterity", scaling.dexterity());
        appendScalingLine(lore, "Intelligence", scaling.intelligence());
        appendScalingLine(lore, "Faith", scaling.faith());
        appendScalingLine(lore, "Arcane", scaling.arcane());
        lore.add(Component.empty());

        WeaponRequirements requirements = weapon.requirements();
        lore.add(text("Requirements", NamedTextColor.LIGHT_PURPLE));
        appendRequireLine(lore, "Strength", requirements.strength());
        appendRequireLine(lore, "Dexterity", requirements.dexterity());
        appendRequireLine(lore, "Intelligence", requirements.intelligence());
        appendRequireLine(lore, "Faith", requirements.faith());
        appendRequireLine(lore, "Arcane", requirements.arcane());
        lore.add(Component.empty());

        lore.add(text("Skill: " + weapon.skillName(), NamedTextColor.DARK_AQUA));
        lore.add(text("FP Cost: " + weapon.skillFpCost(), NamedTextColor.BLUE));
        if (!weapon.passiveEffect().equals("-")) {
            lore.add(text("Passive: " + weapon.passiveEffect(), NamedTextColor.RED));
        }
        lore.add(text("Weight: " + weapon.weight(), NamedTextColor.DARK_GRAY));

        meta.lore(lore);
        meta.getPersistentDataContainer().set(weaponIdKey, PersistentDataType.STRING, weapon.id());
        meta.getPersistentDataContainer().set(smithingLevelKey, PersistentDataType.INTEGER, Math.max(smithingLevel, 0));
        Integer customModelData = customModelDataRegistry.weapon(weapon.id());
        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }
    }

    private String formatDisplayName(WeaponDefinition weapon, int smithingLevel) {
        return smithingLevel > 0 ? weapon.displayName() + " +" + smithingLevel : weapon.displayName();
    }

    private void appendAttackLine(List<Component> lore, String stat, int value) {
        if (value <= 0) {
            return;
        }
        lore.add(text("  " + stat + ": " + value, NamedTextColor.GRAY));
    }

    private void appendScalingLine(List<Component> lore, String stat, WeaponScalingGrade grade) {
        if (grade == WeaponScalingGrade.NONE) {
            return;
        }
        lore.add(text("  " + stat + ": " + grade.name(), NamedTextColor.GRAY));
    }

    private void appendRequireLine(List<Component> lore, String stat, int value) {
        if (value <= 0) {
            return;
        }
        lore.add(text("  " + stat + ": " + value, NamedTextColor.GRAY));
    }

    private Component text(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
