package com.nightcity.tcivilization.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;

/**
 * One player-pinned tracker target (max 3 per player, 03 §5.10.6).
 */
public record TrackerPin(Identifier type, String key, Component label) {
    public static final Codec<TrackerPin> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("type").forGetter(TrackerPin::type),
            Codec.STRING.fieldOf("key").forGetter(TrackerPin::key),
            ComponentSerialization.CODEC.fieldOf("label").forGetter(TrackerPin::label)
    ).apply(instance, TrackerPin::new));

    public TrackerPin {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(key, "key");
        label = label == null ? Component.literal(key) : label;
    }
}
