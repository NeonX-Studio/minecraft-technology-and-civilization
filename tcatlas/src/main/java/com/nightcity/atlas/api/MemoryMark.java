package com.nightcity.atlas.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.UUID;

/**
 * E0 personal fading memory marker (max 3 per player; not a HUD-navigable waypoint, fades with chunk memory, 22 §2.1, §5.3).
 */
public record MemoryMark(
        UUID id,
        Identifier dimension,
        BlockPos pos,
        long placedGameSeconds,
        String label
) {
    public static final int MAX_E0_MARKS = 3;

    public static final Codec<MemoryMark> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(MemoryMark::id),
            Identifier.CODEC.optionalFieldOf("dim", Identifier.withDefaultNamespace("overworld")).forGetter(MemoryMark::dimension),
            BlockPos.CODEC.fieldOf("pos").forGetter(MemoryMark::pos),
            Codec.LONG.fieldOf("placedSeconds").forGetter(MemoryMark::placedGameSeconds),
            Codec.STRING.optionalFieldOf("label", "").forGetter(MemoryMark::label)
    ).apply(instance, MemoryMark::new));

    public MemoryMark {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(pos, "pos");
        label = label == null ? "" : (label.length() > 32 ? label.substring(0, 32) : label);
    }

    public int chunkX() {
        return pos.getX() >> 4;
    }

    public int chunkZ() {
        return pos.getZ() >> 4;
    }
}
