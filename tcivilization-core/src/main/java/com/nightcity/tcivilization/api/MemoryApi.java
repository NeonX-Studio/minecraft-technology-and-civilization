package com.nightcity.tcivilization.api;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Server-side civilization memory recording and querying facade (03 §5.4, §12.1).
 */
public interface MemoryApi {
    @Nullable
    MemoryEntry record(
            UUID civ,
            Identifier type,
            int importance,
            @Nullable GlobalPos pos,
            List<UUID> actors,
            CompoundTag payload
    );

    Stream<MemoryEntry> query(UUID civ, MemoryQuery query);
}
