package com.nightcity.nations.api;

import com.mojang.serialization.Codec;
import java.util.Locale;

public enum NationStatus {
    ACTIVE,
    COLLAPSED;

    public static final Codec<NationStatus> CODEC = Codec.STRING.xmap(
            s -> NationStatus.valueOf(s.toUpperCase(Locale.ROOT)),
            st -> st.name().toLowerCase(Locale.ROOT)
    );
}
