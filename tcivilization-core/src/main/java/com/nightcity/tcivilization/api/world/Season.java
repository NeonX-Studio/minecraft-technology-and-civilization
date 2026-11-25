package com.nightcity.tcivilization.api.world;

import com.nightcity.tcivilization.api.CivCalendar;

/**
 * Astronomical and tropical monsoon/dry seasons (10 §5.12.1, §12.1).
 */
public enum Season {
    SPRING("season.tcivilization.spring"),
    SUMMER("season.tcivilization.summer"),
    AUTUMN("season.tcivilization.autumn"),
    WINTER("season.tcivilization.winter"),
    WET("season.tcwildlands.wet"),
    DRY("season.tcwildlands.dry");

    private final String translationKey;

    Season(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }

    public static Season fromAstronomicalIndex(int index) {
        return switch (Math.floorMod(index, 4)) {
            case 0 -> SPRING;
            case 1 -> SUMMER;
            case 2 -> AUTUMN;
            default -> WINTER;
        };
    }

    public static Season fromCalendar(CivCalendar.Season season) {
        return fromAstronomicalIndex(season.index());
    }

    public CivCalendar.Season toCalendarSeason() {
        return switch (this) {
            case SPRING -> CivCalendar.Season.SPRING;
            case SUMMER, WET -> CivCalendar.Season.SUMMER;
            case AUTUMN -> CivCalendar.Season.AUTUMN;
            case WINTER, DRY -> CivCalendar.Season.WINTER;
        };
    }
}
