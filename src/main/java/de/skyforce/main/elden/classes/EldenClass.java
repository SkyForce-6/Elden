package de.skyforce.main.elden.classes;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum EldenClass {

    ASTROLOGER("astrologer", "Astrologer", 6, 9, 15, 9, 8, 12, 16, 7, 9),
    BANDIT("bandit", "Bandit", 5, 10, 11, 10, 9, 13, 9, 8, 14),
    CONFESSOR("confessor", "Confessor", 10, 10, 13, 10, 12, 12, 9, 14, 9),
    HEAVY_KNIGHT("heavy_knight", "Heavy Knight", 10, 14, 8, 17, 15, 11, 7, 8, 9),
    HERO("hero", "Hero", 7, 14, 9, 12, 16, 9, 7, 8, 11),
    IDUS_KNIGHT("idus_knight", "Idus Knight", 7, 10, 12, 11, 13, 15, 8, 11, 6),
    PRISONER("prisoner", "Prisoner", 9, 11, 12, 11, 11, 14, 14, 6, 9),
    PROPHET("prophet", "Prophet", 7, 10, 14, 8, 11, 10, 7, 16, 10),
    SAMURAI("samurai", "Samurai", 9, 12, 11, 13, 12, 15, 9, 8, 8),
    VAGABOND("vagabond", "Vagabond", 9, 15, 10, 11, 14, 13, 9, 9, 7),
    WARRIOR("warrior", "Warrior", 8, 11, 12, 11, 10, 16, 10, 8, 9),
    WRETCH("wretch", "Wretch", 1, 10, 10, 10, 10, 10, 10, 10, 10);

    private final String key;
    private final String displayName;
    private final int level;
    private final int vig;
    private final int mnd;
    private final int end;
    private final int str;
    private final int dex;
    private final int intl;
    private final int fth;
    private final int arc;

    EldenClass(String key, String displayName, int level, int vig, int mnd, int end, int str, int dex, int intl, int fth, int arc) {
        this.key = key;
        this.displayName = displayName;
        this.level = level;
        this.vig = vig;
        this.mnd = mnd;
        this.end = end;
        this.str = str;
        this.dex = dex;
        this.intl = intl;
        this.fth = fth;
        this.arc = arc;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public int level() {
        return level;
    }

    public int vig() {
        return vig;
    }

    public int mnd() {
        return mnd;
    }

    public int end() {
        return end;
    }

    public int str() {
        return str;
    }

    public int dex() {
        return dex;
    }

    public int intl() {
        return intl;
    }

    public int fth() {
        return fth;
    }

    public int arc() {
        return arc;
    }

    public int totalStats() {
        return vig + mnd + end + str + dex + intl + fth + arc;
    }

    public static Optional<EldenClass> byKey(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        String normalized = input.toLowerCase(Locale.ROOT).trim().replace(' ', '_').replace('-', '_');
        return Arrays.stream(values())
                .filter(value -> value.key.equals(normalized))
                .findFirst();
    }
}

