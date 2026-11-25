package com.nightcity.tcivilization.api;

import com.mojang.serialization.Codec;
import java.util.Locale;

/**
 * Four lifecycle/LOD states of a PersonRecord (03 §5.7.1).
 */
public enum PersonState {
    EMBODIED,
    PROXY,
    ABSTRACT,
    DEAD;

    public static final Codec<PersonState> CODEC = Codec.STRING.xmap(
            s -> PersonState.valueOf(s.toUpperCase(Locale.ROOT)),
            st -> st.name().toLowerCase(Locale.ROOT)
    );
}
