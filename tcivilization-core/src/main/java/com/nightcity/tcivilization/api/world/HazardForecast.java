package com.nightcity.tcivilization.api.world;

import net.minecraft.core.BlockPos;

/**
 * Precursor-based or instrument-based hazard forecast item (10 §5.14, §12.1).
 */
public record HazardForecast(
        HazardType type,
        BlockPos approxEpicenter,
        int radiusBlocks,
        float probability,
        float expectedMagnitude,
        int etaTicksMin,
        int etaTicksMax
) {
}
