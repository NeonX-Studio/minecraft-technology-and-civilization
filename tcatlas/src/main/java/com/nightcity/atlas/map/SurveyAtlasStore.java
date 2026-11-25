package com.nightcity.atlas.map;

import com.nightcity.atlas.api.ChunkKnowledge;
import com.nightcity.atlas.api.SurveyLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe cartographic knowledge & chunk summary store supporting shared civilization archive,
 * per-player SL2 memory decay, cairn memory anchors, and per-region tileVersion invalidation (22 §5.1-5.3, §5.13).
 */
public final class SurveyAtlasStore {
    public static final Identifier OVERWORLD_ID = Identifier.withDefaultNamespace("overworld");
    private static final int CHUNKS_PER_REGION_SIDE = 32;
    private static final int CHUNKS_PER_REGION = CHUNKS_PER_REGION_SIDE * CHUNKS_PER_REGION_SIDE;

    @FunctionalInterface
    public interface RegionDirtyListener {
        void onRegionDirty(Identifier dimension, int regionX, int regionZ, int newVersion);
    }

    public record ResolvedChunk(
            SurveyLevel effectiveSl,
            ChunkKnowledge archiveKnowledge,
            double memoryStrength,
            boolean nearCairn
    ) {
        public int effectiveEpsilon() {
            if (archiveKnowledge.sl().code() >= SurveyLevel.SKETCH.code()) {
                return archiveKnowledge.epsilon();
            }
            if (effectiveSl == SurveyLevel.MEMORY) {
                return 32;
            }
            if (effectiveSl == SurveyLevel.RUMOR) {
                return 128;
            }
            return 256;
        }
    }

    public record ChunkEntry(int chunkX, int chunkZ, ChunkKnowledge knowledge) {
    }

    public record MemoryEntry(int chunkX, int chunkZ, long seenGameSeconds) {
    }

    private static final class DimensionData {
        final Map<Long, int[]> regionKnowledge = new ConcurrentHashMap<>();
        final Map<Long, Integer> regionVersions = new ConcurrentHashMap<>();
        final Map<Long, ChunkTerrainSummary> summaries = new ConcurrentHashMap<>();
        final Set<BlockPos> cairns = Collections.synchronizedSet(new HashSet<>());
    }

    private final Map<Identifier, DimensionData> dimensions = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Identifier, Map<Long, Long>>> playerMemories = new ConcurrentHashMap<>();
    private final List<RegionDirtyListener> listeners = new CopyOnWriteArrayList<>();

    private volatile long worldSeed = 0x53757065724D43L;
    private volatile int standaloneTier = 0;
    private volatile boolean memoryDecayEnabled = true;
    private volatile double decaySpeedMultiplier = 1.0;

    public void setWorldSeed(long worldSeed) {
        this.worldSeed = worldSeed;
    }

    public long worldSeed() {
        return worldSeed;
    }

    public int standaloneTier() {
        return standaloneTier;
    }

    public void setStandaloneTier(int tier) {
        this.standaloneTier = Math.clamp(tier, 0, 10);
    }

    public void setMemoryDecayConfig(boolean enabled, double speedMultiplier) {
        this.memoryDecayEnabled = enabled;
        this.decaySpeedMultiplier = Math.max(0.0, speedMultiplier);
    }

    public boolean memoryDecayEnabled() {
        return memoryDecayEnabled;
    }

    public double decaySpeedMultiplier() {
        return decaySpeedMultiplier;
    }

