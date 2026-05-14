package de.skyforce.main.elden.armor.model;

import java.util.Objects;
import org.bukkit.Material;

public record ArmorDefinition(
        String id,
        String displayName,
        ArmorSlot slot,
        Material material,
        int physicalDefense,
        int magicDefense,
        int fireDefense,
        int lightningDefense,
        int holyDefense,
        int poise,
        double weight
) {
    public ArmorDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(material, "material");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName cannot be blank");
        }
        if (weight < 0.0D) {
            throw new IllegalArgumentException("weight cannot be negative");
        }
    }
}
