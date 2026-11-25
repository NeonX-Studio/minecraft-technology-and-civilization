package com.nightcity.tcivilization.client.api;

import java.util.Objects;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public final class RegisterTargetInfoEvent extends Event implements IModBusEvent {
    private final TargetInfoProvider.Registry registry;

    public RegisterTargetInfoEvent(TargetInfoProvider.Registry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public void register(TargetInfoProvider provider) {
        registry.register(provider);
    }
}
