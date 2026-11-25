package com.nightcity.tcivilization.api.world.event;

import com.nightcity.tcivilization.api.event.TcEvent;

/**
 * Fired when a subsurface reservoir crosses 50%, 10%, or 0% remaining capacity (10 §12.2).
 */
public final class ReservoirDepletedEvent extends TcEvent {
    private final long reservoirId;
    private final float remainingFraction;

    public ReservoirDepletedEvent(long reservoirId, float remainingFraction) {
        this.reservoirId = reservoirId;
        this.remainingFraction = remainingFraction;
    }

    public long reservoirId() {
        return reservoirId;
    }

    public float remainingFraction() {
        return remainingFraction;
    }
}
