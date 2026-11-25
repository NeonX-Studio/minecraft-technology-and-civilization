package com.nightcity.atlas.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;

/**
 * Per-chunk survey knowledge packed into 24 bits: sl(3) | heightKnown(1) | errClass(4) | observedDay(16) (22 §5.1, §11.1).
 */
public record ChunkKnowledge(SurveyLevel sl, boolean heightKnown, int errClass, int observedDay) {
    public static final ChunkKnowledge UNKNOWN = new ChunkKnowledge(SurveyLevel.UNKNOWN, false, 8, 0);

    public static final Codec<ChunkKnowledge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SurveyLevel.CODEC.fieldOf("sl").forGetter(ChunkKnowledge::sl),
            Codec.BOOL.optionalFieldOf("height", false).forGetter(ChunkKnowledge::heightKnown),
            Codec.INT.optionalFieldOf("errClass", 8).forGetter(ChunkKnowledge::errClass),
            Codec.INT.optionalFieldOf("observedDay", 0).forGetter(ChunkKnowledge::observedDay)
    ).apply(instance, ChunkKnowledge::new));

    public ChunkKnowledge {
        Objects.requireNonNull(sl, "sl");
        errClass = Math.clamp(errClass, 0, 15);
        observedDay = Math.clamp(observedDay, 0, 0xFFFF);
    }

    public static int errClassForEpsilon(int epsilonBlocks) {
        if (epsilonBlocks <= 1) {
            return 0;
        }
        int ceilLog2 = 32 - Integer.numberOfLeadingZeros(epsilonBlocks - 1);
        return Math.clamp(ceilLog2, 0, 8);
    }

    public int epsilon() {
        return sl == SurveyLevel.UNKNOWN ? 256 : (1 << Math.min(errClass, 12));
    }

    public int pack() {
        return (sl.code() & 0x7)
                | (heightKnown ? 0x8 : 0)
                | ((errClass & 0xF) << 4)
                | ((observedDay & 0xFFFF) << 8);
    }

    public static ChunkKnowledge unpack(int packed) {
        if (packed == 0) {
            return UNKNOWN;
        }
        SurveyLevel level = SurveyLevel.fromCode(packed & 0x7);
        boolean h = (packed & 0x8) != 0;
        int err = (packed >>> 4) & 0xF;
        int day = (packed >>> 8) & 0xFFFF;
        return new ChunkKnowledge(level, h, err, day);
    }

    /**
     * Commutative and idempotent merge rule (22 §5.1).
     */
    public static ChunkKnowledge merge(ChunkKnowledge a, ChunkKnowledge b) {
        if (a == null || a.sl == SurveyLevel.UNKNOWN) {
            return b == null ? UNKNOWN : b;
        }
        if (b == null || b.sl == SurveyLevel.UNKNOWN) {
            return a;
        }
        ChunkKnowledge base;
        if (b.sl.code() > a.sl.code() || (b.sl == a.sl && b.errClass < a.errClass)) {
            base = b;
        } else {
            base = a;
        }
        boolean mergedHeight = a.heightKnown || b.heightKnown;
        int mergedDay = Math.max(a.observedDay, b.observedDay);
        return new ChunkKnowledge(base.sl, mergedHeight, base.errClass, mergedDay);
    }

    /**
     * Age desaturation factor for data older than 1 MC year (48 days) (22 §5.1).
     */
    public double freshnessDesaturation(int currentDay) {
        int age = Math.max(0, currentDay - observedDay);
        if (age <= 48) {
            return 0.0;
        }
        return Math.min(0.6, age / 480.0);
    }
}
