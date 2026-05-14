package de.skyforce.main.elden.weapon.model;

public enum WeaponType {

    STRAIGHT_SWORD("Straight Sword"),
    THRUSTING_SWORD("Thrusting Sword"),
    GREATSWORD("Greatsword"),
    KATANA("Katana"),
    DAGGER("Dagger"),
    AXE("Axe"),
    GREATAXE("Greataxe"),
    SPEAR("Spear"),
    HALBERD("Halberd"),
    TWINBLADE("Twinblade"),
    STAFF("Staff"),
    SHIELD("Shield"),
    BOW("Bow"),
    CROSSBOW("Crossbow");

    private final String displayName;

    WeaponType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
