package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

/**
 * Post-start live history event entry appended to history.bin live_log and EchoesSavedData (18 §5.8.1, §11.2).
 */
public record LiveHistoryEntry(
        int year,
        EventType type,
        BlockPos pos,
        List<UUID> actors,
        Identifier sourceMod,
        int importance
) {
    public static final Codec<LiveHistoryEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("year").forGetter(LiveHistoryEntry::year),
            EventType.CODEC.fieldOf("type").forGetter(LiveHistoryEntry::type),
            BlockPos.CODEC.fieldOf("pos").forGetter(LiveHistoryEntry::pos),
            UUIDUtil.CODEC.listOf().optionalFieldOf("actors", List.of()).forGetter(LiveHistoryEntry::actors),
            Identifier.CODEC.fieldOf("source_mod").forGetter(LiveHistoryEntry::sourceMod),
            Codec.INT.fieldOf("importance").forGetter(LiveHistoryEntry::importance)
    ).apply(i, LiveHistoryEntry::new));

    public LiveHistoryEntry {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(pos, "pos");
        actors = actors == null ? List.of() : List.copyOf(actors);
        Objects.requireNonNull(sourceMod, "sourceMod");
    }
}
