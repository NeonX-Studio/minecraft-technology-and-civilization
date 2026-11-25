package com.nightcity.nations.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Personality(
        int aggression,
        int expansion,
        int commerce,
        int science,
        int honor,
        int xenophilia
) {
    public static final Codec<Personality> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("aggression").forGetter(Personality::aggression),
            Codec.INT.fieldOf("expansion").forGetter(Personality::expansion),
            Codec.INT.fieldOf("commerce").forGetter(Personality::commerce),
            Codec.INT.fieldOf("science").forGetter(Personality::science),
            Codec.INT.fieldOf("honor").forGetter(Personality::honor),
            Codec.INT.fieldOf("xenophilia").forGetter(Personality::xenophilia)
    ).apply(instance, Personality::new));

    public Personality {
        aggression = clampAxis(aggression);
        expansion = clampAxis(expansion);
        commerce = clampAxis(commerce);
        science = clampAxis(science);
        honor = clampAxis(honor);
        xenophilia = clampAxis(xenophilia);
    }

    private static int clampAxis(int v) {
        return Math.max(0, Math.min(100, v));
    }
}
