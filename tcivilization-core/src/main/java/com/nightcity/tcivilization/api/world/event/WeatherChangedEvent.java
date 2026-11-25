package com.nightcity.tcivilization.api.world.event;

import com.nightcity.tcivilization.api.event.TcEvent;
import com.nightcity.tcivilization.api.world.WeatherState;
import java.util.EnumSet;
import java.util.Objects;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

/**
 * Fired when a 1024x1024 weather cell transitions category (10 §12.2).
 */
public final class WeatherChangedEvent extends TcEvent {
    public enum WeatherFlag {
        FROST,
        HAIL,
        HEAVY_RAIN,
        SNOW_START,
        HEATWAVE,
        THUNDER
    }

    @Nullable
    private final ServerLevel level;
    private final int cellX;
    private final int cellZ;
    private final WeatherState from;
    private final WeatherState to;
    private final EnumSet<WeatherFlag> flags;

    public WeatherChangedEvent(
            @Nullable ServerLevel level,
            int cellX,
            int cellZ,
            WeatherState from,
            WeatherState to,
            EnumSet<WeatherFlag> flags
    ) {
        this.level = level;
        this.cellX = cellX;
        this.cellZ = cellZ;
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        this.flags = flags == null ? EnumSet.noneOf(WeatherFlag.class) : EnumSet.copyOf(flags);
    }

    @Nullable
    public ServerLevel level() {
        return level;
    }

    public int cellX() {
        return cellX;
    }

    public int cellZ() {
        return cellZ;
    }

    public WeatherState from() {
        return from;
    }

    public WeatherState to() {
        return to;
    }

    public EnumSet<WeatherFlag> flags() {
        return EnumSet.copyOf(flags);
    }
}
