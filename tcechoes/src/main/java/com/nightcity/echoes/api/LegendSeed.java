package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;

/**
 * Pre-simulated legend archetype linked to an event node and site (18 §5.6.1, §11.1).
 */
public record LegendSeed(
        int id,
        LegendKind kind,
        int eventId,
        int siteId,
        int chainId,
        int chainIndex,
        byte truth
) {
    public static final Codec<LegendSeed> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("id").forGetter(LegendSeed::id),
            LegendKind.CODEC.fieldOf("kind").forGetter(LegendSeed::kind),
            Codec.INT.fieldOf("event_id").forGetter(LegendSeed::eventId),
            Codec.INT.fieldOf("site_id").forGetter(LegendSeed::siteId),
            Codec.INT.optionalFieldOf("chain_id", -1).forGetter(LegendSeed::chainId),
            Codec.INT.optionalFieldOf("chain_index", 0).forGetter(LegendSeed::chainIndex),
            Codec.BYTE.optionalFieldOf("truth", (byte) 0).forGetter(LegendSeed::truth)
    ).apply(i, LegendSeed::new));

    public LegendSeed {
        Objects.requireNonNull(kind, "kind");
    }
}
