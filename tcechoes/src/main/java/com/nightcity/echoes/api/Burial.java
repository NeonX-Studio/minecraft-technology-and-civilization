package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import java.util.Locale;

/**
 * Ancient funerary custom determining burial site types (18 §5.1.4).
 */
public enum Burial {
    BARROW,
    NECROPOLIS,
    CREMATION,
    CAVE_CUT;

    public static final Codec<Burial> CODEC = Codec.STRING.xmap(
            s -> Burial.valueOf(s.toUpperCase(Locale.ROOT)),
            b -> b.name().toLowerCase(Locale.ROOT)
    );

    public static Burial fromOrdinal(int ord) {
        Burial[] vals = values();
        return (ord >= 0 && ord < vals.length) ? vals[ord] : BARROW;
    }
}
