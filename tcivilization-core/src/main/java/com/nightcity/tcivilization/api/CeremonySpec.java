package com.nightcity.tcivilization.api;

import java.util.List;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Server-to-client ceremony presentation specification (03 §12.1, 31 §9).
 */
public record CeremonySpec(
        Identifier id,
        CeremonyScale scale,
        Component title,
        Component subtitle,
        Identifier emblem,
        List<Component> unlocks,
        @Nullable Identifier music,
        @Nullable Identifier skinAfter
) {
    public CeremonySpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(scale, "scale");
        Objects.requireNonNull(title, "title");
        subtitle = subtitle == null ? Component.empty() : subtitle;
        Objects.requireNonNull(emblem, "emblem");
        unlocks = unlocks == null ? List.of() : unlocks.subList(0, Math.min(8, unlocks.size()));
    }
}
