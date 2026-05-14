package de.skyforce.main.elden.level;

public final class DexterityScaling {

    private static final int MIN_DEXTERITY = 1;
    private static final int MAX_DEXTERITY = 99;

    private static final double AP_MAX_SCORE = cumulativeApScore(MAX_DEXTERITY);
    private static final double CAST_MAX_SCORE = cumulativeCastScore(MAX_DEXTERITY);

    private DexterityScaling() {
    }

    public static double attackScalingRatio(int dexterity) {
        int clamped = Math.max(MIN_DEXTERITY, Math.min(MAX_DEXTERITY, dexterity));
        double score = cumulativeApScore(clamped);
        return AP_MAX_SCORE <= 0.0D ? 0.0D : score / AP_MAX_SCORE;
    }

    public static double castSpeedRatio(int dexterity) {
        int clamped = Math.max(MIN_DEXTERITY, Math.min(MAX_DEXTERITY, dexterity));
        double score = cumulativeCastScore(clamped);
        return CAST_MAX_SCORE <= 0.0D ? 0.0D : score / CAST_MAX_SCORE;
    }

    private static double cumulativeApScore(int dexterity) {
        if (dexterity <= MIN_DEXTERITY) {
            return 0.0D;
        }

        double total = 0.0D;
        for (int value = 2; value <= dexterity; value++) {
            total += apGainAt(value);
        }
        return total;
    }

    private static double cumulativeCastScore(int dexterity) {
        if (dexterity <= MIN_DEXTERITY) {
            return 0.0D;
        }

        double total = 0.0D;
        for (int value = 2; value <= dexterity; value++) {
            total += castGainAt(value);
        }
        return total;
    }

    private static double apGainAt(int dexterityValue) {
        if (dexterityValue <= 16) {
            return 1.00D;
        }
        if (dexterityValue <= 18) {
            return 0.60D;
        }
        if (dexterityValue <= 20) {
            return 0.50D;
        }
        if (dexterityValue <= 50) {
            return 0.75D;
        }
        if (dexterityValue <= 60) {
            return 0.40D;
        }
        if (dexterityValue <= 80) {
            return 0.60D;
        }
        return 0.20D;
    }

    private static double castGainAt(int dexterityValue) {
        if (dexterityValue <= 30) {
            return 1.0D;
        }
        if (dexterityValue <= 45) {
            return 0.5D;
        }
        return 0.0D;
    }
}

