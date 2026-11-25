package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import java.util.Locale;

/**
 * Architectural material style with weathering half-life constant tau in years (18 §5.4.2, §5.4.3).
 */
public enum ArchStyle {
    MUDBRICK(400.0D),
    RAMMED_EARTH(600.0D),
    TIMBER(250.0D),
    DRYSTONE(1_500.0D),
    ASHLAR(2_500.0D),
    MEGALITHIC(6_000.0D),
    TERRACED(2_000.0D),
    CAVE_CUT(8_000.0D);

    public static final Codec<ArchStyle> CODEC = Codec.STRING.xmap(
            s -> ArchStyle.valueOf(s.toUpperCase(Locale.ROOT)),
            st -> st.name().toLowerCase(Locale.ROOT)
    );

    private final double tauYears;

    ArchStyle(double tauYears) {
        this.tauYears = tauYears;
    }

    public double tauYears() {
        return tauYears;
    }

    public static ArchStyle fromOrdinal(int ord) {
        ArchStyle[] vals = values();
        return (ord >= 0 && ord < vals.length) ? vals[ord] : DRYSTONE;
    }
}
