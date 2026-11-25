package com.nightcity.echoes.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;

/**
 * Read-only, thread-safe query view over the 5,000-year world history (18 §12.1).
 * Exposes survivor polity snapshots for tcnations, event graph traversal, and site/route/mine lookups.
 */
public interface WorldHistoryView {
    boolean isLoaded();

    SimulationMode mode();

    long historySeed();

    int formatVersion();

    int generatorVersion();

    int startYear();

    int endYear();

    long simulationMillis();

    List<Culture> cultures();

    Optional<Culture> culture(int cultureId);

    List<Polity> polities();

    Optional<Polity> polity(int polityId);

    /**
     * Surviving polities ordered by finalPopulation descending for tcnations seeding (18 §5.1.8).
     */
    List<Polity> survivors();

    /**
     * Rich survivor snapshots including territory, holy sites, feuds, old alliances, and ancestral claims.
     */
    List<SurvivorPolitySnapshot> survivorSnapshots();

    List<Person> persons();

    Optional<Person> person(int personId);

    List<HistoryEvent> events();

    Optional<HistoryEvent> event(int eventId);

    List<HistoryEvent> events(long regionKey);

    List<EventEdge> edges();

    List<EventEdge> outgoingEdges(int eventId);

    List<EventEdge> incomingEdges(int eventId);

    List<Site> sites();

    Optional<Site> site(int siteId);

    List<Site> sitesInRegion(long regionKey);

    List<SiteSummary> sitesNear(BlockPos pos, int radiusBlocks);

    List<Route> routes();

    List<Route> routesInRegion(long regionKey);

    List<AncientMine> mines();

    List<AncientMine> minesInRegion(long regionKey);

    List<LegendSeed> legendSeeds();

    List<LegendSeed> legendSeedsInRegion(long regionKey);

    List<Extinction> extinctions();

    List<Extinction> extinctions(int continentId);

    Relation relation(int polityA, int polityB);

    List<Long> holySites(int polityId);

    List<LiveHistoryEntry> liveHistory();

    Optional<UUID> historicCivilizationId(int polityId);
}
