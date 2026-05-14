package de.skyforce.main.elden.level;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum AttributeType {

    VIGOR("vig", "Vigor"),
    MIND("mnd", "Mind"),
    ENDURANCE("end", "Endurance"),
    STRENGTH("str", "Strength"),
    DEXTERITY("dex", "Dexterity"),
    INTELLIGENCE("int", "Intelligence"),
    FAITH("fth", "Faith"),
    ARCANE("arc", "Arcane");

    private final String key;
    private final String displayName;

    AttributeType(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<AttributeType> fromInput(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        String normalized = input.toLowerCase(Locale.ROOT).trim();
        return Arrays.stream(values())
                .filter(value -> value.key.equals(normalized)
                        || value.name().toLowerCase(Locale.ROOT).equals(normalized)
                        || value.displayName.toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst();
    }
}

