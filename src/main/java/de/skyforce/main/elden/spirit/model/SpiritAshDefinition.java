package de.skyforce.main.elden.spirit.model;

import org.bukkit.Material;

public record SpiritAshDefinition(
        String id,
        String displayName,
        Material icon,
        String description,
        double fpCost,
        long cooldownTicks,
        long summonDurationTicks,
        String location,
        SpiritAshSummonType summonType
) {

    public SpiritAshDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (icon == null) {
            throw new IllegalArgumentException("icon must not be null");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("location must not be blank");
        }
        if (summonType == null) {
            throw new IllegalArgumentException("summonType must not be null");
        }
        fpCost = Math.max(0.0D, fpCost);
        cooldownTicks = Math.max(1L, cooldownTicks);
        summonDurationTicks = Math.max(20L, summonDurationTicks);
    }
}
