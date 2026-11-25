package com.nightcity.echoes.sim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.echoes.api.AncientMine;
import com.nightcity.echoes.api.Culture;
import com.nightcity.echoes.api.EventEdge;
import com.nightcity.echoes.api.Extinction;
import com.nightcity.echoes.api.HistoryCell;
import com.nightcity.echoes.api.HistoryEvent;
import com.nightcity.echoes.api.LegendSeed;
import com.nightcity.echoes.api.LiveHistoryEntry;
import com.nightcity.echoes.api.Person;
import com.nightcity.echoes.api.Polity;
import com.nightcity.echoes.api.Relation;
import com.nightcity.echoes.api.Route;
import com.nightcity.echoes.api.SimulationMode;
import com.nightcity.echoes.api.Site;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.core.BlockPos;

/**
 * Complete immutable snapshot of simulated and persisted world history stored in <world>/tc/history.bin (18 §5.2.3, §11.1).
 */
public record WorldHistoryData(
        int formatVersion,
        int generatorVersion,
        long historySeed,
        SimulationMode mode,
        int windowCenterRx,
        int windowCenterRz,
        int windowRadiusRegions,
        int startYear,
        int endYear,
        long simulationMillis,
        List<Culture> cultures,
        List<Polity> polities,
        List<Person> persons,
        List<HistoryEvent> events,
        List<EventEdge> edges,
        List<Site> sites,
        List<Route> routes,
        List<AncientMine> mines,
        List<LegendSeed> legendSeeds,
        List<Extinction> extinctions,
        List<BilateralRelation> relations,
        List<PolityMeta> polityMetas,
        List<RegionSummaryEntry> regionIndex,
        List<HistoricalTraceRecord> historicalTraces,
        List<LiveHistoryEntry> liveLog
) {
    public static final int FORMAT_VERSION = 1;
    public static final int GENERATOR_VERSION = 1;

    public enum TraceKind {
        FAULT_SCARP,
        ASH_LAYER,
        FLOOD_TERRACE,
        ANCIENT_WORKINGS;

        public static final Codec<TraceKind> CODEC = Codec.STRING.xmap(
                s -> TraceKind.valueOf(s.toUpperCase(Locale.ROOT)),
                k -> k.name().toLowerCase(Locale.ROOT)
        );
    }

    public record BilateralRelation(int polityA, int polityB, Relation relation) {
        public static final Codec<BilateralRelation> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("a").forGetter(BilateralRelation::polityA),
                Codec.INT.fieldOf("b").forGetter(BilateralRelation::polityB),
                Relation.CODEC.fieldOf("rel").forGetter(BilateralRelation::relation)
        ).apply(i, BilateralRelation::new));
    }

    public record PolityMeta(
            int polityId,
            double aggression,
            List<Long> territoryRegions,
            List<Long> holySiteRegions
    ) {
        public static final Codec<PolityMeta> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("id").forGetter(PolityMeta::polityId),
                Codec.DOUBLE.fieldOf("aggression").forGetter(PolityMeta::aggression),
                Codec.LONG.listOf().optionalFieldOf("territory", List.of()).forGetter(PolityMeta::territoryRegions),
                Codec.LONG.listOf().optionalFieldOf("holy_sites", List.of()).forGetter(PolityMeta::holySiteRegions)
        ).apply(i, PolityMeta::new));

        public PolityMeta {
            territoryRegions = territoryRegions == null ? List.of() : List.copyOf(territoryRegions);
            holySiteRegions = holySiteRegions == null ? List.of() : List.copyOf(holySiteRegions);
        }
    }

    public record RegionSummaryEntry(
            long regionKey,
            byte hazardFlags,
            short dominantCulture,
            short lastPolity,
            float climateWeatheringFactor,
            float sedimentRate,
            List<Integer> siteIds,
            List<Integer> eventIds,
            List<Integer> legendSeedIds
    ) {
        public static final Codec<RegionSummaryEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("region").forGetter(RegionSummaryEntry::regionKey),
                Codec.BYTE.optionalFieldOf("hazards", (byte) 0).forGetter(RegionSummaryEntry::hazardFlags),
                Codec.SHORT.optionalFieldOf("culture", (short) -1).forGetter(RegionSummaryEntry::dominantCulture),
                Codec.SHORT.optionalFieldOf("polity", (short) -1).forGetter(RegionSummaryEntry::lastPolity),
                Codec.FLOAT.optionalFieldOf("k_clim", 1.0F).forGetter(RegionSummaryEntry::climateWeatheringFactor),
                Codec.FLOAT.optionalFieldOf("sediment", 1.0F).forGetter(RegionSummaryEntry::sedimentRate),
                Codec.INT.listOf().optionalFieldOf("sites", List.of()).forGetter(RegionSummaryEntry::siteIds),
                Codec.INT.listOf().optionalFieldOf("events", List.of()).forGetter(RegionSummaryEntry::eventIds),
                Codec.INT.listOf().optionalFieldOf("legends", List.of()).forGetter(RegionSummaryEntry::legendSeedIds)
        ).apply(i, RegionSummaryEntry::new));

        public RegionSummaryEntry {
            siteIds = siteIds == null ? List.of() : List.copyOf(siteIds.size() > HistoryCell.MAX_SITES_PER_CELL
                    ? siteIds.subList(0, HistoryCell.MAX_SITES_PER_CELL) : siteIds);
            eventIds = eventIds == null ? List.of() : List.copyOf(eventIds);
            legendSeedIds = legendSeedIds == null ? List.of() : List.copyOf(legendSeedIds);
        }
    }

    public record HistoricalTraceRecord(
            TraceKind kind,
            BlockPos pos,
            int radiusBlocks,
            int year,
            String depositId,
            float minedFraction
    ) {
        public static final Codec<HistoricalTraceRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
                TraceKind.CODEC.fieldOf("kind").forGetter(HistoricalTraceRecord::kind),
                BlockPos.CODEC.fieldOf("pos").forGetter(HistoricalTraceRecord::pos),
                Codec.INT.fieldOf("radius").forGetter(HistoricalTraceRecord::radiusBlocks),
                Codec.INT.fieldOf("year").forGetter(HistoricalTraceRecord::year),
                Codec.STRING.optionalFieldOf("deposit", "").forGetter(HistoricalTraceRecord::depositId),
                Codec.FLOAT.optionalFieldOf("mined", 0.0F).forGetter(HistoricalTraceRecord::minedFraction)
        ).apply(i, HistoricalTraceRecord::new));
    }

    private record HeaderPart(
            int formatVersion,
            int generatorVersion,
            long historySeed,
            SimulationMode mode,
            int windowCenterRx,
            int windowCenterRz,
            int windowRadiusRegions,
            int startYear,
            int endYear,
            long simulationMillis
    ) {
        static final com.mojang.serialization.MapCodec<HeaderPart> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.fieldOf("formatVersion").forGetter(HeaderPart::formatVersion),
                Codec.INT.optionalFieldOf("generatorVersion", GENERATOR_VERSION).forGetter(HeaderPart::generatorVersion),
                Codec.LONG.fieldOf("historySeed").forGetter(HeaderPart::historySeed),
                SimulationMode.CODEC.fieldOf("mode").forGetter(HeaderPart::mode),
                Codec.INT.optionalFieldOf("windowCenterRx", 0).forGetter(HeaderPart::windowCenterRx),
                Codec.INT.optionalFieldOf("windowCenterRz", 0).forGetter(HeaderPart::windowCenterRz),
                Codec.INT.optionalFieldOf("windowRadiusRegions", 0).forGetter(HeaderPart::windowRadiusRegions),
                Codec.INT.optionalFieldOf("startYear", -5_000).forGetter(HeaderPart::startYear),
                Codec.INT.optionalFieldOf("endYear", -1).forGetter(HeaderPart::endYear),
                Codec.LONG.optionalFieldOf("simulationMillis", 0L).forGetter(HeaderPart::simulationMillis)
        ).apply(i, HeaderPart::new));
    }

    private record BodyPart(
            List<Culture> cultures,
            List<Polity> polities,
            List<Person> persons,
            List<HistoryEvent> events,
            List<EventEdge> edges,
            List<Site> sites,
            List<Route> routes,
            List<AncientMine> mines,
            List<LegendSeed> legendSeeds,
            List<Extinction> extinctions,
            List<BilateralRelation> relations,
            List<PolityMeta> polityMetas,
            List<RegionSummaryEntry> regionIndex,
            List<HistoricalTraceRecord> traces,
            List<LiveHistoryEntry> liveLog
    ) {
        static final com.mojang.serialization.MapCodec<BodyPart> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Culture.CODEC.listOf().optionalFieldOf("cultures", List.of()).forGetter(BodyPart::cultures),
                Polity.CODEC.listOf().optionalFieldOf("polities", List.of()).forGetter(BodyPart::polities),
                Person.CODEC.listOf().optionalFieldOf("persons", List.of()).forGetter(BodyPart::persons),
                HistoryEvent.CODEC.listOf().optionalFieldOf("events", List.of()).forGetter(BodyPart::events),
                EventEdge.CODEC.listOf().optionalFieldOf("edges", List.of()).forGetter(BodyPart::edges),
                Site.CODEC.listOf().optionalFieldOf("sites", List.of()).forGetter(BodyPart::sites),
                Route.CODEC.listOf().optionalFieldOf("routes", List.of()).forGetter(BodyPart::routes),
                AncientMine.CODEC.listOf().optionalFieldOf("mines", List.of()).forGetter(BodyPart::mines),
                LegendSeed.CODEC.listOf().optionalFieldOf("legendSeeds", List.of()).forGetter(BodyPart::legendSeeds),
                Extinction.CODEC.listOf().optionalFieldOf("extinctions", List.of()).forGetter(BodyPart::extinctions),
                BilateralRelation.CODEC.listOf().optionalFieldOf("relations", List.of()).forGetter(BodyPart::relations),
                PolityMeta.CODEC.listOf().optionalFieldOf("polityMetas", List.of()).forGetter(BodyPart::polityMetas),
                RegionSummaryEntry.CODEC.listOf().optionalFieldOf("regionIndex", List.of()).forGetter(BodyPart::regionIndex),
                HistoricalTraceRecord.CODEC.listOf().optionalFieldOf("traces", List.of()).forGetter(BodyPart::traces),
                LiveHistoryEntry.CODEC.listOf().optionalFieldOf("liveLog", List.of()).forGetter(BodyPart::liveLog)
        ).apply(i, BodyPart::new));
    }

    public static final Codec<WorldHistoryData> CODEC = RecordCodecBuilder.create(i -> i.group(
            HeaderPart.MAP_CODEC.forGetter(d -> new HeaderPart(
                    d.formatVersion(), d.generatorVersion(), d.historySeed(), d.mode(),
                    d.windowCenterRx(), d.windowCenterRz(), d.windowRadiusRegions(),
                    d.startYear(), d.endYear(), d.simulationMillis()
            )),
            BodyPart.MAP_CODEC.forGetter(d -> new BodyPart(
                    d.cultures(), d.polities(), d.persons(), d.events(), d.edges(),
                    d.sites(), d.routes(), d.mines(), d.legendSeeds(), d.extinctions(),
                    d.relations(), d.polityMetas(), d.regionIndex(), d.historicalTraces(), d.liveLog()
            ))
    ).apply(i, (h, b) -> new WorldHistoryData(
            h.formatVersion(), h.generatorVersion(), h.historySeed(), h.mode(),
            h.windowCenterRx(), h.windowCenterRz(), h.windowRadiusRegions(),
            h.startYear(), h.endYear(), h.simulationMillis(),
            b.cultures(), b.polities(), b.persons(), b.events(), b.edges(),
            b.sites(), b.routes(), b.mines(), b.legendSeeds(), b.extinctions(),
            b.relations(), b.polityMetas(), b.regionIndex(), b.traces(), b.liveLog()
    )));

    public WorldHistoryData {
        Objects.requireNonNull(mode, "mode");
        cultures = cultures == null ? List.of() : List.copyOf(cultures);
        polities = polities == null ? List.of() : List.copyOf(polities);
        persons = persons == null ? List.of() : List.copyOf(persons);
        events = events == null ? List.of() : List.copyOf(events);
        edges = edges == null ? List.of() : List.copyOf(edges);
        sites = sites == null ? List.of() : List.copyOf(sites);
        routes = routes == null ? List.of() : List.copyOf(routes);
        mines = mines == null ? List.of() : List.copyOf(mines);
        legendSeeds = legendSeeds == null ? List.of() : List.copyOf(legendSeeds);
        extinctions = extinctions == null ? List.of() : List.copyOf(extinctions);
        relations = relations == null ? List.of() : List.copyOf(relations);
        polityMetas = polityMetas == null ? List.of() : List.copyOf(polityMetas);
        regionIndex = regionIndex == null ? List.of() : List.copyOf(regionIndex);
        historicalTraces = historicalTraces == null ? List.of() : List.copyOf(historicalTraces);
        liveLog = liveLog == null ? List.of() : List.copyOf(liveLog);
    }

    public WorldHistoryData withAppendedLiveEntry(LiveHistoryEntry entry) {
        List<LiveHistoryEntry> updated = new ArrayList<>(liveLog.size() + 1);
        updated.addAll(liveLog);
        updated.add(Objects.requireNonNull(entry, "entry"));
        return new WorldHistoryData(
                formatVersion, generatorVersion, historySeed, mode,
                windowCenterRx, windowCenterRz, windowRadiusRegions,
                startYear, endYear, simulationMillis,
                cultures, polities, persons, events, edges, sites, routes, mines,
                legendSeeds, extinctions, relations, polityMetas, regionIndex, historicalTraces, updated
        );
    }
}
