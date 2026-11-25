package com.nightcity.tcivilization.api.world.event;

import com.nightcity.tcivilization.api.event.TcEvent;
import com.nightcity.tcivilization.api.world.ActiveHazardView;
import com.nightcity.tcivilization.api.world.HazardRecord;
import java.util.Objects;

/**
 * Fired when a natural hazard ends, carrying its abstract/embodied damage record (10 §12.2).
 */
public final class DisasterEndedEvent extends TcEvent {
    private final ActiveHazardView hazard;
    private final HazardRecord stats;

    public DisasterEndedEvent(ActiveHazardView hazard, HazardRecord stats) {
        this.hazard = Objects.requireNonNull(hazard, "hazard");
        this.stats = Objects.requireNonNull(stats, "stats");
    }

    public ActiveHazardView hazard() {
        return hazard;
    }

    public HazardRecord stats() {
        return stats;
    }
}
