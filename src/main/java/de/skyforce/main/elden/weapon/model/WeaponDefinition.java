package de.skyforce.main.elden.weapon.model;

import de.skyforce.main.elden.smithing.model.SmithingTrack;
import org.bukkit.Material;

import java.util.Objects;

/**
 * Full weapon definition mirroring Elden Ring weapon stats.
 */
public record WeaponDefinition(
        String id,
        String displayName,
        Material material,
        WeaponType weaponType,
        String attackTypeLabel,
        String skillName,
        String skillFpCost,
        String passiveEffect,
        WeaponAttackStats attackStats,
        WeaponGuardStats guardStats,
        WeaponScaling scaling,
        WeaponRequirements requirements,
        SmithingTrack smithingTrack,
        double weight
) {
    public WeaponDefinition(String id,
                            String displayName,
                            Material material,
                            WeaponType weaponType,
                            String attackTypeLabel,
                            String skillName,
                            String skillFpCost,
                            String passiveEffect,
                            WeaponAttackStats attackStats,
                            WeaponGuardStats guardStats,
                            WeaponScaling scaling,
                            WeaponRequirements requirements,
                            double weight) {
        this(id, displayName, material, weaponType, attackTypeLabel, skillName, skillFpCost, passiveEffect,
                attackStats, guardStats, scaling, requirements, SmithingTrack.STANDARD, weight);
    }

    public WeaponDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(weaponType, "weaponType");
        Objects.requireNonNull(attackTypeLabel, "attackTypeLabel");
        Objects.requireNonNull(skillName, "skillName");
        Objects.requireNonNull(skillFpCost, "skillFpCost");
        Objects.requireNonNull(passiveEffect, "passiveEffect");
        Objects.requireNonNull(attackStats, "attackStats");
        Objects.requireNonNull(guardStats, "guardStats");
        Objects.requireNonNull(scaling, "scaling");
        Objects.requireNonNull(requirements, "requirements");
        Objects.requireNonNull(smithingTrack, "smithingTrack");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName cannot be blank");
        }
        if (attackTypeLabel.isBlank()) {
            throw new IllegalArgumentException("attackTypeLabel cannot be blank");
        }
        if (skillName.isBlank()) {
            throw new IllegalArgumentException("skillName cannot be blank");
        }
        if (skillFpCost.isBlank()) {
            throw new IllegalArgumentException("skillFpCost cannot be blank");
        }
        if (passiveEffect.isBlank()) {
            throw new IllegalArgumentException("passiveEffect cannot be blank");
        }
        if (weight < 0) {
            throw new IllegalArgumentException("weight cannot be negative");
        }
    }
}
