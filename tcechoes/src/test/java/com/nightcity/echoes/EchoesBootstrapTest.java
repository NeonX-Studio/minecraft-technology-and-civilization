package com.nightcity.echoes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nightcity.echoes.api.EchoesApi;
import com.nightcity.echoes.api.HistoryCell;
import com.nightcity.echoes.api.Polity;
import com.nightcity.echoes.api.PolityFate;
import com.nightcity.echoes.api.Site;
import com.nightcity.echoes.api.SiteSummary;
import com.nightcity.echoes.api.SurvivorPolitySnapshot;
import com.nightcity.echoes.api.WorldHistoryView;
import com.nightcity.echoes.compat.tcnations.TcnationsBridge;
import com.nightcity.echoes.data.EchoesRegionLayers;
import com.nightcity.echoes.data.EchoesSavedData;
import com.nightcity.echoes.sim.HistoryTerrainInput;
import com.nightcity.echoes.sim.WorldHistoryData;
import com.nightcity.echoes.sim.WorldHistorySimulator;
import com.nightcity.echoes.world.EchoesWorldBootstrap;
import com.nightcity.tcivilization.CivilizationSavedData;
import com.nightcity.tcivilization.api.CivKind;
import com.nightcity.tcivilization.api.Civilization;
import com.nightcity.tcivilization.api.RegionGridRegistry;
import com.nightcity.tcivilization.api.RegionHandle;
import com.nightcity.tcivilization.api.RegionLayer;
import com.nightcity.tcivilization.api.TcThreads;
import com.nightcity.tcivilization.region.RegionLayerRegistry;
import com.nightcity.tcivilization.region.RegionManager;
import com.nightcity.tcivilization.region.TcrRegionFile;
import com.nightcity.tcivilization.sim.TcEventBus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class EchoesBootstrapTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void cleanup() {
        TEchoes.resetView();
        TcThreads.bindMainThread(null);
    }

    @Test
    void worldBootstrapCreatesHistoricCivilizationsPopulatesRegionLayerAndBuildsStatus() throws IOException {
        TcThreads.bindMainThread(Thread.currentThread());
        RegionLayerRegistry layerRegistry = new RegionLayerRegistry();
        layerRegistry.register(EchoesRegionLayers.HISTORY_LAYER);

        TcEventBus bus = new TcEventBus(e -> {});
        CivilizationSavedData civData = new CivilizationSavedData(bus);
        EchoesSavedData echoesSaved = new EchoesSavedData();

        long seed = 135792468L;
        HistoryTerrainInput standalone = HistoryTerrainInput.standaloneSynthetic(seed, 0, 0, 24);
        WorldHistoryData history = WorldHistorySimulator.simulate(
                standalone, new WorldHistorySimulator.Config(seed, 12, 12_000, 8, 2, false)
        );
        long expectedExtinctCount = history.polities().stream().filter(p -> p.fate() == PolityFate.EXTINCT).count();
        assertTrue(expectedExtinctCount > 0);

        Path worldRoot = tempDir.resolve("world_bootstrap");
        try (RegionManager regionManager = new RegionManager(
                worldRoot, 1, 256, RegionGridRegistry.defaults(), layerRegistry, bus, () -> 0L
        )) {
            EchoesWorldBootstrap.BootstrapResult boot1 = EchoesWorldBootstrap.applyHistoryToWorld(
                    history, true, civData, regionManager, Level.OVERWORLD, echoesSaved
            );
            TEchoes.installView(boot1.view());

            assertEquals(expectedExtinctCount, boot1.historicCivsCreated());
            assertTrue(boot1.populatedRegionCount() > 0);

            for (Polity p : history.polities()) {
                if (p.fate() == PolityFate.EXTINCT) {
                    Optional<UUID> civId = boot1.view().historicCivilizationId(p.id());
                    assertTrue(civId.isPresent());
                    Civilization civ = civData.civ(civId.get()).orElseThrow();
                    assertEquals(CivKind.HISTORIC, civ.kind());
                    String originalName = civ.name();
                    civData.renameCivilization(civId.get(), "MutatedName");
                    assertEquals(originalName, civData.civ(civId.get()).orElseThrow().name());
                }
            }

            EchoesWorldBootstrap.BootstrapResult boot2 = EchoesWorldBootstrap.applyHistoryToWorld(
                    history, false, civData, regionManager, Level.OVERWORLD, echoesSaved
            );
            assertEquals(0, boot2.historicCivsCreated(), "Reload must be idempotent without duplicating HISTORIC civs");

            WorldHistoryView view = EchoesApi.get().view();
            assertTrue(view.isLoaded());
            List<SurvivorPolitySnapshot> survSnaps = TcnationsBridge.survivorSnapshots();
            assertEquals(view.survivors().size(), survSnaps.size());
            if (survSnaps.size() >= 2) {
                assertTrue(survSnaps.get(0).finalPopulation() >= survSnaps.get(1).finalPopulation());
            }

            List<SiteSummary> spawnSites = view.sitesNear(new BlockPos(256, 64, 256), 1_000);
            assertTrue(spawnSites.size() >= 3);

            Site sampleSite = history.sites().getFirst();
            RegionHandle handle = regionManager.get(Level.OVERWORLD, sampleSite.regionX(), sampleSite.regionZ()).orElseThrow();
            HistoryCell sampleCell = handle.layer(EchoesRegionLayers.HISTORY_LAYER);
            assertFalse(sampleCell.sites().isEmpty());

            Tag cellNbt = HistoryCell.CODEC.encodeStart(NbtOps.INSTANCE, sampleCell).getOrThrow();
            assertTrue(TcrRegionFile.serializedTagSize(cellNbt) <= RegionLayer.MAX_SERIALIZED_BYTES);

            List<Component> statusLines = TEchoesEvents.buildStatusMessages(
                    view, echoesSaved.historicCivIds().size(), sampleSite.regionX(), sampleSite.regionZ(), sampleCell
            );
            assertEquals(5, statusLines.size());
            regionManager.flushAllSync();
        }

        Site checkSite = history.sites().getFirst();
        try (RegionManager reloadedRegions = new RegionManager(
                worldRoot, 1, 256, RegionGridRegistry.defaults(), layerRegistry, bus, () -> 100L
        )) {
            RegionHandle h = reloadedRegions.get(Level.OVERWORLD, checkSite.regionX(), checkSite.regionZ()).orElseThrow();
            HistoryCell restoredCell = h.layer(EchoesRegionLayers.HISTORY_LAYER);
            assertTrue(restoredCell.sites().stream().anyMatch(e -> e.siteId() == checkSite.id()));
        }

        String enJson;
        String zhJson;
        try (var enIn = TEchoes.class.getResourceAsStream("/assets/tcechoes/lang/en_us.json");
             var zhIn = TEchoes.class.getResourceAsStream("/assets/tcechoes/lang/zh_cn.json")) {
            assertNotNull(enIn, "Missing /assets/tcechoes/lang/en_us.json on classpath");
            assertNotNull(zhIn, "Missing /assets/tcechoes/lang/zh_cn.json on classpath");
            enJson = new String(enIn.readAllBytes(), StandardCharsets.UTF_8);
            zhJson = new String(zhIn.readAllBytes(), StandardCharsets.UTF_8);
        }
        JsonObject en = JsonParser.parseString(enJson).getAsJsonObject();
        JsonObject zh = JsonParser.parseString(zhJson).getAsJsonObject();
        assertEquals(en.keySet(), zh.keySet());
        assertTrue(en.has("command.tcechoes.status.summary"));
        assertTrue(zh.has("command.tcechoes.status.region"));
    }
}
