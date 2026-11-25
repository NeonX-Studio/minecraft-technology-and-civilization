package com.nightcity.tcivilization.api.world;

import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;

/**
 * Composite 0-100 suitability score with sub-component breakdown (10 §5.18, §12.1).
 */
public record SiteScore(int total, Map<String, Integer> components, List<Component> notes) {
    public SiteScore {
        components = components == null ? Map.of() : Map.copyOf(components);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }
}
