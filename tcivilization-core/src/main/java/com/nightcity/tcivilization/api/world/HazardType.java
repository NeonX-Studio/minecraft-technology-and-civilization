package com.nightcity.tcivilization.api.world;

import com.mojang.serialization.Codec;
import java.util.Locale;

/**
 * Physical natural hazard categories (10 §5.14, §11.4).
 */
public enum HazardType {
    FLOOD,
    EARTHQUAKE,
    TSUNAMI,
    VOLCANO,
    DROUGHT,
    STORM,
    TORNADO,
    HAIL,
    WILDFIRE,
    LANDSLIDE,
    AVALANCHE;

    public static final Codec<HazardType> CODEC = Codec.STRING.xmap(
            s -> HazardType.valueOf(s.toUpperCase(Locale.ROOT)),
            t -> t.name().toLowerCase(Locale.ROOT)
    );
}
