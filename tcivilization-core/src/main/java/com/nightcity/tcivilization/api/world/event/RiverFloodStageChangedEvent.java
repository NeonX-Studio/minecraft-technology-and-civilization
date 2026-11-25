package com.nightcity.tcivilization.api.world.event;

import com.nightcity.tcivilization.api.event.TcEvent;
import com.nightcity.tcivilization.api.world.FloodStage;
import java.util.Objects;
import net.minecraft.core.BlockPos;

/**
 * Fired when a river reach transitions flood stage (10 §12.2).
 */
public final class RiverFloodStageChangedEvent extends TcEvent {
    private final long reachId;
    private final FloodStage from;
    private final FloodStage to;
    private final BlockPos at;

    public RiverFloodStageChangedEvent(long reachId, FloodStage from, FloodStage to, BlockPos at) {
        this.reachId = reachId;
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        this.at = Objects.requireNonNull(at, "at");
    }

    public long reachId() {
        return reachId;
    }

    public FloodStage from() {
        return from;
    }

    public FloodStage to() {
        return to;
    }

    public BlockPos at() {
        return at;
    }
}
