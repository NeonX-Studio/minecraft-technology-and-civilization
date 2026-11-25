package com.nightcity.tcivilization.api.event;

import com.nightcity.tcivilization.api.RegionCell;

/**
 * Published when a region's human-activity load pressure crosses a threshold with 0.05 hysteresis (03 §5.2.4).
 */
public final class RegionLoadPressureEvent extends TcEvent {
    public static final float HYSTERESIS = 0.05f;

    public enum Level {
        NONE(0.0f),
        LOW(0.25f),
        MEDIUM(0.50f),
        HIGH(0.75f),
        CRITICAL(0.90f);

        private final float threshold;

        Level(float threshold) {
            this.threshold = threshold;
        }

        public float threshold() {
            return threshold;
        }

        public float downwardThreshold() {
            return Math.max(0.0f, threshold - HYSTERESIS);
        }
    }

    private final RegionCell region;
    private final Level previousLevel;
    private final Level level;
    private final float pressure;

    public RegionLoadPressureEvent(RegionCell region, Level previousLevel, Level level, float pressure) {
        this.region = region;
        this.previousLevel = previousLevel;
        this.level = level;
        this.pressure = pressure;
    }

    public RegionCell region() {
        return region;
    }

    public Level previousLevel() {
        return previousLevel;
    }

    public Level level() {
        return level;
    }

    public float pressure() {
        return pressure;
    }
}
