package com.nightcity.beyond.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.function.Supplier;

public final class BeyondRegionLayers {
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

    public static final RegionLayerDef<EmptyLayerData> ANOMALY_LAYER = new RegionLayerDef<>(
            Identifier.fromNamespaceAndPath("tcbeyond", "anomaly"),
            EmptyLayerData::new,
            EmptyLayerData.CODEC,
            LayerSync.NEAR
    );

    public static final List<RegionLayerDef<?>> ALL = List.of(ANOMALY_LAYER);

    private BeyondRegionLayers() {
    }
}
