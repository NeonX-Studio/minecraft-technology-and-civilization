package com.nightcity.tcivilization.api.world;

import com.nightcity.tcivilization.api.annotation.AnyThread;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

/**
 * Core-hosted climate and temperature query contract (10 §5.7, §5.12.2, §12.1).
 */
public interface ClimateApi {
    @AnyThread
    ClimateSample sample(BlockPos pos, int dayOfYear);

    @AnyThread
    ClimateNormals normals(BlockPos pos);

    @AnyThread
    String koppen(BlockPos pos);

    float temperatureNow(@Nullable ServerLevel level, BlockPos pos);

    default float temperatureNow(BlockPos pos) {
        return temperatureNow(null, pos);
    }

    @AnyThread
    float globalDeltaT();
}
