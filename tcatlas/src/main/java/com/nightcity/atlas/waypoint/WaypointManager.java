package com.nightcity.atlas.waypoint;

import com.nightcity.atlas.api.MemoryMark;
import com.nightcity.atlas.api.OkabeItoPalette;
import com.nightcity.atlas.api.Waypoint;
import com.nightcity.atlas.compat.tcivilization.TcivilizationBridge;
import com.nightcity.atlas.map.MemoryDecayModel;
import com.nightcity.atlas.map.PlanetProjection;
import com.nightcity.atlas.map.SurveyAtlasStore;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages E0 fading memory marks (max 3), personal & civilization waypoints with Okabe-Ito color+shape,
 * death markers, temporary pings, and active waypoint navigation hooked into TrackerApi (22 §5.3, §5.14).
 */
public final class WaypointManager {
    public static final int DEFAULT_HARD_CAP = 4096;
    public static final int DEFAULT_DEATH_POINT_CAP = 5;
    public static final long PING_DURATION_SECONDS = 60L;

    public record ActiveMemoryMark(MemoryMark mark, double strength) {
    }

    public record AddResult(boolean accepted, @Nullable Waypoint waypoint, String reasonKey) {
    }

    private final SurveyAtlasStore store;
    private final Map<UUID, Waypoint> waypointsById = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<MemoryMark>> playerMemoryMarks = new ConcurrentHashMap<>();
    private final Set<UUID> upgradedMarkPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> bonusUpgradedWaypointIds = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> pingExpirySeconds = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> activeNavByPlayer = new ConcurrentHashMap<>();

    private volatile int waypointHardCap = DEFAULT_HARD_CAP;
    private volatile int maxDeathPoints = DEFAULT_DEATH_POINT_CAP;

