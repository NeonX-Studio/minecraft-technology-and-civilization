package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.tcivilization.api.VersionedShared;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/**
 * Versioned heritage designation entry for optimistic concurrency (18 §5.9.1, §11.2).
 */
public record HeritageEntry(
        int siteId,
        UUID civId,
        Level level,
        long since,
        int baselineBlocks,
        long version
) implements VersionedShared {
    public enum Level {
        LISTED,
        PROTECTED,
        SACRED;

        public static final Codec<Level> CODEC = Codec.STRING.xmap(
                s -> Level.valueOf(s.toUpperCase(Locale.ROOT)),
                l -> l.name().toLowerCase(Locale.ROOT)
        );
    }

    public static final Codec<HeritageEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("site_id").forGetter(HeritageEntry::siteId),
            UUIDUtil.CODEC.fieldOf("civ_id").forGetter(HeritageEntry::civId),
            Level.CODEC.fieldOf("level").forGetter(HeritageEntry::level),
            Codec.LONG.fieldOf("since").forGetter(HeritageEntry::since),
            Codec.INT.fieldOf("baseline_blocks").forGetter(HeritageEntry::baselineBlocks),
            Codec.LONG.optionalFieldOf("version", 1L).forGetter(HeritageEntry::version)
    ).apply(i, HeritageEntry::new));

    public HeritageEntry {
        Objects.requireNonNull(civId, "civId");
        Objects.requireNonNull(level, "level");
        if (version < 1L) {
            version = 1L;
        }
    }

    public HeritageEntry bump(Level newLevel) {
        return new HeritageEntry(siteId, civId, newLevel, since, baselineBlocks, version + 1L);
    }
}
