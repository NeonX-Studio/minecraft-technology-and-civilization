package com.nightcity.tcivilization.api.world.event;

import com.nightcity.tcivilization.api.event.TcEvent;
import com.nightcity.tcivilization.api.world.ActiveHazardView;
import com.nightcity.tcivilization.api.world.HazardFootprint;
import java.util.Objects;

/**
 * Fired when a natural hazard begins active impact (10 §12.2).
 */
public final class DisasterStartedEvent extends TcEvent {
    private final ActiveHazardView hazard;
    private final HazardFootprint footprint;

    public DisasterStartedEvent(ActiveHazardView hazard, HazardFootprint footprint) {
        this.hazard = Objects.requireNonNull(hazard, "hazard");
        this.footprint = Objects.requireNonNull(footprint, "footprint");
    }

    public ActiveHazardView hazard() {
        return hazard;
    }

    public HazardFootprint footprint() {
        return footprint;
    }
}
