package de.skyforce.main.elden.level;

public final class StrengthScaling {

    private static final int MIN_STRENGTH = 1;
    private static final int MAX_STRENGTH = 99;

    private static final double MAX_SCORE = cumulativeScore(MAX_STRENGTH);

    private StrengthScaling() {
    }

    public static int effectiveStrength(int baseStrength, boolean twoHanded, boolean criticalHit) {
        int clamped = Math.max(MIN_STRENGTH, Math.min(MAX_STRENGTH, baseStrength));
        if (!twoHanded || criticalHit) {
            return clamped;
        }

        int boosted = (int) Math.ceil(clamped * 1.5D);
        return Math.min(MAX_STRENGTH, boosted);
    }

    public static double scalingRatio(int strength) {
        int clamped = Math.max(MIN_STRENGTH, Math.min(MAX_STRENGTH, strength));
        double score = cumulativeScore(clamped);
        return MAX_SCORE <= 0.0D ? 0.0D : score / MAX_SCORE;
    }

    private static double cumulativeScore(int strength) {
        if (strength <= MIN_STRENGTH) {
            return 0.0D;
        }

        double total = 0.0D;
        for (int value = 2; value <= strength; value++) {
            total += gainAt(value);
        }
        return total;
    }

    private static double gainAt(int strengthValue) {
        if (strengthValue <= 16) {
            return 1.00D;
        }
        if (strengthValue <= 18) {
            return 0.60D;
        }
        if (strengthValue <= 20) {
            return 0.50D;
        }
        if (strengthValue <= 50) {
            return 0.75D;
        }
        if (strengthValue <= 60) {
            return 0.40D;
        }
        if (strengthValue <= 80) {
            return 0.60D;
        }
        return 0.20D;
    }
}

