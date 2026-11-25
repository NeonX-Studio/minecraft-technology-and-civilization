package com.nightcity.tcivilization.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.tcivilization.api.event.CivDayEvent;
import com.nightcity.tcivilization.api.world.event.SeasonChangedEvent;
import com.nightcity.tcivilization.sim.TcEventBus;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

/**
 * Unified world calendar and day/season event tracker (03 §5.3).
 */
public final class CivCalendar {
    public static final long DAY_TICKS = 24_000L;
    public static final int SEASON_DAYS = 12;
    public static final int SEASONS_PER_YEAR = 4;
    public static final int YEAR_DAYS = SEASON_DAYS * SEASONS_PER_YEAR;

    private CivCalendar() {
    }

    public enum Season {
        SPRING(0, "season.tcivilization.spring"),
        SUMMER(1, "season.tcivilization.summer"),
        AUTUMN(2, "season.tcivilization.autumn"),
        WINTER(3, "season.tcivilization.winter");

        private final int index;
        private final String translationKey;

        Season(int index, String translationKey) {
            this.index = index;
            this.translationKey = translationKey;
        }

        public int index() {
            return index;
        }

        public String translationKey() {
            return translationKey;
        }

        public static Season byIndex(int index) {
            return values()[Math.floorMod(index, 4)];
        }
    }

    public record CalendarDate(long totalDays, int year, int seasonIndex, int dayOfSeason) {
        public Season season() {
            return Season.byIndex(seasonIndex);
        }

        public int southernSeasonIndex() {
            return Math.floorMod(seasonIndex + 2, SEASONS_PER_YEAR);
        }

        public String formatCompact() {
            String[] zh = {"春", "夏", "秋", "冬"};
            return year + "·" + zh[Math.floorMod(seasonIndex, 4)] + "·" + dayOfSeason;
        }
    }

    public record State(long lastDay, int lastSeasonIndex, int lastYear) {
        public static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.optionalFieldOf("last_day", 0L).forGetter(State::lastDay),
                Codec.INT.optionalFieldOf("last_season", 0).forGetter(State::lastSeasonIndex),
                Codec.INT.optionalFieldOf("last_year", 1).forGetter(State::lastYear)
        ).apply(instance, State::new));

        public static State initial() {
            return new State(0L, 0, 1);
        }
    }

    public static CalendarDate fromDayTime(long dayTime) {
        long totalDays = Math.floorDiv(dayTime, DAY_TICKS);
        return fromTotalDays(totalDays);
    }

    public static CalendarDate fromTotalDays(long totalDays) {
        int year = (int) Math.floorDiv(totalDays, YEAR_DAYS) + 1;
        int dayInYear = (int) Math.floorMod(totalDays, YEAR_DAYS);
        int seasonIndex = dayInYear / SEASON_DAYS;
        int dayOfSeason = (dayInYear % SEASON_DAYS) + 1;
        return new CalendarDate(totalDays, year, seasonIndex, dayOfSeason);
    }

    public static State advance(@Nullable MinecraftServer server, long dayTime, State previous,
                                boolean wildlandsLoaded, boolean publishFallbackSeason, TcEventBus eventBus) {
        CalendarDate now = fromDayTime(dayTime);
        if (now.totalDays() < previous.lastDay()) {
            return new State(now.totalDays(), now.seasonIndex(), now.year());
        }
        if (now.totalDays() == previous.lastDay()) {
            return previous;
        }

        long elapsedDays = now.totalDays() - previous.lastDay();
        eventBus.enqueue(new CivDayEvent(
                server, now.totalDays(), now.year(), now.seasonIndex(), now.dayOfSeason(), elapsedDays));

        boolean seasonChanged = now.seasonIndex() != previous.lastSeasonIndex() || now.year() != previous.lastYear();
        if (seasonChanged && !wildlandsLoaded && publishFallbackSeason) {
            eventBus.enqueue(new SeasonChangedEvent(server, now.year(), now.seasonIndex(), true));
        }
        return new State(now.totalDays(), now.seasonIndex(), now.year());
    }
}
