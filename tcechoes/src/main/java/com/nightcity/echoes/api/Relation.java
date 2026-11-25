package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import java.util.Locale;

/**
 * Historical bilateral relationship exported to tcnations (18 §3.6, §5.3.3, §12.1).
 */
public enum Relation {
    NEUTRAL(0),
    OLD_ALLIANCE(15),
    FEUD(-25);

    public static final Codec<Relation> CODEC = Codec.STRING.xmap(
            s -> Relation.valueOf(s.toUpperCase(Locale.ROOT)),
            r -> r.name().toLowerCase(Locale.ROOT)
    );

    private final int diplomacyModifier;

    Relation(int diplomacyModifier) {
        this.diplomacyModifier = diplomacyModifier;
    }

    public int diplomacyModifier() {
        return diplomacyModifier;
    }

    public static Relation fromOrdinal(int ord) {
        Relation[] vals = values();
        return (ord >= 0 && ord < vals.length) ? vals[ord] : NEUTRAL;
    }
}
