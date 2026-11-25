package com.nightcity.tcivilization.api.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import org.jspecify.annotations.Nullable;

/**
 * Core-hosted regional weather and wind query contract (10 §5.13, §12.1).
 */
public interface WeatherApi {
    WeatherState current(@Nullable Level level, BlockPos pos);

    Vec2 wind(BlockPos pos);

    boolean isPrecipitating(@Nullable Level level, BlockPos pos);

    PrecipType precipType(@Nullable Level level, BlockPos pos);

    WeatherForecast forecast(BlockPos pos, int days, ForecastSkill skill);
}
