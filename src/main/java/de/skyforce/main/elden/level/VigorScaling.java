package de.skyforce.main.elden.level;

public final class VigorScaling {

    private static final int MIN_VIGOR = 1;
    private static final int MAX_VIGOR = 99;

    private static final double BASE_HEALTH = 20.0D;
    private static final double MAX_HEALTH = 40.0D;
    private static final double TOTAL_HEALTH_SPAN = MAX_HEALTH - BASE_HEALTH;

    private static final double MAX_SCORE = cumulativeScore(MAX_VIGOR);

    private VigorScaling() {
    }

    public static double maxHealthForVigor(int vigor) {
        int clamped = Math.max(MIN_VIGOR, Math.min(MAX_VIGOR, vigor));
        double score = cumulativeScore(clamped);
        double ratio = MAX_SCORE <= 0.0D ? 0.0D : score / MAX_SCORE;
        return BASE_HEALTH + (TOTAL_HEALTH_SPAN * ratio);
    }

    private static double cumulativeScore(int vigor) {
        if (vigor <= MIN_VIGOR) {
            return 0.0D;
        }

        double total = 0.0D;
        for (int level = 2; level <= vigor; level++) {
            total += hpGainAt(level);
        }
        return total;
    }

    private static double hpGainAt(int vigorLevel) {
        if (vigorLevel <= 40) {
            return lerp(4.0D, 48.0D, vigorLevel - 2, 38);
        }

        if (vigorLevel <= 60) {
            return lerp(26.0D, 13.0D, vigorLevel - 41, 19);
        }

        return lerp(6.0D, 3.0D, vigorLevel - 61, 38);
    }

    private static double lerp(double start, double end, int index, int span) {
        if (span <= 0) {
            return start;
        }

        double t = Math.max(0.0D, Math.min(1.0D, index / (double) span));
        return start + ((end - start) * t);
    }
}

