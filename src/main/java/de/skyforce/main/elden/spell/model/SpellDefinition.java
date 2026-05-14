package de.skyforce.main.elden.spell.model;

import de.skyforce.main.elden.level.AttributeType;
import org.bukkit.Material;

public record SpellDefinition(
        String id,
        String displayName,
        SpellSchool school,
        Material icon,
        String description,
        double fpCost,
        long cooldownTicks,
        long castTimeTicks,
        AttributeType scalingAttribute,
        AttributeType primaryRequirementAttribute,
        int primaryRequirementLevel,
        AttributeType secondaryRequirementAttribute,
        int secondaryRequirementLevel,
        String tradition
) {

    public boolean hasSecondaryRequirement() {
        return secondaryRequirementAttribute != null && secondaryRequirementLevel > 0;
    }
}
