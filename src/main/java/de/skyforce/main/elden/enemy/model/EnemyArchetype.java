package de.skyforce.main.elden.enemy.model;

public enum EnemyArchetype {
    MELEE("Melee"),
    SHIELD("Shield"),
    FAST("Fast"),
    RANGED("Ranged"),
    ELITE("Elite");

    private final String displayName;

    EnemyArchetype(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
