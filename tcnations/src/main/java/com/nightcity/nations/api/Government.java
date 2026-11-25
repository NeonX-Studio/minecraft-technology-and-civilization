package com.nightcity.nations.api;

import com.mojang.serialization.Codec;
import java.util.Locale;

public enum Government {
    TRIBE(0, 2),
    CHIEFDOM(1, 3),
    CITY_STATE(2, 5),
    MONARCHY(3, 6),
    FEUDAL(4, 5),
    MERCHANT_REPUBLIC(4, 7),
    EMPIRE(5, 8),
    CONSTITUTIONAL(6, 10),
    FEDERATION(8, 10);

    public static final Codec<Government> CODEC = Codec.STRING.xmap(
            s -> Government.valueOf(s.toUpperCase(Locale.ROOT)),
            g -> g.name().toLowerCase(Locale.ROOT)
    );

    private final int minEra;
    private final int maxEra;

    Government(int minEra, int maxEra) {
        this.minEra = minEra;
        this.maxEra = maxEra;
    }

    public int minEra() {
        return minEra;
    }

    public int maxEra() {
        return maxEra;
    }
}
