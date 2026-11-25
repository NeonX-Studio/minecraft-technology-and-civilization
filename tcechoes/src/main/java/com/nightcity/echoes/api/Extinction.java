package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.resources.Identifier;

/**
 * Pre-historic megafauna / relict species extirpation record on a continent (18 §5.1.6, §5.3.1, §11.1).
 */
public record Extinction(int continentId, Identifier species, int year, int causeEventId) {
    public static final Codec<Extinction> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("continent_id").forGetter(Extinction::continentId),
            Identifier.CODEC.fieldOf("species").forGetter(Extinction::species),
            Codec.INT.fieldOf("year").forGetter(Extinction::year),
            Codec.INT.fieldOf("cause_event_id").forGetter(Extinction::causeEventId)
    ).apply(i, Extinction::new));

    public Extinction {
        Objects.requireNonNull(species, "species");
    }
}
