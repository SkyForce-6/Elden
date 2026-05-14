package de.skyforce.main.elden.level;

public final class FaithScaling {

    private static final int MIN_FAITH = 1;
    private static final int MAX_FAITH = 99;

    private static final double AP_MAX_SCORE = cumulativeApScore(MAX_FAITH);
    private static final double CAST_MAX_SCORE = cumulativeCastScore(MAX_FAITH);

    private FaithScaling() {
    }

    public static double attackScalingRatio(int faith) {
        int clamped = Math.max(MIN_FAITH, Math.min(MAX_FAITH, faith));
        double score = cumulativeApScore(clamped);
        return AP_MAX_SCORE <= 0.0D ? 0.0D : score / AP_MAX_SCORE;
    }

    public static double castSpeedRatio(int faith) {
        int clamped = Math.max(MIN_FAITH, Math.min(MAX_FAITH, faith));
        double score = cumulativeCastScore(clamped);
        return CAST_MAX_SCORE <= 0.0D ? 0.0D : score / CAST_MAX_SCORE;
    }

    private static double cumulativeApScore(int faith) {
        if (faith <= MIN_FAITH) {
            return 0.0D;
        }

        double total = 0.0D;
        for (int value = 2; value <= faith; value++) {
            total += apGainAt(value);
        }
        return total;
    }

    private static double cumulativeCastScore(int faith) {
        if (faith <= MIN_FAITH) {
            return 0.0D;
        }

        double total = 0.0D;
        for (int value = 2; value <= faith; value++) {
            total += castGainAt(value);
        }
        return total;
    }

    private static double apGainAt(int faithValue) {
        if (faithValue <= 20) {
            return 1.00D;
        }
        if (faithValue <= 50) {
            return 0.70D;
        }
        if (faithValue <= 80) {
            return 0.40D;
        }
        return 0.15D;
    }

    private static double castGainAt(int faithValue) {
        if (faithValue <= 30) {
            return 1.00D;
        }
        if (faithValue <= 45) {
            return 0.70D;
        }
        if (faithValue <= 60) {
            return 0.50D;
        }
        if (faithValue <= 80) {
            return 0.30D;
        }
        return 0.10D;
    }
}

