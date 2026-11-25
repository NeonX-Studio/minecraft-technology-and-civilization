package com.nightcity.echoes.api.impl;

import com.nightcity.echoes.api.AncientMine;
import com.nightcity.echoes.api.Culture;
import com.nightcity.echoes.api.Deity;
import com.nightcity.echoes.api.EventEdge;
import com.nightcity.echoes.api.Extinction;
import com.nightcity.echoes.api.HistoryEvent;
import com.nightcity.echoes.api.LanguageTemplate;
import com.nightcity.echoes.api.LegendSeed;
import com.nightcity.echoes.api.LiveHistoryEntry;
import com.nightcity.echoes.api.Person;
import com.nightcity.echoes.api.Polity;
import com.nightcity.echoes.api.PolityFate;
import com.nightcity.echoes.api.Relation;
import com.nightcity.echoes.api.Route;
import com.nightcity.echoes.api.SimulationMode;
import com.nightcity.echoes.api.Site;
import com.nightcity.echoes.api.SiteSummary;
import com.nightcity.echoes.api.SurvivorPolitySnapshot;
import com.nightcity.echoes.api.WorldHistoryView;
import com.nightcity.echoes.sim.WorldHistoryData;
import com.nightcity.tcivilization.RegionCoordinates;
import com.nightcity.tcivilization.api.RegionApi;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * Immutable, O(1)-indexed, thread-safe WorldHistoryView implementation (18 §12.1).
 */
public final class WorldHistoryViewImpl implements WorldHistoryView {
    public static final WorldHistoryViewImpl UNLOADED = new WorldHistoryViewImpl();

    private final boolean loaded;
    private final WorldHistoryData data;
    private final Map<Integer, Culture> culturesById;
    private final Map<Integer, Polity> politiesById;
    private final List<Polity> survivors;
    private final List<SurvivorPolitySnapshot> survivorSnapshots;
    private final Map<Integer, Person> personsById;
    private final Map<Integer, HistoryEvent> eventsById;
    private final Map<Long, List<HistoryEvent>> eventsByRegion;
    private final Map<Integer, List<EventEdge>> outgoingEdgesByEvent;
    private final Map<Integer, List<EventEdge>> incomingEdgesByEvent;
    private final Map<Integer, Site> sitesById;
    private final Map<Long, List<Site>> sitesByRegion;
    private final Map<Long, List<Route>> routesByRegion;
    private final Map<Long, List<AncientMine>> minesByRegion;
    private final Map<Long, List<LegendSeed>> legendsByRegion;
    private final Map<Integer, List<Extinction>> extinctionsByContinent;
    private final Map<Long, Relation> relationIndex;
    private final Map<Integer, List<Long>> holySitesByPolity;
    private final Map<Integer, UUID> historicCivIds = new ConcurrentHashMap<>();

    private WorldHistoryViewImpl() {
        this.loaded = false;
        this.data = null;
        this.culturesById = Map.of();
        this.politiesById = Map.of();
        this.survivors = List.of();
        this.survivorSnapshots = List.of();
        this.personsById = Map.of();
        this.eventsById = Map.of();
        this.eventsByRegion = Map.of();
        this.outgoingEdgesByEvent = Map.of();
        this.incomingEdgesByEvent = Map.of();
        this.sitesById = Map.of();
        this.sitesByRegion = Map.of();
        this.routesByRegion = Map.of();
        this.minesByRegion = Map.of();
        this.legendsByRegion = Map.of();
        this.extinctionsByContinent = Map.of();
        this.relationIndex = Map.of();
        this.holySitesByPolity = Map.of();
    }

