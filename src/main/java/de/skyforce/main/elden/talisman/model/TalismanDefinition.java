package de.skyforce.main.elden.talisman.model;

import org.bukkit.Material;

public record TalismanDefinition(
        String id,
        String displayName,
        Material material,
        String description,
        TalismanEffectType effectType,
        double value
) {
}
