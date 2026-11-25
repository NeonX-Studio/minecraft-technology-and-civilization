package com.nightcity.tcivilization.api;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

public interface PermissionResolver {
    int priority();

    Result resolve(ServerPlayer player, Identifier action, @Nullable ActionScope scope);

    sealed interface Result permits Pass, Decided {
    }

    record Pass() implements Result {
    }

    record Decided(ActionCheck check) implements Result {
    }
}
