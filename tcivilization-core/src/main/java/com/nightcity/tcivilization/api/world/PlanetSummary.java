package com.nightcity.tcivilization.api.world;

import net.minecraft.core.BlockPos;

/**
 * High-level summary of a precomputed planet or flat fallback world (10 §12.1).
 */
public record PlanetSummary(
        long seed,
        int plateCount,
        int continentCount,
        int hotspotCount,
        float oceanFraction,
        int maxElevationY,
        double maxElevationMeters,
        BlockPos highestPeak
) {
    public static PlanetSummary flatFallback() {
        return new PlanetSummary(0L, 1, 1, 0, 0.0f, 64, 0.0, new BlockPos(0, 64, 0));
    }
}
