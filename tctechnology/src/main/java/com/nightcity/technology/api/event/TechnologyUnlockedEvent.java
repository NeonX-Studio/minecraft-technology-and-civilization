package com.nightcity.technology.api.event;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;

import java.util.UUID;

public final class TechnologyUnlockedEvent extends Event {
    private final UUID civ;
    private final Identifier tech;
    private final Identifier reason;
    private final long tick;

    public TechnologyUnlockedEvent(UUID civ, Identifier tech, Identifier reason, long tick) {
        this.civ = civ;
        this.tech = tech;
        this.reason = reason;
        this.tick = tick;
    }

    public UUID civ() {
        return civ;
    }

    public Identifier tech() {
        return tech;
    }

    public Identifier reason() {
        return reason;
    }

    public long tick() {
        return tick;
    }
}
