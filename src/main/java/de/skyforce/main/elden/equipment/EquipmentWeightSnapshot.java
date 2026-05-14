package de.skyforce.main.elden.equipment;

public record EquipmentWeightSnapshot(
        double currentLoad,
        double maxLoad,
        double loadRatio,
        WeightTier tier
) {
}
