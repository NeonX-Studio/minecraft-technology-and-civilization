package com.nightcity.tcivilization.api;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * Serializable per-region data layer registered by core or theme modules (02 §2.2, 03 §5.2.3).
 */
public interface RegionLayer<T> {
    int MAX_SERIALIZED_BYTES = 16 * 1024;

    Identifier id();

    T createDefault(RegionCell cell);

    Codec<T> codec();

    default LayerSync sync() {
        return LayerSync.NONE;
    }

    default Optional<StreamCodec<RegistryFriendlyByteBuf, T>> streamCodec() {
        return Optional.empty();
    }
}
