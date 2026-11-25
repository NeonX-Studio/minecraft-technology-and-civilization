package com.nightcity.atlas.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

/**
 * Survey level SL0..SL5 representing a holder's cartographic knowledge of a chunk (22 §5.1).
 */
public enum SurveyLevel {
    UNKNOWN(0),
    RUMOR(1),
    MEMORY(2),
    SKETCH(3),
    SURVEYED(4),
    PRECISE(5);

    public static final Codec<SurveyLevel> CODEC = Codec.STRING.comapFlatMap(
            raw -> {
                try {
                    return DataResult.success(SurveyLevel.valueOf(raw.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    return DataResult.error(() -> "Unknown SurveyLevel: " + raw);
                }
            },
            Enum::name
    );

    private final int code;

    SurveyLevel(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static SurveyLevel fromCode(int code) {
        return switch (Math.clamp(code, 0, 5)) {
            case 0 -> UNKNOWN;
            case 1 -> RUMOR;
            case 2 -> MEMORY;
            case 3 -> SKETCH;
            case 4 -> SURVEYED;
            default -> PRECISE;
        };
    }

    public boolean isKnown() {
        return this != UNKNOWN;
    }
}
