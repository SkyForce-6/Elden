package de.skyforce.main.elden.level;

public final class ArcaneScaling {

    private static final int MIN_ARCANE = 1;
    private static final int MAX_ARCANE = 99;

    private static final double AP_MAX_SCORE = cumulativeApScore(MAX_ARCANE);
    private static final double CAST_MAX_SCORE = cumulativeCastScore(MAX_ARCANE);
    private static final double STATUS_MAX_SCORE = cumulativeStatusScore(MAX_ARCANE);

    private ArcaneScaling() {
    }

    public static double attackScalingRatio(int arcane) {
        int clamped = Math.max(MIN_ARCANE, Math.min(MAX_ARCANE, arcane));
        double score = cumulativeApScore(clamped);
        return AP_MAX_SCORE <= 0.0D ? 0.0D : score / AP_MAX_SCORE;
    }

    public static double castSpeedRatio(int arcane) {
        int clamped = Math.max(MIN_ARCANE, Math.min(MAX_ARCANE, arcane));
        double score = cumulativeCastScore(clamped);
        return CAST_MAX_SCORE <= 0.0D ? 0.0D : score / CAST_MAX_SCORE;
    }

    public static double statusScalingRatio(int arcane) {
        int clamped = Math.max(MIN_ARCANE, Math.min(MAX_ARCANE, arcane));
        double score = cumulativeStatusScore(clamped);
        return STATUS_MAX_SCORE <= 0.0D ? 0.0D : score / STATUS_MAX_SCORE;
    }

    public static int itemDiscovery(int arcane) {
        int clamped = Math.max(MIN_ARCANE, Math.min(MAX_ARCANE, arcane));
        return 100 + clamped;
    }

    private static double cumulativeApScore(int arcane) {
        if (arcane <= MIN_ARCANE) {
            return 0.0D;
        }

        double total = 0.0D;
        for (int value = 2; value <= arcane; value++) {
            total += apGainAt(value);
        }
        return total;
    }

    private static double cumulativeCastScore(int arcane) {
        if (arcane <= MIN_ARCANE) {
            return 0.0D;
        }

        double total = 0.0D;
        for (int value = 2; value <= arcane; value++) {
            total += castGainAt(value);
        }
        return total;
    }

    private static double cumulativeStatusScore(int arcane) {
        if (arcane <= MIN_ARCANE) {
            return 0.0D;
        }

        double total = 0.0D;
        for (int value = 2; value <= arcane; value++) {
            total += statusGainAt(value);
        }
        return total;
    }

    private static double apGainAt(int arcaneValue) {
        if (arcaneValue <= 18) {
            return 1.0D;
        }
        if (arcaneValue <= 20) {
            return 0.7D;
        }
        if (arcaneValue <= 60) {
            return 0.65D;
        }
        if (arcaneValue <= 80) {
            return 0.45D;
        }
        return 0.2D;
    }

    private static double castGainAt(int arcaneValue) {
        if (arcaneValue <= 30) {
            return 1.0D;
        }
        if (arcaneValue <= 45) {
            return 0.6D;
        }
        return 0.1D;
    }

    private static double statusGainAt(int arcaneValue) {
        if (arcaneValue <= 40) {
            return 1.0D;
        }
        if (arcaneValue <= 45) {
            return 0.6D;
        }
        if (arcaneValue <= 60) {
            return 0.35D;
        }
        return 0.1D;
    }
}

