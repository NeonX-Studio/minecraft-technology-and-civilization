package com.nightcity.tcivilization.api.world;

import com.mojang.logging.LogUtils;
import com.nightcity.tcivilization.api.TcThreads;
import java.util.Objects;
import org.slf4j.Logger;

/**
 * Core-hosted world API installation and lookup entry point (03 §12.5, 10 §12.1).
 * Returns flat fallback implementations when tcwildlands is absent; never returns null.
 */
public final class TcWorldApis {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile WorldApiSet activeSet = FlatWorldFallback.SET;
    private static volatile SeasonalColorApi activeSeasonalColors = FlatWorldFallback.SEASONAL_COLORS;
    private static volatile String providerModId = "tcivilization";
    private static volatile boolean clientInstalled = false;

    private TcWorldApis() {
    }

    public static synchronized void install(WorldApiSet set) {
        install("tcwildlands", set);
    }

    public static synchronized void install(String modId, WorldApiSet set) {
        Objects.requireNonNull(modId, "modId");
        Objects.requireNonNull(set, "set");
        if (!"tcivilization".equals(providerModId)) {
            if (TcThreads.isStrictMode()) {
                throw new IllegalStateException("TcWorldApis already installed by " + providerModId + ", rejected " + modId);
            }
            LOGGER.error("TcWorldApis already installed by {}, ignoring second installer {}", providerModId, modId);
            return;
        }
        activeSet = set;
        providerModId = modId;
    }

    public static synchronized void installClient(SeasonalColorApi colors) {
        Objects.requireNonNull(colors, "colors");
        if (clientInstalled) {
            if (TcThreads.isStrictMode()) {
                throw new IllegalStateException("TcWorldApis client SeasonalColorApi already installed");
            }
            LOGGER.error("TcWorldApis client SeasonalColorApi already installed, ignoring duplicate");
            return;
        }
        activeSeasonalColors = colors;
        clientInstalled = true;
    }

    public static PlanetApi planet() {
        return activeSet.planet();
    }

    public static ClimateApi climate() {
        return activeSet.climate();
    }

    public static WeatherApi weather() {
        return activeSet.weather();
    }

    public static GeoApi geo() {
        return activeSet.geo();
    }

    public static HydroApi hydro() {
        return activeSet.hydro();
    }

    public static SuitabilityApi suitability() {
        return activeSet.suitability();
    }

    public static HazardApi hazards() {
        return activeSet.hazards();
    }

    public static ProspectingApi prospecting() {
        return activeSet.prospecting();
    }

    public static LocalSeasonProvider seasons() {
        return activeSet.seasons();
    }

    public static TerrainPreviewApi terrainPreview() {
        return activeSet.terrainPreview();
    }

    public static SeasonalColorApi seasonalColors() {
        return activeSeasonalColors;
    }

    public static boolean isProvidedBy(String modid) {
        return providerModId.equals(modid);
    }

    public static synchronized void resetForTests() {
        activeSet = FlatWorldFallback.SET;
        activeSeasonalColors = FlatWorldFallback.SEASONAL_COLORS;
        providerModId = "tcivilization";
        clientInstalled = false;
    }
}
