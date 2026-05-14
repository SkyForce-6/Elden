package de.skyforce.main.elden.smithing.model;

public enum SmithingTrack {
    STANDARD("Standard", 25, 0.04),
    SOMBER("Somber", 10, 0.10);

    private final String displayName;
    private final int maxLevel;
    private final double damageBonusPerLevel;

    SmithingTrack(String displayName, int maxLevel, double damageBonusPerLevel) {
        this.displayName = displayName;
        this.maxLevel = maxLevel;
        this.damageBonusPerLevel = damageBonusPerLevel;
    }

    public String displayName() {
        return displayName;
    }

    public int maxLevel() {
        return maxLevel;
    }

    public double damageBonusPerLevel() {
        return damageBonusPerLevel;
    }
}
