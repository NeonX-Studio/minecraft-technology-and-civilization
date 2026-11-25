package com.nightcity.tcivilization.client.api;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Supplies context-sensitive Z command wheel options and executes chosen actions (03 §12.2, 31 §5.3).
 */
public interface WheelProvider {
    Identifier id();

    List<WheelOption> options(WheelContext ctx);

    default List<WheelOption> predict(ClientWheelContext ctx) {
        return List.of();
    }

    void execute(ServerPlayer player, WheelTarget target, Identifier optionId);
}
