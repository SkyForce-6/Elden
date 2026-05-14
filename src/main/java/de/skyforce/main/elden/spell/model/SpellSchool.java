package de.skyforce.main.elden.spell.model;

public enum SpellSchool {
    SORCERY("Sorcery"),
    INCANTATION("Incantation");

    private final String displayName;

    SpellSchool(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
