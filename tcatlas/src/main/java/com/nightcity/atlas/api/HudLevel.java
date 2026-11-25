package com.nightcity.atlas.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

public enum HudLevel {
    MINIMAL,
    STANDARD,
    TACTICAL,
    HIDDEN;

    public static final Codec<HudLevel> CODEC = Codec.STRING.comapFlatMap(
            raw -> {
                try {
                    return DataResult.success(HudLevel.valueOf(raw.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    return DataResult.error(() -> "Unknown HudLevel: " + raw);
                }
            },
            Enum::name
    );

    public HudLevel next() {
        return switch (this) {
            case MINIMAL -> STANDARD;
            case STANDARD -> TACTICAL;
            case TACTICAL -> HIDDEN;
            case HIDDEN -> MINIMAL;
        };
    }

    public boolean includes(HudLevel widgetMinLevel) {
        if (this == HIDDEN || widgetMinLevel == HIDDEN) {
            return false;
        }
        return this.ordinal() >= widgetMinLevel.ordinal();
    }
}
