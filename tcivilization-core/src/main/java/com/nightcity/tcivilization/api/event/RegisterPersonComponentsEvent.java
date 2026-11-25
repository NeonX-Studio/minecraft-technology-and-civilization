package com.nightcity.tcivilization.api.event;

import com.nightcity.tcivilization.api.PersonComponentType;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public final class RegisterPersonComponentsEvent extends Event implements IModBusEvent {
    private final Map<Identifier, PersonComponentType<?>> target;

    public RegisterPersonComponentsEvent(Map<Identifier, PersonComponentType<?>> target) {
        this.target = Objects.requireNonNull(target, "target");
    }

    public <T> void register(PersonComponentType<T> type) {
        Objects.requireNonNull(type, "type");
        if (target.putIfAbsent(type.id(), type) != null) {
            throw new IllegalArgumentException("PersonComponentType already registered: " + type.id());
        }
    }

    public Map<Identifier, PersonComponentType<?>> registeredComponents() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(target));
    }
}
