package de.skyforce.main.elden.weapon.model;

public record WeaponGuardStats(
        int physical,
        int magic,
        int fire,
        int lightning,
        int holy,
        int boost
) {
    public WeaponGuardStats {
        if (physical < 0 || magic < 0 || fire < 0 || lightning < 0 || holy < 0 || boost < 0) {
            throw new IllegalArgumentException("guard values cannot be negative");
        }
    }
}