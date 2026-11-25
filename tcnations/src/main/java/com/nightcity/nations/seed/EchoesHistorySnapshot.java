package com.nightcity.nations.seed;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.Identifier;

/**
 * Decoupled snapshot of terminal world-history state read from tcechoes via soft reflection bridge (23 §5.1.1).
 */
public record EchoesHistorySnapshot(
        boolean loaded,
        List<PolityCandidate> polities,
        Map<Long, HistoricRelation> relations
) {
    public enum HistoricRelation {
        NEUTRAL,
        FEUD,
        ALLIANCE
    }

    public record PolityCandidate(
            int polityId,
            String name,
            Identifier culture,
            int capitalRegionX,
            int capitalRegionZ,
            int eraIndex,
            double population,
            boolean survivor,
            List<Long> holySites
    ) {
        public PolityCandidate {
            if (name == null || name.isBlank()) {
                name = "Ancient Polity " + polityId;
            }
            culture = culture == null ? Identifier.fromNamespaceAndPath("tcnations", "culture/river_valley") : culture;
            eraIndex = Math.max(0, Math.min(10, eraIndex));
            population = Math.max(0.0D, population);
            holySites = holySites == null ? List.of() : List.copyOf(holySites);
        }
    }

    public EchoesHistorySnapshot {
        polities = polities == null ? List.of() : List.copyOf(polities);
        relations = relations == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(relations));
    }

    public static EchoesHistorySnapshot unloaded() {
        return new EchoesHistorySnapshot(false, List.of(), Map.of());
    }

    public static long pairKey(int polityA, int polityB) {
        int lo = Math.min(polityA, polityB);
        int hi = Math.max(polityA, polityB);
        return ((long) lo << 32) | (hi & 0xffff_ffffL);
    }

    public HistoricRelation relationBetween(int polityA, int polityB) {
        if (polityA == polityB) {
            return HistoricRelation.NEUTRAL;
        }
        return Objects.requireNonNullElse(relations.get(pairKey(polityA, polityB)), HistoricRelation.NEUTRAL);
    }
}
