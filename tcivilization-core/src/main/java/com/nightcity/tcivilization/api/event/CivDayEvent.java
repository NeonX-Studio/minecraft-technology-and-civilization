package com.nightcity.tcivilization.api.event;

import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

/**
 * Published once on the main thread when the overworld advances into a new day (03 §5.3).
 */
public final class CivDayEvent extends TcEvent {
    @Nullable
    private final MinecraftServer server;
    private final long day;
    private final int year;
    private final int seasonIndex;
    private final int dayOfSeason;
    private final long elapsedDays;

    public CivDayEvent(@Nullable MinecraftServer server, long day, int year,
                       int seasonIndex, int dayOfSeason, long elapsedDays) {
        this.server = server;
        this.day = day;
        this.year = year;
        this.seasonIndex = seasonIndex;
        this.dayOfSeason = dayOfSeason;
        this.elapsedDays = elapsedDays;
    }

    @Nullable
    public MinecraftServer server() {
        return server;
    }

    public long day() {
        return day;
    }

    public int year() {
        return year;
    }

    public int seasonIndex() {
        return seasonIndex;
    }

    public int dayOfSeason() {
        return dayOfSeason;
    }

    public long elapsedDays() {
        return elapsedDays;
    }
}
