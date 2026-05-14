package de.skyforce.main.elden.armor.model;

public enum ArmorSlot {
    HEAD("Helm"),
    CHEST("Chest Armor"),
    LEGS("Leg Armor"),
    FEET("Boots");

    private final String displayName;

    ArmorSlot(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
