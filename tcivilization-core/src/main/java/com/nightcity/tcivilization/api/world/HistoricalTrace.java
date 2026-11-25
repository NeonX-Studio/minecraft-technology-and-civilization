package com.nightcity.tcivilization.api.world;

import java.util.Objects;
import java.util.OptionalLong;
import net.minecraft.core.BlockPos;

/**
 * Historical geomorphological or ancient mining trace submitted by Echoes before WorldHistoryReadyEvent (10 §12.1).
 */
public record HistoricalTrace(
        Kind kind,
        BlockPos pos,
        int radius,
        int year,
        OptionalLong depositId,
        float minedFraction
) {
    public enum Kind {
        FAULT_SCARP,
        ASH_LAYER,
        FLOOD_TERRACE,
        ANCIENT_WORKINGS
    }

    public HistoricalTrace {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(pos, "pos");
        depositId = depositId == null ? OptionalLong.empty() : depositId;
    }
}
