package com.nightcity.tcivilization.client.api;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;

public record WheelTarget(
        Kind kind,
        Optional<UUID> entityId,
        Optional<BlockPos> blockPos,
        String displayName
) {
    public enum Kind {
        NONE,
        PERSON,
        ENTITY,
        BLOCK
    }

    public WheelTarget {
        Objects.requireNonNull(kind, "kind");
        entityId = entityId == null ? Optional.empty() : entityId;
        blockPos = blockPos == null ? Optional.empty() : blockPos;
        displayName = displayName == null ? "" : displayName;
    }

    public static WheelTarget none() {
        return new WheelTarget(Kind.NONE, Optional.empty(), Optional.empty(), "");
    }
}
