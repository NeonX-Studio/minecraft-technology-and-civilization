package com.nightcity.tcivilization.api;

import java.util.Optional;
import java.util.OptionalLong;
import net.minecraft.resources.Identifier;

/**
 * Query filter and pagination parameters for MemoryApi.query (03 §5.4, §12.1).
 */
public record MemoryQuery(
        int minImportance,
        Optional<Identifier> type,
        OptionalLong region,
        int offset,
        int limit
) {
    public MemoryQuery {
        minImportance = Math.clamp(minImportance, 1, 5);
        type = type == null ? Optional.empty() : type;
        region = region == null ? OptionalLong.empty() : region;
        offset = Math.max(0, offset);
        limit = Math.clamp(limit, 1, 500);
    }

    public static MemoryQuery page(int pageIndex, int pageSize) {
        int clampedSize = Math.clamp(pageSize, 1, 50);
        return new MemoryQuery(1, Optional.empty(), OptionalLong.empty(), Math.max(0, pageIndex) * clampedSize, clampedSize);
    }
}
