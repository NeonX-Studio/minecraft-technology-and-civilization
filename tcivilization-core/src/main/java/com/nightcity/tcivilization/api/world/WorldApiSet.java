package com.nightcity.tcivilization.api.world;

import java.util.Objects;

/**
 * Bundle of server/common world APIs installed into TcWorldApis during FMLCommonSetupEvent (03 §12.5, 10 §12.1).
 */
public record WorldApiSet(
        PlanetApi planet,
        ClimateApi climate,
        WeatherApi weather,
        GeoApi geo,
        HydroApi hydro,
        SuitabilityApi suitability,
        HazardApi hazards,
        ProspectingApi prospecting,
        LocalSeasonProvider seasons,
        TerrainPreviewApi terrainPreview
) {
    public WorldApiSet {
        Objects.requireNonNull(planet, "planet");
        Objects.requireNonNull(climate, "climate");
        Objects.requireNonNull(weather, "weather");
        Objects.requireNonNull(geo, "geo");
        Objects.requireNonNull(hydro, "hydro");
        Objects.requireNonNull(suitability, "suitability");
        Objects.requireNonNull(hazards, "hazards");
        Objects.requireNonNull(prospecting, "prospecting");
        Objects.requireNonNull(seasons, "seasons");
        terrainPreview = terrainPreview == null ? FlatWorldFallback.TERRAIN_PREVIEW : terrainPreview;
    }

    public WorldApiSet(
            PlanetApi planet,
            ClimateApi climate,
            WeatherApi weather,
            GeoApi geo,
            HydroApi hydro,
            SuitabilityApi suitability,
            HazardApi hazards,
            ProspectingApi prospecting,
            LocalSeasonProvider seasons
    ) {
        this(planet, climate, weather, geo, hydro, suitability, hazards, prospecting, seasons, FlatWorldFallback.TERRAIN_PREVIEW);
    }
}
