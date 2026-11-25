package com.nightcity.tcivilization.api.world;

import net.minecraft.core.BlockPos;

/**
 * Client-side seasonal grass/foliage/water/sky color modifier (10 §5.12.3, §12.1).
 */
public interface SeasonalColorApi {
    int tint(BlockPos pos, TintTarget target, int baseColor);
}
