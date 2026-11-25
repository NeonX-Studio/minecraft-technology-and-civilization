package com.nightcity.tcivilization.api.world.event;

import com.nightcity.tcivilization.api.event.TcEvent;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

/**
 * Published when the global calendar season changes. Owned by core in api.world.event;
 * published by tcwildlands when installed, or by core as fallback when tcwildlands is absent (03 §5.3, §12.5).
 */
public final class SeasonChangedEvent extends TcEvent {
    @Nullable
    private final MinecraftServer server;
    private final int year;
    private final int seasonIndex;
    private final boolean fallbackFromCore;

    public SeasonChangedEvent(@Nullable MinecraftServer server, int year, int seasonIndex, boolean fallbackFromCore) {
        this.server = server;
        this.year = year;
        this.seasonIndex = seasonIndex;
        this.fallbackFromCore = fallbackFromCore;
    }

    public SeasonChangedEvent(@Nullable MinecraftServer server, int year, int seasonIndex) {
        this(server, year, seasonIndex, false);
    }

    @Nullable
    public MinecraftServer server() {
        return server;
    }

    public int year() {
        return year;
    }

    public int seasonIndex() {
        return seasonIndex;
    }

    public boolean fallbackFromCore() {
        return fallbackFromCore;
    }
}
