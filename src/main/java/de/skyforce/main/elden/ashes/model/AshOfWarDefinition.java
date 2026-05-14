package de.skyforce.main.elden.ashes.model;

import de.skyforce.main.elden.weapon.model.WeaponType;
import java.util.Set;

public record AshOfWarDefinition(
        String id,
        String displayName,
        String description,
        String weaponType,
        String affinity,
        String location,
        double fpCost,
        long cooldownTicks,
        Set<WeaponType> compatibleWeaponTypes
) {

    public AshOfWarDefinition {
        compatibleWeaponTypes = Set.copyOf(compatibleWeaponTypes);
    }

    public static AshOfWarDefinition of(String id, String displayName, String description,
                                        String weaponType, String affinity, String location,
                                        double fpCost, long cooldownTicks,
                                        Set<WeaponType> compatibleWeaponTypes) {
        return new AshOfWarDefinition(
            id,
            displayName,
            description,
            weaponType,
            affinity,
            location,
            fpCost,
            cooldownTicks,
            compatibleWeaponTypes
        );
    }
}
