package com.nightcity.echoes.api;

import java.util.Objects;
import net.minecraft.core.BlockPos;

/**
 * Lightweight read-only projection of a Site for EchoesApi queries (18 §12.1).
 */
public record SiteSummary(
        int id,
        SiteType type,
        BlockPos anchor,
        long regionKey,
        int cultureId,
        int polityId,
        int builtYear,
        int abandonedYear,
        ArchStyle style,
        SiteOrigin origin,
        float weathering,
        byte burialDepth,
        int flags
) {
    public SiteSummary {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(style, "style");
        Objects.requireNonNull(origin, "origin");
    }
}
