package com.nightcity.tcivilization.api.world;

import net.minecraft.resources.Identifier;

/**
 * Soil parent material, texture, pH, drainage, and regolith thickness (10 §5.18.3, §12.1).
 */
public record SoilParent(
        Identifier material,
        Identifier bedrock,
        Texture texture,
        float organicMatter,
        float ph,
        Drainage drainage,
        float depthBlocks
) {
    public enum Texture {
        SAND,
        LOAM,
        CLAY,
        SILT,
        PEAT
    }

    public enum Drainage {
        POOR,
        MODERATE,
        GOOD,
        EXCESSIVE
    }
}
