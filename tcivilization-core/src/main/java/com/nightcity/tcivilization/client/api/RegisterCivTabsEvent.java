package com.nightcity.tcivilization.client.api;

import java.util.Objects;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public final class RegisterCivTabsEvent extends Event implements IModBusEvent {
    private final CivPanelTabRegistry registry;

    public RegisterCivTabsEvent(CivPanelTabRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public void register(CivPanelTabSpec spec) {
        registry.register(spec);
    }

    public CivPanelTabRegistry registry() {
        return registry;
    }
}
