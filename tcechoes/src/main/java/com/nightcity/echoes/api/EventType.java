package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import java.util.Locale;

/**
 * Historical and live-history event categories (18 §5.1.7, §5.8.1).
 */
public enum EventType {
    FOUNDED,
    MIGRATED,
    WAR,
    BATTLE,
    SACK,
    ALLIANCE,
    COLLAPSE,
    DISASTER,
    INVENTION,
    MONUMENT,
    HUNT_EXTINCTION,
    PLAGUE,
    FAMINE,
    SEA_RAID,
    HERESY,
    LOST_EXPEDITION,
    ERA_ADVANCED,
    STELE_RAISED;

    public static final Codec<EventType> CODEC = Codec.STRING.xmap(
            s -> EventType.valueOf(s.toUpperCase(Locale.ROOT)),
            t -> t.name().toLowerCase(Locale.ROOT)
    );

    public static EventType fromOrdinal(int ord) {
        EventType[] vals = values();
        return (ord >= 0 && ord < vals.length) ? vals[ord] : FOUNDED;
    }
}