    public WorldHistoryViewImpl(WorldHistoryData data, Map<Integer, UUID> initialHistoricCivIds) {
        this.loaded = true;
        this.data = data;
        if (initialHistoricCivIds != null) {
            this.historicCivIds.putAll(initialHistoricCivIds);
        }

        Map<Integer, Culture> cMap = new LinkedHashMap<>();
        for (Culture c : data.cultures()) {
            cMap.put(c.id(), c);
        }
        this.culturesById = Collections.unmodifiableMap(cMap);

        Map<Integer, Polity> pMap = new LinkedHashMap<>();
        List<Polity> survList = new ArrayList<>();
        for (Polity p : data.polities()) {
            UUID mappedCiv = this.historicCivIds.get(p.id());
            Polity effective = mappedCiv != null ? p.withCivId(mappedCiv) : p;
            pMap.put(effective.id(), effective);
            if (effective.fate() == PolityFate.SURVIVOR) {
                survList.add(effective);
            }
        }
        survList.sort(Comparator.comparingInt(Polity::finalPopulation).reversed().thenComparingInt(Polity::id));
        this.politiesById = Collections.unmodifiableMap(pMap);
        this.survivors = List.copyOf(survList);

        Map<Integer, Person> perMap = new LinkedHashMap<>();
        for (Person person : data.persons()) {
            perMap.put(person.id(), person);
        }
        this.personsById = Collections.unmodifiableMap(perMap);

        Map<Integer, HistoryEvent> eMap = new LinkedHashMap<>();
        Map<Long, List<HistoryEvent>> eReg = new HashMap<>();
        for (HistoryEvent ev : data.events()) {
            eMap.put(ev.id(), ev);
            eReg.computeIfAbsent(ev.region(), k -> new ArrayList<>()).add(ev);
        }
        this.eventsById = Collections.unmodifiableMap(eMap);
        this.eventsByRegion = freezeListMap(eReg);

        Map<Integer, List<EventEdge>> outMap = new HashMap<>();
        Map<Integer, List<EventEdge>> inMap = new HashMap<>();
        for (EventEdge edge : data.edges()) {
            outMap.computeIfAbsent(edge.from(), k -> new ArrayList<>()).add(edge);
            inMap.computeIfAbsent(edge.to(), k -> new ArrayList<>()).add(edge);
        }
        this.outgoingEdgesByEvent = freezeListMap(outMap);
        this.incomingEdgesByEvent = freezeListMap(inMap);

        Map<Integer, Site> sMap = new LinkedHashMap<>();
        Map<Long, List<Site>> sReg = new HashMap<>();
        for (Site site : data.sites()) {
            sMap.put(site.id(), site);
            sReg.computeIfAbsent(site.regionKey(), k -> new ArrayList<>()).add(site);
        }
        this.sitesById = Collections.unmodifiableMap(sMap);
        this.sitesByRegion = freezeListMap(sReg);

        Map<Long, List<Route>> rReg = new HashMap<>();
        for (Route route : data.routes()) {
            for (long rk : route.points()) {
                rReg.computeIfAbsent(rk, k -> new ArrayList<>()).add(route);
            }
        }
        this.routesByRegion = freezeListMap(rReg);

        Map<Long, List<AncientMine>> mReg = new HashMap<>();
        for (AncientMine mine : data.mines()) {
            mReg.computeIfAbsent(mine.regionKey(), k -> new ArrayList<>()).add(mine);
        }
        this.minesByRegion = freezeListMap(mReg);

        Map<Long, List<LegendSeed>> lReg = new HashMap<>();
        for (LegendSeed seed : data.legendSeeds()) {
            Site s = sMap.get(seed.siteId());
            if (s != null) {
                lReg.computeIfAbsent(s.regionKey(), k -> new ArrayList<>()).add(seed);
            } else {
                HistoryEvent ev = eMap.get(seed.eventId());
                if (ev != null) {
                    lReg.computeIfAbsent(ev.region(), k -> new ArrayList<>()).add(seed);
                }
            }
        }
        this.legendsByRegion = freezeListMap(lReg);

        Map<Integer, List<Extinction>> extMap = new HashMap<>();
        for (Extinction ext : data.extinctions()) {
            extMap.computeIfAbsent(ext.continentId(), k -> new ArrayList<>()).add(ext);
        }
        this.extinctionsByContinent = freezeListMap(extMap);

        Map<Long, Relation> relMap = new HashMap<>();
        for (WorldHistoryData.BilateralRelation br : data.relations()) {
            relMap.put(pairKey(br.polityA(), br.polityB()), br.relation());
        }
        this.relationIndex = Collections.unmodifiableMap(relMap);

        Map<Integer, WorldHistoryData.PolityMeta> metaById = new HashMap<>();
        Map<Integer, List<Long>> holyMap = new HashMap<>();
        for (WorldHistoryData.PolityMeta pm : data.polityMetas()) {
            metaById.put(pm.polityId(), pm);
            holyMap.put(pm.polityId(), pm.holySiteRegions());
        }
        this.holySitesByPolity = Collections.unmodifiableMap(holyMap);

        List<SurvivorPolitySnapshot> snaps = new ArrayList<>(this.survivors.size());
        for (Polity surv : this.survivors) {
            Culture cult = cMap.get(surv.cultureId());
            WorldHistoryData.PolityMeta meta = metaById.get(surv.id());
            List<Long> territory = meta != null && !meta.territoryRegions().isEmpty()
                    ? meta.territoryRegions()
                    : List.of(surv.capitalRegion());
            Set<Long> territorySet = new LinkedHashSet<>(territory);
            List<Long> holy = meta != null ? meta.holySiteRegions() : List.of();
            double aggr = meta != null ? meta.aggression() : 0.5D;

            List<Integer> feuds = new ArrayList<>();
            List<Integer> allies = new ArrayList<>();
            for (Polity other : this.survivors) {
                if (other.id() == surv.id()) {
                    continue;
                }
                Relation rel = relMap.getOrDefault(pairKey(surv.id(), other.id()), Relation.NEUTRAL);
                if (rel == Relation.FEUD) {
                    feuds.add(other.id());
                } else if (rel == Relation.OLD_ALLIANCE) {
                    allies.add(other.id());
                }
            }

            List<Integer> ancestralSites = new ArrayList<>();
            for (long rk : territorySet) {
                List<Site> localSites = this.sitesByRegion.getOrDefault(rk, List.of());
                for (Site st : localSites) {
                    Polity owner = pMap.get(st.polityId());
                    if (st.cultureId() == surv.cultureId() && owner != null && owner.fate() == PolityFate.EXTINCT) {
                        ancestralSites.add(st.id());
                    }
                }
            }

            snaps.add(new SurvivorPolitySnapshot(
                    surv.id(),
                    surv.name(),
                    surv.cultureId(),
                    cult != null ? cult.identifier() : Identifier.fromNamespaceAndPath("tcechoes", "culture_" + surv.cultureId()),
                    cult != null ? cult.language() : LanguageTemplate.RIVERINE,
                    cult != null ? cult.deity() : Deity.ANCESTOR,
                    surv.capitalRegion(),
                    territory,
                    holy,
                    surv.peakEra(),
                    surv.finalEra(),
                    surv.finalPopulation(),
                    aggr,
                    feuds,
                    allies,
                    ancestralSites
            ));
        }
        this.survivorSnapshots = List.copyOf(snaps);
    }

