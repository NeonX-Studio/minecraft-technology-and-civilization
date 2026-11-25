package com.nightcity.tcivilization.api.world;

import net.minecraft.world.phys.Vec2;

/**
 * Reach-level hydrological metrics at a world block position (10 §5.5, §12.1).
 */
public record RiverInfo(
        long reachId,
        int strahlerOrder,
        float dischargeM3s,
        int waterSurfaceY,
        int bankfullY,
        int widthBlocks,
        int depthBlocks,
        Vec2 velocityBlocksPerSec,
        FloodStage floodStage
) {
}
