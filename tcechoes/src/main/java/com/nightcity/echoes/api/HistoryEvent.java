package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;

/**
 * Node in the simulated historical event graph (18 §5.1.7, §11.1).
 */
public record HistoryEvent(
        int id,
        EventType type,
        int year,
        long region,
        List<Integer> polities,
        List<Integer> persons,
        int importance,
        int severity
) {
    public static final Codec<HistoryEvent> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("id").forGetter(HistoryEvent::id),
            EventType.CODEC.fieldOf("type").forGetter(HistoryEvent::type),
            Codec.INT.fieldOf("year").forGetter(HistoryEvent::year),
            Codec.LONG.fieldOf("region").forGetter(HistoryEvent::region),
            Codec.INT.listOf().optionalFieldOf("polities", List.of()).forGetter(HistoryEvent::polities),
            Codec.INT.listOf().optionalFieldOf("persons", List.of()).forGetter(HistoryEvent::persons),
            Codec.INT.fieldOf("importance").forGetter(HistoryEvent::importance),
            Codec.INT.fieldOf("severity").forGetter(HistoryEvent::severity)
    ).apply(i, HistoryEvent::new));

    public HistoryEvent {
        Objects.requireNonNull(type, "type");
        polities = polities == null ? List.of() : List.copyOf(polities);
        persons = persons == null ? List.of() : List.copyOf(persons);
    }
}
