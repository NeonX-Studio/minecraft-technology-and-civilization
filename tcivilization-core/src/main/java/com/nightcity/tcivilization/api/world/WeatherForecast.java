package com.nightcity.tcivilization.api.world;

import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * Multi-day weather forecast result with skill-dependent uncertainty (10 §5.13.4, §12.1).
 */
public record WeatherForecast(
        BlockPos pos,
        int days,
        ForecastSkill skill,
        List<DayEntry> entries,
        float errorMargin
) {
    public WeatherForecast {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public record DayEntry(
            int dayOffset,
            float precipProbability,
            float expectedPrecipMm,
            float minTempC,
            float maxTempC,
            PrecipType dominantType
    ) {
    }
}
