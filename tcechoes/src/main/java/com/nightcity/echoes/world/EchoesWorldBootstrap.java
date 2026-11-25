package com.nightcity.echoes.world;

import com.mojang.logging.LogUtils;
import com.nightcity.echoes.EchoesConfig;
import com.nightcity.echoes.TEchoes;
import com.nightcity.echoes.api.AncientMine;
import com.nightcity.echoes.api.Culture;
import com.nightcity.echoes.api.HistoryCell;
import com.nightcity.echoes.api.Polity;
import com.nightcity.echoes.api.PolityFate;
import com.nightcity.echoes.api.Route;
import com.nightcity.echoes.api.Site;
import com.nightcity.echoes.api.event.WorldHistoryReadyEvent;
import com.nightcity.echoes.api.impl.WorldHistoryViewImpl;
import com.nightcity.echoes.compat.tcnations.TcnationsBridge;
import com.nightcity.echoes.compat.tcwildlands.TcwildlandsBridge;
import com.nightcity.echoes.data.EchoesRegionLayers;
import com.nightcity.echoes.data.EchoesSavedData;
import com.nightcity.echoes.sim.HistoryBinIo;
import com.nightcity.echoes.sim.HistoryTerrainInput;
import com.nightcity.echoes.sim.WorldHistoryData;
import com.nightcity.echoes.sim.WorldHistorySimulator;
import com.nightcity.tcivilization.RegionCoordinates;
import com.nightcity.tcivilization.TcServerContext;
import com.nightcity.tcivilization.api.Civilization;
import com.nightcity.tcivilization.api.CivilizationApi;
import com.nightcity.tcivilization.api.RegionApi;
import com.nightcity.tcivilization.api.RegionHandle;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Coordinates world-history pre-simulation or history.bin loading on world startup,
 * registers read-only HISTORIC civilizations via CivilizationApi.createHistoricCivilization,
 * and writes site/route/mine/weathering layers into tcechoes:history (18 §5.2.2, §12.1).
 * P1 intentionally does NOT place worldgen block structures.
 */
public final class EchoesWorldBootstrap {
    private static final Logger LOGGER = LogUtils.getLogger();

    public record BootstrapResult(
            WorldHistoryViewImpl view,
            boolean freshlyGenerated,
            int historicCivsCreated,
            int populatedRegionCount
    ) {
    }

    private EchoesWorldBootstrap() {
    }

    public static BootstrapResult initializeServer(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        ServerLevel overworld = server.overworld();
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        Path historyFile = HistoryBinIo.resolvePath(worldRoot);
        EchoesSavedData savedData = EchoesSavedData.get(overworld);

        WorldHistoryData historyData;
        boolean freshlyGenerated;

        if (Files.isRegularFile(historyFile)) {
            try {
                historyData = HistoryBinIo.read(historyFile);
                freshlyGenerated = false;
            } catch (IOException ex) {
                LOGGER.error("Failed loading existing {}: {}", historyFile, ex.getMessage());
                throw new IllegalStateException("Corrupt or unreadable tc/history.bin", ex);
            }
        } else {
            long worldSeed = overworld.getSeed();
            BlockPos spawnPos = overworld.getRespawnData().pos();
            int spawnRx = RegionCoordinates.regionX(spawnPos.getX());
            int spawnRz = RegionCoordinates.regionZ(spawnPos.getZ());
            int standaloneRadius = EchoesConfig.standaloneRadiusRegions();

            HistoryTerrainInput terrain = TcwildlandsBridge.resolveTerrainInput(overworld, worldRoot, worldSeed)
                    .orElseGet(() -> HistoryTerrainInput.fromServerLevelStandalone(
                            overworld, worldSeed, spawnRx, spawnRz, standaloneRadius
                    ));

            int survivorCount = TcnationsBridge.resolveSurvivorCount();
            boolean nationsLoaded = TcnationsBridge.isNationsLoaded();
            WorldHistorySimulator.Config simCfg = new WorldHistorySimulator.Config(
                    worldSeed,
                    EchoesConfig.cultureCount(),
                    EchoesConfig.maxSites(),
                    survivorCount,
                    EchoesConfig.threadCount(),
                    nationsLoaded
            );

            historyData = WorldHistorySimulator.simulate(terrain, simCfg);
            freshlyGenerated = true;
            try {
                HistoryBinIo.writeAtomic(historyFile, historyData);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed writing tc/history.bin", ex);
            }
        }

        CivilizationApi civApi = CivilizationApi.get(server);
        RegionApi regionApi = TcServerContext.get(server).regions();
        BootstrapResult result = applyHistoryToWorld(
                historyData, freshlyGenerated, civApi, regionApi, Level.OVERWORLD, savedData
        );

        TcwildlandsBridge.submitHistoricalTraces(historyData.historicalTraces());
        TEchoes.installView(result.view());
        NeoForge.EVENT_BUS.post(new WorldHistoryReadyEvent(server, result.view(), freshlyGenerated));
        LOGGER.info(
                "Echoes history ready (mode={}, freshlyGenerated={}, cultures={}, polities={}, sites={}, routes={}, mines={}, historicCivsCreated={}, regions={})",
                historyData.mode(), freshlyGenerated, historyData.cultures().size(), historyData.polities().size(),
                historyData.sites().size(), historyData.routes().size(), historyData.mines().size(),
                result.historicCivsCreated(), result.populatedRegionCount()
        );
        return result;
    }

