package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import java.util.Locale;

/**
 * Historical figure role in world history (18 §5.1.7, §11.1).
 */
public enum PersonRole {
    FOUNDER,
    RULER,
    GENERAL,
    PROPHET,
    EXPLORER,
    ARTISAN;

    public static final Codec<PersonRole> CODEC = Codec.STRING.xmap(
            s -> PersonRole.valueOf(s.toUpperCase(Locale.ROOT)),
            r -> r.name().toLowerCase(Locale.ROOT)
    );

    public static PersonRole fromOrdinal(int ord) {
        PersonRole[] vals = values();
        return (ord >= 0 && ord < vals.length) ? vals[ord] : FOUNDER;
    }
}
