package com.nightcity.tcivilization.region;

import com.nightcity.tcivilization.api.RegionLayer;
import com.nightcity.tcivilization.api.event.RegisterRegionLayersEvent;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;

/**
 * Registry of RegionLayer definitions, always containing tcivilization:civ.
 */
public final class RegionLayerRegistry {
    private static final RegionLayerRegistry GLOBAL = new RegionLayerRegistry();

    private final Map<Identifier, RegionLayer<?>> layers = new LinkedHashMap<>();
    private volatile boolean frozen = false;

    public RegionLayerRegistry() {
        layers.put(CivRegionLayer.ID, CivRegionLayer.INSTANCE);
    }

    public static RegionLayerRegistry global() {
        return GLOBAL;
    }

    public synchronized <T> void register(RegionLayer<T> layer) {
        Objects.requireNonNull(layer, "layer");
        if (frozen) {
            throw new IllegalStateException("RegionLayerRegistry is already frozen");
        }
        Identifier id = Objects.requireNonNull(layer.id(), "layer.id");
        if (layers.containsKey(id) && layers.get(id) != layer) {
            throw new IllegalArgumentException("Duplicate RegionLayer registration: " + id);
        }
        layers.put(id, layer);
    }

    public synchronized void dispatchRegistration(IEventBus modBus) {
        if (frozen) {
            return;
        }
        RegisterRegionLayersEvent event = new RegisterRegionLayersEvent(layers);
        com.nightcity.tcivilization.TCivilization.postModBusEvent(modBus, event);
        frozen = true;
    }

    public synchronized Optional<RegionLayer<?>> get(Identifier id) {
        return Optional.ofNullable(layers.get(id));
    }

    public synchronized Map<Identifier, RegionLayer<?>> all() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(layers));
    }

    public synchronized void resetForTests() {
        frozen = false;
        layers.clear();
        layers.put(CivRegionLayer.ID, CivRegionLayer.INSTANCE);
    }
}
