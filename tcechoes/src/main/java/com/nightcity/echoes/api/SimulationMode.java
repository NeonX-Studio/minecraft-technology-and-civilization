package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import java.util.Locale;

/**
 * World history pre-simulation grid mode (18 §5.1.1, §5.2.3).
 */
public enum SimulationMode {
    PLANET(0),
    STANDALONE(1);

    public static final Codec<SimulationMode> CODEC = Codec.STRING.xmap(
            s -> SimulationMode.valueOf(s.toUpperCase(Locale.ROOT)),
            m -> m.name().toLowerCase(Locale.ROOT)
    );

    private final byte id;

    SimulationMode(int id) {
        this.id = (byte) id;
    }

    public byte id() {
        return id;
    }

    public static SimulationMode fromId(int id) {
        return id == 1 ? STANDALONE : PLANET;
    }
}
