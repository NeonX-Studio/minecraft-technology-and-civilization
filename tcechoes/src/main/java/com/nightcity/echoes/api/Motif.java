package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import java.util.Locale;

/**
 * Ceramic and decorative motif family (18 §5.1.4).
 */
public enum Motif {
    GEOMETRIC,
    CORD_MARKED,
    PAINTED,
    ANIMAL,
    PLANT,
    SPIRAL,
    ANTHROPOMORPHIC,
    WAVE;

    public static final Codec<Motif> CODEC = Codec.STRING.xmap(
            s -> Motif.valueOf(s.toUpperCase(Locale.ROOT)),
            m -> m.name().toLowerCase(Locale.ROOT)
    );

    public static Motif fromOrdinal(int ord) {
        Motif[] vals = values();
        return (ord >= 0 && ord < vals.length) ? vals[ord] : GEOMETRIC;
    }
}
