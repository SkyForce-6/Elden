package de.skyforce.main.elden.weapon.model;

public record WeaponAttackStats(
        int physical,
        int magic,
        int fire,
        int lightning,
        int holy,
        int critical
) {
    public WeaponAttackStats {
        if (physical < 0 || magic < 0 || fire < 0 || lightning < 0 || holy < 0) {
            throw new IllegalArgumentException("attack values cannot be negative");
        }
        if (critical < 0) {
            throw new IllegalArgumentException("critical cannot be negative");
        }
    }

    public int totalBaseDamage() {
        return physical + magic + fire + lightning + holy;
    }
}
