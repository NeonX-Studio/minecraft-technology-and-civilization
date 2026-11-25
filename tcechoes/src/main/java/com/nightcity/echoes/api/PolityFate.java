package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import java.util.Locale;

/**
 * End-of-simulation fate of an ancient polity (18 §5.1.8, §11.1).
 */
public enum PolityFate {
    SURVIVOR,
    REMNANT,
    EXTINCT;

    public static final Codec<PolityFate> CODEC = Codec.STRING.xmap(
            s -> PolityFate.valueOf(s.toUpperCase(Locale.ROOT)),
            f -> f.name().toLowerCase(Locale.ROOT)
    );

    public static PolityFate fromOrdinal(int ord) {
        PolityFate[] vals = values();
        return (ord >= 0 && ord < vals.length) ? vals[ord] : EXTINCT;
    }
}
