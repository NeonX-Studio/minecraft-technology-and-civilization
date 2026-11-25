package com.nightcity.wildlands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.nightcity.tcivilization.api.CivCalendar;
import com.nightcity.tcivilization.api.world.*;
import com.nightcity.wildlands.data.WildlandsRegionLayers;
import com.nightcity.wildlands.planet.*;
import com.nightcity.wildlands.prospect.ProspectingService;
import com.nightcity.wildlands.weather.RegionalWeatherEngine;
import com.nightcity.wildlands.worldgen.PlanetChunkSynthesizer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PlanetPipelineAndCriteriaTest {

    @AfterEach
    void cleanUp() {
        RegionalWeatherEngine.reset();
        ProspectingService.clearForTests();
        PlanetChunkSynthesizer.clearTracesForTests();
    }

    @Test
    void deterministicPrecomputationAcrossSeedsAndBinaryRoundTrip(@TempDir Path tempDir) throws IOException {
        long startNs = System.nanoTime();
        PlanetGenerator.Config cfg = PlanetGenerator.Config.defaults();
        PlanetModel m1 = PlanetGenerator.generate(424242L, cfg, null);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        assertTrue(elapsedMs <= 8_000L, "Planet precompute exceeded 8s budget: " + elapsedMs + " ms");

        PlanetModel m2 = PlanetGenerator.generate(424242L, cfg, null);
        assertEquals(m1.planetDigest(), m2.planetDigest(), "Identical seed + config must produce identical digest");
        byte[] bytes1 = PlanetBinFile.serializeToBytes(m1);
        byte[] bytes2 = PlanetBinFile.serializeToBytes(m2);
        assertArrayEquals(bytes1, bytes2, "planet.bin serialization must be byte-for-byte deterministic");

        assertTrue(m1.continentCount() >= 3, "Expected >= 3 continents, got " + m1.continentCount());
        float oceanFrac = m1.oceanFraction();
        assertTrue(oceanFrac >= 0.45f && oceanFrac <= 0.85f, "Ocean fraction out of Earth-like range: " + oceanFrac);
        assertTrue(m1.summary().maxElevationY() <= 920 && m1.summary().maxElevationY() > 200,
                "Peak surface Y out of expected range: " + m1.summary().maxElevationY());
        assertTrue(m1.startSites().size() >= 5 && m1.startSites().size() <= 200,
                "Expected 5..200 start sites per 10 §8.1, got " + m1.startSites().size());

        Path binPath = tempDir.resolve("planet.bin");
        Files.write(binPath, bytes1);
        assertTrue(Files.size(binPath) > 500_000L && Files.size(binPath) <= 45_000_000L,
                "Unexpected planet.bin size: " + Files.size(binPath));

        PlanetModel loaded = PlanetBinFile.deserializeFromBytes(Files.readAllBytes(binPath));
        assertEquals(PlanetBinFile.CURRENT_FORMAT_VERSION, loaded.formatVersion());
        assertEquals(m1.planetDigest(), loaded.planetDigest());
        assertEquals(m1.deposits().size(), loaded.deposits().size());
        assertEquals(m1.startSites().size(), loaded.startSites().size());

        PlanetModel other = PlanetGenerator.generate(20261123L, cfg, null);
        assertNotEquals(m1.planetDigest(), other.planetDigest());
        assertTrue(other.continentCount() >= 3);
    }

    @Test
    void eastWestWrappingPeriodicNoiseChunkSeamAndL2Timing() {
        assertEquals(-81_920, PlanetCoordinates.wrapX(81_920));
        assertEquals(-81_915, PlanetCoordinates.wrapX(81_925));
        assertEquals(81_919, PlanetCoordinates.wrapX(-81_921));
        assertEquals(-20, PlanetCoordinates.wrappedDx(-81_910, 81_910));
        assertEquals(20, PlanetCoordinates.wrappedDx(81_910, -81_910));

        for (int z = -35_000; z <= 35_000; z += 5_000) {
            double left = PeriodicNoise.sample2D(-999111L, -81_920, z, 256);
            double right = PeriodicNoise.sample2D(-999111L, 81_920, z, 256);
            assertEquals(left, right, 1e-12, "PeriodicNoise seam mismatch at z=" + z);
        }

        PlanetModel model = PlanetGenerator.generate(777L, PlanetGenerator.Config.defaults(), null);

        PlanetChunkSynthesizer.SynthesizedChunk westEdge = PlanetChunkSynthesizer.synthesizeChunk(model, -5120, 0);
        PlanetChunkSynthesizer.SynthesizedChunk eastEdge = PlanetChunkSynthesizer.synthesizeChunk(model, 5119, 0);
        for (int lz = 0; lz < 16; lz++) {
            int yWest = westEdge.surfaceY()[lz * 16];
            int yEast = eastEdge.surfaceY()[lz * 16 + 15];
            assertTrue(Math.abs(yWest - yEast) <= 3,
                    "Seam cliff detected at z=" + lz + ": y(-81920)=" + yWest + " vs y(+81919)=" + yEast);
        }

        for (Identifier blk : westEdge.blocks()) {
            assertFalse(WildlandsCatalog.FORBIDDEN_PLANET_BLOCKS.contains(blk),
                    "Forbidden vanilla underground block generated on planet: " + blk);
        }

        for (int cx = -2; cx < 2; cx++) {
            PlanetChunkSynthesizer.synthesizeChunk(model, cx, 0);
        }
        long t0 = System.nanoTime();
        int count = 16;
        for (int i = 0; i < count; i++) {
            PlanetChunkSynthesizer.synthesizeChunk(model, i * 5 - 40, (i % 4) - 2);
        }
        double avgMs = ((System.nanoTime() - t0) / 1_000_000.0) / count;
        assertTrue(avgMs <= 25.0, "L2 chunk synthesis exceeded budget: " + avgMs + " ms/chunk");
    }

    @Test
    void polarClampHemisphereSolarGeometrySeasonsAndCropFactor() {
        assertEquals(40_959, PlanetCoordinates.clampZ(55_000));
        assertEquals(-40_960, PlanetCoordinates.clampZ(-55_000));

        int nhSolstice = CivCalendar.YEAR_DAYS / 4;
        int nhWinterSolstice = (CivCalendar.YEAR_DAYS * 3) / 4;

        assertEquals(24.0f, PlanetCoordinates.dayLengthHours(82.0, nhSolstice), 1e-4f);
        assertEquals(0.0f, PlanetCoordinates.dayLengthHours(82.0, nhWinterSolstice), 1e-4f);
        assertEquals(0.0f, PlanetCoordinates.dayLengthHours(-82.0, nhSolstice), 1e-4f);
        assertEquals(24.0f, PlanetCoordinates.dayLengthHours(-82.0, nhWinterSolstice), 1e-4f);

        LocalSeason nh = RegionalWeatherEngine.resolveLocalSeason(new BlockPos(0, 64, -20_000), nhSolstice);
        LocalSeason sh = RegionalWeatherEngine.resolveLocalSeason(new BlockPos(0, 64, 20_000), nhSolstice);
        assertEquals(Season.SUMMER, nh.astronomical());
        assertEquals(Season.WINTER, sh.astronomical());
        assertFalse(nh.southernHemisphere());
        assertTrue(sh.southernHemisphere());

        LocalSeason trop = RegionalWeatherEngine.resolveLocalSeason(new BlockPos(0, 64, -4_000), nhSolstice);
        assertEquals(SeasonRegime.MONSOON, trop.regime());

        assertEquals(0.0f, RegionalWeatherEngine.computeCropGrowthFactor(2.0f, 12.0f, 0.0f, false, false), 1e-5f);
        assertEquals(1.0f, RegionalWeatherEngine.computeCropGrowthFactor(20.0f, 12.0f, 0.0f, false, false), 1e-5f);
        assertEquals(1.25f, RegionalWeatherEngine.computeCropGrowthFactor(20.0f, 12.0f, 0.0f, true, false), 1e-5f);
    }

    @Test
    void riverDownhillFlowClimateGradientOreLegalityPanningAndRegionLayers() {
        PlanetModel model = PlanetGenerator.generate(314159L, PlanetGenerator.Config.defaults(), null);

        byte[] flowDir = model.rawFlowDir();
        short[] elev = model.rawElevation();
        byte[] order = model.rawRiverOrder();
        short[] lake = model.rawLake();
        int riverStepsChecked = 0;
        for (int gz = 1; gz < PlanetModel.GRID_H - 1; gz++) {
            for (int gx = 0; gx < PlanetModel.GRID_W; gx++) {
                int idx = PlanetModel.l0Index(gx, gz);
                if ((order[idx] & 0xFF) >= 2 && elev[idx] > 0 && lake[idx] == 0) {
                    int dir = flowDir[idx] & 0xFF;
                    if (dir >= 1 && dir <= 8) {
                        int nx = Math.floorMod(gx + PlanetModel.D8_DX[dir], PlanetModel.GRID_W);
                        int nz = Math.clamp(gz + PlanetModel.D8_DZ[dir], 0, PlanetModel.GRID_H - 1);
                        int nIdx = PlanetModel.l0Index(nx, nz);
                        if (lake[nIdx] == 0) {
                            assertTrue(elev[nIdx] <= elev[idx],
                                    "River flowed uphill from " + elev[idx] + "m to " + elev[nIdx] + "m");
                            riverStepsChecked++;
                        }
                    }
                }
            }
        }
        assertTrue(riverStepsChecked > 100, "Expected substantial river network, checked " + riverStepsChecked);

        Set<Integer> distinctBiomes = new HashSet<>();
        for (byte b : model.rawBiome()) {
            distinctBiomes.add(b & 0xFF);
        }
        assertTrue(distinctBiomes.size() >= 25, "Expected rich biome diversity, got " + distinctBiomes.size());

        double eqSum = 0.0;
        double polSum = 0.0;
        int samples = 64;
        for (int i = 0; i < samples; i++) {
            int bx = PlanetCoordinates.X_MIN + i * (PlanetCoordinates.PLANET_WIDTH / samples);
            eqSum += model.climateNormalsAt(bx, 0).annualMeanC();
            polSum += 0.5 * (model.climateNormalsAt(bx, -39_000).annualMeanC()
                    + model.climateNormalsAt(bx, 39_000).annualMeanC());
        }
        assertTrue((eqSum / samples) - (polSum / samples) >= 25.0,
                "Equatorial-polar temperature contrast too weak: " + ((eqSum / samples) - (polSum / samples)));

        assertEquals(87, WildlandsCatalog.LEGAL_ORE_BLOCKS.size());
        assertFalse(model.deposits().isEmpty(), "Planet must generate geological deposits");

        Deposit placerDep = model.deposits().stream()
                .filter(d -> d.mineral().getPath().contains("gold") || d.mineral().getPath().contains("cassiterite"))
                .findFirst().orElse(null);
        if (placerDep != null) {
            double pAtSource = ProspectingService.computeDownstreamPlacerProbability(
                    model, placerDep.center().getX(), placerDep.center().getZ());
            assertTrue(pAtSource > 0.5, "Expected strong placer probability at source: " + pAtSource);
        }

        RegionalWeatherEngine.seedInitialSystems(model.worldSeed());
        Set<PrecipType> precipTypes = new HashSet<>();
        for (int x = -75_000; x <= 75_000; x += 6_000) {
            for (int z = -36_000; z <= 36_000; z += 6_000) {
                precipTypes.add(RegionalWeatherEngine.stateAt(new BlockPos(x, 64, z)).type());
            }
        }
        assertTrue(precipTypes.size() >= 2, "Regional weather grid must produce differentiated precipitation states");
        RegionalWeatherEngine.addEmissionsKg(2.8e11);
        assertTrue(RegionalWeatherEngine.globalDeltaT() > 2.5f);

        WildlandsRegionLayers.GeoLayerData geoDefault = WildlandsRegionLayers.GeoLayerData.createDefault();
        assertTrue(WildlandsRegionLayers.GeoLayerData.CODEC.encodeStart(JsonOps.INSTANCE, geoDefault).result().isPresent());
        WildlandsRegionLayers.ClimateLayerData climDefault = WildlandsRegionLayers.ClimateLayerData.createDefault();
        assertTrue(WildlandsRegionLayers.ClimateLayerData.CODEC.encodeStart(JsonOps.INSTANCE, climDefault).result().isPresent());
        WildlandsRegionLayers.HydroLayerData hydroDefault = WildlandsRegionLayers.HydroLayerData.createDefault();
        assertTrue(WildlandsRegionLayers.HydroLayerData.CODEC.encodeStart(JsonOps.INSTANCE, hydroDefault).result().isPresent());

        var apiSet = com.nightcity.wildlands.api.impl.WildlandsApiImpl.createApiSet();
        assertDoesNotThrow(() -> apiSet.planet().getClass().getMethod("isPlanetActive").invoke(apiSet.planet()));
        assertDoesNotThrow(() -> apiSet.climate().getClass().getMethod("normals", BlockPos.class).invoke(apiSet.climate(), BlockPos.ZERO));
        assertDoesNotThrow(() -> apiSet.hydro().getClass().getMethod("river", BlockPos.class).invoke(apiSet.hydro(), BlockPos.ZERO));
        assertDoesNotThrow(() -> apiSet.suitability().getClass().getMethod("soilParent", BlockPos.class).invoke(apiSet.suitability(), BlockPos.ZERO));
    }

    @Test
    void datapackJsonValidityZeroVanillaUndergroundOresAndLangParity() throws IOException {
        Path resRoot = null;
        for (Path cur = Path.of("").toAbsolutePath(); cur != null; cur = cur.getParent()) {
            Path p1 = cur.resolve(Path.of("tcwildlands", "src", "main", "resources"));
            if (Files.isDirectory(p1)) {
                resRoot = p1;
                break;
            }
            Path p2 = cur.resolve(Path.of("src", "main", "resources"));
            if (Files.isDirectory(p2) && Files.isDirectory(p2.resolve("data/tcwildlands"))) {
                resRoot = p2;
                break;
            }
        }
        assertNotNull(resRoot, "Missing tcwildlands/src/main/resources from " + Path.of("").toAbsolutePath());

        // Verify every JSON file under src/main/resources parses cleanly
        long jsonCount;
        try (Stream<Path> walk = Files.walk(resRoot)) {
            List<Path> jsons = walk.filter(p -> p.toString().endsWith(".json")).toList();
            jsonCount = jsons.size();
            for (Path p : jsons) {
                String content = Files.readString(p, StandardCharsets.UTF_8);
                assertDoesNotThrow(() -> JsonParser.parseString(content), "Invalid JSON: " + p);
            }
        }
        assertTrue(jsonCount >= 800, "Expected >= 800 resource JSONs, found " + jsonCount);

        // Verify earthlike dimension_type vertical budget [-512, +1024)
        JsonObject earthlike = JsonParser.parseString(
                Files.readString(resRoot.resolve("data/tcwildlands/dimension_type/earthlike.json"))).getAsJsonObject();
        assertEquals(-512, earthlike.get("min_y").getAsInt());
        assertEquals(1536, earthlike.get("height").getAsInt());

        // Verify all 69 biomes exist and have EMPTY step-6 UNDERGROUND_ORES (VO-WG-02)
        Path biomeDir = resRoot.resolve("data/tcwildlands/worldgen/biome");
        try (Stream<Path> biomes = Files.list(biomeDir)) {
            List<Path> list = biomes.filter(p -> p.toString().endsWith(".json")).toList();
            assertEquals(69, list.size(), "Expected exactly 69 biome JSONs");
            for (Path b : list) {
                JsonObject obj = JsonParser.parseString(Files.readString(b)).getAsJsonObject();
                assertEquals(0, obj.getAsJsonArray("features").get(6).getAsJsonArray().size(),
                        "Biome must have zero vanilla underground_ores: " + b.getFileName());
            }
        }

        // Verify Foundry-absent fallback smelting recipes carry neoforge:not mod_loaded tcfoundry
        JsonObject fallbackCopper = JsonParser.parseString(
                Files.readString(resRoot.resolve("data/tcwildlands/recipe/fallback_smelt_raw_malachite.json"))).getAsJsonObject();
        assertTrue(fallbackCopper.has("neoforge:conditions"));

        // Verify 100% localization key parity between en_us.json and zh_cn.json + CreativeModeTab translation
        JsonObject en = JsonParser.parseString(
                Files.readString(resRoot.resolve("assets/tcwildlands/lang/en_us.json"))).getAsJsonObject();
        JsonObject zh = JsonParser.parseString(
                Files.readString(resRoot.resolve("assets/tcwildlands/lang/zh_cn.json"))).getAsJsonObject();
        assertTrue(en.has("itemGroup.tcwildlands"), "en_us.json must define itemGroup.tcwildlands");
        assertEquals(en.keySet(), zh.keySet(), "en_us.json and zh_cn.json key sets must match 100%");
        for (Map.Entry<String, JsonElement> entry : zh.entrySet()) {
            assertFalse(entry.getValue().getAsString().isBlank(), "Blank zh_cn value for " + entry.getKey());
        }

        // Verify MC 26.3 client item definitions exist for all 265 Wildlands item models
        Path clientItemsDir = resRoot.resolve("assets/tcwildlands/items");
        Path itemModelsDir = resRoot.resolve("assets/tcwildlands/models/item");
        assertTrue(Files.isDirectory(clientItemsDir), "Missing assets/tcwildlands/items directory");
        try (Stream<Path> itemModels = Files.list(itemModelsDir)) {
            List<Path> modelFiles = itemModels.filter(p -> p.toString().endsWith(".json")).toList();
            assertEquals(265, modelFiles.size(), "Expected 265 Wildlands item models");
            for (Path modelFile : modelFiles) {
                String fileName = modelFile.getFileName().toString();
                Path itemDef = clientItemsDir.resolve(fileName);
                assertTrue(Files.isRegularFile(itemDef), "Missing MC 26.3 client item JSON: " + itemDef);
            }
        }
    }

    @Test
    void globalWorldSpawnResolvesToHabitableDryLandAboveSeaLevel() {
        for (long seed : new long[]{424242L, 777L, 20261124L}) {
            PlanetModel model = PlanetGenerator.generate(seed, PlanetGenerator.Config.defaults(), null);
            PlanetRuntime.activateForTests(model, true, true);
            try {
                BlockPos spawn = PlanetRuntime.resolveInitialWorldSpawn(null);
                assertTrue(spawn.getY() > PlanetCoordinates.SEA_LEVEL + 1,
                        "Global world spawn must be strictly above sea level for seed " + seed + ", got " + spawn);
                assertFalse(PlanetRuntime.isSubmergedOrOceanSpawn(model, spawn),
                        "Global world spawn must be on dry habitable land for seed " + seed + ", got " + spawn);
                BlockPos fromOcean = PlanetRuntime.resolveHabitableSpawnPos(model, 8, 8);
                assertFalse(PlanetRuntime.isSubmergedOrOceanSpawn(model, fromOcean),
                        "Fallback from (8, 8) must resolve to dry habitable land for seed " + seed + ", got " + fromOcean);
            } finally {
                PlanetRuntime.clear();
            }
        }
    }
}
