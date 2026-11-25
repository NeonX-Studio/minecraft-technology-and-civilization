package com.nightcity.echoes.api.event;

import com.nightcity.echoes.api.WorldHistoryView;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

/**
 * Fired on NeoForge.EVENT_BUS once world history pre-simulation or history.bin loading finishes (18 §12.2).
 */
public final class WorldHistoryReadyEvent extends Event {
    private final MinecraftServer server;
    private final WorldHistoryView view;
    private final boolean freshlyGenerated;

    public WorldHistoryReadyEvent(MinecraftServer server, WorldHistoryView view, boolean freshlyGenerated) {
        this.server = server;
        this.view = Objects.requireNonNull(view, "view");
        this.freshlyGenerated = freshlyGenerated;
    }

    public MinecraftServer server() {
        return server;
    }

    public WorldHistoryView view() {
        return view;
    }

    public boolean freshlyGenerated() {
        return freshlyGenerated;
    }
}
