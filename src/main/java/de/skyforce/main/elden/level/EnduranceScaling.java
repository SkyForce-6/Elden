package de.skyforce.main.elden.level;

public final class EnduranceScaling {

    private static final int MIN_ENDURANCE = 1;
    private static final int MAX_ENDURANCE = 99;

    private EnduranceScaling() {
    }

    public static double staminaBonusRatioForEndurance(int endurance) {
        int clamped = Math.max(MIN_ENDURANCE, Math.min(MAX_ENDURANCE, endurance));
        double score = cumulativeStaminaScore(clamped);
        double maxScore = cumulativeStaminaScore(MAX_ENDURANCE);
        return maxScore <= 0.0D ? 0.0D : score / maxScore;
    }

    public static double equipLoadForEndurance(int endurance) {
        int clamped = Math.max(MIN_ENDURANCE, Math.min(MAX_ENDURANCE, endurance));
        double baseEquipLoad = 40.0D;
        return baseEquipLoad + cumulativeEquipLoadGain(clamped);
    }

    private static double cumulativeStaminaScore(int endurance) {
        if (endurance <= MIN_ENDURANCE) {
            return 0.0D;
        }

        double total = 0.0D;
        for (int level = 2; level <= endurance; level++) {
            total += staminaGainAt(level);
        }
        return total;
    }

    private static double cumulativeEquipLoadGain(int endurance) {
        if (endurance <= 8) {
            return 0.0D;
        }

        double total = 0.0D;
        for (int level = 9; level <= endurance; level++) {
            total += equipLoadGainAt(level);
        }
        return total;
    }

    private static double staminaGainAt(int enduranceLevel) {
        if (enduranceLevel <= 15) {
            return lerp(1.0D, 2.0D, enduranceLevel - 2, 13);
        }
        if (enduranceLevel <= 30) {
            return lerp(1.0D, 2.0D, enduranceLevel - 16, 14);
        }
        if (enduranceLevel <= 50) {
            return lerp(1.0D, 2.0D, enduranceLevel - 31, 19);
        }

        return ((enduranceLevel - 51) % 4 == 0) ? 1.0D : 0.0D;
    }

    private static double equipLoadGainAt(int enduranceLevel) {
        if (enduranceLevel <= 25) {
            return ((enduranceLevel - 9) % 2 == 0) ? 1.6D : 1.5D;
        }
        if (enduranceLevel <= 60) {
            return lerp(1.5D, 1.0D, enduranceLevel - 26, 34);
        }

        return lerp(1.1D, 1.0D, enduranceLevel - 61, 38);
    }

    private static double lerp(double start, double end, int index, int span) {
        if (span <= 0) {
            return start;
        }

        double t = Math.max(0.0D, Math.min(1.0D, index / (double) span));
        return start + ((end - start) * t);
    }
}

