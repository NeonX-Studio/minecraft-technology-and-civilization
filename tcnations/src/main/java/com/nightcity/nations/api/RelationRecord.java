package com.nightcity.nations.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

public record RelationRecord(
        UUID a,
        UUID b,
        double trust,
        double fearAB,
        double fearBA,
        List<OpinionModifier> modifiers,
        boolean contacted,
        long firstContactTick
) {
    public static final Codec<RelationRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("a").forGetter(RelationRecord::a),
            UUIDUtil.CODEC.fieldOf("b").forGetter(RelationRecord::b),
            Codec.DOUBLE.optionalFieldOf("trust", 50.0D).forGetter(RelationRecord::trust),
            Codec.DOUBLE.optionalFieldOf("fear_ab", 0.0D).forGetter(RelationRecord::fearAB),
            Codec.DOUBLE.optionalFieldOf("fear_ba", 0.0D).forGetter(RelationRecord::fearBA),
            OpinionModifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(RelationRecord::modifiers),
            Codec.BOOL.optionalFieldOf("contacted", false).forGetter(RelationRecord::contacted),
            Codec.LONG.optionalFieldOf("first_contact_tick", -1L).forGetter(RelationRecord::firstContactTick)
    ).apply(instance, RelationRecord::new));

    public RelationRecord {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        trust = Math.max(0.0D, Math.min(100.0D, trust));
        fearAB = Math.max(0.0D, Math.min(100.0D, fearAB));
        fearBA = Math.max(0.0D, Math.min(100.0D, fearBA));
        modifiers = modifiers == null ? List.of() : List.copyOf(modifiers);
    }

    public double opinionAt(long currentTick) {
        double sum = 0.0D;
        for (OpinionModifier mod : modifiers) {
            sum += mod.valueAt(currentTick);
        }
        return Math.max(-100.0D, Math.min(100.0D, sum));
    }
}
