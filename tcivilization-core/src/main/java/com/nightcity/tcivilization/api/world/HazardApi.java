package com.nightcity.tcivilization.api.world;

import com.nightcity.tcivilization.api.annotation.AnyThread;
import com.nightcity.tcivilization.api.annotation.MainThread;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

/**
 * Core-hosted natural hazard risk, SPI drought index, forecast, and mitigation contract (10 §5.14, §12.1).
 */
public interface HazardApi {
    @AnyThread
    float hazardLevel(BlockPos pos);

    @AnyThread
    HazardProfile profile(BlockPos pos);

    List<HazardForecast> forecast(BlockPos pos, int radius, int horizonTicks, ForecastSkill skill);

    float droughtIndex(BlockPos pos);

    List<ActiveHazardView> active(@Nullable ServerLevel level);

    @MainThread
    void registerMitigation(MitigationRef ref);

    @MainThread
    void requestManagedBurn(ServerLevel level, BlockPos center, int radius, float spreadFactor);
}
