package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import java.util.Locale;

/**
 * Ancient trade/transport route kind (18 §5.3.4, §11.1).
 */
public enum RouteKind {
    LAND,
    SEA;

    public static final Codec<RouteKind> CODEC = Codec.STRING.xmap(
            s -> RouteKind.valueOf(s.toUpperCase(Locale.ROOT)),
            k -> k.name().toLowerCase(Locale.ROOT)
    );

    public static RouteKind fromOrdinal(int ord) {
        return ord == 1 ? SEA : LAND;
    }
}
