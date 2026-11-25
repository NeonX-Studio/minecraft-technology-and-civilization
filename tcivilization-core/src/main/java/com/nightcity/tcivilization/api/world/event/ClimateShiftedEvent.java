package com.nightcity.tcivilization.api.world.event;

import com.nightcity.tcivilization.api.event.TcEvent;

/**
 * Fired whenever global temperature anomaly shifts by >= 0.25 C (10 §5.17, §12.2).
 */
public final class ClimateShiftedEvent extends TcEvent {
    private final float fromDeltaT;
    private final float toDeltaT;
    private final double co2Ppm;

    public ClimateShiftedEvent(float fromDeltaT, float toDeltaT, double co2Ppm) {
        this.fromDeltaT = fromDeltaT;
        this.toDeltaT = toDeltaT;
        this.co2Ppm = co2Ppm;
    }

    public float fromDeltaT() {
        return fromDeltaT;
    }

    public float toDeltaT() {
        return toDeltaT;
    }

    public double co2Ppm() {
        return co2Ppm;
    }
}
