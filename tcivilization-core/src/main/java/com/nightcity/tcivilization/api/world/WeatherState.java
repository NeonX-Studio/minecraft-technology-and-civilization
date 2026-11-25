package com.nightcity.tcivilization.api.world;

import java.util.Optional;

/**
 * Instantaneous local weather state at a position (10 §12.1).
 */
public record WeatherState(
        PrecipType type,
        float precipMmPerHour,
        float cloud,
        float windSpeed,
        float windDirDeg,
        int convective,
        float tempAnomalyC,
        Optional<WeatherSystemKind> system
) {
    public WeatherState {
        type = type == null ? PrecipType.NONE : type;
        system = system == null ? Optional.empty() : system;
    }

    public static WeatherState clear() {
        return new WeatherState(PrecipType.NONE, 0.0f, 0.2f, 2.5f, 270.0f, 0, 0.0f, Optional.empty());
    }
}
