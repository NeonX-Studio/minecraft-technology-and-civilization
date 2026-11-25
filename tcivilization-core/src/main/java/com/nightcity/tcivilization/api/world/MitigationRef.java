package com.nightcity.tcivilization.api.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * Engineered disaster mitigation structure registered with HazardApi (10 §12.1).
 */
public record MitigationRef(
        Identifier id,
        Kind kind,
        BlockPos center,
        int radiusBlocks,
        float protectionFactor
) {
    public enum Kind {
        FIREBREAK,
        BREAKWATER,
        SEISMIC_RETROFIT,
        AVALANCHE_SHED
    }
}
