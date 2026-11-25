package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Per-region data stored in the tcechoes:history RegionLayer (18 §11.3, P1 Deliverable 2).
 * Stores ruin site placements, ancient route segments, ancient mines, and weathering parameters.
 */
public record HistoryCell(
        int formatVersion,
        List<SiteEntry> sites,
        List<RouteEntry> routes,
        List<MineEntry> mines,
        byte hazardFlags,
        short dominantCulture,
        short lastPolity,
        float climateWeatheringFactor,
        float sedimentRate,
        List<Integer> legendSeedIds
) {
    public static final int FORMAT_VERSION = 1;
    public static final int MAX_SITES_PER_CELL = 16;

    public static final byte HAZARD_BATTLEFIELD = 1 << 0;
    public static final byte HAZARD_FALLEN_REALM = 1 << 1;
    public static final byte HAZARD_BANDITRY = 1 << 2;
    public static final byte HAZARD_SACKED_CITY = 1 << 3;
    public static final byte HAZARD_HOLY_SITE = 1 << 4;
    public static final byte HAZARD_DISASTER = 1 << 5;

    public record SiteEntry(
            int siteId,
            SiteType type,
            BlockPos anchor,
            int cultureId,
            int polityId,
            ArchStyle style,
            float weathering,
            byte burialDepth
    ) {
        public static final Codec<SiteEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("id").forGetter(SiteEntry::siteId),
                SiteType.CODEC.fieldOf("type").forGetter(SiteEntry::type),
                BlockPos.CODEC.fieldOf("anchor").forGetter(SiteEntry::anchor),
                Codec.INT.fieldOf("culture").forGetter(SiteEntry::cultureId),
                Codec.INT.fieldOf("polity").forGetter(SiteEntry::polityId),
                ArchStyle.CODEC.fieldOf("style").forGetter(SiteEntry::style),
                Codec.FLOAT.fieldOf("w").forGetter(SiteEntry::weathering),
                Codec.BYTE.fieldOf("d").forGetter(SiteEntry::burialDepth)
        ).apply(i, SiteEntry::new));

        public SiteEntry {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(anchor, "anchor");
            Objects.requireNonNull(style, "style");
            weathering = Math.clamp(weathering, 0.0F, 1.0F);
            burialDepth = (byte) Math.clamp(burialDepth, 0, 6);
        }

        public static SiteEntry fromSite(Site site) {
            return new SiteEntry(
                    site.id(), site.type(), site.anchor(), site.cultureId(),
                    site.polityId(), site.style(), site.weathering(), site.burialDepth()
            );
        }
    }

    public record RouteEntry(int routeId, RouteKind kind, float weathering) {
        public static final Codec<RouteEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("id").forGetter(RouteEntry::routeId),
                RouteKind.CODEC.fieldOf("kind").forGetter(RouteEntry::kind),
                Codec.FLOAT.fieldOf("w").forGetter(RouteEntry::weathering)
        ).apply(i, RouteEntry::new));

        public RouteEntry {
            Objects.requireNonNull(kind, "kind");
            weathering = Math.clamp(weathering, 0.0F, 1.0F);
        }
    }

    public record MineEntry(
            int mineId,
            String depositId,
            BlockPos entrance,
            float minedFraction,
            float weathering
    ) {
        public static final Codec<MineEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("id").forGetter(MineEntry::mineId),
                Codec.STRING.fieldOf("deposit").forGetter(MineEntry::depositId),
                BlockPos.CODEC.fieldOf("entrance").forGetter(MineEntry::entrance),
                Codec.FLOAT.fieldOf("mined").forGetter(MineEntry::minedFraction),
                Codec.FLOAT.fieldOf("w").forGetter(MineEntry::weathering)
        ).apply(i, MineEntry::new));

        public MineEntry {
            Objects.requireNonNull(depositId, "depositId");
            Objects.requireNonNull(entrance, "entrance");
            minedFraction = Math.clamp(minedFraction, 0.0F, 0.6F);
            weathering = Math.clamp(weathering, 0.0F, 1.0F);
        }

        public static MineEntry fromMine(AncientMine mine) {
            return new MineEntry(mine.id(), mine.depositId(), mine.entrance(), mine.minedFraction(), mine.weathering());
        }
    }

    public static final Codec<HistoryCell> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("formatVersion", FORMAT_VERSION).forGetter(HistoryCell::formatVersion),
            SiteEntry.CODEC.listOf().optionalFieldOf("sites", List.of()).forGetter(HistoryCell::sites),
            RouteEntry.CODEC.listOf().optionalFieldOf("routes", List.of()).forGetter(HistoryCell::routes),
            MineEntry.CODEC.listOf().optionalFieldOf("mines", List.of()).forGetter(HistoryCell::mines),
            Codec.BYTE.optionalFieldOf("hazardFlags", (byte) 0).forGetter(HistoryCell::hazardFlags),
            Codec.SHORT.optionalFieldOf("dominantCulture", (short) -1).forGetter(HistoryCell::dominantCulture),
            Codec.SHORT.optionalFieldOf("lastPolity", (short) -1).forGetter(HistoryCell::lastPolity),
            Codec.FLOAT.optionalFieldOf("kClim", 1.0F).forGetter(HistoryCell::climateWeatheringFactor),
            Codec.FLOAT.optionalFieldOf("sedimentRate", 1.0F).forGetter(HistoryCell::sedimentRate),
            Codec.INT.listOf().optionalFieldOf("legendSeeds", List.of()).forGetter(HistoryCell::legendSeedIds)
    ).apply(i, HistoryCell::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, HistoryCell> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public HistoryCell {
        if (sites == null || sites.isEmpty()) {
            sites = List.of();
        } else if (sites.size() > MAX_SITES_PER_CELL) {
            sites = List.copyOf(sites.subList(0, MAX_SITES_PER_CELL));
        } else {
            sites = List.copyOf(sites);
        }
        routes = routes == null ? List.of() : List.copyOf(routes);
        mines = mines == null ? List.of() : List.copyOf(mines);
        climateWeatheringFactor = Math.clamp(climateWeatheringFactor, 0.5F, 3.0F);
        sedimentRate = Math.clamp(sedimentRate, 0.5F, 4.0F);
        legendSeedIds = legendSeedIds == null ? List.of() : List.copyOf(legendSeedIds);
    }

    public static HistoryCell empty() {
        return new HistoryCell(FORMAT_VERSION, List.of(), List.of(), List.of(), (byte) 0, (short) -1, (short) -1, 1.0F, 1.0F, List.of());
    }

    public boolean isEmpty() {
        return sites.isEmpty()
                && routes.isEmpty()
                && mines.isEmpty()
                && hazardFlags == 0
                && dominantCulture < 0
                && lastPolity < 0
                && legendSeedIds.isEmpty();
    }

    public short[] siteIds() {
        short[] out = new short[sites.size()];
        for (int i = 0; i < sites.size(); i++) {
            out[i] = (short) Math.min(Short.MAX_VALUE, sites.get(i).siteId());
        }
        return out;
    }

    public int[] legendSeedIdArray() {
        int[] out = new int[legendSeedIds.size()];
        for (int i = 0; i < legendSeedIds.size(); i++) {
            out[i] = legendSeedIds.get(i);
        }
        return out;
    }

    public float meanSiteWeathering() {
        if (sites.isEmpty()) {
            return 0.0F;
        }
        double sum = 0.0D;
        for (SiteEntry s : sites) {
            sum += s.weathering();
        }
        return (float) (sum / sites.size());
    }

    public HistoryCell withSite(Site site) {
        List<SiteEntry> next = new ArrayList<>(sites);
        next.removeIf(e -> e.siteId() == site.id());
        if (next.size() < MAX_SITES_PER_CELL) {
            next.add(SiteEntry.fromSite(site));
        }
        return new HistoryCell(
                formatVersion, next, routes, mines, hazardFlags, dominantCulture,
                lastPolity, climateWeatheringFactor, sedimentRate, legendSeedIds
        );
    }
}
