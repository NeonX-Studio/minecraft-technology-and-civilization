package com.nightcity.tcivilization.client.api;

import java.util.Objects;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public final class RegisterHudWidgetsEvent extends Event implements IModBusEvent {
    private final HudWidgetRegistry registry;

    public RegisterHudWidgetsEvent(HudWidgetRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public void register(HudWidgetSpec spec) {
        registry.register(spec);
    }

    public HudWidgetRegistry registry() {
        return registry;
    }
}
