package com.nightcity.tcivilization.api.event;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.resources.Identifier;

public final class ChronicleFeatEvent extends TcEvent {
    private final UUID civId;
    private final Identifier featId;
    private final int importance;
    private final List<UUID> contributors;

    public ChronicleFeatEvent(UUID civId, Identifier featId, int importance, List<UUID> contributors) {
        this.civId = Objects.requireNonNull(civId, "civId");
        this.featId = Objects.requireNonNull(featId, "featId");
        this.importance = importance;
        this.contributors = contributors == null ? List.of() : List.copyOf(contributors);
    }

    public UUID civId() {
        return civId;
    }

    public Identifier featId() {
        return featId;
    }

    public int importance() {
        return importance;
    }

    public List<UUID> contributors() {
        return contributors;
    }
}
