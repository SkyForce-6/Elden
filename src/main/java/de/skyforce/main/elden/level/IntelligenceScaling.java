package de.skyforce.main.elden.level;

public final class IntelligenceScaling {

    private static final int MIN_INTELLIGENCE = 1;
    private static final int MAX_INTELLIGENCE = 99;

    private static final double AP_MAX_SCORE = cumulativeApScore(MAX_INTELLIGENCE);
    private static final double CAST_MAX_SCORE = cumulativeCastScore(MAX_INTELLIGENCE);

    private IntelligenceScaling() {
    }

    public static double attackScalingRatio(int intelligence) {
        int clamped = Math.max(MIN_INTELLIGENCE, Math.min(MAX_INTELLIGENCE, intelligence));
        double score = cumulativeApScore(clamped);
        return AP_MAX_SCORE <= 0.0D ? 0.0D : score / AP_MAX_SCORE;
    }

    public static double castSpeedRatio(int intelligence) {
        int clamped = Math.max(MIN_INTELLIGENCE, Math.min(MAX_INTELLIGENCE, intelligence));
        double score = cumulativeCastScore(clamped);
        return CAST_MAX_SCORE <= 0.0D ? 0.0D : score / CAST_MAX_SCORE;
    }

    private static double cumulativeApScore(int intelligence) {
        if (intelligence <= MIN_INTELLIGENCE) {
            return 0.0D;
        }

        double total = 0.0D;
        for (int value = 2; value <= intelligence; value++) {
            total += apGainAt(value);
        }
        return total;
    }

    private static double cumulativeCastScore(int intelligence) {
        if (intelligence <= MIN_INTELLIGENCE) {
            return 0.0D;
        }

        double total = 0.0D;
        for (int value = 2; value <= intelligence; value++) {
            total += castGainAt(value);
        }
        return total;
    }

    private static double apGainAt(int intelligenceValue) {
        if (intelligenceValue <= 20) {
            return 1.00D;
        }
        if (intelligenceValue <= 50) {
            return 0.70D;
        }
        if (intelligenceValue <= 80) {
            return 0.40D;
        }
        return 0.15D;
    }

    private static double castGainAt(int intelligenceValue) {
        if (intelligenceValue <= 30) {
            return 1.00D;
        }
        if (intelligenceValue <= 45) {
            return 0.70D;
        }
        if (intelligenceValue <= 60) {
            return 0.50D;
        }
        if (intelligenceValue <= 80) {
            return 0.30D;
        }
        return 0.10D;
    }
}

