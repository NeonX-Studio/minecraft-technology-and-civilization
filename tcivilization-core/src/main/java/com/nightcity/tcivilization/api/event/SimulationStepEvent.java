package com.nightcity.tcivilization.api.event;

import com.nightcity.tcivilization.api.RegionCell;
import com.nightcity.tcivilization.api.SimTier;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class SimulationStepEvent extends TcEvent {
    private final SimTier tier;
    @Nullable
    private final RegionCell region;

    public SimulationStepEvent(SimTier tier, @Nullable RegionCell region) {
        this.tier = Objects.requireNonNull(tier, "tier");
        this.region = region;
    }

    public SimTier tier() {
        return tier;
    }

    @Nullable
    public RegionCell region() {
        return region;
    }
}
