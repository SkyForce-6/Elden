package de.skyforce.main.elden.level;

public final class LevelFormula {

    private LevelFormula() {
    }

    public static int nextLevelRuneCost(int currentLevel) {
        int level = Math.max(1, currentLevel);
        double x = ((level + 81) - 92) * 0.02D;
        if (x < 0.0D) {
            x = 0.0D;
        }

        double result = ((x + 0.1D) * Math.pow(level + 81, 2)) + 1.0D;
        return (int) Math.floor(result);
    }
}

