package com.nightcity.tcivilization.api.world;

import com.nightcity.tcivilization.api.annotation.AnyThread;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * Fast noise-only terrain preview API for ungenerated chunks (22 §5.10, 10 §12.1).
 */
public interface TerrainPreviewApi {
    enum SurfaceKind {
        OCEAN,
        RIVER,
        LAKE,
        ICE,
        BEACH,
        DESERT,
        ROCK,
        GRASS,
        FOREST,
        WETLAND,
        VOLCANIC
    }

    record PreviewSample(
            int x,
            int z,
            int surfaceY,
            int waterY,
            boolean hasWater,
            SurfaceKind surfaceKind,
            Identifier biome,
            Identifier topBlock
    ) {
    }

    @AnyThread
    PreviewSample sample(int x, int z);

    @AnyThread
    default List<PreviewSample> sample(int chunkMinX, int chunkMinZ, int step) {
        return sampleGrid(chunkMinX, chunkMinZ, 16, 16, step);
    }

    @AnyThread
    default List<PreviewSample> sampleGrid(int minX, int minZ, int widthBlocks, int heightBlocks, int step) {
        int safeStep = Math.max(1, step);
        int nx = Math.max(1, widthBlocks / safeStep);
        int nz = Math.max(1, heightBlocks / safeStep);
        List<PreviewSample> out = new ArrayList<>(nx * nz);
        for (int gz = 0; gz < nz; gz++) {
            int z = minZ + gz * safeStep;
            for (int gx = 0; gx < nx; gx++) {
                int x = minX + gx * safeStep;
                out.add(sample(x, z));
            }
        }
        return out;
    }
}
