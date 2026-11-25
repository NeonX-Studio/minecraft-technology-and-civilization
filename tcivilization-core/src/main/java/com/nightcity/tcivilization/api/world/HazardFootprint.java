package com.nightcity.tcivilization.api.world;

import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * Spatial footprint and intensity grid of an active disaster (10 §12.2).
 */
public record HazardFootprint(
        BlockPos center,
        int radiusBlocks,
        List<BlockPos> polygonVertices,
        int gridStepBlocks,
        int gridWidth,
        int gridHeight,
        float[] intensityGrid
) {
    public HazardFootprint {
        polygonVertices = polygonVertices == null ? List.of() : List.copyOf(polygonVertices);
        intensityGrid = intensityGrid == null ? new float[0] : intensityGrid.clone();
    }
}
