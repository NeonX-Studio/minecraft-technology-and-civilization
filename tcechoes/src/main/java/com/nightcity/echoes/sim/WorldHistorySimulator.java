package com.nightcity.echoes.sim;

import com.nightcity.echoes.api.AncientMine;
import com.nightcity.echoes.api.ArchStyle;
import com.nightcity.echoes.api.Burial;
import com.nightcity.echoes.api.Culture;
import com.nightcity.echoes.api.Deity;
import com.nightcity.echoes.api.EdgeType;
import com.nightcity.echoes.api.EventEdge;
import com.nightcity.echoes.api.EventType;
import com.nightcity.echoes.api.Extinction;
import com.nightcity.echoes.api.HistoryCell;
import com.nightcity.echoes.api.HistoryEvent;
import com.nightcity.echoes.api.LanguageTemplate;
import com.nightcity.echoes.api.LegendKind;
import com.nightcity.echoes.api.LegendSeed;
import com.nightcity.echoes.api.Motif;
import com.nightcity.echoes.api.Person;
import com.nightcity.echoes.api.PersonRole;
import com.nightcity.echoes.api.Polity;
import com.nightcity.echoes.api.PolityFate;
import com.nightcity.echoes.api.Relation;
import com.nightcity.echoes.api.Route;
import com.nightcity.echoes.api.RouteKind;
import com.nightcity.echoes.api.ScriptType;
import com.nightcity.echoes.api.SimulationMode;
import com.nightcity.echoes.api.Site;
import com.nightcity.echoes.api.SiteOrigin;
import com.nightcity.echoes.api.SiteType;
import com.nightcity.echoes.api.impl.WorldHistoryViewImpl;
import com.nightcity.tcivilization.api.RegionApi;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * Deterministic 5,000-year (-5000..-1, 500 steps x 10 years) world history pre-simulation engine (18 §5.1-§5.4).
 * Uses StrictMath, SplitMix64 channel hashing, double-buffered parallel cell phases, and ordered serial phases.
 */
public final class WorldHistorySimulator {
    public static final int START_YEAR = -5_000;
    public static final int END_YEAR = -1;
    public static final int YEARS_PER_STEP = 10;
    public static final int TOTAL_STEPS = 500;

    public static final int MAX_POLITIES = 2_048;
    public static final int MAX_PERSONS = 4_096;
    public static final int MAX_EVENTS = 60_000;
    public static final int MAX_EDGES = 120_000;
    public static final int MAX_ROUTES = 3_000;
    public static final int MAX_MINES = 2_000;
    public static final int MAX_LEGEND_SEEDS = 20_000;

    private static final double K0_POP_PER_CELL = 4_000.0D;
    private static final double[] ERA_TECH_MULT = {0.3D, 1.0D, 1.6D, 2.2D, 3.0D, 4.0D};
    private static final double[] ERA_COMPLEXITY_THRESHOLDS = {0.0D, 40.0D, 160.0D, 480.0D, 1_200.0D, 3_000.0D};

    private static final String[] RELICT_SPECIES_PATHS = {
            "woolly_mammoth", "aurochs", "great_auk", "island_dodo", "passenger_pigeon", "marsupial_wolf"
    };
    private static final double[] RELICT_HUNT_FACTORS = {0.08D, 0.05D, 0.06D, 0.12D, 0.02D, 0.04D};
    private static final String[] EPITHETS = {
            "River-Crosser", "Wall-Builder", "Sun-Speaker", "Iron-Hand",
            "Star-Reader", "Peace-Weaver", "Ash-Walker", "Sea-Finder"
    };

    public record Config(
            long worldSeed,
            int cultureCount,
            int maxSites,
            int survivorCount,
            int threadCount,
            boolean nationsLoaded
    ) {
        public Config {
            cultureCount = Math.clamp(cultureCount, 4, 32);
            maxSites = Math.clamp(maxSites, 256, 40_000);
            survivorCount = Math.clamp(survivorCount, 1, 32);
            threadCount = Math.clamp(threadCount, 0, 8);
        }

        public static Config defaults(long worldSeed, boolean nationsLoaded) {
            return new Config(worldSeed, 12, 12_000, 8, 0, nationsLoaded);
        }
    }

    @FunctionalInterface
    public interface ProgressListener {
        void onStep(int step, int year, double fraction, String headline);
    }

    private static final class MutablePolity {
        final int id;
        final int cultureId;
        final String name;
        final int foundedYear;
        final double aggression;
        final List<Integer> predecessors;
        final UUID deterministicCivId;
        int capitalCell;
        int era;
        int peakEra;
        double complexity;
        double stability = 0.6D;
        boolean alive = true;
        int endYear = END_YEAR;
        int finalPopulation;
        int foundingEventId = -1;
        int lastMajorEventId = -1;
        final List<Integer> ownedCells = new ArrayList<>();
        final List<Long> holySiteRegions = new ArrayList<>();

        MutablePolity(
                int id,
                int cultureId,
                String name,
                int foundedYear,
                int capitalCell,
                int initialEra,
                double aggression,
                List<Integer> predecessors,
                UUID deterministicCivId
        ) {
            this.id = id;
            this.cultureId = cultureId;
            this.name = name;
            this.foundedYear = foundedYear;
            this.capitalCell = capitalCell;
            this.era = initialEra;
            this.peakEra = initialEra;
            this.aggression = aggression;
            this.predecessors = List.copyOf(predecessors);
            this.deterministicCivId = deterministicCivId;
        }
    }

    private WorldHistorySimulator() {
    }

    public static WorldHistoryData simulate(HistoryTerrainInput terrain, Config config) {
        return simulate(terrain, config, null);
    }

