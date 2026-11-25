package com.nightcity.tcivilization.api.event;

import com.nightcity.tcivilization.api.RegionLayer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/**
 * Mod-bus event fired during common setup to register RegionLayer definitions (03 §5.2.3).
 */
public final class RegisterRegionLayersEvent extends Event implements IModBusEvent {
    private final Map<Identifier, RegionLayer<?>> target;

    public RegisterRegionLayersEvent(Map<Identifier, RegionLayer<?>> target) {
        this.target = Objects.requireNonNull(target, "target");
    }

    public <T> void register(RegionLayer<T> layer) {
        Objects.requireNonNull(layer, "layer");
        Identifier id = Objects.requireNonNull(layer.id(), "layer.id");
        if (target.containsKey(id)) {
            throw new IllegalArgumentException("RegionLayer already registered: " + id);
        }
        target.put(id, layer);
    }

    public Map<Identifier, RegionLayer<?>> registeredLayers() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(target));
    }
}