    public void addDirtyListener(RegionDirtyListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public int canonicalChunkX(Identifier dim, int chunkX, boolean wrapX) {
        if (!wrapX) {
            return chunkX;
        }
        int blockX = PlanetProjection.wrapBlockX(chunkX << 4);
        return blockX >> 4;
    }

    public static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFF_FFFFL);
    }

    public static long regionKey(int regionX, int regionZ) {
        return ((long) regionX << 32) | (regionZ & 0xFFFF_FFFFL);
    }

    public ChunkKnowledge archiveKnowledge(Identifier dim, int chunkX, int chunkZ, boolean wrapX) {
        int cx = canonicalChunkX(dim, chunkX, wrapX);
        int rx = Math.floorDiv(cx, CHUNKS_PER_REGION_SIDE);
        int rz = Math.floorDiv(chunkZ, CHUNKS_PER_REGION_SIDE);
        DimensionData dd = dimensions.get(dim);
        if (dd == null) {
            return ChunkKnowledge.UNKNOWN;
        }
        int[] slots = dd.regionKnowledge.get(regionKey(rx, rz));
        if (slots == null) {
            return ChunkKnowledge.UNKNOWN;
        }
        int idx = (Math.floorMod(chunkZ, CHUNKS_PER_REGION_SIDE) << 5) | Math.floorMod(cx, CHUNKS_PER_REGION_SIDE);
        int packed;
        synchronized (slots) {
            packed = slots[idx];
        }
        return ChunkKnowledge.unpack(packed);
    }

    /**
     * Merges knowledge into the shared archive using commutative merge rule (22 §5.1) and bumps region tileVersion.
     */
    public boolean mergeArchiveKnowledge(Identifier dim, int chunkX, int chunkZ, ChunkKnowledge incoming, boolean wrapX) {
        if (incoming == null || incoming.sl() == SurveyLevel.UNKNOWN) {
            return false;
        }
        int cx = canonicalChunkX(dim, chunkX, wrapX);
        int rx = Math.floorDiv(cx, CHUNKS_PER_REGION_SIDE);
        int rz = Math.floorDiv(chunkZ, CHUNKS_PER_REGION_SIDE);
        DimensionData dd = dimensions.computeIfAbsent(dim, k -> new DimensionData());
        long rKey = regionKey(rx, rz);
        int[] slots = dd.regionKnowledge.computeIfAbsent(rKey, k -> new int[CHUNKS_PER_REGION]);
        int idx = (Math.floorMod(chunkZ, CHUNKS_PER_REGION_SIDE) << 5) | Math.floorMod(cx, CHUNKS_PER_REGION_SIDE);
        boolean changed;
        synchronized (slots) {
            ChunkKnowledge before = ChunkKnowledge.unpack(slots[idx]);
            ChunkKnowledge after = ChunkKnowledge.merge(before, incoming);
            int packedAfter = after.pack();
            changed = packedAfter != slots[idx];
            if (changed) {
                slots[idx] = packedAfter;
            }
        }
        if (changed) {
            bumpRegionVersion(dim, dd, rx, rz);
        }
        return changed;
    }

    public void recordPersonalSighting(UUID playerId, Identifier dim, int chunkX, int chunkZ, long nowGameSeconds, boolean wrapX) {
        int cx = canonicalChunkX(dim, chunkX, wrapX);
        Map<Long, Long> chunks = playerMemories
                .computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(dim, k -> new ConcurrentHashMap<>());
        Long prev = chunks.put(chunkKey(cx, chunkZ), nowGameSeconds);
        if (prev == null || Math.abs(nowGameSeconds - prev) >= 15L) {
            int rx = Math.floorDiv(cx, CHUNKS_PER_REGION_SIDE);
            int rz = Math.floorDiv(chunkZ, CHUNKS_PER_REGION_SIDE);
            DimensionData dd = dimensions.computeIfAbsent(dim, k -> new DimensionData());
            bumpRegionVersion(dim, dd, rx, rz);
        }
    }

    public double memoryStrength(
            @Nullable UUID playerId,
            Identifier dim,
            int chunkX,
            int chunkZ,
            long nowGameSeconds,
            int explorationSkillLevel,
            boolean wrapX
    ) {
        if (playerId == null) {
            return 0.0;
        }
        Map<Identifier, Map<Long, Long>> byDim = playerMemories.get(playerId);
        if (byDim == null) {
            return 0.0;
        }
        Map<Long, Long> chunks = byDim.get(dim);
        if (chunks == null) {
            return 0.0;
        }
        int cx = canonicalChunkX(dim, chunkX, wrapX);
        Long seen = chunks.get(chunkKey(cx, chunkZ));
        if (seen == null) {
            return 0.0;
        }
        boolean nearCairn = isChunkAnchoredByCairn(dim, cx, chunkZ, wrapX);
        return MemoryDecayModel.strength(
                seen, nowGameSeconds, standaloneTier, explorationSkillLevel,
                nearCairn, memoryDecayEnabled, decaySpeedMultiplier
        );
    }

    /**
     * Prunes forgotten SL2 memories (m < 0.05) returning number of forgotten chunks removed (22 §5.3).
     */
    public int pruneForgottenMemories(UUID playerId, Identifier dim, long nowGameSeconds, int explorationSkillLevel, boolean wrapX) {
        Map<Identifier, Map<Long, Long>> byDim = playerMemories.get(playerId);
        if (byDim == null) {
            return 0;
        }
        Map<Long, Long> chunks = byDim.get(dim);
        if (chunks == null) {
            return 0;
        }
        int removed = 0;
        Iterator<Map.Entry<Long, Long>> it = chunks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Long> e = it.next();
            int cx = (int) (e.getKey() >> 32);
            int cz = (int) e.getKey().longValue();
            boolean nearCairn = isChunkAnchoredByCairn(dim, cx, cz, wrapX);
            double m = MemoryDecayModel.strength(
                    e.getValue(), nowGameSeconds, standaloneTier, explorationSkillLevel,
                    nearCairn, memoryDecayEnabled, decaySpeedMultiplier
            );
            if (MemoryDecayModel.isForgotten(m)) {
                it.remove();
                removed++;
            }
        }
        return removed;
    }

    public ResolvedChunk resolveChunk(
            @Nullable UUID viewerId,
            Identifier dim,
            int chunkX,
            int chunkZ,
            long nowGameSeconds,
            int explorationSkillLevel,
            boolean wrapX
    ) {
        ChunkKnowledge archive = archiveKnowledge(dim, chunkX, chunkZ, wrapX);
        double mem = memoryStrength(viewerId, dim, chunkX, chunkZ, nowGameSeconds, explorationSkillLevel, wrapX);
        boolean nearCairn = isChunkAnchoredByCairn(dim, canonicalChunkX(dim, chunkX, wrapX), chunkZ, wrapX);
        if (archive.sl().code() >= SurveyLevel.SKETCH.code()) {
            return new ResolvedChunk(archive.sl(), archive, mem, nearCairn);
        }
        if (!MemoryDecayModel.isForgotten(mem)) {
            return new ResolvedChunk(SurveyLevel.MEMORY, archive, mem, nearCairn);
        }
        return new ResolvedChunk(archive.sl(), archive, 0.0, nearCairn);
    }

    public void registerCairn(Identifier dim, BlockPos pos) {
        DimensionData dd = dimensions.computeIfAbsent(dim, k -> new DimensionData());
        dd.cairns.add(pos.immutable());
    }

    public void removeCairn(Identifier dim, BlockPos pos) {
        DimensionData dd = dimensions.get(dim);
        if (dd != null) {
            dd.cairns.remove(pos);
        }
    }

    public Set<BlockPos> cairns(Identifier dim) {
        DimensionData dd = dimensions.get(dim);
        if (dd == null) {
            return Set.of();
        }
        synchronized (dd.cairns) {
            return Set.copyOf(dd.cairns);
        }
    }

    public boolean isChunkAnchoredByCairn(Identifier dim, int chunkX, int chunkZ, boolean wrapX) {
        DimensionData dd = dimensions.get(dim);
        if (dd == null || dd.cairns.isEmpty()) {
            return false;
        }
        double centerX = (chunkX << 4) + 8.0;
        double centerZ = (chunkZ << 4) + 8.0;
        synchronized (dd.cairns) {
            for (BlockPos c : dd.cairns) {
                if (PlanetProjection.distance(centerX, centerZ, c.getX() + 0.5, c.getZ() + 0.5, wrapX)
                        <= MemoryDecayModel.CAIRN_ANCHOR_RADIUS_BLOCKS) {
                    return true;
                }
            }
        }
        return false;
    }

    public void putSummary(Identifier dim, int chunkX, int chunkZ, ChunkTerrainSummary summary, boolean wrapX) {
        int cx = canonicalChunkX(dim, chunkX, wrapX);
        DimensionData dd = dimensions.computeIfAbsent(dim, k -> new DimensionData());
        dd.summaries.put(chunkKey(cx, chunkZ), summary);
        int rx = Math.floorDiv(cx, CHUNKS_PER_REGION_SIDE);
        int rz = Math.floorDiv(chunkZ, CHUNKS_PER_REGION_SIDE);
        bumpRegionVersion(dim, dd, rx, rz);
    }

    public ChunkTerrainSummary getOrCreateSummary(Identifier dim, int chunkX, int chunkZ, boolean wrapX) {
        int cx = canonicalChunkX(dim, chunkX, wrapX);
        DimensionData dd = dimensions.computeIfAbsent(dim, k -> new DimensionData());
        return dd.summaries.computeIfAbsent(chunkKey(cx, chunkZ),
                k -> ChunkTerrainSummary.fromPreview(worldSeed, cx, chunkZ, wrapX));
    }

    public int regionTileVersion(Identifier dim, int regionX, int regionZ) {
        DimensionData dd = dimensions.get(dim);
        if (dd == null) {
            return 0;
        }
        return dd.regionVersions.getOrDefault(regionKey(regionX, regionZ), 0);
    }

    public int knownArchiveChunkCount(Identifier dim) {
        DimensionData dd = dimensions.get(dim);
        if (dd == null) {
            return 0;
        }
        int count = 0;
        for (int[] slots : dd.regionKnowledge.values()) {
            synchronized (slots) {
                for (int v : slots) {
                    if ((v & 0x7) != 0) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public List<ChunkEntry> exportArchiveEntries(Identifier dim) {
        DimensionData dd = dimensions.get(dim);
        if (dd == null) {
            return List.of();
        }
        List<ChunkEntry> out = new ArrayList<>();
        for (Map.Entry<Long, int[]> e : dd.regionKnowledge.entrySet()) {
            int rx = (int) (e.getKey() >> 32);
            int rz = (int) e.getKey().longValue();
            int[] slots = e.getValue();
            synchronized (slots) {
                for (int i = 0; i < CHUNKS_PER_REGION; i++) {
                    int packed = slots[i];
                    if ((packed & 0x7) != 0) {
                        int lx = i & 31;
                        int lz = i >>> 5;
                        out.add(new ChunkEntry((rx << 5) + lx, (rz << 5) + lz, ChunkKnowledge.unpack(packed)));
                    }
                }
            }
        }
        return out;
    }

    public List<MemoryEntry> exportPlayerMemories(UUID playerId, Identifier dim) {
        Map<Identifier, Map<Long, Long>> byDim = playerMemories.get(playerId);
        if (byDim == null) {
            return List.of();
        }
        Map<Long, Long> map = byDim.get(dim);
        if (map == null) {
            return List.of();
        }
        List<MemoryEntry> out = new ArrayList<>(map.size());
        for (Map.Entry<Long, Long> e : map.entrySet()) {
            int cx = (int) (e.getKey() >> 32);
            int cz = (int) e.getKey().longValue();
            out.add(new MemoryEntry(cx, cz, e.getValue()));
        }
        return out;
    }

    public void clearAll() {
        dimensions.clear();
        playerMemories.clear();
        standaloneTier = 0;
    }

    private void bumpRegionVersion(Identifier dim, DimensionData dd, int rx, int rz) {
        long key = regionKey(rx, rz);
        int next = dd.regionVersions.merge(key, 1, Integer::sum);
        for (RegionDirtyListener l : listeners) {
            l.onRegionDirty(dim, rx, rz, next);
        }
    }
}
