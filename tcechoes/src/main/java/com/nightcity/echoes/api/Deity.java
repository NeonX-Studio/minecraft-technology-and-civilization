package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import java.util.Locale;

/**
 * Primary deity domain influencing polity aggression, holy sites, and legends (18 §5.1.4, §5.1.5).
 */
public enum Deity {
    SUN(0.0D),
    RIVER(0.0D),
    MOUNTAIN(0.0D),
    OCEAN(0.0D),
    ANCESTOR(-0.1D),
    BEAST(0.2D);

    public static final Codec<Deity> CODEC = Codec.STRING.xmap(
            s -> Deity.valueOf(s.toUpperCase(Locale.ROOT)),
            d -> d.name().toLowerCase(Locale.ROOT)
    );

    private final double aggressionModifier;

    Deity(double aggressionModifier) {
        this.aggressionModifier = aggressionModifier;
    }

    public double aggressionModifier() {
        return aggressionModifier;
    }

    public static Deity fromOrdinal(int ord) {
        Deity[] vals = values();
        return (ord >= 0 && ord < vals.length) ? vals[ord] : ANCESTOR;
    }
}
