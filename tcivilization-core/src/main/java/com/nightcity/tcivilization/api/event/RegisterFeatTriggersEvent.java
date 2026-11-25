package com.nightcity.tcivilization.api.event;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public final class RegisterFeatTriggersEvent extends Event implements IModBusEvent {
    private final Set<Identifier> triggers = new LinkedHashSet<>();

    public void register(Identifier triggerId) {
        Objects.requireNonNull(triggerId, "triggerId");
        if (!triggers.add(triggerId)) {
            throw new IllegalArgumentException("Feat trigger already registered: " + triggerId);
        }
    }

    public Set<Identifier> registeredTriggers() {
        return Collections.unmodifiableSet(triggers);
    }
}
