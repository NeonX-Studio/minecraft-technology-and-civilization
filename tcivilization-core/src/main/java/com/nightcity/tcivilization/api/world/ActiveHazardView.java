package com.nightcity.tcivilization.api.world;

import net.minecraft.core.BlockPos;

/**
 * Read-only view of an active or warning-stage hazard event (10 §5.14, §12.1).
 */
public record ActiveHazardView(
        long id,
        HazardType type,
        Stage stage,
        BlockPos epicenter,
        int radiusBlocks,
        float magnitude,
        long startTick,
        int remainingTicks
) {
    public enum Stage {
        WARNING,
        ACTIVE,
        SUBSIDING
    }
}
