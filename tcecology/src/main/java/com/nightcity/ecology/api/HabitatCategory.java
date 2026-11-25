package com.nightcity.ecology.api;

import com.mojang.serialization.Codec;
import java.util.Locale;

public enum HabitatCategory {
    FOREST,
    EDGE,
    SHRUB,
    GRASS,
    WETLAND,
    FRESHWATER,
    MOUNTAIN,
    ARID,
    BOREAL,
    TUNDRA,
    TROPICAL,
    COASTAL,
    PELAGIC,
    REEF,
    POLAR_SEA,
    UNDERGROUND_WATER;

    public static final Codec<HabitatCategory> CODEC = Codec.STRING.xmap(
            s -> HabitatCategory.valueOf(s.toUpperCase(Locale.ROOT)),
            h -> h.name().toLowerCase(Locale.ROOT)
    );
}
