package com.nightcity.tcivilization.client.api;

import java.util.Objects;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public final class RegisterPersonSectionsEvent extends Event implements IModBusEvent {
    private final PersonSectionRegistry registry;

    public RegisterPersonSectionsEvent(PersonSectionRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public void register(SectionSpec spec) {
        registry.register(spec);
    }
}
