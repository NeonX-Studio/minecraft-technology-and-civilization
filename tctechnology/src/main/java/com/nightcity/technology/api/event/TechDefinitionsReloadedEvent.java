package com.nightcity.technology.api.event;

import com.nightcity.technology.data.TechDefinitionLoader;
import net.neoforged.bus.api.Event;

public final class TechDefinitionsReloadedEvent extends Event {
    private final TechDefinitionLoader.TechGraphSnapshot snapshot;

    public TechDefinitionsReloadedEvent(TechDefinitionLoader.TechGraphSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public TechDefinitionLoader.TechGraphSnapshot snapshot() {
        return snapshot;
    }
}