    /**
     * Pure server/test-callable bootstrap applying WorldHistoryData onto CivilizationApi, RegionApi, and EchoesSavedData.
     */
    public static BootstrapResult applyHistoryToWorld(
            WorldHistoryData historyData,
            boolean freshlyGenerated,
            CivilizationApi civApi,
            RegionApi regionApi,
            ResourceKey<Level> dimension,
            EchoesSavedData savedData
    ) {
        Objects.requireNonNull(historyData, "historyData");
        Objects.requireNonNull(civApi, "civApi");
        Objects.requireNonNull(regionApi, "regionApi");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(savedData, "savedData");

        Map<Integer, Culture> cultureMap = new LinkedHashMap<>();
        for (Culture c : historyData.cultures()) {
            cultureMap.put(c.id(), c);
        }

        int createdHistoricCivs = 0;
        for (Polity p : historyData.polities()) {
            if (p.fate() != PolityFate.EXTINCT) {
                continue;
            }
            Optional<UUID> existingId = savedData.historicCivId(p.id());
            if (existingId.isPresent() && civApi.civ(existingId.get()).isPresent()) {
                continue;
            }
            Culture cult = cultureMap.get(p.cultureId());
            Identifier cultId = cult != null
                    ? cult.identifier()
                    : Identifier.fromNamespaceAndPath(TEchoes.MOD_ID, "culture_" + p.cultureId());
            Civilization created = civApi.createHistoricCivilization(
                    p.name(), cultId, BannerPatternLayers.EMPTY, p.endYear()
            );
            savedData.putHistoricCivId(p.id(), created.id());
            createdHistoricCivs++;
        }

        Map<Long, HistoryCell> cells = buildHistoryCells(historyData);
        int writtenRegions = 0;
        if (!savedData.isRegionsPopulated()) {
            for (Map.Entry<Long, HistoryCell> entry : cells.entrySet()) {
                long rk = entry.getKey();
                int rx = (int) (rk >> 32);
                int rz = (int) rk;
                Optional<RegionHandle> handleOpt = regionApi.get(dimension, rx, rz);
                if (handleOpt.isPresent()) {
                    regionApi.update(handleOpt.get(), EchoesRegionLayers.HISTORY_LAYER, prev -> entry.getValue());
                    writtenRegions++;
                }
            }
            savedData.setRegionsPopulated(true);
        }

        savedData.markHistoryInitialized(historyData.historySeed(), historyData.generatorVersion(), historyData.mode());
        WorldHistoryViewImpl view = new WorldHistoryViewImpl(historyData, savedData.historicCivIds());
        return new BootstrapResult(view, freshlyGenerated, createdHistoricCivs, writtenRegions);
    }

