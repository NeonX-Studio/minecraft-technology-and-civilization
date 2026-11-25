package com.nightcity.tcivilization.api.world;

import com.nightcity.tcivilization.api.annotation.AnyThread;
import com.nightcity.tcivilization.api.annotation.MainThread;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Core-hosted geology, stratigraphy, mineral deposit, and reservoir contract (10 §5.10, §12.1).
 */
public interface GeoApi {
    @AnyThread
    Identifier rockAt(int x, int y, int z);

    @AnyThread
    StrataColumn strata(int x, int z);

    @AnyThread
    GeoProvince province(int x, int z);

    @AnyThread
    float heatFlow(BlockPos pos);

    @AnyThread
    float seismicHazardPga(BlockPos pos);

    @AnyThread
    Optional<Reservoir> reservoirAt(BlockPos pos);

    @MainThread
    long extract(long reservoirId, long amountMb, boolean simulate);

    @AnyThread
    DepositSummary depositSummary(long regionKey, Optional<UUID> civFilter);

    List<Deposit> knownDeposits(UUID civ, BlockPos center, int radius);

    @MainThread
    void markDiscovered(long depositId, UUID civ, Optional<ServerPlayer> by, ProspectMethod method);

    @MainThread
    void recordHistoricalTrace(HistoricalTrace trace);
}
