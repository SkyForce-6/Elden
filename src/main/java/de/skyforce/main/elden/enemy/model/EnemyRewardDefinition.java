package de.skyforce.main.elden.enemy.model;

import de.skyforce.main.elden.smithing.model.SmithingTrack;

public record EnemyRewardDefinition(
        EnemyRewardType rewardType,
        SmithingTrack smithingTrack,
        int smithingTier,
        int amount,
        String displayName
) {
}
