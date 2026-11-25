package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import java.util.Locale;

/**
 * Provenance of a recorded archaeological site (18 §11.1).
 */
public enum SiteOrigin {
    SIMULATED,
    VANILLA,
    LIVE;

    public static final Codec<SiteOrigin> CODEC = Codec.STRING.xmap(
            s -> SiteOrigin.valueOf(s.toUpperCase(Locale.ROOT)),
            o -> o.name().toLowerCase(Locale.ROOT)
    );

    public static SiteOrigin fromOrdinal(int ord) {
        SiteOrigin[] vals = values();
        return (ord >= 0 && ord < vals.length) ? vals[ord] : SIMULATED;
    }
}