    public WaypointManager(SurveyAtlasStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public void setWaypointHardCap(int hardCap) {
        this.waypointHardCap = Math.clamp(hardCap, 64, 65536);
    }

    public void setMaxDeathPoints(int count) {
        this.maxDeathPoints = Math.clamp(count, 0, 20);
    }

    public static int personalCapForTier(int tier, int hardCap) {
        return switch (Math.clamp(tier, 0, 10)) {
            case 0 -> 0;
            case 1 -> 8;
            case 2, 3 -> 32;
            case 4 -> 64;
            case 5, 6 -> 128;
            case 7, 8 -> 256;
            case 9 -> 512;
            default -> hardCap;
        };
    }

    public static int civCapForTier(int tier, int hardCap) {
        return switch (Math.clamp(tier, 0, 10)) {
            case 0 -> 0;
            case 1 -> 16;
            case 2, 3 -> 64;
            case 4 -> 128;
            case 5, 6 -> 256;
            case 7, 8 -> 512;
            case 9 -> 1024;
            default -> hardCap;
        };
    }

    public int personalCap(int tier) {
        return personalCapForTier(tier, waypointHardCap);
    }

    public int civCap(int tier) {
        return civCapForTier(tier, waypointHardCap);
    }

    /**
     * Places an E0 fading memory mark (max 3; oldest evicted on 4th). Does NOT enter HUD TrackerApi.
     */
    public synchronized MemoryMark placeMemoryMark(
            UUID playerId,
            Identifier dimension,
            BlockPos rawPos,
            long nowGameSeconds,
            String label,
            boolean wrapX
    ) {
        int wx = wrapX ? PlanetProjection.wrapBlockX(rawPos.getX()) : rawPos.getX();
        BlockPos pos = new BlockPos(wx, rawPos.getY(), rawPos.getZ());
        store.recordPersonalSighting(playerId, dimension, pos.getX() >> 4, pos.getZ() >> 4, nowGameSeconds, wrapX);

        Deque<MemoryMark> deque = playerMemoryMarks.computeIfAbsent(playerId, k -> new ArrayDeque<>());
        while (deque.size() >= MemoryMark.MAX_E0_MARKS) {
            deque.removeFirst();
        }
        String cleanLabel = (label == null || label.isBlank()) ? ("记忆 #" + (deque.size() + 1)) : label;
        MemoryMark mark = new MemoryMark(UUID.randomUUID(), dimension, pos, nowGameSeconds, cleanLabel);
        deque.addLast(mark);
        return mark;
    }

    /**
     * Returns active E0 memory marks with their current chunk memory strength, pruning forgotten ones (m < 0.05).
     */
    public synchronized List<ActiveMemoryMark> activeMemoryMarks(
            UUID playerId,
            @Nullable Identifier dimensionFilter,
            long nowGameSeconds,
            int explorationSkillLevel,
            boolean wrapX
    ) {
        Deque<MemoryMark> deque = playerMemoryMarks.get(playerId);
        if (deque == null || deque.isEmpty()) {
            return List.of();
        }
        List<ActiveMemoryMark> result = new ArrayList<>(deque.size());
        Iterator<MemoryMark> it = deque.iterator();
        while (it.hasNext()) {
            MemoryMark m = it.next();
            double strength = store.memoryStrength(
                    playerId, m.dimension(), m.chunkX(), m.chunkZ(), nowGameSeconds, explorationSkillLevel, wrapX
            );
            if (MemoryDecayModel.isForgotten(strength)) {
                it.remove();
                continue;
            }
            if (dimensionFilter == null || dimensionFilter.equals(m.dimension())) {
                result.add(new ActiveMemoryMark(m, strength));
            }
        }
        return List.copyOf(result);
    }

    public synchronized List<MemoryMark> rawMemoryMarks(UUID playerId) {
        Deque<MemoryMark> deque = playerMemoryMarks.get(playerId);
        return deque == null ? List.of() : List.copyOf(deque);
    }

    public synchronized void loadMemoryMarks(UUID playerId, List<MemoryMark> marks) {
        Deque<MemoryMark> deque = playerMemoryMarks.computeIfAbsent(playerId, k -> new ArrayDeque<>());
        deque.clear();
        for (MemoryMark m : marks) {
            if (deque.size() < MemoryMark.MAX_E0_MARKS) {
                deque.addLast(m);
            }
        }
    }

    /**
     * One-time automatic conversion of surviving E0 memory marks into personal waypoints upon reaching T1+ (22 §5.3).
     */
    public synchronized List<Waypoint> upgradeMemoryMarksIfEligible(UUID playerId, int tier, long currentDay, long nowGameSeconds, boolean wrapX) {
        if (tier < 1 || !upgradedMarkPlayers.add(playerId)) {
            return List.of();
        }
        List<ActiveMemoryMark> surviving = activeMemoryMarks(playerId, null, nowGameSeconds, 0, wrapX);
        Deque<MemoryMark> deque = playerMemoryMarks.get(playerId);
        if (deque != null) {
            deque.clear();
        }
        List<Waypoint> upgraded = new ArrayList<>();
        for (ActiveMemoryMark am : surviving) {
            MemoryMark m = am.mark();
            Waypoint wp = new Waypoint(
                    m.id(),
                    m.label().isBlank() ? "旧日记忆" : m.label(),
                    OkabeItoPalette.WaypointCategory.CUSTOM,
                    OkabeItoPalette.WaypointCategory.CUSTOM.identifier(),
                    OkabeItoPalette.WaypointCategory.CUSTOM.defaultArgb(),
                    OkabeItoPalette.WaypointCategory.CUSTOM.defaultShape(),
                    Waypoint.Share.PRIVATE,
                    m.dimension(),
                    m.pos(),
                    32,
                    playerId,
                    currentDay,
                    Optional.of("Converted from E0 memory mark"),
                    Optional.empty()
            );
            bonusUpgradedWaypointIds.add(wp.id());
            waypointsById.put(wp.id(), wp);
            upgraded.add(wp);
        }
        return upgraded;
    }

    public synchronized AddResult addWaypoint(Waypoint candidate, int tier, boolean wrapX) {
        Objects.requireNonNull(candidate, "candidate");
        BlockPos normalizedPos = wrapX
                ? new BlockPos(PlanetProjection.wrapBlockX(candidate.pos().getX()), candidate.pos().getY(), candidate.pos().getZ())
                : candidate.pos();
        Waypoint wp = new Waypoint(
                candidate.id(), candidate.name(), candidate.category(), candidate.icon(),
                candidate.argb(), candidate.shape(), candidate.share(), candidate.dimension(),
                normalizedPos, candidate.epsilon(), candidate.creator(), candidate.createdDay(),
                candidate.note(), candidate.routeOrder()
        );

        if (wp.category().countsTowardCap()) {
            if (wp.share() == Waypoint.Share.CIVILIZATION || wp.share() == Waypoint.Share.GROUP) {
                int cap = civCap(tier);
                if (countSharedWaypoints() >= cap) {
                    return new AddResult(false, null, "gui.tcatlas.waypoint.cap_reached");
                }
            } else {
                int cap = personalCap(tier);
                if (countPersonalWaypoints(wp.creator()) >= cap) {
                    return new AddResult(false, null, "gui.tcatlas.waypoint.cap_reached");
                }
            }
        }
        waypointsById.put(wp.id(), wp);
        return new AddResult(true, wp, "");
    }

    public synchronized Optional<Waypoint> updateWaypoint(UUID actorId, Waypoint updated, boolean wrapX) {
        Waypoint existing = waypointsById.get(updated.id());
        if (existing == null) {
            return Optional.empty();
        }
        if (existing.share() == Waypoint.Share.PRIVATE && !existing.creator().equals(actorId)) {
            return Optional.empty();
        }
        BlockPos normalizedPos = wrapX
                ? new BlockPos(PlanetProjection.wrapBlockX(updated.pos().getX()), updated.pos().getY(), updated.pos().getZ())
                : updated.pos();
        Waypoint stored = new Waypoint(
                existing.id(), updated.name(), updated.category(), updated.icon(),
                updated.argb(), updated.shape(), updated.share(), updated.dimension(),
                normalizedPos, updated.epsilon(), existing.creator(), existing.createdDay(),
                updated.note(), updated.routeOrder()
        );
        waypointsById.put(stored.id(), stored);
        for (Map.Entry<UUID, UUID> nav : activeNavByPlayer.entrySet()) {
            if (stored.id().equals(nav.getValue())) {
                TcivilizationBridge.syncNavigationTrackerPin(nav.getKey(), stored);
            }
        }
        return Optional.of(stored);
    }

    public synchronized boolean removeWaypoint(UUID actorId, UUID waypointId) {
        Waypoint existing = waypointsById.get(waypointId);
        if (existing == null) {
            return false;
        }
        if (existing.share() == Waypoint.Share.PRIVATE && !existing.creator().equals(actorId)) {
            return false;
        }
        waypointsById.remove(waypointId);
        bonusUpgradedWaypointIds.remove(waypointId);
        pingExpirySeconds.remove(waypointId);
        for (Map.Entry<UUID, UUID> e : List.copyOf(activeNavByPlayer.entrySet())) {
            if (waypointId.equals(e.getValue())) {
                stopNavigation(e.getKey());
            }
        }
        return true;
    }

    public synchronized Waypoint recordDeathPoint(UUID playerId, Identifier dim, BlockPos pos, int positionEps, long currentDay, boolean wrapX) {
        if (maxDeathPoints <= 0) {
            return null;
        }
        List<Waypoint> deaths = waypointsById.values().stream()
                .filter(w -> w.category() == OkabeItoPalette.WaypointCategory.DEATH && playerId.equals(w.creator()))
                .sorted(Comparator.comparingLong(Waypoint::createdDay))
                .toList();
        int toRemove = (deaths.size() + 1) - maxDeathPoints;
        for (int i = 0; i < toRemove && i < deaths.size(); i++) {
            waypointsById.remove(deaths.get(i).id());
        }
        BlockPos norm = wrapX ? new BlockPos(PlanetProjection.wrapBlockX(pos.getX()), pos.getY(), pos.getZ()) : pos;
        Waypoint deathWp = new Waypoint(
                UUID.randomUUID(), "死亡点", OkabeItoPalette.WaypointCategory.DEATH,
                OkabeItoPalette.WaypointCategory.DEATH.identifier(),
                OkabeItoPalette.WaypointCategory.DEATH.defaultArgb(),
                OkabeItoPalette.WaypointCategory.DEATH.defaultShape(),
                Waypoint.Share.PRIVATE, dim, norm, Math.min(32, Math.max(1, positionEps)),
                playerId, currentDay, Optional.empty(), Optional.empty()
        );
        waypointsById.put(deathWp.id(), deathWp);
        return deathWp;
    }

    public synchronized Waypoint placePing(UUID playerId, Identifier dim, BlockPos pos, long nowGameSeconds, long currentDay, boolean wrapX) {
        BlockPos norm = wrapX ? new BlockPos(PlanetProjection.wrapBlockX(pos.getX()), pos.getY(), pos.getZ()) : pos;
        Waypoint ping = new Waypoint(
                UUID.randomUUID(), "标记", OkabeItoPalette.WaypointCategory.PING,
                OkabeItoPalette.WaypointCategory.PING.identifier(),
                OkabeItoPalette.WaypointCategory.PING.defaultArgb(),
                OkabeItoPalette.WaypointCategory.PING.defaultShape(),
                Waypoint.Share.CIVILIZATION, dim, norm, 4,
                playerId, currentDay, Optional.empty(), Optional.empty()
        );
        waypointsById.put(ping.id(), ping);
        pingExpirySeconds.put(ping.id(), nowGameSeconds + PING_DURATION_SECONDS);
        return ping;
    }

    public synchronized void pruneExpiredPings(long nowGameSeconds) {
        Iterator<Map.Entry<UUID, Long>> it = pingExpirySeconds.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> e = it.next();
            if (nowGameSeconds >= e.getValue()) {
                waypointsById.remove(e.getKey());
                it.remove();
            }
        }
    }

