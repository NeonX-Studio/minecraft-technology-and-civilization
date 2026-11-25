package com.nightcity.tcivilization.api.event;

import com.nightcity.tcivilization.api.RegionCell;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public final class RegionFirstVisitEvent extends TcEvent {
    private final UUID civId;
    private final RegionCell region;
    @Nullable
    private final UUID actorId;

    public RegionFirstVisitEvent(UUID civId, RegionCell region, @Nullable UUID actorId) {
        this.civId = Objects.requireNonNull(civId, "civId");
        this.region = Objects.requireNonNull(region, "region");
        this.actorId = actorId;
    }

    public UUID civId() {
        return civId;
    }

    public RegionCell region() {
        return region;
    }

    @Nullable
    public UUID actorId() {
        return actorId;
    }
}
