package com.nightcity.tcivilization.client.api;

import java.util.Objects;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public final class RegisterDeepLinksEvent extends Event implements IModBusEvent {
    private final DeepLinkRegistry registry;

    public RegisterDeepLinksEvent(DeepLinkRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public void register(String modid, DeepLinkHandler handler) {
        registry.register(modid, handler);
    }
}
