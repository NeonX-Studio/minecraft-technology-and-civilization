package com.nightcity.tcivilization.api.world.event;

import com.nightcity.tcivilization.api.event.TcEvent;
import com.nightcity.tcivilization.api.world.PlanetSummary;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the game event bus once planet.bin has been precomputed or loaded from disk (10 §12.2).
 */
public final class PlanetGeneratedEvent extends TcEvent {
    @Nullable
    private final MinecraftServer server;
    private final PlanetSummary summary;
    private final boolean freshlyGenerated;

    public PlanetGeneratedEvent(@Nullable MinecraftServer server, PlanetSummary summary, boolean freshlyGenerated) {
        this.server = server;
        this.summary = Objects.requireNonNull(summary, "summary");
        this.freshlyGenerated = freshlyGenerated;
    }

    @Nullable
    public MinecraftServer server() {
        return server;
    }

    public PlanetSummary summary() {
        return summary;
    }

    public boolean freshlyGenerated() {
        return freshlyGenerated;
    }
}
