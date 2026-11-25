package com.nightcity.tcivilization.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.tcivilization.TCivilization;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;

/**
 * Compact current task summary stored in PersonRecord (03 §5.7.2).
 */
public record TaskSummary(Identifier type, Optional<GlobalPos> targetPos, float progress) {
    public static final Identifier IDLE_ID = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "idle");

    public static final Codec<TaskSummary> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("type").forGetter(TaskSummary::type),
            GlobalPos.CODEC.optionalFieldOf("target").forGetter(TaskSummary::targetPos),
            Codec.FLOAT.optionalFieldOf("progress", 0.0f).forGetter(TaskSummary::progress)
    ).apply(i, TaskSummary::new));

    public TaskSummary {
        Objects.requireNonNull(type, "type");
        targetPos = targetPos == null ? Optional.empty() : targetPos;
        progress = Math.clamp(progress, 0.0f, 1.0f);
    }

    public static TaskSummary idle() {
        return new TaskSummary(IDLE_ID, Optional.empty(), 0.0f);
    }
}
