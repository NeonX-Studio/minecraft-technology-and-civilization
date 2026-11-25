package com.nightcity.tcivilization.api.world.event;

import com.nightcity.tcivilization.api.event.TcEvent;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

public final class EmissionRecordedEvent extends TcEvent {
    @Nullable
    private final ServerLevel level;
    private final long regionKey;
    private final Identifier pollutant;
    private final double amountKg;
    private final Identifier source;

    public EmissionRecordedEvent(
            @Nullable ServerLevel level,
            long regionKey,
            Identifier pollutant,
            double amountKg,
            Identifier source
    ) {
        this.level = level;
        this.regionKey = regionKey;
        this.pollutant = Objects.requireNonNull(pollutant, "pollutant");
        this.amountKg = amountKg;
        this.source = Objects.requireNonNull(source, "source");
    }

    @Nullable
    public ServerLevel level() {
        return level;
    }

    public long regionKey() {
        return regionKey;
    }

    public Identifier pollutant() {
        return pollutant;
    }

    public double amountKg() {
        return amountKg;
    }

    public Identifier source() {
        return source;
    }
}
