package de.skyforce.main.elden.boss.model;

public record BossRewardDefinition(
        BossRewardType firstKillRewardType,
        String firstKillRewardId,
        String firstKillRewardName,
        String remembranceId,
        String remembranceName,
        BossRewardType remembranceExchangeRewardType,
        String remembranceExchangeRewardId,
        String remembranceExchangeRewardName,
        int remembranceRuneValue
) {
}
