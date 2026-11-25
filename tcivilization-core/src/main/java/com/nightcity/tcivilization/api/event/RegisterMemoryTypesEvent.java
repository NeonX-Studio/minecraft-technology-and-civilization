package com.nightcity.tcivilization.api.event;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public final class RegisterMemoryTypesEvent extends Event implements IModBusEvent {
    public record MemoryTypeSpec(Identifier id, int defaultImportance, Identifier icon) {
        public MemoryTypeSpec {
            Objects.requireNonNull(id, "id");
            defaultImportance = Math.clamp(defaultImportance, 1, 5);
            Objects.requireNonNull(icon, "icon");
        }
    }

    private final Map<Identifier, MemoryTypeSpec> types = new LinkedHashMap<>();

    public void register(MemoryTypeSpec spec) {
        Objects.requireNonNull(spec, "spec");
        if (types.putIfAbsent(spec.id(), spec) != null) {
            throw new IllegalArgumentException("MemoryType already registered: " + spec.id());
        }
    }

    public Map<Identifier, MemoryTypeSpec> registeredTypes() {
        return Collections.unmodifiableMap(types);
    }
}