    public static WorldHistoryData simulate(HistoryTerrainInput terrain, Config config, ProgressListener listener) {
        long startNanos = System.nanoTime();
        long historySeed = SplitMix64.deriveHistorySeed(config.worldSeed());
        int w = terrain.width();
        int h = terrain.height();
        int cellCount = w * h;
        boolean wrapX = terrain.wrapsX();
        int minRx = terrain.minRx();
        int minRz = terrain.minRz();

        HistoryTerrainInput.CellSample[] grid = new HistoryTerrainInput.CellSample[cellCount];
        long[] regionKeys = new long[cellCount];
        boolean[] ocean = new boolean[cellCount];
        double[] hab = new double[cellCount];
        int[] continentOf = new int[cellCount];
        int maxContinentId = 0;
        List<Integer> landIndices = new ArrayList<>(cellCount / 3);

        for (int dz = 0; dz < h; dz++) {
            int rz = minRz + dz;
            for (int dx = 0; dx < w; dx++) {
                int rx = minRx + dx;
                int idx = dz * w + dx;
                HistoryTerrainInput.CellSample s = terrain.sample(rx, rz);
                grid[idx] = s;
                regionKeys[idx] = s.regionKey();
                ocean[idx] = s.ocean();
                hab[idx] = s.habitability();
                continentOf[idx] = s.continentId();
                if (!s.ocean()) {
                    landIndices.add(idx);
                    if (s.continentId() > maxContinentId) {
                        maxContinentId = s.continentId();
                    }
                }
            }
        }

        int[][] neighbors4 = new int[cellCount][];
        for (int dz = 0; dz < h; dz++) {
            for (int dx = 0; dx < w; dx++) {
                int idx = dz * w + dx;
                int[] tmp = new int[4];
                int n = 0;
                if (dx > 0) {
                    tmp[n++] = dz * w + (dx - 1);
                } else if (wrapX) {
                    tmp[n++] = dz * w + (w - 1);
                }
                if (dx + 1 < w) {
                    tmp[n++] = dz * w + (dx + 1);
                } else if (wrapX) {
                    tmp[n++] = dz * w;
                }
                if (dz > 0) {
                    tmp[n++] = (dz - 1) * w + dx;
                }
                if (dz + 1 < h) {
                    tmp[n++] = (dz + 1) * w + dx;
                }
                neighbors4[idx] = Arrays.copyOf(tmp, n);
            }
        }

        double[] popCurr = new double[cellCount];
        double[] popNext = new double[cellCount];
        double[] scarCurr = new double[cellCount];
        double[] scarNext = new double[cellCount];
        double[] stepDisasterSev = new double[cellCount];
        int[] ownerPolity = new int[cellCount];
        Arrays.fill(ownerPolity, -1);
        int[] dominantCulture = new int[cellCount];
        Arrays.fill(dominantCulture, -1);
        int[] lastPolity = new int[cellCount];
        Arrays.fill(lastPolity, -1);
        int[] continuousSettledSteps = new int[cellCount];
        boolean[] tellRecorded = new boolean[cellCount];
        byte[] cellHazards = new byte[cellCount];

        List<Culture> cultures = new ArrayList<>();
        ScriptType[] cultureScripts = new ScriptType[32];
        Arrays.fill(cultureScripts, ScriptType.NONE);
        List<MutablePolity> allPolities = new ArrayList<>();
        TreeMap<Integer, MutablePolity> activePolities = new TreeMap<>();
        List<Person> persons = new ArrayList<>();
        List<HistoryEvent> events = new ArrayList<>();
        List<EventEdge> edges = new ArrayList<>();
        List<Site> rawSites = new ArrayList<>();
        List<Route> routes = new ArrayList<>();
        List<AncientMine> mines = new ArrayList<>();
        List<Extinction> extinctions = new ArrayList<>();
        List<WorldHistoryData.HistoricalTraceRecord> traces = new ArrayList<>();

        TreeMap<Long, Integer> pairTradeSteps = new TreeMap<>();
        TreeMap<Long, Boolean> pairRouteBuilt = new TreeMap<>();
        TreeMap<Long, Integer> pairWarCount = new TreeMap<>();
        TreeMap<Long, Boolean> pairSacked = new TreeMap<>();
        TreeMap<Long, Integer> pairAllySteps = new TreeMap<>();

        List<HistoryTerrainInput.MineralDeposit> depositList = terrain.deposits();
        int[] depositSettledYears = new int[depositList.size()];
        int[] depositFirstYear = new int[depositList.size()];
        int[] depositLastYear = new int[depositList.size()];
        Arrays.fill(depositFirstYear, Integer.MAX_VALUE);
        Arrays.fill(depositLastYear, START_YEAR);

        int continentSlots = Math.max(1, maxContinentId + 1);
        double[][] speciesPop = new double[continentSlots][RELICT_SPECIES_PATHS.length];
        boolean[][] speciesExtinct = new boolean[continentSlots][RELICT_SPECIES_PATHS.length];
        for (int c = 0; c < continentSlots; c++) {
            Arrays.fill(speciesPop[c], 100.0D);
        }

        // Great Collapse window (-300..-150 => steps 470..485, duration 3..6 steps)
        long collapseMix = SplitMix64.mix64(historySeed ^ 0x434F4C4C41505345L);
        int collapseStartStep = 470 + SplitMix64.intRange(collapseMix, 16);
        int collapseDuration = 3 + SplitMix64.intRange(collapseMix >>> 16, 4);
        int collapseEndStep = Math.min(TOTAL_STEPS - 1, collapseStartStep + collapseDuration);

        // 1. Seed Dawn cultures on fertile hearths
        int minSpacing = terrain.mode() == SimulationMode.PLANET ? 20 : 6;
        List<Integer> hearthCells = selectCultureHearths(grid, landIndices, config.cultureCount(), minSpacing, historySeed, wrapX, w);
        for (int cid = 0; cid < hearthCells.size(); cid++) {
            int hIdx = hearthCells.get(cid);
            HistoryTerrainInput.CellSample cell = grid[hIdx];
            long cHash = SplitMix64.rand(historySeed, 0, cell.regionKey(), 0x43554C54L + cid);
            LanguageTemplate lang = LanguageTemplate.fromOrdinal(SplitMix64.intRange(cHash, LanguageTemplate.values().length));
            ArchStyle primary = cell.biomeBand().primaryStyle();
            ArchStyle secondary = cell.biomeBand().secondaryStyle();
            Burial burial = cell.mountain() ? Burial.CAVE_CUT
                    : Burial.fromOrdinal(SplitMix64.intRange(cHash >>> 8, Burial.values().length));
            Motif motif = Motif.fromOrdinal(SplitMix64.intRange(cHash >>> 16, Motif.values().length));
            Deity deity = cell.riverValley() ? Deity.RIVER
                    : (cell.coastal() ? Deity.OCEAN : Deity.fromOrdinal(SplitMix64.intRange(cHash >>> 24, Deity.values().length)));
            String dispName = lang.generateRootName(cHash ^ cell.regionKey());
            Culture culture = new Culture(
                    cid, "culture.tcechoes." + cid, dispName, lang, ScriptType.NONE,
                    primary, secondary, burial, motif, deity, cell.regionKey()
            );
            cultures.add(culture);

            MutablePolity founding = spawnPolity(
                    allPolities, activePolities, culture, START_YEAR, hIdx, 0, List.of(), historySeed
            );
            ownerPolity[hIdx] = founding.id;
            dominantCulture[hIdx] = cid;
            lastPolity[hIdx] = founding.id;
            popCurr[hIdx] = 360.0D + 240.0D * cell.habitability();
            founding.ownedCells.add(hIdx);

            int founderPersonId = addPerson(persons, founding.id, culture, START_YEAR - 25, START_YEAR + 35, PersonRole.FOUNDER, historySeed);
            int evId = addEvent(events, EventType.FOUNDED, START_YEAR, cell.regionKey(),
                    List.of(founding.id), founderPersonId >= 0 ? List.of(founderPersonId) : List.of(),
                    computeImportance(popCurr[hIdx], true, false, 1), 0);
            founding.foundingEventId = evId;
            founding.lastMajorEventId = evId;

            SiteType dawnSite = cell.tundra() ? SiteType.FROZEN_CAMP
                    : (cell.mountain() ? SiteType.ROCK_SHELTER : SiteType.CAMP_REMAINS);
            addSite(rawSites, dawnSite, cell, cid, founding.id, START_YEAR, START_YEAR + 220,
                    primary, 0, historySeed, config.nationsLoaded());
        }

        ensureNearSpawnEarlySites(terrain, grid, landIndices, cultures, allPolities, rawSites, historySeed, config.nationsLoaded());

        int workers = resolveWorkerCount(config.threadCount());
        ExecutorService pool = workers > 1 ? Executors.newFixedThreadPool(workers, r -> {
            Thread t = new Thread(r, "TC-EchoesSim");
            t.setDaemon(true);
            return t;
        }) : null;

        try {
            for (int step = 0; step < TOTAL_STEPS; step++) {
                int year = START_YEAR + step * YEARS_PER_STEP;
                boolean inCollapse = step >= collapseStartStep && step <= collapseEndStep;
                double climateFactor = inCollapse ? 0.7D : 1.0D;

                // Parallel cell phase: carrying capacity, logistic growth, disaster rolls, scar decay
                int[] ownerSnap = ownerPolity;
                MutablePolity[] polityLookup = allPolities.toArray(MutablePolity[]::new);
                runCellPhase(
                        pool, workers, cellCount, step, climateFactor,
                        historySeed, grid, regionKeys, ocean, hab,
                        ownerSnap, polityLookup, popCurr, scarCurr, popNext, scarNext, stepDisasterSev
                );

                double[] swapPop = popCurr;
                popCurr = popNext;
                popNext = swapPop;
                double[] swapScar = scarCurr;
                scarCurr = scarNext;
                scarNext = swapScar;

                // Record major cell disasters & continent population sums
                double[] continentSettledPop = new double[continentSlots];
                for (int idx : landIndices) {
                    int owner = ownerPolity[idx];
                    if (owner >= 0 && popCurr[idx] >= 50.0D) {
                        continuousSettledSteps[idx]++;
                        int cont = continentOf[idx];
                        if (cont >= 0 && cont < continentSlots) {
                            continentSettledPop[cont] += popCurr[idx];
                        }
                        if (!tellRecorded[idx] && continuousSettledSteps[idx] >= 80) {
                            tellRecorded[idx] = true;
                            MutablePolity mp = allPolities.get(owner);
                            addSite(rawSites, SiteType.TELL, grid[idx], mp.cultureId, mp.id,
                                    year - 800, year, cultures.get(mp.cultureId).primaryStyle(),
                                    0, historySeed, config.nationsLoaded());
                        }
                    } else {
                        continuousSettledSteps[idx] = 0;
                    }

                    double sev = stepDisasterSev[idx];
                    if (sev >= 0.72D && owner >= 0) {
                        HistoryTerrainInput.CellSample cell = grid[idx];
                        cellHazards[idx] |= HistoryCell.HAZARD_DISASTER;
                        int sevInt = (int) StrictMath.round(sev * 100.0D);
                        int dEv = addEvent(events, EventType.DISASTER, year, cell.regionKey(),
                                List.of(owner), List.of(),
                                computeImportance(popCurr[idx], allPolities.get(owner).capitalCell == idx, sev >= 0.88D, 0),
                                sevInt);
                        MutablePolity mp = allPolities.get(owner);
                        if (mp.lastMajorEventId >= 0 && dEv >= 0) {
                            addEdge(edges, mp.lastMajorEventId, dEv, EdgeType.CAUSED);
                        }
                        WorldHistoryData.TraceKind tk = cell.mountain()
                                ? ((step + idx) % 2 == 0 ? WorldHistoryData.TraceKind.FAULT_SCARP : WorldHistoryData.TraceKind.ASH_LAYER)
                                : WorldHistoryData.TraceKind.FLOOD_TERRACE;
                        if (traces.size() < 1_024) {
                            BlockPos tracePos = anchorInCell(cell, historySeed, step, 0x54524143L);
                            traces.add(new WorldHistoryData.HistoricalTraceRecord(tk, tracePos, 192, year, "", 0.0F));
                        }
                        if (cell.coastal() && popCurr[idx] >= 1_800.0D) {
                            int flags = tk == WorldHistoryData.TraceKind.ASH_LAYER ? Site.FLAG_ASH_SEALED : 0;
                            addSite(rawSites, SiteType.SUNKEN_CITY, cell, mp.cultureId, mp.id,
                                    mp.foundedYear, year, ArchStyle.ASHLAR, flags, historySeed, config.nationsLoaded());
                        }
                    }
                }

                // Megafauna hunting & extinctions per continent (18 §5.1.6)
                for (int c = 0; c < continentSlots; c++) {
                    double huntPop = continentSettledPop[c];
                    for (int sp = 0; sp < RELICT_SPECIES_PATHS.length; sp++) {
                        if (speciesExtinct[c][sp]) {
                            continue;
                        }
                        double cur = speciesPop[c][sp];
                        double next = cur + 0.05D * cur * (1.0D - cur / 100.0D) - RELICT_HUNT_FACTORS[sp] * (huntPop / 10_000.0D);
                        if (next <= 0.0D) {
                            speciesPop[c][sp] = 0.0D;
                            speciesExtinct[c][sp] = true;
                            long repRegion = findRepresentativeContinentRegion(landIndices, continentOf, regionKeys, c);
                            int extEv = addEvent(events, EventType.HUNT_EXTINCTION, year, repRegion,
                                    List.of(), List.of(), computeImportance(huntPop * 0.1D, false, true, 0), 80);
                            extinctions.add(new Extinction(
                                    c,
                                    Identifier.fromNamespaceAndPath("tcecology", RELICT_SPECIES_PATHS[sp]),
                                    year,
                                    Math.max(0, extEv)
                            ));
                        } else {
                            speciesPop[c][sp] = next;
                        }
                    }
                }

                // Ancient mines accumulation every 5 steps (50 years) for speed with identical scale
                if (step % 5 == 0 && !depositList.isEmpty()) {
                    for (int di = 0; di < depositList.size(); di++) {
                        HistoryTerrainInput.MineralDeposit dep = depositList.get(di);
                        if (!dep.surfaceSigns()) {
                            continue;
                        }
                        boolean nearSettlement = isDepositNearSettledCell(dep, terrain, ownerPolity, w, h, minRx, minRz, wrapX);
                        if (nearSettlement) {
                            depositSettledYears[di] += YEARS_PER_STEP * 5;
                            if (year < depositFirstYear[di]) {
                                depositFirstYear[di] = year;
                            }
                            depositLastYear[di] = year;
                        }
                    }
                }

                // Serial polity step in ascending polity ID order
                List<Integer> activeIds = new ArrayList<>(activePolities.keySet());
                TreeMap<Long, Boolean> evaluatedNeighbors = new TreeMap<>();
                int[] warCountThisStep = new int[allPolities.size() + 64];
                double[] disasterSumThisStep = new double[allPolities.size() + 64];

                for (int pid : activeIds) {
                    MutablePolity p = activePolities.get(pid);
                    if (p == null || !p.alive) {
                        continue;
                    }
                    p.ownedCells.removeIf(cIdx -> ownerPolity[cIdx] != p.id);
                    if (p.ownedCells.isEmpty()) {
                        markExtinct(p, activePolities, year);
                        continue;
                    }
                    if (ownerPolity[p.capitalCell] != p.id) {
                        p.capitalCell = p.ownedCells.getFirst();
                    }

                    double totalPop = 0.0D;
                    double disSum = 0.0D;
                    List<Integer> migrationSources = new ArrayList<>();
                    for (int cIdx : p.ownedCells) {
                        double cp = popCurr[cIdx];
                        totalPop += cp;
                        disSum += stepDisasterSev[cIdx];
                        double kCell = carryingCapacity(hab[cIdx], p.era, climateFactor, scarCurr[cIdx]);
                        if (cp > 0.90D * kCell && kCell > 100.0D) {
                            migrationSources.add(cIdx);
                        }
                    }
                    p.finalPopulation = (int) StrictMath.round(totalPop);
                    if (p.id < disasterSumThisStep.length) {
                        disasterSumThisStep[p.id] = disSum;
                    }

                    // Migration & territorial expansion (up to 2 expansions per polity per step)
                    int expanded = 0;
                    for (int srcIdx : migrationSources) {
                        if (expanded >= 2) {
                            break;
                        }
                        int bestTarget = pickMigrationTarget(srcIdx, p, grid, neighbors4, ownerPolity, hab);
                        if (bestTarget < 0) {
                            continue;
                        }
                        double kSrc = carryingCapacity(hab[srcIdx], p.era, climateFactor, scarCurr[srcIdx]);
                        double ratio = StrictMath.min(1.0D, (popCurr[srcIdx] / kSrc - 0.90D) / 0.10D);
                        double moved = popCurr[srcIdx] * 0.20D * StrictMath.max(0.15D, ratio);
                        popCurr[srcIdx] -= moved;
                        if (ownerPolity[bestTarget] < 0) {
                            expanded++;
                            popCurr[bestTarget] += moved;
                            Culture cult = cultures.get(p.cultureId);
                            int distFromHearth = regionChebyshevDistance(regionKeys[bestTarget], cult.hearthRegion(), wrapX);
                            int splitThreshold = terrain.mode() == SimulationMode.PLANET ? 30 : 10;
                            double splitRoll = SplitMix64.rand01(historySeed, step, regionKeys[bestTarget], 0x53504C54L + p.id);
                            if (distFromHearth > splitThreshold && splitRoll < 0.30D && allPolities.size() < MAX_POLITIES) {
                                MutablePolity child = spawnPolity(
                                        allPolities, activePolities, cult, year, bestTarget,
                                        Math.max(0, p.era - 1), List.of(p.id), historySeed
                                );
                                ownerPolity[bestTarget] = child.id;
                                dominantCulture[bestTarget] = child.cultureId;
                                lastPolity[bestTarget] = child.id;
                                child.ownedCells.add(bestTarget);
                                int migEv = addEvent(events, EventType.MIGRATED, year, regionKeys[bestTarget],
                                        List.of(p.id, child.id), List.of(),
                                        computeImportance(moved, true, false, 0), 0);
                                child.foundingEventId = migEv;
                                child.lastMajorEventId = migEv;
                                if (p.lastMajorEventId >= 0 && migEv >= 0) {
                                    addEdge(edges, p.lastMajorEventId, migEv, EdgeType.CAUSED);
                                }
                            } else {
                                ownerPolity[bestTarget] = p.id;
                                dominantCulture[bestTarget] = p.cultureId;
                                lastPolity[bestTarget] = p.id;
                                p.ownedCells.add(bestTarget);
                            }
                        } else if (ownerPolity[bestTarget] == p.id) {
                            popCurr[bestTarget] += moved;
                        }
                    }

                    // Discover bordering polities
                    for (int cIdx : p.ownedCells) {
                        for (int nb : neighbors4[cIdx]) {
                            int otherId = ownerPolity[nb];
                            if (otherId >= 0 && otherId != p.id) {
                                evaluatedNeighbors.put(WorldHistoryViewImpl.pairKey(p.id, otherId), Boolean.TRUE);
                            }
                        }
                    }
                }

                // Also discover regional overland trade partners on the same continent within 28 regions
                List<MutablePolity> activeSnap = new ArrayList<>(activePolities.values());
                for (int i = 0; i < activeSnap.size(); i++) {
                    MutablePolity pa = activeSnap.get(i);
                    HistoryTerrainInput.CellSample caCell = grid[pa.capitalCell];
                    for (int j = i + 1; j < activeSnap.size(); j++) {
                        MutablePolity pb = activeSnap.get(j);
                        HistoryTerrainInput.CellSample cbCell = grid[pb.capitalCell];
                        if (caCell.continentId() >= 0 && caCell.continentId() == cbCell.continentId()) {
                            int dist = regionChebyshevDistance(caCell.regionKey(), cbCell.regionKey(), wrapX);
                            if (dist <= 28) {
                                evaluatedNeighbors.put(WorldHistoryViewImpl.pairKey(pa.id, pb.id), Boolean.TRUE);
                            }
                        }
                    }
                }

                // Trade, alliances, and wars across bordering / regional polity pairs
                int[] tradePartnerCounts = new int[allPolities.size()];
                int[] allyCounts = new int[allPolities.size()];
                for (Long pk : evaluatedNeighbors.keySet()) {
                    int aId = (int) (pk >> 32);
                    int bId = (int) (long) pk;
                    MutablePolity a = activePolities.get(aId);
                    MutablePolity b = activePolities.get(bId);
                    if (a == null || b == null || !a.alive || !b.alive) {
                        continue;
                    }
                    Culture ca = cultures.get(a.cultureId);
                    Culture cb = cultures.get(b.cultureId);
                    double cDist = ca.distanceTo(cb);
                    boolean hasFeud = pairWarCount.getOrDefault(pk, 0) >= 2 || pairSacked.getOrDefault(pk, false);
                    double resPressure = StrictMath.min(1.0D, (a.finalPopulation + b.finalPopulation) / (K0_POP_PER_CELL * Math.max(1, a.ownedCells.size() + b.ownedCells.size()) * 2.0D));
                    double tension = Math.clamp(0.5D * resPressure + 0.3D * cDist + (hasFeud ? 0.2D : 0.0D), 0.0D, 1.0D);
                    double warProb = ((a.aggression + b.aggression) * 0.5D) * tension * 0.05D;
                    double roll = SplitMix64.rand01(historySeed, step, pk, 0x574152L);

                    if (roll < warProb && a.era >= 1 && b.era >= 1) {
                        if (a.id < warCountThisStep.length) warCountThisStep[a.id]++;
                        if (b.id < warCountThisStep.length) warCountThisStep[b.id]++;
                        pairTradeSteps.put(pk, 0);
                        int wars = pairWarCount.getOrDefault(pk, 0) + 1;
                        pairWarCount.put(pk, wars);

                        double powA = Math.max(1.0D, a.finalPopulation) * (1.0D + 0.4D * a.era);
                        double powB = Math.max(1.0D, b.finalPopulation) * (1.0D + 0.4D * b.era);
                        double winRoll = SplitMix64.rand01(historySeed, step, pk, 0x42415454L);
                        MutablePolity winner = (winRoll < powA / (powA + powB)) ? a : b;
                        MutablePolity loser = (winner == a) ? b : a;

                        int borderCell = findSharedBorderCell(loser, winner, neighbors4, ownerPolity);
                        if (borderCell >= 0) {
                            long battleRegion = regionKeys[borderCell];
                            cellHazards[borderCell] |= HistoryCell.HAZARD_BATTLEFIELD;
                            int genId = (step % 4 == 0) ? addPerson(persons, winner.id, cultures.get(winner.cultureId), year - 30, year + 20, PersonRole.GENERAL, historySeed) : -1;
                            int imp = computeImportance((a.finalPopulation + b.finalPopulation) * 0.25D, borderCell == loser.capitalCell, false, genId >= 0 ? 1 : 0);
                            int warEv = addEvent(events, EventType.BATTLE, year, battleRegion,
                                    List.of(winner.id, loser.id), genId >= 0 ? List.of(genId) : List.of(), imp, 55);
                            if (winner.lastMajorEventId >= 0 && warEv >= 0) {
                                addEdge(edges, winner.lastMajorEventId, warEv, EdgeType.FEUD);
                            }
                            winner.lastMajorEventId = warEv >= 0 ? warEv : winner.lastMajorEventId;

                            if (imp >= 40) {
                                addSite(rawSites, SiteType.BATTLEFIELD, grid[borderCell], loser.cultureId, loser.id,
                                        year, year, ArchStyle.DRYSTONE, 0, historySeed, config.nationsLoaded());
                                if (wars >= 2) {
                                    addSite(rawSites, SiteType.FORTRESS_RUIN, grid[borderCell], loser.cultureId, loser.id,
                                            loser.foundedYear, year, ArchStyle.DRYSTONE, 0, historySeed, config.nationsLoaded());
                                }
                            }

                            boolean capturedCapital = (borderCell == loser.capitalCell);
                            ownerPolity[borderCell] = winner.id;
                            lastPolity[borderCell] = winner.id;
                            loser.ownedCells.remove(Integer.valueOf(borderCell));
                            winner.ownedCells.add(borderCell);
                            popCurr[borderCell] *= 0.72D;

                            if (capturedCapital && SplitMix64.rand01(historySeed, step, battleRegion, 0x5341434BL) < 0.10D) {
                                pairSacked.put(pk, Boolean.TRUE);
                                cellHazards[borderCell] |= HistoryCell.HAZARD_SACKED_CITY;
                                int sackEv = addEvent(events, EventType.SACK, year, battleRegion,
                                        List.of(winner.id, loser.id), List.of(),
                                        computeImportance(popCurr[borderCell] * 2.0D, true, true, 0), 90);
                                if (warEv >= 0 && sackEv >= 0) {
                                    addEdge(edges, warEv, sackEv, EdgeType.CAUSED);
                                }
                                addSite(rawSites, SiteType.CITY_RUIN, grid[borderCell], loser.cultureId, loser.id,
                                        loser.foundedYear, year, cultures.get(loser.cultureId).primaryStyle(),
                                        Site.FLAG_SACKED, historySeed, config.nationsLoaded());
                            }
                        }
                    } else {
                        // Peaceful trade & potential alliance
                        if (a.id < tradePartnerCounts.length) tradePartnerCounts[a.id]++;
                        if (b.id < tradePartnerCounts.length) tradePartnerCounts[b.id]++;
                        int tSteps = pairTradeSteps.getOrDefault(pk, 0) + 1;
                        pairTradeSteps.put(pk, tSteps);

                        if (tension < 0.35D) {
                            int aSteps = pairAllySteps.getOrDefault(pk, 0) + 1;
                            pairAllySteps.put(pk, aSteps);
                            if (a.id < allyCounts.length) allyCounts[a.id]++;
                            if (b.id < allyCounts.length) allyCounts[b.id]++;
                            if (aSteps == 5) {
                                int alEv = addEvent(events, EventType.ALLIANCE, year, regionKeys[a.capitalCell],
                                        List.of(a.id, b.id), List.of(),
                                        computeImportance(a.finalPopulation + b.finalPopulation, true, false, 0), 0);
                                if (a.lastMajorEventId >= 0 && alEv >= 0) {
                                    addEdge(edges, a.lastMajorEventId, alEv, EdgeType.ALLIED);
                                }
                            }
                        }

                        if (tSteps >= 4 && !pairRouteBuilt.getOrDefault(pk, false) && routes.size() < MAX_ROUTES) {
                            pairRouteBuilt.put(pk, Boolean.TRUE);
                            buildTradeRoute(routes, rawSites, grid, regionKeys, neighbors4, a, b, year - 100, year, historySeed, config.nationsLoaded());
                        }
                    }
                }

                // Coastal maritime trade routes every 10 steps for E2+ coastal capitals
                if (step >= 15 && step % 10 == 0 && routes.size() < MAX_ROUTES) {
                    buildMaritimeRoutes(routes, rawSites, events, grid, regionKeys, neighbors4, activePolities, year, step, historySeed, config.nationsLoaded());
                }

                // Complexity, era promotion, stability, plague, and collapse
                for (int pid : new ArrayList<>(activePolities.keySet())) {
                    MutablePolity p = activePolities.get(pid);
                    if (p == null || !p.alive) {
                        continue;
                    }
                    p.ownedCells.removeIf(cIdx -> ownerPolity[cIdx] != p.id);
                    if (p.ownedCells.isEmpty()) {
                        markExtinct(p, activePolities, year);
                        continue;
                    }

                    int wars = p.id < warCountThisStep.length ? warCountThisStep[p.id] : 0;
                    int tradePartners = p.id < tradePartnerCounts.length ? tradePartnerCounts[p.id] : 0;
                    double dis = p.id < disasterSumThisStep.length ? disasterSumThisStep[p.id] : 0.0D;

                    p.complexity += StrictMath.log10(StrictMath.max(10.0D, p.finalPopulation))
                            * (1.0D + 0.2D * tradePartners)
                            * (wars > 0 ? 0.5D : 1.0D);

                    while (p.era < 5 && p.complexity >= ERA_COMPLEXITY_THRESHOLDS[p.era + 1]) {
                        p.era++;
                        if (p.era > p.peakEra) {
                            p.peakEra = p.era;
                        }
                        onPolityEraAdvance(p, year, grid, cultures, cultureScripts, persons, events, edges, rawSites, cellHazards, historySeed, config.nationsLoaded());
                    }

                    boolean plague = inCollapse && SplitMix64.rand01(historySeed, step, regionKeys[p.capitalCell], 0x504C4147L + p.id) < 0.15D;
                    if (plague) {
                        for (int cIdx : p.ownedCells) {
                            popCurr[cIdx] *= 0.78D;
                        }
                        addEvent(events, EventType.PLAGUE, year, regionKeys[p.capitalCell],
                                List.of(p.id), List.of(), computeImportance(p.finalPopulation * 0.22D, true, false, 0), 65);
                    }

                    double govCap = 4.0D * (p.era + 1) * (p.era + 1);
                    double overext = StrictMath.max(0.0D, (p.ownedCells.size() / govCap) - 1.0D);
                    p.stability = StrictMath.min(1.0D, p.stability + 0.02D
                            - 0.10D * wars
                            - 0.15D * StrictMath.min(2.0D, dis)
                            - 0.05D * overext
                            - (plague ? 0.20D : 0.0D)
                            - (inCollapse ? 0.08D : 0.0D));

                    if (p.stability < 0.0D) {
                        collapsePolity(
                                p, year, step, grid, regionKeys, neighbors4, ownerPolity, dominantCulture, lastPolity,
                                popCurr, cellHazards, cultures, allPolities, activePolities, persons, events, edges, rawSites,
                                historySeed, config.nationsLoaded()
                        );
                    }
                }

                if (listener != null && (step % 25 == 0 || step == TOTAL_STEPS - 1)) {
                    String headline = events.isEmpty() ? ""
                            : (year + ": " + events.getLast().type().name() + " (polities=" + activePolities.size() + ")");
                    listener.onStep(step, year, (step + 1) / (double) TOTAL_STEPS, headline);
                }
            }
        } finally {
            if (pool != null) {
                pool.shutdown();
            }
        }

        // Finalize ancient mines from accumulated deposit years (18 §5.3.5)
        for (int di = 0; di < depositList.size() && mines.size() < MAX_MINES; di++) {
            int yrs = depositSettledYears[di];
            if (yrs < 150) {
                continue;
            }
            HistoryTerrainInput.MineralDeposit dep = depositList.get(di);
            HistoryTerrainInput.CellSample cell = terrain.sample(dep.rx(), dep.rz());
            float minedFrac = (float) StrictMath.min(0.6D, (yrs / 3_000.0D) * dep.scale().scaleFactor());
            if (minedFrac <= 0.02F) {
                continue;
            }
            BlockPos entrance = anchorInCell(cell, historySeed, di, 0x4D494E45L);
            int fromY = depositFirstYear[di] == Integer.MAX_VALUE ? START_YEAR : depositFirstYear[di];
            int toY = depositLastYear[di];
            float wMine = computeWeathering(Math.max(50, -toY), cell.climateWeatheringFactor(), ArchStyle.DRYSTONE);
            int mineId = mines.size();
            mines.add(new AncientMine(mineId, dep.depositId(), entrance, minedFrac, fromY, toY, wMine));

            int domCult = findNearestCulture(cell.regionKey(), cultures, wrapX);
            SiteType mSiteType = "stone".equals(dep.mineral()) ? SiteType.ANCIENT_QUARRY
                    : ((di % 4 == 0) ? SiteType.FURNACE_SITE : SiteType.ANCIENT_MINE);
            addSite(rawSites, mSiteType, cell, domCult, -1, fromY, toY,
                    ArchStyle.DRYSTONE, 0, historySeed, config.nationsLoaded());
            if (traces.size() < 2_048) {
                traces.add(new WorldHistoryData.HistoricalTraceRecord(
                        WorldHistoryData.TraceKind.ANCIENT_WORKINGS, entrance, 96, toY, dep.depositId(), minedFrac
                ));
            }
        }

        // Guarantee at least 1 frozen_camp if woolly_mammoth went extinct on every continent (18 §5.1.6)
        boolean allMammothExtinct = true;
        for (int c = 0; c < continentSlots; c++) {
            if (!speciesExtinct[c][0]) {
                allMammothExtinct = false;
                break;
            }
        }
        if (allMammothExtinct && rawSites.stream().noneMatch(s -> s.type() == SiteType.FROZEN_CAMP) && !landIndices.isEmpty()) {
            int coldestIdx = landIndices.getFirst();
            for (int idx : landIndices) {
                if (grid[idx].freezeThaw() > grid[coldestIdx].freezeThaw()) {
                    coldestIdx = idx;
                }
            }
            addSite(rawSites, SiteType.FROZEN_CAMP, grid[coldestIdx], 0, -1, -4_200, -3_600,
                    ArchStyle.TIMBER, 0, historySeed, config.nationsLoaded());
        }

        // Finalize culture scripts
        List<Culture> finalCultures = new ArrayList<>(cultures.size());
        for (Culture c : cultures) {
            ScriptType sc = c.id() < cultureScripts.length ? cultureScripts[c.id()] : ScriptType.NONE;
            finalCultures.add(new Culture(
                    c.id(), c.nameKey(), c.displayName(), c.language(), sc,
                    c.primaryStyle(), c.secondaryStyle(), c.burial(), c.motif(), c.deity(), c.hearthRegion()
            ));
        }

        // Finalize polities: Twilight regression to E0-E2 for survivors, rank by finalPopulation desc
        List<MutablePolity> survivingList = new ArrayList<>();
        for (MutablePolity mp : activePolities.values()) {
            mp.ownedCells.removeIf(cIdx -> ownerPolity[cIdx] != mp.id);
            if (mp.ownedCells.isEmpty()) {
                mp.alive = false;
                mp.endYear = END_YEAR;
                continue;
            }
            double sum = 0.0D;
            for (int cIdx : mp.ownedCells) {
                sum += popCurr[cIdx];
            }
            mp.finalPopulation = Math.max(40, (int) StrictMath.round(sum));
            mp.era = Math.min(mp.era, 2);
            survivingList.add(mp);
        }
        survivingList.sort(Comparator.comparingInt((MutablePolity p) -> p.finalPopulation).reversed()
                .thenComparingInt(p -> p.id));

        List<Polity> finalPolities = new ArrayList<>(allPolities.size());
        List<WorldHistoryData.PolityMeta> polityMetas = new ArrayList<>(allPolities.size());
        for (MutablePolity mp : allPolities) {
            PolityFate fate;
            if (!mp.alive) {
                fate = PolityFate.EXTINCT;
            } else {
                int rank = survivingList.indexOf(mp);
                fate = (rank >= 0 && rank < config.survivorCount()) ? PolityFate.SURVIVOR : PolityFate.REMNANT;
                if (!config.nationsLoaded() || fate == PolityFate.REMNANT) {
                    HistoryTerrainInput.CellSample capCell = grid[mp.capitalCell];
                    addSite(rawSites, SiteType.CITY_RUIN, capCell, mp.cultureId, mp.id,
                            mp.foundedYear, -10, finalCultures.get(mp.cultureId).primaryStyle(),
                            0, historySeed, false);
                }
            }
            finalPolities.add(new Polity(
                    mp.id, mp.cultureId, mp.name, mp.foundedYear, mp.alive ? END_YEAR : mp.endYear,
                    mp.peakEra, mp.era, mp.finalPopulation, regionKeys[mp.capitalCell],
                    fate, mp.predecessors, mp.deterministicCivId
            ));
            List<Long> terrKeys = new ArrayList<>(mp.ownedCells.size());
            for (int cIdx : mp.ownedCells) {
                terrKeys.add(regionKeys[cIdx]);
            }
            polityMetas.add(new WorldHistoryData.PolityMeta(
                    mp.id, mp.aggression, terrKeys, mp.holySiteRegions
            ));
        }

        // Trim & re-index sites respecting per-cell <= 16 and global <= maxSites
        List<Site> finalSites = trimAndReindexSites(rawSites, config.maxSites());

        // Trim events & edges if needed
        List<HistoryEvent> finalEvents = events.size() <= MAX_EVENTS ? List.copyOf(events) : trimEvents(events);
        List<EventEdge> finalEdges = edges.size() <= MAX_EDGES ? List.copyOf(edges) : List.copyOf(edges.subList(0, MAX_EDGES));

        // Generate LegendSeeds (including near-spawn ORIGIN seed + 2-3 step clue chains)
        List<LegendSeed> legendSeeds = generateLegendSeeds(terrain, finalSites, finalEvents, historySeed);

        // Attach legendSeedIds back onto sites
        Map<Integer, List<Integer>> seedsBySite = new TreeMap<>();
        for (LegendSeed ls : legendSeeds) {
            if (ls.siteId() >= 0) {
                seedsBySite.computeIfAbsent(ls.siteId(), k -> new ArrayList<>()).add(ls.id());
            }
        }
        List<Site> enrichedSites = new ArrayList<>(finalSites.size());
        for (Site s : finalSites) {
            List<Integer> sSeeds = seedsBySite.getOrDefault(s.id(), List.of());
            enrichedSites.add(new Site(
                    s.id(), s.type(), s.anchor(), s.cultureId(), s.polityId(),
                    s.builtYear(), s.abandonedYear(), s.style(), s.origin(),
                    s.weathering(), s.burialDepth(), sSeeds, s.flags()
            ));
        }

        // Build bilateral relations table
        List<WorldHistoryData.BilateralRelation> relations = new ArrayList<>();
        TreeMap<Long, Relation> mergedRel = new TreeMap<>();
        for (Map.Entry<Long, Integer> e : pairAllySteps.entrySet()) {
            if (e.getValue() >= 5) {
                mergedRel.put(e.getKey(), Relation.OLD_ALLIANCE);
            }
        }
        for (Map.Entry<Long, Integer> e : pairWarCount.entrySet()) {
            if (e.getValue() >= 2) {
                mergedRel.put(e.getKey(), Relation.FEUD);
            }
        }
        for (Map.Entry<Long, Boolean> e : pairSacked.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) {
                mergedRel.put(e.getKey(), Relation.FEUD);
            }
        }
        for (Map.Entry<Long, Relation> e : mergedRel.entrySet()) {
            int aId = (int) (e.getKey() >> 32);
            int bId = (int) (long) e.getKey();
            relations.add(new WorldHistoryData.BilateralRelation(aId, bId, e.getValue()));
        }

