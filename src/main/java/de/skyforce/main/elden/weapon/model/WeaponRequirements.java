package de.skyforce.main.elden.weapon.model;

public record WeaponRequirements(
        int strength,
        int dexterity,
        int intelligence,
        int faith,
        int arcane
) {
    public WeaponRequirements {
        if (strength < 0 || dexterity < 0 || intelligence < 0 || faith < 0 || arcane < 0) {
            throw new IllegalArgumentException("requirements cannot be negative");
        }
    }

    public static WeaponRequirements of(
            int strength,
            int dexterity,
            int intelligence,
            int faith,
            int arcane
    ) {
        return new WeaponRequirements(strength, dexterity, intelligence, faith, arcane);
    }

    public static WeaponRequirements strDex(int strength, int dexterity) {
        return new WeaponRequirements(strength, dexterity, 0, 0, 0);
    }

    public static WeaponRequirements none() {
        return new WeaponRequirements(0, 0, 0, 0, 0);
    }
}