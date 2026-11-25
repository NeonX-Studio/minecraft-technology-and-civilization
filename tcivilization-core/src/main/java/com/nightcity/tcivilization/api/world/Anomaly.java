package com.nightcity.tcivilization.api.world;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * Detected geological anomaly returned by ProspectingApi (10 §12.1).
 */
public record Anomaly(
        BlockPos approxCenter,
        int uncertaintyBlocks,
        AnomalyKind kind,
        float strength,
        Optional<Identifier> mineralHint
) {
    public Anomaly {
        mineralHint = mineralHint == null ? Optional.empty() : mineralHint;
    }
}
