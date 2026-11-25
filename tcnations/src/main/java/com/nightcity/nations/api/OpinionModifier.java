package com.nightcity.nations.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.resources.Identifier;

public record OpinionModifier(
        Identifier id,
        double initial,
        long createdTick,
        int halfLifeDays
) {
    public static final Codec<OpinionModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(OpinionModifier::id),
            Codec.DOUBLE.fieldOf("initial").forGetter(OpinionModifier::initial),
            Codec.LONG.optionalFieldOf("created_tick", 0L).forGetter(OpinionModifier::createdTick),
            Codec.INT.optionalFieldOf("half_life_days", -1).forGetter(OpinionModifier::halfLifeDays)
    ).apply(instance, OpinionModifier::new));

    public OpinionModifier {
        Objects.requireNonNull(id, "id");
    }

    public double valueAt(long currentTick) {
        if (halfLifeDays < 0) {
            return initial;
        }
        if (halfLifeDays == 0) {
            return 0.0D;
        }
        double elapsedDays = Math.max(0.0D, (currentTick - createdTick) / 24_000.0D);
        double decayed = initial * StrictMath.pow(0.5D, elapsedDays / halfLifeDays);
        return StrictMath.abs(decayed) < 0.5D ? 0.0D : decayed;
    }
}
