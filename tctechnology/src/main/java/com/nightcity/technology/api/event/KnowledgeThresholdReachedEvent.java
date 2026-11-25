package com.nightcity.technology.api.event;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;

import java.util.UUID;

public final class KnowledgeThresholdReachedEvent extends Event {
    private final UUID civ;
    private final Identifier tech;
    private final int insightRp;

    public KnowledgeThresholdReachedEvent(UUID civ, Identifier tech, int insightRp) {
        this.civ = civ;
        this.tech = tech;
        this.insightRp = insightRp;
    }

    public UUID civ() {
        return civ;
    }

    public Identifier tech() {
        return tech;
    }

    public int insightRp() {
        return insightRp;
    }
}
