package com.nightcity.tcivilization.api.world;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * Earthwork, bridge, tunnel, surface, and hazard cost estimate along a corridor (10 §5.18.4, §12.1).
 */
public record TerrainCost(
        double distanceBlocks,
        float slopeMax,
        float slopeMean,
        double earthworkM3,
        List<Span> bridges,
        List<Span> tunnels,
        Map<Identifier, Float> surfaceDistribution,
        List<HazardSpan> hazards,
        float totalCostIndex
) {
    public record Span(int startOffsetBlocks, int lengthBlocks, int reliefBlocks) {
    }

    public record HazardSpan(int startOffsetBlocks, int lengthBlocks, HazardType type, float risk) {
    }

    public TerrainCost {
        bridges = bridges == null ? List.of() : List.copyOf(bridges);
        tunnels = tunnels == null ? List.of() : List.copyOf(tunnels);
        surfaceDistribution = surfaceDistribution == null ? Map.of() : Map.copyOf(surfaceDistribution);
        hazards = hazards == null ? List.of() : List.copyOf(hazards);
    }
}
