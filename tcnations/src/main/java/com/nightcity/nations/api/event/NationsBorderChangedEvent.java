package com.nightcity.nations.api.event;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

public final class NationsBorderChangedEvent extends Event {
    private final ResourceKey<Level> dimension;
    private final long regionKey;
    private final Optional<UUID> oldOwner;
    private final Optional<UUID> newOwner;

    public NationsBorderChangedEvent(
            ResourceKey<Level> dimension,
            long regionKey,
            Optional<UUID> oldOwner,
            Optional<UUID> newOwner
    ) {
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.regionKey = regionKey;
        this.oldOwner = oldOwner == null ? Optional.empty() : oldOwner;
        this.newOwner = newOwner == null ? Optional.empty() : newOwner;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public long regionKey() {
        return regionKey;
    }

    public Optional<UUID> oldOwner() {
        return oldOwner;
    }

    public Optional<UUID> newOwner() {
        return newOwner;
    }
}
