package com.nightcity.tcivilization.api.world;

import com.nightcity.tcivilization.RegionCoordinates;
import com.nightcity.tcivilization.api.TcThreads;
import com.nightcity.tcivilization.api.world.event.EmissionRecordedEvent;
import com.nightcity.tcivilization.sim.TcEventBus;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

/**
 * Core-hosted global and per-region pollutant emission ledger (03 §12.5, 10 §5.17).
 */
public final class EmissionLedger {
    public static final Identifier CO2E = Identifier.parse("tcivilization:co2e");

    private static final Map<Identifier, Double> GLOBAL_TOTALS = new ConcurrentHashMap<>();
    private static final Map<RegionPollutantKey, Double> REGION_TOTALS = new ConcurrentHashMap<>();
    private static final Map<CivPollutantKey, Double> CIV_TOTALS = new ConcurrentHashMap<>();

    private record RegionPollutantKey(long regionKey, Identifier pollutant) {
    }

    private record CivPollutantKey(UUID civ, Identifier pollutant) {
    }

    private EmissionLedger() {
    }

    public static void record(
            @Nullable ServerLevel level,
            BlockPos pos,
            Identifier pollutant,
            double amountKg,
            Identifier source
    ) {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(pollutant, "pollutant");
        Objects.requireNonNull(source, "source");
        TcThreads.assertMain();
        if (amountKg == 0.0) {
            return;
        }
        if (amountKg < 0.0 && !CO2E.equals(pollutant)) {
            return;
        }
        long regionKey = RegionCoordinates.fromBlock(pos.getX(), pos.getZ()).key();
        GLOBAL_TOTALS.merge(pollutant, amountKg, (prev, delta) -> Math.max(0.0, prev + delta));
        REGION_TOTALS.merge(new RegionPollutantKey(regionKey, pollutant), amountKg, (prev, delta) -> Math.max(0.0, prev + delta));
        TcEventBus.global().enqueue(new EmissionRecordedEvent(level, regionKey, pollutant, amountKg, source));
    }

    public static void recordForCiv(
            @Nullable ServerLevel level,
            BlockPos pos,
            @Nullable UUID civ,
            Identifier pollutant,
            double amountKg,
            Identifier source
    ) {
        record(level, pos, pollutant, amountKg, source);
        if (civ != null && amountKg != 0.0) {
            CIV_TOTALS.merge(new CivPollutantKey(civ, pollutant), amountKg, (prev, delta) -> Math.max(0.0, prev + delta));
        }
    }

    public static double globalTotalKg(Identifier pollutant) {
        Objects.requireNonNull(pollutant, "pollutant");
        return GLOBAL_TOTALS.getOrDefault(pollutant, 0.0);
    }

    public static double regionTotalKg(long regionKey, Identifier pollutant, int days) {
        Objects.requireNonNull(pollutant, "pollutant");
        if (days <= 0) {
            return 0.0;
        }
        return REGION_TOTALS.getOrDefault(new RegionPollutantKey(regionKey, pollutant), 0.0);
    }

    public static double civTotalKg(UUID civ, Identifier pollutant, int days) {
        Objects.requireNonNull(civ, "civ");
        Objects.requireNonNull(pollutant, "pollutant");
        if (days <= 0) {
            return 0.0;
        }
        return CIV_TOTALS.getOrDefault(new CivPollutantKey(civ, pollutant), 0.0);
    }

    public static void clearForTests() {
        GLOBAL_TOTALS.clear();
        REGION_TOTALS.clear();
        CIV_TOTALS.clear();
    }
}
