package com.nightcity.tcivilization.client.api;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;

/**
 * Registration descriptor for a HUD widget (03 §12.2, 31 §3.2).
 */
public record HudWidgetSpec(
        Identifier id,
        HudLevel minLevel,
        HudAnchor defaultAnchor,
        int defaultX,
        int defaultY,
        int prefWidth,
        int prefHeight,
        int compactHeight,
        int priority,
        boolean defaultVisible,
        Optional<Identifier> unlockTech,
        int refreshTicks,
        Set<Identifier> dataChannels,
        Predicate<HudContext> relevant,
        Supplier<HudWidget> factory
) {
    public HudWidgetSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(minLevel, "minLevel");
        Objects.requireNonNull(defaultAnchor, "defaultAnchor");
        unlockTech = unlockTech == null ? Optional.empty() : unlockTech;
        dataChannels = dataChannels == null ? Set.of() : Set.copyOf(dataChannels);
        relevant = relevant == null ? ctx -> true : relevant;
        Objects.requireNonNull(factory, "factory");
    }
}
