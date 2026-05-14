package de.skyforce.main.elden.weapon.model;

import java.util.Objects;

public record WeaponScaling(
        WeaponScalingGrade strength,
        WeaponScalingGrade dexterity,
        WeaponScalingGrade intelligence,
        WeaponScalingGrade faith,
        WeaponScalingGrade arcane
) {
    public WeaponScaling {
        Objects.requireNonNull(strength, "strength");
        Objects.requireNonNull(dexterity, "dexterity");
        Objects.requireNonNull(intelligence, "intelligence");
        Objects.requireNonNull(faith, "faith");
        Objects.requireNonNull(arcane, "arcane");
    }

    public static WeaponScaling of(
            WeaponScalingGrade strength,
            WeaponScalingGrade dexterity,
            WeaponScalingGrade intelligence,
            WeaponScalingGrade faith,
            WeaponScalingGrade arcane
    ) {
        return new WeaponScaling(strength, dexterity, intelligence, faith, arcane);
    }

    public static WeaponScaling strDex(WeaponScalingGrade strength, WeaponScalingGrade dexterity) {
        return new WeaponScaling(
                strength,
                dexterity,
                WeaponScalingGrade.NONE,
                WeaponScalingGrade.NONE,
                WeaponScalingGrade.NONE
        );
    }

    public static WeaponScaling none() {
        return new WeaponScaling(
                WeaponScalingGrade.NONE,
                WeaponScalingGrade.NONE,
                WeaponScalingGrade.NONE,
                WeaponScalingGrade.NONE,
                WeaponScalingGrade.NONE
        );
    }
}