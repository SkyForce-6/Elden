package de.skyforce.main.elden.enemy.model;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public record EnemyDefinition(
        String id,
        String displayName,
        EntityType entityType,
        EnemyArchetype archetype,
        double maxHealth,
        double baseDamage,
        double movementSpeed,
        double followRange,
        int runeReward,
        long respawnDelayTicks,
        double maxLeashDistance,
        boolean elite,
        EnemyRewardDefinition guaranteedReward,
        Material mainHand,
        Material helmet,
        Material chestplate,
        Material leggings,
        Material boots,
        boolean baby
) {
}
