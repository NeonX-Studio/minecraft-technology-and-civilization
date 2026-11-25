package com.nightcity.discovery.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.function.Supplier;

public final class DiscoveryRegionLayers {
    public enum LayerSync {
        NONE, NEAR, ON_DEMAND, GLOBAL_SUMMARY
    }

    public record EmptyLayerData(int formatVersion) {
        public static final Codec<EmptyLayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("format_version", 1).forGetter(EmptyLayerData::formatVersion)
        ).apply(instance, EmptyLayerData::new));

        public EmptyLayerData() {
            this(1);
        }
    }

    public record RegionLayerDef<T>(
            Identifier id,
            Supplier<T> defaultFactory,
            Codec<T> codec,
            LayerSync sync
    ) {
        public T createDefault() {
            return defaultFactory.get();
        }
    }

    public static final RegionLayerDef<EmptyLayerData> FIELD_RECORD_LAYER = new RegionLayerDef<>(
            Identifier.fromNamespaceAndPath("tcdiscovery", "field_record"),
            EmptyLayerData::new,
            EmptyLayerData.CODEC,
            LayerSync.ON_DEMAND
    );

    public static final List<RegionLayerDef<?>> ALL = List.of(FIELD_RECORD_LAYER);

    private DiscoveryRegionLayers() {
    }
}
