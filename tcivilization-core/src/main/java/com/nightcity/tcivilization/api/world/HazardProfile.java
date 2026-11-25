package com.nightcity.tcivilization.api.world;

/**
 * Long-term 0-1 hazard susceptibility profile at a world position (10 §5.14, §12.1).
 */
public record HazardProfile(
        float flood,
        float earthquake,
        float volcano,
        float landslide,
        float avalanche,
        float wildfire,
        float storm
) {
    public static HazardProfile zero() {
        return new HazardProfile(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public float composite() {
        float max = Math.max(flood, Math.max(earthquake, Math.max(volcano, Math.max(landslide, Math.max(avalanche, Math.max(wildfire, storm))))));
        float mean = (flood + earthquake + volcano + landslide + avalanche + wildfire + storm) / 7.0f;
        return Math.clamp(0.65f * max + 0.35f * mean, 0.0f, 1.0f);
    }
}
