package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import java.util.Locale;

/**
 * Ancient writing system classification (18 §5.1.4, §5.5.4, §11.1).
 */
public enum ScriptType {
    NONE(0),
    PICTOGRAPHIC(6),
    LOGOGRAPHIC(2),
    SYLLABIC(4),
    ALPHABETIC(5);

    public static final Codec<ScriptType> CODEC = Codec.STRING.xmap(
            s -> ScriptType.valueOf(s.toUpperCase(Locale.ROOT)),
            t -> t.name().toLowerCase(Locale.ROOT)
    );

    private final int baseMasteryDelta;

    ScriptType(int baseMasteryDelta) {
        this.baseMasteryDelta = baseMasteryDelta;
    }

    public int baseMasteryDelta() {
        return baseMasteryDelta;
    }

    public static ScriptType fromOrdinal(int ord) {
        ScriptType[] vals = values();
        return (ord >= 0 && ord < vals.length) ? vals[ord] : NONE;
    }
}
