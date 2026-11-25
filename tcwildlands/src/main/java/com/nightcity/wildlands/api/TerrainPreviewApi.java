package com.nightcity.wildlands.api;

import com.nightcity.tcivilization.api.world.TcWorldApis;
import net.minecraft.resources.Identifier;

/**
 * Convenience facade matching tcatlas WildlandsTerrainBridge reflective lookup
 * while delegating directly to core-hosted TcWorldApis.terrainPreview() (22 §5.10, 10 §12.1).
 */
public final class TerrainPreviewApi {
    private static final TerrainPreviewApi INSTANCE = new TerrainPreviewApi();

    public record PreviewResult(
            int x,
            int z,
            int surfaceY,
            int height,
            int waterY,
            boolean hasWater,
            Identifier biome,
            Identifier topBlock
    ) {
    }

    private TerrainPreviewApi() {
    }

    public static TerrainPreviewApi get() {
        return INSTANCE;
    }

    public PreviewResult sample(int x, int z, int step) {
        com.nightcity.tcivilization.api.world.TerrainPreviewApi.PreviewSample s = TcWorldApis.terrainPreview().sample(x, z);
        return new PreviewResult(s.x(), s.z(), s.surfaceY(), s.surfaceY(), s.waterY(), s.hasWater(), s.biome(), s.topBlock());
    }
}
