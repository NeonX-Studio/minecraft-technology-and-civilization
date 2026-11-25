package com.nightcity.tcivilization.client.api;

import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

public record WheelContext(@Nullable ServerPlayer player, WheelTarget target) {
    public WheelContext {
        Objects.requireNonNull(target, "target");
    }
}
