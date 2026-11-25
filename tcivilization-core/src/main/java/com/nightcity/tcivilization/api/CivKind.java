package com.nightcity.tcivilization.api;

import com.mojang.serialization.Codec;
import java.util.Locale;

/**
 * Classification of a Civilization record (03 §5.1.1).
 */
public enum CivKind {
    PLAYER,
    AI,
    HISTORIC;

    public static final Codec<CivKind> CODEC = Codec.STRING.xmap(
            raw -> CivKind.valueOf(raw.toUpperCase(Locale.ROOT)),
            kind -> kind.name().toLowerCase(Locale.ROOT)
    );
}
