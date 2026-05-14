package de.skyforce.main.elden.level;

public final class MindScaling {

    private static final int MIN_MIND = 1;
    private static final int MAX_MIND = 99;

    private MindScaling() {
    }

    public static double focusBonusRatioForMind(int mind) {
        int clamped = Math.max(MIN_MIND, Math.min(MAX_MIND, mind));
        double score = cumulativeScore(clamped);
        double maxScore = cumulativeScore(MAX_MIND);
        return maxScore <= 0.0D ? 0.0D : score / maxScore;
    }

    public static double staminaBonusRatioForMind(int mind) {
        return focusBonusRatioForMind(mind);
    }

    private static double cumulativeScore(int mind) {
        if (mind <= MIN_MIND) {
            return 0.0D;
        }

        double total = 0.0D;
        for (int level = 2; level <= mind; level++) {
            total += fpGainAt(level);
        }
        return total;
    }

    private static double fpGainAt(int mindLevel) {
        if (mindLevel <= 15) {
            return lerp(3.0D, 4.0D, mindLevel - 2, 13);
        }
        if (mindLevel <= 35) {
            return ((mindLevel - 16) % 2 == 0) ? 5.0D : 6.0D;
        }
        if (mindLevel <= 50) {
            return lerp(7.0D, 6.0D, mindLevel - 36, 14);
        }
        if (mindLevel <= 60) {
            return lerp(6.0D, 4.0D, mindLevel - 51, 9);
        }

        return ((mindLevel - 61) % 2 == 0) ? 2.0D : 3.0D;
    }

    private static double lerp(double start, double end, int index, int span) {
        if (span <= 0) {
            return start;
        }

        double t = Math.max(0.0D, Math.min(1.0D, index / (double) span));
        return start + ((end - start) * t);
    }
}
