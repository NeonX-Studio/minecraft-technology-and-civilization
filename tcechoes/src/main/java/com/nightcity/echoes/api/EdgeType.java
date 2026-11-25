package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import java.util.Locale;

/**
 * Directed causal or relational edge between HistoryEvent nodes (18 §5.1.7, §11.1).
 */
public enum EdgeType {
    CAUSED,
    PARTICIPATED,
    SUCCEEDED,
    ALLIED,
    FEUD;

    public static final Codec<EdgeType> CODEC = Codec.STRING.xmap(
            s -> EdgeType.valueOf(s.toUpperCase(Locale.ROOT)),
            t -> t.name().toLowerCase(Locale.ROOT)
    );

    public static EdgeType fromOrdinal(int ord) {
        EdgeType[] vals = values();
        return (ord >= 0 && ord < vals.length) ? vals[ord] : CAUSED;
    }
}
