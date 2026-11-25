package com.nightcity.tcivilization.api.world;

/**
 * Technological forecast skill tier (10 §5.13.4, §12.1).
 */
public enum ForecastSkill {
    FOLK(0, 1.0f),
    ALMANAC(2, 0.50f),
    BAROMETER(1, 0.25f),
    SYNOPTIC(2, 0.15f),
    NUMERICAL(5, 0.08f);

    private final int maxDays;
    private final float relativeError;

    ForecastSkill(int maxDays, float relativeError) {
        this.maxDays = maxDays;
        this.relativeError = relativeError;
    }

    public int maxDays() {
        return maxDays;
    }

    public float relativeError() {
        return relativeError;
    }
}
