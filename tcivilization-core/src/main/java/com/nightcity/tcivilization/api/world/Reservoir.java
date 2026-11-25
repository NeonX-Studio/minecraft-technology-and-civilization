package com.nightcity.tcivilization.api.world;

import net.minecraft.core.BlockPos;

/**
 * Subsurface oil, gas, brine, or geothermal fluid reservoir (10 §5.11.4, §12.1).
 */
public record Reservoir(
        long id,
        Kind kind,
        BlockPos center,
        int radiusXZ,
        int topY,
        int bottomY,
        long initialCapacityMb,
        long remainingMb,
        float pressureBar
) {
    public enum Kind {
        CRUDE_OIL,
        NATURAL_GAS,
        BRINE,
        GEOTHERMAL
    }
}
