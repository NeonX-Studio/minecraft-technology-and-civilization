package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.tcivilization.api.RegionApi;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;

/**
 * Simulated or live-recorded ruin site placement (18 §5.4, §11.1).
 */
public record Site(
        int id,
        SiteType type,
        BlockPos anchor,
        int cultureId,
        int polityId,
        int builtYear,
        int abandonedYear,
        ArchStyle style,
        SiteOrigin origin,
        float weathering,
        byte burialDepth,
        List<Integer> legendSeeds,
        int flags
) {
    public static final int FLAG_HOLY_SITE = 1 << 0;
    public static final int FLAG_SACKED = 1 << 1;
    public static final int FLAG_ASH_SEALED = 1 << 2;
    public static final int FLAG_BILINGUAL_TABLET = 1 << 3;
    public static final int FLAG_LOOTED = 1 << 4;

    public static final Codec<Site> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("id").forGetter(Site::id),
            SiteType.CODEC.fieldOf("type").forGetter(Site::type),
            BlockPos.CODEC.fieldOf("anchor").forGetter(Site::anchor),
            Codec.INT.fieldOf("culture_id").forGetter(Site::cultureId),
            Codec.INT.fieldOf("polity_id").forGetter(Site::polityId),
            Codec.INT.fieldOf("built_year").forGetter(Site::builtYear),
            Codec.INT.fieldOf("abandoned_year").forGetter(Site::abandonedYear),
            ArchStyle.CODEC.fieldOf("style").forGetter(Site::style),
            SiteOrigin.CODEC.fieldOf("origin").forGetter(Site::origin),
            Codec.FLOAT.fieldOf("weathering").forGetter(Site::weathering),
            Codec.BYTE.fieldOf("burial_depth").forGetter(Site::burialDepth),
            Codec.INT.listOf().optionalFieldOf("legend_seeds", List.of()).forGetter(Site::legendSeeds),
            Codec.INT.optionalFieldOf("flags", 0).forGetter(Site::flags)
    ).apply(i, Site::new));

    public Site {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(style, "style");
        Objects.requireNonNull(origin, "origin");
        weathering = Math.clamp(weathering, 0.0F, 1.0F);
        burialDepth = (byte) Math.clamp(burialDepth, 0, 6);
        legendSeeds = legendSeeds == null ? List.of() : List.copyOf(legendSeeds);
    }

    public int regionX() {
        return Math.floorDiv(anchor.getX(), 512);
    }

    public int regionZ() {
        return Math.floorDiv(anchor.getZ(), 512);
    }

    public long regionKey() {
        return RegionApi.regionKey(regionX(), regionZ());
    }

    public SiteSummary toSummary() {
        return new SiteSummary(
                id, type, anchor, regionKey(), cultureId, polityId,
                builtYear, abandonedYear, style, origin, weathering, burialDepth, flags
        );
    }
}