    public WorldHistoryData rawData() {
        return data;
    }

    public void bindHistoricCivilization(int polityId, UUID civId) {
        if (civId != null) {
            historicCivIds.put(polityId, civId);
        }
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public SimulationMode mode() {
        return loaded ? data.mode() : SimulationMode.STANDALONE;
    }

    @Override
    public long historySeed() {
        return loaded ? data.historySeed() : 0L;
    }

    @Override
    public int formatVersion() {
        return loaded ? data.formatVersion() : 0;
    }

    @Override
    public int generatorVersion() {
        return loaded ? data.generatorVersion() : 0;
    }

    @Override
    public int startYear() {
        return loaded ? data.startYear() : -5_000;
    }

    @Override
    public int endYear() {
        return loaded ? data.endYear() : -1;
    }

    @Override
    public long simulationMillis() {
        return loaded ? data.simulationMillis() : 0L;
    }

    @Override
    public List<Culture> cultures() {
        return loaded ? data.cultures() : List.of();
    }

    @Override
    public Optional<Culture> culture(int cultureId) {
        return Optional.ofNullable(culturesById.get(cultureId));
    }

    @Override
    public List<Polity> polities() {
        return loaded ? data.polities() : List.of();
    }

    @Override
    public Optional<Polity> polity(int polityId) {
        Polity base = politiesById.get(polityId);
        if (base == null) {
            return Optional.empty();
        }
        UUID bound = historicCivIds.get(polityId);
        return Optional.of(bound != null ? base.withCivId(bound) : base);
    }

    @Override
    public List<Polity> survivors() {
        return survivors;
    }

    @Override
    public List<SurvivorPolitySnapshot> survivorSnapshots() {
        return survivorSnapshots;
    }

    @Override
    public List<Person> persons() {
        return loaded ? data.persons() : List.of();
    }

    @Override
    public Optional<Person> person(int personId) {
        return Optional.ofNullable(personsById.get(personId));
    }

    @Override
    public List<HistoryEvent> events() {
        return loaded ? data.events() : List.of();
    }

    @Override
    public Optional<HistoryEvent> event(int eventId) {
        return Optional.ofNullable(eventsById.get(eventId));
    }

    @Override
    public List<HistoryEvent> events(long regionKey) {
        return eventsByRegion.getOrDefault(regionKey, List.of());
    }

    @Override
    public List<EventEdge> edges() {
        return loaded ? data.edges() : List.of();
    }

    @Override
    public List<EventEdge> outgoingEdges(int eventId) {
        return outgoingEdgesByEvent.getOrDefault(eventId, List.of());
    }

    @Override
    public List<EventEdge> incomingEdges(int eventId) {
        return incomingEdgesByEvent.getOrDefault(eventId, List.of());
    }

    @Override
    public List<Site> sites() {
        return loaded ? data.sites() : List.of();
    }

    @Override
    public Optional<Site> site(int siteId) {
        return Optional.ofNullable(sitesById.get(siteId));
    }

    @Override
    public List<Site> sitesInRegion(long regionKey) {
        return sitesByRegion.getOrDefault(regionKey, List.of());
    }

    @Override
    public List<SiteSummary> sitesNear(BlockPos pos, int radiusBlocks) {
        if (!loaded || pos == null || radiusBlocks < 0) {
            return List.of();
        }
        int rReg = Math.floorDiv(radiusBlocks, 512) + 1;
        int cx = Math.floorDiv(pos.getX(), 512);
        int cz = Math.floorDiv(pos.getZ(), 512);
        boolean wrap = data.mode() == SimulationMode.PLANET;
        long maxDistSq = (long) radiusBlocks * (long) radiusBlocks;
        List<SiteSummary> found = new ArrayList<>();
        Set<Integer> seen = new LinkedHashSet<>();

        for (int dz = -rReg; dz <= rReg; dz++) {
            int rz = cz + dz;
            for (int dx = -rReg; dx <= rReg; dx++) {
                int rx = cx + dx;
                if (wrap) {
                    rx = Math.floorMod(rx + RegionCoordinates.REGION_WIDTH / 2, RegionCoordinates.REGION_WIDTH)
                            - RegionCoordinates.REGION_WIDTH / 2;
                }
                long rk = RegionApi.regionKey(rx, rz);
                for (Site site : sitesByRegion.getOrDefault(rk, List.of())) {
                    if (!seen.contains(site.id())) {
                        long ddx = site.anchor().getX() - pos.getX();
                        if (wrap) {
                            if (ddx > RegionCoordinates.HALF_WIDTH) ddx -= RegionCoordinates.WORLD_WIDTH;
                            if (ddx < -RegionCoordinates.HALF_WIDTH) ddx += RegionCoordinates.WORLD_WIDTH;
                        }
                        long ddz = site.anchor().getZ() - pos.getZ();
                        if (ddx * ddx + ddz * ddz <= maxDistSq) {
                            seen.add(site.id());
                            found.add(site.toSummary());
                        }
                    }
                }
            }
        }
        found.sort(Comparator.comparingInt(SiteSummary::id));
        return List.copyOf(found);
    }

    @Override
    public List<Route> routes() {
        return loaded ? data.routes() : List.of();
    }

    @Override
    public List<Route> routesInRegion(long regionKey) {
        return routesByRegion.getOrDefault(regionKey, List.of());
    }

    @Override
    public List<AncientMine> mines() {
        return loaded ? data.mines() : List.of();
    }

    @Override
    public List<AncientMine> minesInRegion(long regionKey) {
        return minesByRegion.getOrDefault(regionKey, List.of());
    }

    @Override
    public List<LegendSeed> legendSeeds() {
        return loaded ? data.legendSeeds() : List.of();
    }

    @Override
    public List<LegendSeed> legendSeedsInRegion(long regionKey) {
        return legendsByRegion.getOrDefault(regionKey, List.of());
    }

    @Override
    public List<Extinction> extinctions() {
        return loaded ? data.extinctions() : List.of();
    }

    @Override
    public List<Extinction> extinctions(int continentId) {
        return extinctionsByContinent.getOrDefault(continentId, List.of());
    }

    @Override
    public Relation relation(int polityA, int polityB) {
        if (polityA == polityB) {
            return Relation.NEUTRAL;
        }
        return relationIndex.getOrDefault(pairKey(polityA, polityB), Relation.NEUTRAL);
    }

    @Override
    public List<Long> holySites(int polityId) {
        return holySitesByPolity.getOrDefault(polityId, List.of());
    }

    @Override
    public List<LiveHistoryEntry> liveHistory() {
        return loaded ? data.liveLog() : List.of();
    }

    @Override
    public Optional<UUID> historicCivilizationId(int polityId) {
        UUID bound = historicCivIds.get(polityId);
        if (bound != null) {
            return Optional.of(bound);
        }
        Polity p = politiesById.get(polityId);
        return p != null ? Optional.of(p.civId()) : Optional.empty();
    }

    public static long pairKey(int polityA, int polityB) {
        int lo = Math.min(polityA, polityB);
        int hi = Math.max(polityA, polityB);
        return ((long) lo << 32) | (hi & 0xffff_ffffL);
    }

    private static <K, V> Map<K, List<V>> freezeListMap(Map<K, List<V>> raw) {
        Map<K, List<V>> out = new HashMap<>(raw.size());
        for (Map.Entry<K, List<V>> entry : raw.entrySet()) {
            out.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(out);
    }
}
