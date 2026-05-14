package de.skyforce.main.elden.compass;

import java.util.Locale;

public enum CompassDirection {
    NORTH("N", "North"),
    NORTH_EAST("NE", "Northeast"),
    EAST("E", "East"),
    SOUTH_EAST("SE", "Southeast"),
    SOUTH("S", "South"),
    SOUTH_WEST("SW", "Southwest"),
    WEST("W", "West"),
    NORTH_WEST("NW", "Northwest");

    private static final CompassDirection[] EIGHT_WAY = {
            SOUTH,
            SOUTH_WEST,
            WEST,
            NORTH_WEST,
            NORTH,
            NORTH_EAST,
            EAST,
            SOUTH_EAST
    };

    private static final CompassDirection[] FOUR_WAY = {
            SOUTH,
            WEST,
            NORTH,
            EAST
    };

    private final String shortLabel;
    private final String longLabel;

    CompassDirection(String shortLabel, String longLabel) {
        this.shortLabel = shortLabel;
        this.longLabel = longLabel;
    }

    public String shortLabel() {
        return shortLabel;
    }

    public String longLabel() {
        return longLabel;
    }

    public static CompassDirection fromYaw(float yaw, boolean showIntercardinal) {
        double normalized = normalizeYaw(yaw);

        if (showIntercardinal) {
            int index = (int) Math.floor((normalized / 45.0D) + 0.5D) % 8;
            return EIGHT_WAY[index];
        }

        int index = (int) Math.floor((normalized / 90.0D) + 0.5D) % 4;
        return FOUR_WAY[index];
    }

    public CompassDirection next(boolean showIntercardinal) {
        CompassDirection[] values = showIntercardinal ? EIGHT_WAY : FOUR_WAY;
        int index = indexOf(values, this);
        return values[(index + 1) % values.length];
    }

    public CompassDirection previous(boolean showIntercardinal) {
        CompassDirection[] values = showIntercardinal ? EIGHT_WAY : FOUR_WAY;
        int index = indexOf(values, this);
        return values[(index - 1 + values.length) % values.length];
    }

    private static int indexOf(CompassDirection[] array, CompassDirection direction) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == direction) {
                return i;
            }
        }
        return 0;
    }

    public static double normalizeYaw(float yaw) {
        double normalized = yaw % 360.0D;
        if (normalized < 0.0D) {
            normalized += 360.0D;
        }
        return normalized;
    }

    public static String parseDisplayMode(String mode) {
        if (mode == null) {
            return "COMPASS_BAR";
        }

        String normalized = mode.toUpperCase(Locale.ROOT);
        if (normalized.equals("OPTION_1")) {
            return "COMPASS_BAR";
        }

        return normalized;
    }
}