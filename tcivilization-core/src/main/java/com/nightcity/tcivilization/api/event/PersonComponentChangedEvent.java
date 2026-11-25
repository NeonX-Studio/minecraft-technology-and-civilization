package com.nightcity.tcivilization.api.event;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.resources.Identifier;

public final class PersonComponentChangedEvent extends TcEvent {
    private final UUID personId;
    private final Identifier componentId;
    private final boolean added;

    public PersonComponentChangedEvent(UUID personId, Identifier componentId, boolean added) {
        this.personId = Objects.requireNonNull(personId, "personId");
        this.componentId = Objects.requireNonNull(componentId, "componentId");
        this.added = added;
    }

    public UUID personId() {
        return personId;
    }

    public Identifier componentId() {
        return componentId;
    }

    public boolean added() {
        return added;
    }
}
