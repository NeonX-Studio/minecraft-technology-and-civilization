package com.nightcity.tcivilization.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/**
 * Civilization chronicle / memory log entry (03 §5.4.1, §11).
 */
public record MemoryEntry(
        long id,
        long tick,
        Identifier type,
        UUID civ,
        OptionalLong region,
        Optional<GlobalPos> pos,
        List<UUID> actors,
        int importance,
        CompoundTag payload
) {
    public static final int MAX_PAYLOAD_BYTES = 2 * 1024;

    public static final Codec<MemoryEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("id").forGetter(MemoryEntry::id),
            Codec.LONG.fieldOf("tick").forGetter(MemoryEntry::tick),
            Identifier.CODEC.fieldOf("type").forGetter(MemoryEntry::type),
            UUIDUtil.CODEC.fieldOf("civ").forGetter(MemoryEntry::civ),
            Codec.LONG.optionalFieldOf("region").xmap(
                    opt -> opt.map(OptionalLong::of).orElseGet(OptionalLong::empty),
                    optLong -> optLong.isPresent() ? Optional.of(optLong.getAsLong()) : Optional.empty()
            ).forGetter(MemoryEntry::region),
            GlobalPos.CODEC.optionalFieldOf("pos").forGetter(MemoryEntry::pos),
            UUIDUtil.CODEC.listOf().optionalFieldOf("actors", List.of()).forGetter(MemoryEntry::actors),
            Codec.intRange(1, 5).fieldOf("importance").forGetter(MemoryEntry::importance),
            CompoundTag.CODEC.optionalFieldOf("payload", new CompoundTag()).forGetter(e -> e.payload.copy())
    ).apply(i, MemoryEntry::new));

    public MemoryEntry {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(civ, "civ");
        region = region == null ? OptionalLong.empty() : region;
        pos = pos == null ? Optional.empty() : pos;
        actors = actors == null ? List.of() : List.copyOf(actors);
        importance = Math.clamp(importance, 1, 5);
        payload = payload == null ? new CompoundTag() : payload.copy();
    }
}
