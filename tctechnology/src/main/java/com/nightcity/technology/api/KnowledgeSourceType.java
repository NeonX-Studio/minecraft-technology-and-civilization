package com.nightcity.technology.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

public enum KnowledgeSourceType {
    PRACTICE(1.0),
    OBSERVATION(1.0),
    EXPERIMENT(1.5),
    ARTIFACT(0.25),
    DIFFUSION(1.0),
    TEACHING(1.0),
    BOOK(1.0),
    ECHO(1.0),
    SUBMIT(1.0);

    public static final Codec<KnowledgeSourceType> CODEC = Codec.STRING.comapFlatMap(
            s -> {
                try {
                    return DataResult.success(KnowledgeSourceType.valueOf(s.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    return DataResult.error(() -> "Unknown KnowledgeSourceType: " + s);
                }
            },
            Enum::name
    );

    private final double defaultMultiplier;

    KnowledgeSourceType(double defaultMultiplier) {
        this.defaultMultiplier = defaultMultiplier;
    }

    public double defaultMultiplier() {
        return defaultMultiplier;
    }
}
