package com.nightcity.nations.api;

import com.mojang.serialization.Codec;
import java.util.Locale;

public enum EconomyCategory {
    FOOD,
    TIMBER,
    STONE,
    METAL,
    TEXTILE,
    FUEL_ENERGY,
    MANUFACTURED,
    LUXURY;

    public static final Codec<EconomyCategory> CODEC = Codec.STRING.xmap(
            s -> EconomyCategory.valueOf(s.toUpperCase(Locale.ROOT)),
            c -> c.name().toLowerCase(Locale.ROOT)
    );
}
