package de.skyforce.main.elden.boss.model;

import org.bukkit.Particle;
import org.bukkit.entity.EntityType;

public record BossDefinition(
        String id,
        String displayName,
        BossArchetype archetype,
        BossRewardDefinition rewards,
        EntityType entityType,
        double maxHealth,
        double baseDamage,
        double movementSpeed,
        int runeReward,
        double leashRadius,
        double arenaRadius,
        long resetAfterIdleTicks,
        double phaseTwoThreshold,
        double phaseThreeThreshold,
        double phaseTwoDamageMultiplier,
        double phaseTwoSpeedMultiplier,
        double phaseThreeDamageMultiplier,
        double phaseThreeSpeedMultiplier,
        long abilityCooldownTicks,
        Particle ambientParticle
) {
}
