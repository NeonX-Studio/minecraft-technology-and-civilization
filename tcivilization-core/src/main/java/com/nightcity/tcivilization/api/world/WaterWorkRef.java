package com.nightcity.tcivilization.api.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * Reference to an engineered hydraulic structure (dike, dam, canal, sluice) registered with HydroApi (10 §11.4, §12.1).
 */
public record WaterWorkRef(
        Identifier id,
        Kind kind,
        BlockPos start,
        BlockPos end,
        float crestHeightBlocks,
        float capacityM3s
) {
    public enum Kind {
        DIKE,
        DAM,
        CANAL,
        SLUICE
    }
}
