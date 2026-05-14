package de.skyforce.main.elden.equipment;

public enum WeightTier {
    LIGHT("Light Load"),
    MEDIUM("Medium Load"),
    HEAVY("Heavy Load"),
    OVERLOADED("Overloaded");

    private final String displayName;

    WeightTier(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
