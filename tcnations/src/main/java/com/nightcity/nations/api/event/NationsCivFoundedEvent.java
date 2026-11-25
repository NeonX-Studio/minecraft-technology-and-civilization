package com.nightcity.nations.api.event;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.Event;

public final class NationsCivFoundedEvent extends Event {
    public enum Cause {
        SEED,
        SPLIT,
        COLONY,
        REBIRTH
    }

    private final UUID civId;
    private final BlockPos capital;
    private final Cause cause;

    public NationsCivFoundedEvent(UUID civId, BlockPos capital, Cause cause) {
        this.civId = Objects.requireNonNull(civId, "civId");
        this.capital = Objects.requireNonNull(capital, "capital");
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    public UUID civId() {
        return civId;
    }

    public BlockPos capital() {
        return capital;
    }

    public Cause cause() {
        return cause;
    }
}
