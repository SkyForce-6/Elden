package de.skyforce.main.elden.ashes.service;

import de.skyforce.main.elden.ashes.model.WeaponCategory;
import org.bukkit.inventory.ItemStack;

public final class WeaponCategoryResolver {

    public WeaponCategory resolve(ItemStack item) {
        if (item == null) {
            return null;
        }

        return switch (item.getType()) {
            case IRON_SWORD, DIAMOND_SWORD, NETHERITE_SWORD -> WeaponCategory.SWORD;
            case BOW -> WeaponCategory.KATANA; // Temporary example; adjust properly later.
            case SHIELD -> WeaponCategory.SHIELD;
            default -> null;
        };
    }
}