    /**
     * Aggregates simulated sites, routes, ancient mines, hazards, and weathering factors into per-region HistoryCells.
     */
    public static Map<Long, HistoryCell> buildHistoryCells(WorldHistoryData history) {
        Map<Integer, Site> siteById = new LinkedHashMap<>();
        TreeMap<Long, List<HistoryCell.SiteEntry>> sitesByReg = new TreeMap<>();
        for (Site s : history.sites()) {
            siteById.put(s.id(), s);
            List<HistoryCell.SiteEntry> list = sitesByReg.computeIfAbsent(s.regionKey(), k -> new ArrayList<>());
            if (list.size() < HistoryCell.MAX_SITES_PER_CELL) {
                list.add(HistoryCell.SiteEntry.fromSite(s));
            }
        }

        TreeMap<Long, List<HistoryCell.RouteEntry>> routesByReg = new TreeMap<>();
        for (Route r : history.routes()) {
            HistoryCell.RouteEntry entry = new HistoryCell.RouteEntry(r.id(), r.kind(), r.weathering());
            for (long rk : r.points()) {
                List<HistoryCell.RouteEntry> list = routesByReg.computeIfAbsent(rk, k -> new ArrayList<>());
                if (list.size() < 16 && list.stream().noneMatch(e -> e.routeId() == r.id())) {
                    list.add(entry);
                }
            }
        }

        TreeMap<Long, List<HistoryCell.MineEntry>> minesByReg = new TreeMap<>();
        for (AncientMine m : history.mines()) {
            List<HistoryCell.MineEntry> list = minesByReg.computeIfAbsent(m.regionKey(), k -> new ArrayList<>());
            if (list.size() < 8) {
                list.add(HistoryCell.MineEntry.fromMine(m));
            }
        }

        TreeMap<Long, WorldHistoryData.RegionSummaryEntry> summaryByReg = new TreeMap<>();
        for (WorldHistoryData.RegionSummaryEntry e : history.regionIndex()) {
            summaryByReg.put(e.regionKey(), e);
        }

        TreeMap<Long, Boolean> allKeys = new TreeMap<>();
        for (Long k : summaryByReg.keySet()) allKeys.put(k, Boolean.TRUE);
        for (Long k : sitesByReg.keySet()) allKeys.put(k, Boolean.TRUE);
        for (Long k : routesByReg.keySet()) allKeys.put(k, Boolean.TRUE);
        for (Long k : minesByReg.keySet()) allKeys.put(k, Boolean.TRUE);

        Map<Long, HistoryCell> result = new LinkedHashMap<>(allKeys.size());
        for (Long rk : allKeys.keySet()) {
            WorldHistoryData.RegionSummaryEntry sum = summaryByReg.get(rk);
            List<HistoryCell.SiteEntry> sList = sitesByReg.getOrDefault(rk, List.of());
            List<HistoryCell.RouteEntry> rList = routesByReg.getOrDefault(rk, List.of());
            List<HistoryCell.MineEntry> mList = minesByReg.getOrDefault(rk, List.of());
            byte haz = sum != null ? sum.hazardFlags() : 0;
            short dom = sum != null ? sum.dominantCulture() : -1;
            short last = sum != null ? sum.lastPolity() : -1;
            float kClim = sum != null ? sum.climateWeatheringFactor() : 1.2F;
            float sed = sum != null ? sum.sedimentRate() : 1.0F;
            List<Integer> legends = sum != null ? sum.legendSeedIds() : List.of();
            HistoryCell cell = new HistoryCell(
                    HistoryCell.FORMAT_VERSION,
                    sList,
                    rList,
                    mList,
                    haz,
                    dom,
                    last,
                    kClim,
                    sed,
                    legends
            );
            if (!cell.isEmpty()) {
                result.put(rk, cell);
            }
        }
        return result;
    }
}
