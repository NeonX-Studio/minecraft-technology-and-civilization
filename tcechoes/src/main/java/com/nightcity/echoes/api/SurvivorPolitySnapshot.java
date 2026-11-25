package com.nightcity.echoes.api;

import java.util.List;
import java.util.Objects;
import net.minecraft.resources.Identifier;

/**
 * Read-only end-state snapshot of a surviving polity exposed for tcnations seeding (18 §3.6, §5.1.8, 23 §5.1.1).
 */
public record SurvivorPolitySnapshot(
        int polityId,
        String name,
        int cultureId,
        Identifier cultureIdentifier,
        LanguageTemplate language,
        Deity deity,
        long capitalRegion,
        List<Long> territoryRegions,
        List<Long> holySiteRegions,
        int peakEra,
        int finalEra,
        int finalPopulation,
        double aggression,
        List<Integer> feudPolityIds,
        List<Integer> oldAllyPolityIds,
        List<Integer> ancestralSiteIds
) {
    public SurvivorPolitySnapshot {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(cultureIdentifier, "cultureIdentifier");
        Objects.requireNonNull(language, "language");
        Objects.requireNonNull(deity, "deity");
        territoryRegions = territoryRegions == null ? List.of() : List.copyOf(territoryRegions);
        holySiteRegions = holySiteRegions == null ? List.of() : List.copyOf(holySiteRegions);
        feudPolityIds = feudPolityIds == null ? List.of() : List.copyOf(feudPolityIds);
        oldAllyPolityIds = oldAllyPolityIds == null ? List.of() : List.copyOf(oldAllyPolityIds);
        ancestralSiteIds = ancestralSiteIds == null ? List.of() : List.copyOf(ancestralSiteIds);
    }
}
