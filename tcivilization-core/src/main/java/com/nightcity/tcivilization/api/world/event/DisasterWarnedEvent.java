package com.nightcity.tcivilization.api.world.event;

import com.nightcity.tcivilization.api.event.TcEvent;
import com.nightcity.tcivilization.api.world.ActiveHazardView;
import com.nightcity.tcivilization.api.world.ForecastSkill;
import java.util.Objects;

/**
 * Fired when a natural hazard enters precursor warning stage (10 §12.2).
 */
public final class DisasterWarnedEvent extends TcEvent {
    private final ActiveHazardView hazard;
    private final int leadTicks;
    private final ForecastSkill skill;

    public DisasterWarnedEvent(ActiveHazardView hazard, int leadTicks, ForecastSkill skill) {
        this.hazard = Objects.requireNonNull(hazard, "hazard");
        this.leadTicks = leadTicks;
        this.skill = Objects.requireNonNull(skill, "skill");
    }

    public ActiveHazardView hazard() {
        return hazard;
    }

    public int leadTicks() {
        return leadTicks;
    }

    public ForecastSkill skill() {
        return skill;
    }
}
