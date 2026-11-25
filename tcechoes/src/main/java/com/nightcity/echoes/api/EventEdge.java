package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;

/**
 * Directed edge between two HistoryEvent nodes (18 §5.1.7, §11.1).
 */
public record EventEdge(int from, int to, EdgeType type) {
    public static final Codec<EventEdge> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("from").forGetter(EventEdge::from),
            Codec.INT.fieldOf("to").forGetter(EventEdge::to),
            EdgeType.CODEC.fieldOf("type").forGetter(EventEdge::type)
    ).apply(i, EventEdge::new));

    public EventEdge {
        Objects.requireNonNull(type, "type");
    }
}
