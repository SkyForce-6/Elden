package de.skyforce.main.elden.flask;

import org.bukkit.Material;

public enum FlaskType {
    CRIMSON("crimson", "Flask of Crimson Tears", Material.POTION),
    CERULEAN("cerulean", "Flask of Cerulean Tears", Material.POTION);

    private final String key;
    private final String displayName;
    private final Material material;

    FlaskType(String key, String displayName, Material material) {
        this.key = key;
        this.displayName = displayName;
        this.material = material;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public Material material() {
        return material;
    }
}

