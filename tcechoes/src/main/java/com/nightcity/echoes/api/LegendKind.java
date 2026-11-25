package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import java.util.Locale;

/**
 * All 14 legend template categories (18 §5.6.3).
 */
public enum LegendKind {
    ORIGIN,
    FOUNDING,
    WAR,
    FALL,
    KING_TOMB,
    LOST_CITY,
    HOARD,
    MONSTER,
    SACRED_GROVE,
    LAMENT,
    SEA_RAIDER,
    PROPHECY,
    DISASTER,
    HERO;

    public static final Codec<LegendKind> CODEC = Codec.STRING.xmap(
            s -> LegendKind.valueOf(s.toUpperCase(Locale.ROOT)),
            k -> k.name().toLowerCase(Locale.ROOT)
    );

    public static LegendKind fromOrdinal(int ord) {
        LegendKind[] vals = values();
        return (ord >= 0 && ord < vals.length) ? vals[ord] : FOUNDING;
    }
}
