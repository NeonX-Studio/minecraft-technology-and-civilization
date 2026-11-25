package com.nightcity.tcivilization.api.world;

import net.minecraft.resources.Identifier;

/**
 * Aquifer depth, host lithology, and mineralization at a world position (10 §5.11.2, §12.1).
 */
public record GroundwaterInfo(
        float waterTableDepthMeters,
        Identifier aquiferRock,
        float salinityGPerL,
        boolean artesian
) {
}