    public List<Waypoint> visibleWaypoints(@Nullable UUID viewerId, @Nullable Identifier dimFilter) {
        List<Waypoint> out = new ArrayList<>();
        for (Waypoint w : waypointsById.values()) {
            if (dimFilter != null && !dimFilter.equals(w.dimension())) {
                continue;
            }
            if (w.share() == Waypoint.Share.PRIVATE && (viewerId == null || !viewerId.equals(w.creator()))) {
                continue;
            }
            out.add(w);
        }
        out.sort(Comparator.comparing(Waypoint::name));
        return out;
    }

    public List<Waypoint> allWaypointsSnapshot() {
        return List.copyOf(waypointsById.values());
    }

    public synchronized void replaceAllWaypoints(List<Waypoint> loaded) {
        waypointsById.clear();
        for (Waypoint w : loaded) {
            waypointsById.put(w.id(), w);
        }
    }

    public Optional<Waypoint> getWaypoint(UUID id) {
        return Optional.ofNullable(waypointsById.get(id));
    }

    public int countPersonalWaypoints(UUID playerId) {
        int count = 0;
        for (Waypoint w : waypointsById.values()) {
            if (w.share() == Waypoint.Share.PRIVATE
                    && playerId.equals(w.creator())
                    && w.category().countsTowardCap()
                    && !bonusUpgradedWaypointIds.contains(w.id())) {
                count++;
            }
        }
        return count;
    }

