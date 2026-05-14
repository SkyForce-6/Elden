package de.skyforce.main.elden.smithing.model;

public record SmithingRequirement(
        SmithingTrack track,
        int targetLevel,
        int runeCost,
        int stoneTier,
        int stoneAmount
) {
}
