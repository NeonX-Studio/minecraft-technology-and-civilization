package com.nightcity.tcivilization.api.event;

import com.nightcity.tcivilization.api.RegionCell;
import com.nightcity.tcivilization.api.SimTier;
import java.util.Objects;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

public final class RegionTierChangedEvent extends TcEvent {
    @Nullable
    private final ServerLevel level;
    private final RegionCell region;
    private final SimTier oldTier;
    private final SimTier newTier;

    public RegionTierChangedEvent(@Nullable ServerLevel level, RegionCell region, SimTier oldTier, SimTier newTier) {
        this.level = level;
        this.region = Objects.requireNonNull(region, "region");
        this.oldTier = Objects.requireNonNull(oldTier, "oldTier");
        this.newTier = Objects.requireNonNull(newTier, "newTier");
    }

    @Nullable
    public ServerLevel level() {
        return level;
    }

    public RegionCell region() {
        return region;
    }

    public SimTier oldTier() {
        return oldTier;
    }

    public SimTier newTier() {
        return newTier;
    }
}