    public int countSharedWaypoints() {
        int count = 0;
        for (Waypoint w : waypointsById.values()) {
            if (w.share() != Waypoint.Share.PRIVATE && w.category().countsTowardCap()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Starts active navigation to a waypoint and pins it into tcivilization TrackerApi (22 §5.14).
     */
    public boolean startNavigation(UUID playerId, UUID waypointId) {
        Waypoint wp = waypointsById.get(waypointId);
        if (wp == null) {
            return false;
        }
        UUID previous = activeNavByPlayer.put(playerId, waypointId);
        if (previous != null && !previous.equals(waypointId)) {
            TcivilizationBridge.unpinNavigationTracker(playerId, previous);
        }
        TcivilizationBridge.syncNavigationTrackerPin(playerId, wp);
        return true;
    }

    public void stopNavigation(UUID playerId) {
        UUID removed = activeNavByPlayer.remove(playerId);
        if (removed != null) {
            TcivilizationBridge.unpinNavigationTracker(playerId, removed);
        }
    }

    public Optional<Waypoint> activeNavigation(UUID playerId) {
        UUID wpId = activeNavByPlayer.get(playerId);
        if (wpId == null) {
            return Optional.empty();
        }
        Waypoint wp = waypointsById.get(wpId);
        if (wp == null) {
            activeNavByPlayer.remove(playerId);
            return Optional.empty();
        }
        return Optional.of(wp);
    }

    /**
     * Checks arrival condition: real distance <= 8 + 0.5 * eps_nav (22 §5.5).
     */
    public boolean checkAndHandleArrival(UUID playerId, Identifier dim, double playerX, double playerZ, double playerPosEps, boolean wrapX) {
        Optional<Waypoint> navOpt = activeNavigation(playerId);
        if (navOpt.isEmpty()) {
            return false;
        }
        Waypoint target = navOpt.get();
        if (!target.dimension().equals(dim)) {
            return false;
        }
        double dist = PlanetProjection.distance(playerX, playerZ, target.pos().getX() + 0.5, target.pos().getZ() + 0.5, wrapX);
        double navEps = playerPosEps + target.epsilon();
        if (dist <= 8.0 + 0.5 * navEps) {
            if (target.category() == OkabeItoPalette.WaypointCategory.ROUTE && target.routeOrder().isPresent()) {
                int nextOrder = target.routeOrder().get() + 1;
                Optional<Waypoint> nextLeg = visibleWaypoints(playerId, dim).stream()
                        .filter(w -> w.category() == OkabeItoPalette.WaypointCategory.ROUTE
                                && w.routeOrder().isPresent() && w.routeOrder().get() == nextOrder)
                        .findFirst();
                if (nextLeg.isPresent()) {
                    startNavigation(playerId, nextLeg.get().id());
                    return true;
                }
            }
            stopNavigation(playerId);
            return true;
        }
        return false;
    }

    public Map<UUID, List<MemoryMark>> exportAllMemoryMarks() {
        Map<UUID, List<MemoryMark>> snapshot = new LinkedHashMap<>();
        synchronized (this) {
            for (Map.Entry<UUID, Deque<MemoryMark>> e : playerMemoryMarks.entrySet()) {
                if (!e.getValue().isEmpty()) {
                    snapshot.put(e.getKey(), List.copyOf(e.getValue()));
                }
            }
        }
        return snapshot;
    }

    public synchronized void clearAll() {
        waypointsById.clear();
        playerMemoryMarks.clear();
        upgradedMarkPlayers.clear();
        bonusUpgradedWaypointIds.clear();
        pingExpirySeconds.clear();
        activeNavByPlayer.clear();
    }
}
