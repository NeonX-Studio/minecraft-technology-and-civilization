package com.nightcity.tcivilization.api.world;

/**
 * Daily climatological sample at a position and day of year, excluding transient weather systems (10 §12.1).
 */
public record ClimateSample(
        float meanC,
        float minC,
        float maxC,
        float precipMmPerDay,
        float humidity,
        float frostProbability,
        float gddBase5,
        float snowDepthBlocks,
        Season season
) {
}
