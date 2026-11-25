package com.nightcity.tcivilization.api.event;

import com.nightcity.tcivilization.api.Civilization;
import java.util.Objects;

public final class CivilizationCreatedEvent extends TcEvent {
    private final Civilization civilization;

    public CivilizationCreatedEvent(Civilization civilization) {
        this.civilization = Objects.requireNonNull(civilization, "civilization");
    }

    public Civilization civilization() {
        return civilization;
    }
}
