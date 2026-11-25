package com.nightcity.tcivilization.api;

import net.minecraft.resources.Identifier;

/**
 * Contributes a weighted [0, 1] pressure component to a region's load calculation (03 §5.2.4).
 */
public interface RegionPressureContributor {
    Identifier id();

    float weight();

    float evaluate(RegionHandle region);
}
