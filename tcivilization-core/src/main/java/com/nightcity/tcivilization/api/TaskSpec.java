package com.nightcity.tcivilization.api;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;

public record TaskSpec(Identifier type, Optional<GlobalPos> targetPos, int durationTicks) {
    public TaskSpec {
        Objects.requireNonNull(type, "type");
        targetPos = targetPos == null ? Optional.empty() : targetPos;
        durationTicks = Math.max(1, durationTicks);
    }
}