        // Build compact region_index for every cell that experienced history, sites, events, or legends
        List<WorldHistoryData.RegionSummaryEntry> regionIndex = buildRegionIndex(
                cellCount, grid, regionKeys, cellHazards, dominantCulture, lastPolity,
                enrichedSites, finalEvents, legendSeeds
        );

        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000L;
        return new WorldHistoryData(
                WorldHistoryData.FORMAT_VERSION,
                WorldHistoryData.GENERATOR_VERSION,
                historySeed,
                terrain.mode(),
                terrain.centerRx(),
                terrain.centerRz(),
                terrain.standaloneRadiusRegions(),
                START_YEAR,
                END_YEAR,
                elapsedMillis,
                finalCultures,
                finalPolities,
                persons,
                finalEvents,
                finalEdges,
                enrichedSites,
                routes,
                mines,
                legendSeeds,
                extinctions,
                relations,
                polityMetas,
                regionIndex,
                traces,
                List.of()
        );
    }

    private static int resolveWorkerCount(int configured) {
        if (configured > 0) {
            return configured;
        }
        return Math.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, 8);
    }

    private static void runCellPhase(
            ExecutorService pool,
            int workers,
            int cellCount,
            int step,
            double climateFactor,
            long historySeed,
            HistoryTerrainInput.CellSample[] grid,
            long[] regionKeys,
            boolean[] ocean,
            double[] hab,
            int[] ownerSnap,
            MutablePolity[] polityLookup,
            double[] popCurr,
            double[] scarCurr,
            double[] popNext,
            double[] scarNext,
            double[] stepDisasterSev
    ) {
        if (pool == null || workers <= 1 || cellCount < 4_096) {
            computeCellSlice(0, cellCount, step, climateFactor, historySeed, grid, regionKeys, ocean, hab,
                    ownerSnap, polityLookup, popCurr, scarCurr, popNext, scarNext, stepDisasterSev);
            return;
        }
        int chunk = (cellCount + workers - 1) / workers;
        List<Callable<Void>> tasks = new ArrayList<>(workers);
        for (int t = 0; t < workers; t++) {
            int from = t * chunk;
            int to = Math.min(cellCount, from + chunk);
            if (from >= to) {
                break;
            }
            tasks.add(() -> {
                computeCellSlice(from, to, step, climateFactor, historySeed, grid, regionKeys, ocean, hab,
                        ownerSnap, polityLookup, popCurr, scarCurr, popNext, scarNext, stepDisasterSev);
                return null;
            });
        }
        try {
            List<Future<Void>> futures = pool.invokeAll(tasks);
            for (Future<Void> f : futures) {
                f.get();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("History simulation interrupted", ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("History simulation worker failed", ex.getCause());
        }
    }

    private static void computeCellSlice(
            int from,
            int to,
            int step,
            double climateFactor,
            long historySeed,
            HistoryTerrainInput.CellSample[] grid,
            long[] regionKeys,
            boolean[] ocean,
            double[] hab,
            int[] ownerSnap,
            MutablePolity[] polityLookup,
            double[] popCurr,
            double[] scarCurr,
            double[] popNext,
            double[] scarNext,
            double[] stepDisasterSev
    ) {
        for (int i = from; i < to; i++) {
            if (ocean[i]) {
                popNext[i] = 0.0D;
                scarNext[i] = 0.0D;
                stepDisasterSev[i] = 0.0D;
                continue;
            }
            double decayedScar = StrictMath.max(0.0D, scarCurr[i] - 0.20D);
            int owner = ownerSnap[i];
            double p = popCurr[i];
            double sev = 0.0D;

            double pHaz = 10.0D * grid[i].annualHazardRate();
            long hMix = SplitMix64.rand(historySeed, step, regionKeys[i], 0x48415AL);
            if (SplitMix64.double01(hMix) < pHaz) {
                sev = 0.25D + 0.75D * SplitMix64.double01(SplitMix64.mix64(hMix + 1));
                decayedScar = StrictMath.max(decayedScar, sev);
            }
            stepDisasterSev[i] = sev;
            scarNext[i] = decayedScar;

            if (owner < 0 || owner >= polityLookup.length || p <= 1.0D) {
                popNext[i] = p * 0.85D;
                continue;
            }
            int era = polityLookup[owner].era;
            double k = carryingCapacity(hab[i], era, climateFactor, decayedScar);
            if (k <= 1.0D) {
                popNext[i] = 0.0D;
                continue;
            }
            double rho = (era == 0) ? 0.075D : 0.15D;
            double grown = p + rho * p * (1.0D - p / k);
            if (sev > 0.0D) {
                double lossFrac = (0.20D + 0.40D * SplitMix64.double01(SplitMix64.mix64(hMix + 2))) * sev;
                grown *= (1.0D - StrictMath.min(0.65D, lossFrac));
            }
            popNext[i] = StrictMath.max(10.0D, grown);
        }
    }

    private static double carryingCapacity(double h, int era, double climateFactor, double scar) {
        int clampedEra = Math.clamp(era, 0, ERA_TECH_MULT.length - 1);
        double scarMult = StrictMath.max(0.35D, 1.0D - 0.5D * scar);
        return K0_POP_PER_CELL * h * ERA_TECH_MULT[clampedEra] * climateFactor * scarMult;
    }

    private static List<Integer> selectCultureHearths(
            HistoryTerrainInput.CellSample[] grid,
            List<Integer> landIndices,
            int targetCount,
            int baseMinSpacing,
            long historySeed,
            boolean wrapX,
            int gridWidth
    ) {
        List<Integer> chosen = new ArrayList<>(targetCount);
        if (landIndices.isEmpty()) {
            return chosen;
        }
        double[] thresholds = {0.60D, 0.40D, 0.15D};
        int[] spacings = {baseMinSpacing, Math.max(3, baseMinSpacing / 2), 2};

        for (int pass = 0; pass < thresholds.length && chosen.size() < targetCount; pass++) {
            final int currentPass = pass;
            double minH = thresholds[currentPass];
            int minDist = spacings[currentPass];
            List<Integer> candidates = new ArrayList<>();
            for (int idx : landIndices) {
                if (grid[idx].habitability() >= minH) {
                    candidates.add(idx);
                }
            }
            candidates.sort((a, b) -> {
                double scoreA = grid[a].habitability() + 0.25D * SplitMix64.rand01(historySeed, currentPass, grid[a].regionKey(), 0x48525448L);
                double scoreB = grid[b].habitability() + 0.25D * SplitMix64.rand01(historySeed, currentPass, grid[b].regionKey(), 0x48525448L);
                int cmp = Double.compare(scoreB, scoreA);
                return cmp != 0 ? cmp : Integer.compare(a, b);
            });
            for (int cand : candidates) {
                if (chosen.size() >= targetCount) {
                    break;
                }
                if (chosen.contains(cand)) {
                    continue;
                }
                boolean farEnough = true;
                for (int existing : chosen) {
                    int dist = regionChebyshevDistance(grid[cand].regionKey(), grid[existing].regionKey(), wrapX);
                    if (dist < minDist) {
                        farEnough = false;
                        break;
                    }
                }
                if (farEnough) {
                    chosen.add(cand);
                }
            }
        }
        return chosen;
    }

    private static void ensureNearSpawnEarlySites(
            HistoryTerrainInput terrain,
            HistoryTerrainInput.CellSample[] grid,
            List<Integer> landIndices,
            List<Culture> cultures,
            List<MutablePolity> polities,
            List<Site> rawSites,
            long historySeed,
            boolean nationsLoaded
    ) {
        if (cultures.isEmpty() || landIndices.isEmpty()) {
            return;
        }
        int cx = terrain.centerRx();
        int cz = terrain.centerRz();
        List<HistoryTerrainInput.CellSample> nearSpawn = new ArrayList<>();
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int rx = cx + dx;
                int rz = cz + dz;
                if (rx >= terrain.minRx() && rx <= terrain.maxRxInclusive()
                        && rz >= terrain.minRz() && rz <= terrain.maxRzInclusive()) {
                    HistoryTerrainInput.CellSample s = terrain.sample(rx, rz);
                    if (!s.ocean()) {
                        nearSpawn.add(s);
                    }
                }
            }
        }
        if (nearSpawn.isEmpty()) {
            nearSpawn.add(grid[landIndices.getFirst()]);
        }
        Culture nearestCulture = cultures.get(findNearestCulture(RegionApi.regionKey(cx, cz), cultures, terrain.wrapsX()));
        int polityId = polities.isEmpty() ? -1 : polities.getFirst().id;
        SiteType[] earlyTypes = {SiteType.ROCK_SHELTER, SiteType.CAMP_REMAINS, SiteType.MEGALITH};
        for (int i = 0; i < 3; i++) {
            HistoryTerrainInput.CellSample target = nearSpawn.get(i % nearSpawn.size());
            BlockPos pos = new BlockPos(
                    cx * 512 + 128 + i * 96,
                    Math.max(64, target.elevation()),
                    cz * 512 + 128 + ((i * 112) % 256)
            );
            int built = -4_400 + i * 500;
            int aband = built + 350;
            ArchStyle style = earlyTypes[i].defaultStyle();
            float w = computeWeathering(-aband, target.climateWeatheringFactor(), style);
            byte burial = computeBurialDepth(-aband, target.sedimentRate());
            rawSites.add(new Site(
                    rawSites.size(), earlyTypes[i], pos, nearestCulture.id(), polityId,
                    built, aband, style, SiteOrigin.SIMULATED, w, burial, List.of(), 0
            ));
        }
    }

    private static MutablePolity spawnPolity(
            List<MutablePolity> allPolities,
            TreeMap<Integer, MutablePolity> activePolities,
            Culture culture,
            int year,
            int capitalCell,
            int initialEra,
            List<Integer> predecessors,
            long historySeed
    ) {
        int id = allPolities.size();
        long pMix = SplitMix64.rand(historySeed, year, capitalCell, 0x504F4CL + id);
        String name = culture.language().generatePolityName(pMix ^ id);
        double baseAggr = 0.25D + 0.65D * SplitMix64.double01(SplitMix64.mix64(pMix + 7));
        double aggr = Math.clamp(baseAggr + culture.deity().aggressionModifier(), 0.2D, 1.0D);
        UUID detUuid = SplitMix64.deterministicUuid(historySeed, 0x4349565F48495354L, id);
        MutablePolity mp = new MutablePolity(id, culture.id(), name, year, capitalCell, initialEra, aggr, predecessors, detUuid);
        allPolities.add(mp);
        activePolities.put(id, mp);
        return mp;
    }

    private static void onPolityEraAdvance(
            MutablePolity p,
            int year,
            HistoryTerrainInput.CellSample[] grid,
            List<Culture> cultures,
            ScriptType[] cultureScripts,
            List<Person> persons,
            List<HistoryEvent> events,
            List<EventEdge> edges,
            List<Site> rawSites,
            byte[] cellHazards,
            long historySeed,
            boolean nationsLoaded
    ) {
        Culture c = cultures.get(p.cultureId);
        HistoryTerrainInput.CellSample cap = grid[p.capitalCell];
        if (p.era == 2 && p.cultureId < cultureScripts.length && cultureScripts[p.cultureId] == ScriptType.NONE) {
            ScriptType[] choices = {ScriptType.PICTOGRAPHIC, ScriptType.LOGOGRAPHIC, ScriptType.SYLLABIC, ScriptType.ALPHABETIC};
            cultureScripts[p.cultureId] = choices[(c.language().ordinal() + p.cultureId) % choices.length];
        }
        int scholarId = addPerson(persons, p.id, c, year - 35, year + 25,
                p.era == 1 ? PersonRole.PROPHET : PersonRole.ARTISAN, historySeed);
        int invEv = addEvent(events, EventType.INVENTION, year, cap.regionKey(),
                List.of(p.id), scholarId >= 0 ? List.of(scholarId) : List.of(),
                computeImportance(p.finalPopulation, true, false, scholarId >= 0 ? 1 : 0), 0);
        if (p.lastMajorEventId >= 0 && invEv >= 0) {
            addEdge(edges, p.lastMajorEventId, invEv, EdgeType.CAUSED);
        }
        p.lastMajorEventId = invEv >= 0 ? invEv : p.lastMajorEventId;

        if (p.era == 1 && (c.deity() == Deity.SUN || c.deity() == Deity.ANCESTOR)) {
            addSite(rawSites, SiteType.MEGALITH, cap, c.id(), p.id, year, year + 600, ArchStyle.MEGALITHIC,
                    Site.FLAG_HOLY_SITE, historySeed, nationsLoaded);
            p.holySiteRegions.add(cap.regionKey());
            cellHazards[p.capitalCell] |= HistoryCell.HAZARD_HOLY_SITE;
        } else if (p.era == 2) {
            SiteType temple = cap.biomeBand() == HistoryTerrainInput.BiomeBand.WETLAND_JUNGLE
                    ? SiteType.TERRACE_TEMPLE : SiteType.TEMPLE_RUIN;
            addSite(rawSites, temple, cap, c.id(), p.id, year, year + 700, ArchStyle.ASHLAR,
                    Site.FLAG_HOLY_SITE, historySeed, nationsLoaded);
            addSite(rawSites, SiteType.OBSERVATORY_RUIN, cap, c.id(), p.id, year, year + 650, ArchStyle.ASHLAR,
                    0, historySeed, nationsLoaded);
            p.holySiteRegions.add(cap.regionKey());
            cellHazards[p.capitalCell] |= HistoryCell.HAZARD_HOLY_SITE;
        } else if (p.era == 3) {
            addSite(rawSites, SiteType.LIBRARY_RUIN, cap, c.id(), p.id, year, year + 500, ArchStyle.ASHLAR,
                    Site.FLAG_BILINGUAL_TABLET, historySeed, nationsLoaded);
            if (cap.elevation() >= 85) {
                addSite(rawSites, SiteType.AQUEDUCT_RUIN, cap, c.id(), p.id, year, year + 500, ArchStyle.ASHLAR,
                        0, historySeed, nationsLoaded);
            }
        } else if (p.era == 4) {
            addEvent(events, EventType.HERESY, year, cap.regionKey(), List.of(p.id), List.of(),
                    computeImportance(p.finalPopulation * 0.3D, false, false, 0), 40);
            addSite(rawSites, SiteType.CULT_SANCTUM, cap, c.id(), p.id, year, year + 300, ArchStyle.CAVE_CUT,
                    0, historySeed, nationsLoaded);
        }
    }

    private static void collapsePolity(
            MutablePolity p,
            int year,
            int step,
            HistoryTerrainInput.CellSample[] grid,
            long[] regionKeys,
            int[][] neighbors4,
            int[] ownerPolity,
            int[] dominantCulture,
            int[] lastPolity,
            double[] popCurr,
            byte[] cellHazards,
            List<Culture> cultures,
            List<MutablePolity> allPolities,
            TreeMap<Integer, MutablePolity> activePolities,
            List<Person> persons,
            List<HistoryEvent> events,
            List<EventEdge> edges,
            List<Site> rawSites,
            long historySeed,
            boolean nationsLoaded
    ) {
        Culture c = cultures.get(p.cultureId);
        HistoryTerrainInput.CellSample cap = grid[p.capitalCell];
        cellHazards[p.capitalCell] |= HistoryCell.HAZARD_FALLEN_REALM;
        int lastKingId = addPerson(persons, p.id, c, year - 45, year, PersonRole.RULER, historySeed);
        int colEv = addEvent(events, EventType.COLLAPSE, year, cap.regionKey(),
                List.of(p.id), lastKingId >= 0 ? List.of(lastKingId) : List.of(),
                computeImportance(p.finalPopulation, true, true, lastKingId >= 0 ? 1 : 0), 85);
        if (p.lastMajorEventId >= 0 && colEv >= 0) {
            addEdge(edges, p.lastMajorEventId, colEv, EdgeType.CAUSED);
        }

        // Capital & tomb & settlement ruins
        addSite(rawSites, SiteType.CITY_RUIN, cap, c.id(), p.id, p.foundedYear, year, c.primaryStyle(),
                0, historySeed, nationsLoaded);
        if (c.burial() == Burial.NECROPOLIS && p.peakEra >= 2) {
            addSite(rawSites, SiteType.NECROPOLIS, cap, c.id(), p.id, p.foundedYear + 50, year, ArchStyle.ASHLAR,
                    Site.FLAG_HOLY_SITE, historySeed, nationsLoaded);
        } else if (c.burial() == Burial.BARROW && p.peakEra >= 1) {
            addSite(rawSites, SiteType.BARROW, cap, c.id(), p.id, p.foundedYear + 30, year, ArchStyle.DRYSTONE,
                    0, historySeed, nationsLoaded);
        }

        List<Integer> formerCells = new ArrayList<>(p.ownedCells);
        for (int idx : formerCells) {
            if (idx != p.capitalCell && popCurr[idx] >= 600.0D) {
                SiteType st = popCurr[idx] >= 3_000.0D ? SiteType.CITY_RUIN : SiteType.VILLAGE_RUIN;
                addSite(rawSites, st, grid[idx], c.id(), p.id, p.foundedYear, year, c.secondaryStyle(),
                        0, historySeed, nationsLoaded);
            }
        }

        markExtinct(p, activePolities, year);

        // Partition former territory into connected components -> 50% become successor polities (18 §5.1.5 step 8)
        List<List<Integer>> components = connectedComponents(formerCells, neighbors4);
        for (int ci = 0; ci < components.size(); ci++) {
            List<Integer> comp = components.get(ci);
            int compHead = comp.getFirst();
            double succRoll = SplitMix64.rand01(historySeed, step, regionKeys[compHead], 0x53554343L + ci);
            if (succRoll < 0.50D && allPolities.size() < MAX_POLITIES) {
                MutablePolity succ = spawnPolity(
                        allPolities, activePolities, c, year, compHead,
                        Math.max(0, p.era - 1), List.of(p.id), historySeed
                );
                for (int cIdx : comp) {
                    ownerPolity[cIdx] = succ.id;
                    dominantCulture[cIdx] = succ.cultureId;
                    lastPolity[cIdx] = succ.id;
                    popCurr[cIdx] *= 0.75D;
                    succ.ownedCells.add(cIdx);
                }
                int sEv = addEvent(events, EventType.FOUNDED, year, regionKeys[compHead],
                        List.of(succ.id, p.id), List.of(), computeImportance(popCurr[compHead], true, false, 0), 0);
                succ.foundingEventId = sEv;
                succ.lastMajorEventId = sEv;
                if (colEv >= 0 && sEv >= 0) {
                    addEdge(edges, colEv, sEv, EdgeType.SUCCEEDED);
                }
            } else {
                for (int cIdx : comp) {
                    ownerPolity[cIdx] = -1;
                    popCurr[cIdx] *= 0.30D;
                    cellHazards[cIdx] |= HistoryCell.HAZARD_BANDITRY;
                }
            }
        }
    }

    private static void markExtinct(MutablePolity p, TreeMap<Integer, MutablePolity> activePolities, int year) {
        p.alive = false;
        p.endYear = year;
        p.ownedCells.clear();
        activePolities.remove(p.id);
    }

    private static List<List<Integer>> connectedComponents(List<Integer> cells, int[][] neighbors4) {
        List<List<Integer>> out = new ArrayList<>();
        if (cells.isEmpty()) {
            return out;
        }
        boolean[] remaining = new boolean[neighbors4.length];
        for (int idx : cells) {
            remaining[idx] = true;
        }
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int start : cells) {
            if (!remaining[start]) {
                continue;
            }
            List<Integer> comp = new ArrayList<>();
            remaining[start] = false;
            queue.add(start);
            while (!queue.isEmpty()) {
                int cur = queue.poll();
                comp.add(cur);
                for (int nb : neighbors4[cur]) {
                    if (remaining[nb]) {
                        remaining[nb] = false;
                        queue.add(nb);
                    }
                }
            }
            out.add(comp);
        }
        return out;
    }

    private static int pickMigrationTarget(
            int srcIdx,
            MutablePolity p,
            HistoryTerrainInput.CellSample[] grid,
            int[][] neighbors4,
            int[] ownerPolity,
            double[] hab
    ) {
        int best = -1;
        double bestScore = -1e9D;
        for (int nb : neighbors4[srcIdx]) {
            HistoryTerrainInput.CellSample cell = grid[nb];
            if (cell.ocean()) {
                continue;
            }
            boolean owned = ownerPolity[nb] >= 0;
            if (owned && ownerPolity[nb] != p.id) {
                continue;
            }
            double score = hab[nb] - 0.10D * cell.terrainCost() - (owned ? 0.30D : 0.0D);
            if (score > bestScore) {
                bestScore = score;
                best = nb;
            }
        }
        return best;
    }

    private static int findSharedBorderCell(MutablePolity loser, MutablePolity winner, int[][] neighbors4, int[] ownerPolity) {
        for (int cIdx : loser.ownedCells) {
            for (int nb : neighbors4[cIdx]) {
                if (ownerPolity[nb] == winner.id) {
                    return cIdx;
                }
            }
        }
        return loser.ownedCells.isEmpty() ? -1 : loser.ownedCells.getFirst();
    }

    private static void buildTradeRoute(
            List<Route> routes,
            List<Site> rawSites,
            HistoryTerrainInput.CellSample[] grid,
            long[] regionKeys,
            int[][] neighbors4,
            MutablePolity a,
            MutablePolity b,
            int fromYear,
            int toYear,
            long historySeed,
            boolean nationsLoaded
    ) {
        List<Integer> pathCells = shortestCellPath(a.capitalCell, b.capitalCell, grid, neighbors4, false, 128);
        if (pathCells.size() < 2) {
            return;
        }
        List<Long> pts = new ArrayList<>(pathCells.size());
        for (int idx : pathCells) {
            pts.add(regionKeys[idx]);
        }
        float wRoute = computeWeathering(Math.max(50, -toYear), grid[a.capitalCell].climateWeatheringFactor(), ArchStyle.DRYSTONE);
        routes.add(new Route(routes.size(), RouteKind.LAND, a.id, b.id, fromYear, toYear, pts, wRoute));

        for (int i = 1; i < pathCells.size() - 1; i++) {
            int idx = pathCells.get(i);
            if (i % 4 == 0) {
                addSite(rawSites, SiteType.WAYSTATION_RUIN, grid[idx], a.cultureId, a.id,
                        fromYear, toYear, ArchStyle.DRYSTONE, 0, historySeed, nationsLoaded);
            } else if (i % 2 == 0) {
                addSite(rawSites, SiteType.ANCIENT_ROAD, grid[idx], a.cultureId, a.id,
                        fromYear, toYear, ArchStyle.DRYSTONE, 0, historySeed, nationsLoaded);
            }
        }
    }

    private static void buildMaritimeRoutes(
            List<Route> routes,
            List<Site> rawSites,
            List<HistoryEvent> events,
            HistoryTerrainInput.CellSample[] grid,
            long[] regionKeys,
            int[][] neighbors4,
            TreeMap<Integer, MutablePolity> activePolities,
            int year,
            int step,
            long historySeed,
            boolean nationsLoaded
    ) {
        List<MutablePolity> coastalPolities = new ArrayList<>();
        List<Integer> portCells = new ArrayList<>();
        for (MutablePolity mp : activePolities.values()) {
            if (mp.era >= 1) {
                int port = -1;
                if (grid[mp.capitalCell].coastal()) {
                    port = mp.capitalCell;
                } else {
                    for (int cIdx : mp.ownedCells) {
                        if (grid[cIdx].coastal()) {
                            port = cIdx;
                            break;
                        }
                    }
                }
                if (port >= 0) {
                    coastalPolities.add(mp);
                    portCells.add(port);
                }
            }
        }
        for (int i = 0; i + 1 < coastalPolities.size() && routes.size() < MAX_ROUTES; i += 2) {
            MutablePolity a = coastalPolities.get(i);
            MutablePolity b = coastalPolities.get(i + 1);
            int portA = portCells.get(i);
            int portB = portCells.get(i + 1);
            List<Integer> seaPath = shortestCellPath(portA, portB, grid, neighbors4, true, 128);
            if (seaPath.size() < 2) {
                continue;
            }
            List<Long> pts = new ArrayList<>(seaPath.size());
            for (int idx : seaPath) {
                pts.add(regionKeys[idx]);
            }
            float wSea = computeWeathering(Math.max(50, -year), 1.4D, ArchStyle.TIMBER);
            routes.add(new Route(routes.size(), RouteKind.SEA, a.id, b.id, year - 100, year, pts, wSea));
            addSite(rawSites, SiteType.ANCIENT_HARBOR, grid[portA], a.cultureId, a.id,
                    year - 100, year, ArchStyle.ASHLAR, 0, historySeed, nationsLoaded);
            int midIdx = seaPath.get(seaPath.size() / 2);
            if (grid[midIdx].ocean() && SplitMix64.rand01(historySeed, step, regionKeys[midIdx], 0x57524543L) < 0.35D) {
                addEvent(events, EventType.SEA_RAID, year, regionKeys[midIdx], List.of(a.id, b.id), List.of(), 45, 50);
                addSite(rawSites, SiteType.ANCIENT_WRECK, grid[midIdx], a.cultureId, a.id,
                        year, year, ArchStyle.TIMBER, 0, historySeed, nationsLoaded);
            }
        }
    }

    private static List<Integer> shortestCellPath(
            int startIdx,
            int goalIdx,
            HistoryTerrainInput.CellSample[] grid,
            int[][] neighbors4,
            boolean allowOcean,
            int maxSteps
    ) {
        if (startIdx == goalIdx) {
            return List.of(startIdx);
        }
        record Node(int cell, double f, int depth) implements Comparable<Node> {
            @Override
            public int compareTo(Node o) {
                int c = Double.compare(this.f, o.f);
                return c != 0 ? c : Integer.compare(this.cell, o.cell);
            }
        }
        TreeMap<Integer, Double> gScore = new TreeMap<>();
        TreeMap<Integer, Integer> cameFrom = new TreeMap<>();
        PriorityQueue<Node> open = new PriorityQueue<>();
        gScore.put(startIdx, 0.0D);
        open.add(new Node(startIdx, 0.0D, 0));
        int visited = 0;

        int goalRx = grid[goalIdx].rx();
        int goalRz = grid[goalIdx].rz();

        while (!open.isEmpty() && visited < 4096) {
            Node cur = open.poll();
            visited++;
            if (cur.cell == goalIdx) {
                List<Integer> rev = new ArrayList<>();
                Integer p = goalIdx;
                while (p != null) {
                    rev.add(p);
                    p = cameFrom.get(p);
                }
                java.util.Collections.reverse(rev);
                if (rev.size() <= Route.MAX_POINTS) {
                    return rev;
                }
                List<Integer> sampled = new ArrayList<>(Route.MAX_POINTS);
                for (int i = 0; i < Route.MAX_POINTS; i++) {
                    int idx = (i == Route.MAX_POINTS - 1) ? (rev.size() - 1) : (i * (rev.size() - 1)) / (Route.MAX_POINTS - 1);
                    sampled.add(rev.get(idx));
                }
                return sampled;
            }
            if (cur.depth >= maxSteps) {
                continue;
            }
            double curG = gScore.getOrDefault(cur.cell, Double.POSITIVE_INFINITY);
            for (int nb : neighbors4[cur.cell]) {
                HistoryTerrainInput.CellSample s = grid[nb];
                if (!allowOcean && s.ocean()) {
                    continue;
                }
                double stepCost = s.ocean() ? 1.1D : s.terrainCost();
                double nextG = curG + stepCost;
                if (nextG < gScore.getOrDefault(nb, Double.POSITIVE_INFINITY)) {
                    gScore.put(nb, nextG);
                    cameFrom.put(nb, cur.cell);
                    double h = StrictMath.abs(s.rx() - goalRx) + StrictMath.abs(s.rz() - goalRz);
                    open.add(new Node(nb, nextG + h * 0.8D, cur.depth + 1));
                }
            }
        }
        return List.of(startIdx, goalIdx);
    }

    private static boolean isDepositNearSettledCell(
            HistoryTerrainInput.MineralDeposit dep,
            HistoryTerrainInput terrain,
            int[] ownerPolity,
            int w,
            int h,
            int minRx,
            int minRz,
            boolean wrapX
    ) {
        for (int dz = -4; dz <= 4; dz++) {
            int rz = dep.rz() + dz;
            if (rz < terrain.minRz() || rz > terrain.maxRzInclusive()) {
                continue;
            }
            int zOff = (rz - minRz) * w;
            for (int dx = -4; dx <= 4; dx++) {
                int rx = dep.rx() + dx;
                int xIdx;
                if (wrapX) {
                    xIdx = Math.floorMod(rx - minRx, w);
                } else {
                    if (rx < terrain.minRx() || rx > terrain.maxRxInclusive()) {
                        continue;
                    }
                    xIdx = rx - minRx;
                }
                if (ownerPolity[zOff + xIdx] >= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int addPerson(
            List<Person> persons,
            int polityId,
            Culture culture,
            int born,
            int died,
            PersonRole role,
            long historySeed
    ) {
        if (persons.size() >= MAX_PERSONS) {
            return -1;
        }
        int id = persons.size();
        long h = SplitMix64.rand(historySeed, born, polityId, 0x50455253L + id);
        String name = culture.language().generateRootName(h);
        String ep = EPITHETS[SplitMix64.intRange(h >>> 12, EPITHETS.length)];
        persons.add(new Person(id, polityId, name, ep, born, died, role, List.of()));
        return id;
    }

    private static int addEvent(
            List<HistoryEvent> events,
            EventType type,
            int year,
            long regionKey,
            List<Integer> polities,
            List<Integer> persons,
            int importance,
            int severity
    ) {
        if (events.size() >= MAX_EVENTS * 2) {
            return -1;
        }
        int id = events.size();
        events.add(new HistoryEvent(id, type, year, regionKey, polities, persons, importance, severity));
        return id;
    }

    private static void addEdge(List<EventEdge> edges, int from, int to, EdgeType type) {
        if (edges.size() < MAX_EDGES && from >= 0 && to >= 0 && from != to) {
            edges.add(new EventEdge(from, to, type));
        }
    }

    private static void addSite(
            List<Site> rawSites,
            SiteType type,
            HistoryTerrainInput.CellSample cell,
            int cultureId,
            int polityId,
            int builtYear,
            int abandonedYear,
            ArchStyle style,
            int flags,
            long historySeed,
            boolean nationsLoaded
    ) {
        int clampedAband = Math.min(END_YEAR, Math.max(builtYear, abandonedYear));
        int age = Math.max(0, -clampedAband);
        float w = (!nationsLoaded && age <= 15)
                ? 0.05F
                : computeWeathering(age, cell.climateWeatheringFactor(), style);
        byte burial = computeBurialDepth(age, cell.sedimentRate());
        BlockPos anchor = anchorInCell(cell, historySeed, rawSites.size(), 0x53495445L);
        rawSites.add(new Site(
                rawSites.size(), type, anchor, cultureId, polityId,
                builtYear, clampedAband, style, SiteOrigin.SIMULATED,
                w, burial, List.of(), flags
        ));
    }

    public static float computeWeathering(int ageYears, double kClim, ArchStyle style) {
        double exponent = -(Math.max(0, ageYears) * kClim) / style.tauYears();
        double w = 1.0D - StrictMath.exp(exponent);
        return (float) Math.clamp(w, 0.0D, 1.0D);
    }

    public static byte computeBurialDepth(int ageYears, double sedimentRate) {
        long d = StrictMath.round((Math.max(0, ageYears) / 800.0D) * sedimentRate);
        return (byte) Math.clamp(d, 0L, 6L);
    }

    private static int computeImportance(double affectedPop, boolean capitalRelated, boolean collapseOrExtinction, int personCount) {
        double base = 10.0D * StrictMath.log10(StrictMath.max(0.0D, affectedPop) + 1.0D);
        if (capitalRelated) {
            base += 20.0D;
        }
        if (collapseOrExtinction) {
            base += 15.0D;
        }
        base += 5.0D * personCount;
        return (int) StrictMath.round(base);
    }

    private static BlockPos anchorInCell(HistoryTerrainInput.CellSample cell, long historySeed, int saltIndex, long channel) {
        long h = SplitMix64.rand(historySeed, saltIndex, cell.regionKey(), channel);
        int offX = 48 + SplitMix64.intRange(h, 416);
        int offZ = 48 + SplitMix64.intRange(h >>> 16, 416);
        int y = cell.ocean() ? 42 : Math.clamp(cell.elevation(), 62, 320);
        return new BlockPos(cell.rx() * 512 + offX, y, cell.rz() * 512 + offZ);
    }

    private static int regionChebyshevDistance(long rkA, long rkB, boolean wrapX) {
        int ax = (int) (rkA >> 32);
        int az = (int) rkA;
        int bx = (int) (rkB >> 32);
        int bz = (int) rkB;
        int dx = StrictMath.abs(ax - bx);
        if (wrapX && dx > 160) {
            dx = 320 - dx;
        }
        int dz = StrictMath.abs(az - bz);
        return StrictMath.max(dx, dz);
    }

    private static int findNearestCulture(long regionKey, List<Culture> cultures, boolean wrapX) {
        if (cultures.isEmpty()) {
            return 0;
        }
        int best = cultures.getFirst().id();
        int bestDist = Integer.MAX_VALUE;
        for (Culture c : cultures) {
            int d = regionChebyshevDistance(regionKey, c.hearthRegion(), wrapX);
            if (d < bestDist) {
                bestDist = d;
                best = c.id();
            }
        }
        return best;
    }

    private static long findRepresentativeContinentRegion(List<Integer> landIndices, int[] continentOf, long[] regionKeys, int continentId) {
        for (int idx : landIndices) {
            if (continentOf[idx] == continentId) {
                return regionKeys[idx];
            }
        }
        return landIndices.isEmpty() ? 0L : regionKeys[landIndices.getFirst()];
    }

    private static List<Site> trimAndReindexSites(List<Site> rawSites, int maxSites) {
        List<Site> ordered = new ArrayList<>(rawSites);
        ordered.sort(Comparator.comparingInt((Site s) -> sitePriority(s.type())).reversed()
                .thenComparingInt(Site::id));

        TreeMap<Long, Integer> perCellCounts = new TreeMap<>();
        List<Site> kept = new ArrayList<>(Math.min(maxSites, ordered.size()));
        for (Site s : ordered) {
            if (kept.size() >= maxSites) {
                break;
            }
            long rk = s.regionKey();
            int count = perCellCounts.getOrDefault(rk, 0);
            if (count >= HistoryCell.MAX_SITES_PER_CELL) {
                continue;
            }
            perCellCounts.put(rk, count + 1);
            kept.add(s);
        }
        kept.sort(Comparator.comparingInt(Site::id));
        List<Site> reindexed = new ArrayList<>(kept.size());
        for (int i = 0; i < kept.size(); i++) {
            Site s = kept.get(i);
            reindexed.add(new Site(
                    i, s.type(), s.anchor(), s.cultureId(), s.polityId(),
                    s.builtYear(), s.abandonedYear(), s.style(), s.origin(),
                    s.weathering(), s.burialDepth(), s.legendSeeds(), s.flags()
            ));
        }
        return reindexed;
    }

    private static int sitePriority(SiteType t) {
        return switch (t) {
            case CITY_RUIN, NECROPOLIS, SUNKEN_CITY, LIBRARY_RUIN -> 100;
            case TEMPLE_RUIN, TERRACE_TEMPLE, FORTRESS_RUIN, TELL, CULT_SANCTUM -> 85;
            case ANCIENT_MINE, ANCIENT_HARBOR, OBSERVATORY_RUIN, AQUEDUCT_RUIN, MEGALITH -> 75;
            case ROCK_SHELTER, FROZEN_CAMP, BARROW, BATTLEFIELD -> 65;
            default -> 50;
        };
    }

    private static List<HistoryEvent> trimEvents(List<HistoryEvent> events) {
        List<HistoryEvent> copy = new ArrayList<>(events);
        copy.sort(Comparator.comparingInt(HistoryEvent::importance).reversed().thenComparingInt(HistoryEvent::id));
        List<HistoryEvent> top = new ArrayList<>(copy.subList(0, MAX_EVENTS));
        top.sort(Comparator.comparingInt(HistoryEvent::id));
        return List.copyOf(top);
    }

    private static List<LegendSeed> generateLegendSeeds(
            HistoryTerrainInput terrain,
            List<Site> sites,
            List<HistoryEvent> events,
            long historySeed
    ) {
        List<LegendSeed> seeds = new ArrayList<>();
        int chainSeq = 0;

        // 1. Origin legend pointing to closest early site near spawn/window center (18 §2.1, §5.6.3)
        BlockPos spawnCenter = new BlockPos(terrain.centerRx() * 512 + 256, 64, terrain.centerRz() * 512 + 256);
        Site closestEarly = null;
        long bestDistSq = Long.MAX_VALUE;
        for (Site s : sites) {
            if (s.type() == SiteType.ROCK_SHELTER || s.type() == SiteType.CAMP_REMAINS || s.type() == SiteType.MEGALITH) {
                long dx = s.anchor().getX() - spawnCenter.getX();
                long dz = s.anchor().getZ() - spawnCenter.getZ();
                long dSq = dx * dx + dz * dz;
                if (dSq < bestDistSq) {
                    bestDistSq = dSq;
                    closestEarly = s;
                }
            }
        }
        if (closestEarly != null) {
            int evId = events.isEmpty() ? 0 : events.getFirst().id();
            seeds.add(new LegendSeed(seeds.size(), LegendKind.ORIGIN, evId, closestEarly.id(), chainSeq++, 0, (byte) 1));
        }

        // 2. Map region -> site for linking major events to physical ruins
        TreeMap<Long, Integer> primarySiteByRegion = new TreeMap<>();
        List<Site> royalTombs = new ArrayList<>();
        List<Site> cities = new ArrayList<>();
        for (Site s : sites) {
            primarySiteByRegion.putIfAbsent(s.regionKey(), s.id());
            if (s.type() == SiteType.NECROPOLIS || s.type() == SiteType.BARROW) {
                royalTombs.add(s);
            } else if (s.type() == SiteType.CITY_RUIN) {
                cities.add(s);
            }
        }

        // 3. Multi-step royal tomb clue chains (city king-list -> burial curse -> royal tomb)
        int maxChains = Math.min(24, Math.min(cities.size(), royalTombs.size()));
        for (int i = 0; i < maxChains && seeds.size() + 3 <= MAX_LEGEND_SEEDS; i++) {
            Site city = cities.get(i);
            Site tomb = royalTombs.get(i);
            int cId = chainSeq++;
            int evId = events.isEmpty() ? 0 : events.get(i % events.size()).id();
            seeds.add(new LegendSeed(seeds.size(), LegendKind.FOUNDING, evId, city.id(), cId, 0, (byte) 1));
            seeds.add(new LegendSeed(seeds.size(), LegendKind.FALL, evId, city.id(), cId, 1, (byte) 1));
            seeds.add(new LegendSeed(seeds.size(), LegendKind.KING_TOMB, evId, tomb.id(), cId, 2, (byte) 1));
        }

        // 4. Standalone event-backed legends for importance >= 40 (18 §5.1.7)
        for (HistoryEvent ev : events) {
            if (seeds.size() >= MAX_LEGEND_SEEDS) {
                break;
            }
            if (ev.importance() < 40) {
                continue;
            }
            int siteId = primarySiteByRegion.getOrDefault(ev.region(), -1);
            LegendKind kind = switch (ev.type()) {
                case FOUNDED, MIGRATED -> LegendKind.FOUNDING;
                case WAR, BATTLE, SACK -> LegendKind.WAR;
                case COLLAPSE -> LegendKind.FALL;
                case DISASTER, PLAGUE, FAMINE -> LegendKind.DISASTER;
                case HUNT_EXTINCTION -> LegendKind.LAMENT;
                case SEA_RAID -> LegendKind.SEA_RAIDER;
                case HERESY -> LegendKind.PROPHECY;
                default -> LegendKind.LOST_CITY;
            };
            seeds.add(new LegendSeed(
                    seeds.size(), kind, ev.id(), siteId, -1, 0,
                    (byte) SplitMix64.intRange(SplitMix64.mix64(historySeed ^ ev.id()), 2)
            ));
        }

        return List.copyOf(seeds);
    }

    private static List<WorldHistoryData.RegionSummaryEntry> buildRegionIndex(
            int cellCount,
            HistoryTerrainInput.CellSample[] grid,
            long[] regionKeys,
            byte[] cellHazards,
            int[] dominantCulture,
            int[] lastPolity,
            List<Site> sites,
            List<HistoryEvent> events,
            List<LegendSeed> legendSeeds
    ) {
        TreeMap<Long, List<Integer>> sitesPerReg = new TreeMap<>();
        Map<Integer, Long> siteRegionById = new TreeMap<>();
        for (Site s : sites) {
            long rk = s.regionKey();
            siteRegionById.put(s.id(), rk);
            sitesPerReg.computeIfAbsent(rk, k -> new ArrayList<>()).add(s.id());
        }

        TreeMap<Long, List<Integer>> eventsPerReg = new TreeMap<>();
        Map<Integer, Long> eventRegionById = new TreeMap<>();
        for (HistoryEvent ev : events) {
            eventRegionById.put(ev.id(), ev.region());
            List<Integer> list = eventsPerReg.computeIfAbsent(ev.region(), k -> new ArrayList<>());
            if (list.size() < 32) {
                list.add(ev.id());
            }
        }

        TreeMap<Long, List<Integer>> legendsPerReg = new TreeMap<>();
        for (LegendSeed ls : legendSeeds) {
            Long rk = ls.siteId() >= 0 ? siteRegionById.get(ls.siteId()) : eventRegionById.get(ls.eventId());
            if (rk != null) {
                List<Integer> list = legendsPerReg.computeIfAbsent(rk, k -> new ArrayList<>());
                if (list.size() < 32) {
                    list.add(ls.id());
                }
            }
        }

        List<WorldHistoryData.RegionSummaryEntry> index = new ArrayList<>();
        for (int i = 0; i < cellCount; i++) {
            long rk = regionKeys[i];
            List<Integer> sIds = sitesPerReg.getOrDefault(rk, List.of());
            List<Integer> eIds = eventsPerReg.getOrDefault(rk, List.of());
            List<Integer> lIds = legendsPerReg.getOrDefault(rk, List.of());
            byte haz = cellHazards[i];
            short dom = (short) dominantCulture[i];
            short last = (short) lastPolity[i];
            if (sIds.isEmpty() && eIds.isEmpty() && lIds.isEmpty() && haz == 0 && dom < 0 && last < 0) {
                continue;
            }
            index.add(new WorldHistoryData.RegionSummaryEntry(
                    rk,
                    haz,
                    dom,
                    last,
                    (float) grid[i].climateWeatheringFactor(),
                    (float) grid[i].sedimentRate(),
                    sIds,
                    eIds,
                    lIds
            ));
        }
        return List.copyOf(index);
    }
}
