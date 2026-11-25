package com.nightcity.tcivilization.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

/**
 * Completed milestone record persisted by core so modules can read milestones without requiring tctechnology (03 §5.5.1).
 */
public record MilestoneRecord(
        Identifier id,
        UUID civ,
        long completedTick,
        List<UUID> contributors
) {
    public static final Codec<MilestoneRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("id").forGetter(MilestoneRecord::id),
            UUIDUtil.CODEC.fieldOf("civ").forGetter(MilestoneRecord::civ),
            Codec.LONG.fieldOf("completed_tick").forGetter(MilestoneRecord::completedTick),
            UUIDUtil.CODEC.listOf().optionalFieldOf("contributors", List.of()).forGetter(MilestoneRecord::contributors)
    ).apply(i, MilestoneRecord::new));

    public MilestoneRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(civ, "civ");
        contributors = contributors == null ? List.of() : List.copyOf(contributors);
    }
}
