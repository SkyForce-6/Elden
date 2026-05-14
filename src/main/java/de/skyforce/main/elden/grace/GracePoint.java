package de.skyforce.main.elden.grace;

import org.bukkit.Location;

public final class GracePoint {

    private final String key;
    private final String displayName;
    private final Location location;

    public GracePoint(String key, String displayName, Location location) {
        this.key = key;
        this.displayName = displayName;
        this.location = location.clone();
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Location getLocation() {
        return location.clone();
    }
}