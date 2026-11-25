package com.nightcity.tcivilization.api;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side persistent alert poller invoked every 100 ticks (03 §5.10.5, §12.1).
 */
public interface AlertProvider {
    record Alert(Severity severity, Component message, Optional<String> deepLink) {
        public Alert {
            deepLink = deepLink == null ? Optional.empty() : deepLink;
        }
    }

    Identifier id();

    List<Alert> poll(ServerPlayer viewer);
}
