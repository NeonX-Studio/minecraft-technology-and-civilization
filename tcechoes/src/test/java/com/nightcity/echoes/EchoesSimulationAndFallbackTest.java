package com.nightcity.echoes;

import com.nightcity.echoes.api.AncientMine;
import com.nightcity.echoes.api.LegendKind;
import com.nightcity.echoes.api.PolityFate;
import com.nightcity.echoes.api.Route;
import com.nightcity.echoes.api.SimulationMode;
import com.nightcity.echoes.api.Site;
import com.nightcity.echoes.compat.tcwildlands.TcwildlandsBridge;
import com.nightcity.echoes.sim.HistoryBinIo;
import com.nightcity.echoes.sim.HistoryTerrainInput;
import com.nightcity.echoes.sim.WorldHistoryData;
import com.nightcity.echoes.sim.WorldHistorySimulator;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class EchoesSimulationAndFallbackTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void reset() {
        TEchoes.resetView();
        TcwildlandsBridge.setCustomProvider(null);
    }

    @Test
    void fixedSeedProducesByteIdenticalHistoryBinAcrossRunsAndThreadCounts() throws IOException {
        long seed = 202611230001L;
        HistoryTerrainInput terrain1 = HistoryTerrainInput.standaloneSynthetic(seed, 0, 0, 24);
        HistoryTerrainInput terrain2 = HistoryTerrainInput.standaloneSynthetic(seed, 0, 0, 24);

        WorldHistoryData runSingleThread = WorldHistorySimulator.simulate(
                terrain1, new WorldHistorySimulator.Config(seed, 12, 12_000, 8, 1, false)
        );
        WorldHistoryData runMultiThread = WorldHistorySimulator.simulate(
                terrain2, new WorldHistorySimulator.Config(seed, 12, 12_000, 8, 4, false)
        );

        byte[] bytes1 = HistoryBinIo.encodeToBytes(runSingleThread);
        byte[] bytes2 = HistoryBinIo.encodeToBytes(runMultiThread);
        assertArrayEquals(bytes1, bytes2,
                "Fixed seed must produce byte-for-byte identical history.bin across runs and thread counts");

        long otherSeed = 202611239999L;
        WorldHistoryData runOtherSeed = WorldHistorySimulator.simulate(
                HistoryTerrainInput.standaloneSynthetic(otherSeed, 0, 0, 24),
                new WorldHistorySimulator.Config(otherSeed, 12, 12_000, 8, 2, false)
        );
        byte[] otherBytes = HistoryBinIo.encodeToBytes(runOtherSeed);
        assertFalse(Arrays.equals(bytes1, otherBytes), "Distinct seeds should produce distinct history.bin bytes");
    }

    @Test
    void planetMode5000YearSimulationCompletesWithin30SecondsAndProducesCompleteHistory() throws IOException {
        long seed = 42424242L;
        long t0 = System.nanoTime();
        HistoryTerrainInput planetTerrain = HistoryTerrainInput.planetSynthetic(seed);
        assertEquals(SimulationMode.PLANET, planetTerrain.mode());
        assertEquals(320, planetTerrain.width());
        assertEquals(160, planetTerrain.height());

        WorldHistoryData data = WorldHistorySimulator.simulate(
                planetTerrain, new WorldHistorySimulator.Config(seed, 12, 12_000, 8, 4, true)
        );
        long elapsedMs = (System.nanoTime() - t0) / 1_000_000L;

        assertTrue(elapsedMs <= 30_000L, "5,000-year planet simulation exceeded 30s budget: " + elapsedMs + " ms");
        assertEquals(-5_000, data.startYear());
        assertEquals(-1, data.endYear());
        assertEquals(12, data.cultures().size());
        assertTrue(data.polities().size() >= 12, "Expected expansions/splits beyond initial hearths");
        assertFalse(data.events().isEmpty(), "Expected non-empty historical event graph");
        assertFalse(data.edges().isEmpty(), "Expected causal/relational edges in event graph");
        assertFalse(data.sites().isEmpty(), "Expected simulated ruin sites");
        assertFalse(data.routes().isEmpty(), "Expected ancient overland/sea trade routes");
        assertFalse(data.mines().isEmpty(), "Expected ancient mines near mineral deposits");
        assertFalse(data.legendSeeds().isEmpty(), "Expected legend seeds derived from events and sites");

        long survivorCount = data.polities().stream().filter(p -> p.fate() == PolityFate.SURVIVOR).count();
        long extinctCount = data.polities().stream().filter(p -> p.fate() == PolityFate.EXTINCT).count();
        assertTrue(survivorCount >= 1 && survivorCount <= 8, "Survivor count out of bounds: " + survivorCount);
        assertTrue(extinctCount >= 1, "Expected at least one extinct ancient polity");

        for (Site site : data.sites()) {
            assertTrue(site.weathering() >= 0.0F && site.weathering() <= 1.0F);
            assertTrue(site.burialDepth() >= 0 && site.burialDepth() <= 6);
        }

        byte[] encoded = HistoryBinIo.encodeToBytes(data);
        assertTrue(encoded.length <= 4 * 1024 * 1024, "Compressed history.bin exceeded 4 MB budget: " + encoded.length);

        byte[] secondRun = HistoryBinIo.encodeToBytes(WorldHistorySimulator.simulate(
                HistoryTerrainInput.planetSynthetic(seed),
                new WorldHistorySimulator.Config(seed, 12, 12_000, 8, 1, true)
        ));
        assertArrayEquals(encoded, secondRun, "Planet mode history.bin must also be byte-identical across runs");
    }

    @Test
    void standaloneFallbackWindowWithoutWildlandsCovers49x49RegionsAndApproximatesBiomes() {
        assertEquals(1.0D, HistoryTerrainInput.classifyBiomeKey("minecraft:plains").weight(), 1e-6);
        assertEquals(1.0D, HistoryTerrainInput.classifyBiomeKey("minecraft:river").weight(), 1e-6);
        assertEquals(0.7D, HistoryTerrainInput.classifyBiomeKey("minecraft:taiga").weight(), 1e-6);
        assertEquals(0.7D, HistoryTerrainInput.classifyBiomeKey("minecraft:savanna").weight(), 1e-6);
        assertEquals(0.5D, HistoryTerrainInput.classifyBiomeKey("minecraft:jungle").weight(), 1e-6);
        assertEquals(0.2D, HistoryTerrainInput.classifyBiomeKey("minecraft:badlands").weight(), 1e-6);
        assertEquals(0.2D, HistoryTerrainInput.classifyBiomeKey("minecraft:jagged_peaks").weight(), 1e-6);
        assertEquals(0.0D, HistoryTerrainInput.classifyBiomeKey("minecraft:deep_ocean").weight(), 1e-6);

        assertTrue(TcwildlandsBridge.resolveTerrainInput(null, tempDir, 777L).isEmpty(),
                "Without Wildlands loaded, bridge must return empty so standalone 49x49 window fallback activates");

        int spawnRx = 6;
        int spawnRz = -4;
        int radius = 24;
        HistoryTerrainInput standalone = HistoryTerrainInput.standaloneSynthetic(777L, spawnRx, spawnRz, radius);
        assertEquals(SimulationMode.STANDALONE, standalone.mode());
        assertEquals(49, standalone.width());
        assertEquals(49, standalone.height());

        long t0 = System.nanoTime();
        WorldHistoryData data = WorldHistorySimulator.simulate(
                standalone, new WorldHistorySimulator.Config(777L, 12, 12_000, 8, 2, false)
        );
        long elapsedMs = (System.nanoTime() - t0) / 1_000_000L;
        assertTrue(elapsedMs <= 8_000L, "Standalone 49x49 simulation exceeded 8s budget: " + elapsedMs + " ms");

        for (Site s : data.sites()) {
            assertTrue(s.regionX() >= spawnRx - radius && s.regionX() <= spawnRx + radius);
            assertTrue(s.regionZ() >= spawnRz - radius && s.regionZ() <= spawnRz + radius);
        }
        for (Route r : data.routes()) {
            for (long rk : r.points()) {
                int rx = (int) (rk >> 32);
                int rz = (int) rk;
                assertTrue(rx >= spawnRx - radius && rx <= spawnRx + radius);
                assertTrue(rz >= spawnRz - radius && rz <= spawnRz + radius);
            }
        }
        for (AncientMine m : data.mines()) {
            int rx = Math.floorDiv(m.entrance().getX(), 512);
            int rz = Math.floorDiv(m.entrance().getZ(), 512);
            assertTrue(rx >= spawnRx - radius && rx <= spawnRx + radius);
            assertTrue(rz >= spawnRz - radius && rz <= spawnRz + radius);
        }
        assertTrue(data.legendSeeds().stream().anyMatch(ls -> ls.kind() == LegendKind.ORIGIN));
    }
}
