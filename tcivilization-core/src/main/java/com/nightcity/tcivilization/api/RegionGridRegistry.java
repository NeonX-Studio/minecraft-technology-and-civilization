package com.nightcity.tcivilization.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Registry of region grid specifications keyed by dimension identifier. */
public final class RegionGridRegistry {
    private final Map<String, RegionGridSpec> specs = new LinkedHashMap<>();

    public static RegionGridRegistry defaults() {
        RegionGridRegistry registry = new RegionGridRegistry();
        registry.register(RegionGridSpec.overworld());
        registry.register(RegionGridSpec.nether());
        registry.register(RegionGridSpec.end());
        return registry;
    }

    public void register(RegionGridSpec spec) {
        if (specs.putIfAbsent(spec.dimension(), spec) != null) {
            throw new IllegalArgumentException("Region grid already registered: " + spec.dimension());
        }
    }

    public Optional<RegionGridSpec> get(String dimension) {
        return Optional.ofNullable(specs.get(dimension));
    }

    public Optional<RegionCell> atBlock(String dimension, int blockX, int blockZ) {
        return get(dimension).map(spec -> spec.atBlock(blockX, blockZ));
    }
}
