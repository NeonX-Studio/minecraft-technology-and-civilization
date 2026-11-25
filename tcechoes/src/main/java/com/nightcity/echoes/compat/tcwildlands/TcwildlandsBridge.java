package com.nightcity.echoes.compat.tcwildlands;

import com.nightcity.echoes.sim.HistoryTerrainInput;
import com.nightcity.echoes.sim.WorldHistoryData;
import com.nightcity.tcivilization.api.world.HistoricalTrace;
import com.nightcity.tcivilization.api.world.TcWorldApis;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;

/**
 * Soft-dependency bridge to tcwildlands via core-hosted TcWorldApis (18 §3.4, §5.1.1, 02 §1.4).
 */
public final class TcwildlandsBridge {
    @FunctionalInterface
    public interface PlanetTerrainProvider {
        Optional<HistoryTerrainInput> create(ServerLevel overworld, Path worldRoot, long worldSeed);
    }

    private static volatile boolean initialized;
    private static volatile PlanetTerrainProvider customProvider;
    private static final List<WorldHistoryData.HistoricalTraceRecord> SUBMITTED_TRACES = new CopyOnWriteArrayList<>();

    private TcwildlandsBridge() {
    }

    public static void init() {
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static boolean isWildlandsLoaded() {
        if (TcWorldApis.isProvidedBy("tcwildlands")) {
            return true;
        }
        ModList list = ModList.get();
        return list != null && list.isLoaded("tcwildlands");
    }

    public static void setCustomProvider(PlanetTerrainProvider provider) {
        customProvider = provider;
    }

    public static Optional<HistoryTerrainInput> resolveTerrainInput(
            ServerLevel overworld,
            Path worldRoot,
            long worldSeed
    ) {
        PlanetTerrainProvider provider = customProvider;
        if (provider != null) {
            return provider.create(overworld, worldRoot, worldSeed);
        }
        if (!isWildlandsLoaded()) {
            return Optional.empty();
        }
        Path planetBin = worldRoot != null ? worldRoot.resolve("tc").resolve("planet.bin") : null;
        boolean planetFilePresent = planetBin != null && Files.isRegularFile(planetBin);
        if (TcWorldApis.planet().isPlanetActive() || planetFilePresent || isEarthlikeDimension(overworld)) {
            return Optional.of(HistoryTerrainInput.planetSynthetic(worldSeed));
        }
        return Optional.empty();
    }

    public static void submitHistoricalTraces(List<WorldHistoryData.HistoricalTraceRecord> traces) {
        SUBMITTED_TRACES.clear();
        if (traces == null || traces.isEmpty()) {
            return;
        }
        SUBMITTED_TRACES.addAll(traces);
        if (!isWildlandsLoaded()) {
            return;
        }
        for (WorldHistoryData.HistoricalTraceRecord tr : traces) {
            try {
                HistoricalTrace.Kind kind = HistoricalTrace.Kind.valueOf(tr.kind().name());
                OptionalLong depId = parseDepositLongId(tr.depositId());
                TcWorldApis.geo().recordHistoricalTrace(
                        new HistoricalTrace(kind, tr.pos(), tr.radiusBlocks(), tr.year(), depId, tr.minedFraction())
                );
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static OptionalLong parseDepositLongId(String depositId) {
        if (depositId == null || depositId.isBlank()) {
            return OptionalLong.empty();
        }
        try {
            return OptionalLong.of(Long.parseLong(depositId));
        } catch (NumberFormatException ignored) {
            return OptionalLong.of(Integer.toUnsignedLong(depositId.hashCode()));
        }
    }

    public static List<WorldHistoryData.HistoricalTraceRecord> submittedTraces() {
        return List.copyOf(SUBMITTED_TRACES);
    }

    private static boolean isEarthlikeDimension(ServerLevel level) {
        if (level == null) {
            return false;
        }
        try {
            String dimType = level.dimensionTypeRegistration().unwrapKey()
                    .map(k -> k.identifier().toString())
                    .orElse("");
            return "tcwildlands:earthlike".equals(dimType);
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}