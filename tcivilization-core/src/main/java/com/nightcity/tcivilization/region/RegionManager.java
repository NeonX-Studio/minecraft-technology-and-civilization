package com.nightcity.tcivilization.region;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.nightcity.tcivilization.api.RegionApi;
import com.nightcity.tcivilization.api.RegionCell;
import com.nightcity.tcivilization.api.RegionGridRegistry;
import com.nightcity.tcivilization.api.RegionGridSpec;
import com.nightcity.tcivilization.api.RegionHandle;
import com.nightcity.tcivilization.api.RegionLayer;
import com.nightcity.tcivilization.api.TcThreads;
import com.nightcity.tcivilization.sim.TcEventBus;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.LongSupplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

/**
 * Server RegionApi implementation backed by an LRU-256 pin-aware cache, .tcr binary files,
 * and the dedicated single-threaded TC-RegionIO executor (03 §5.2.3-5.2.5).
 */
public final class RegionManager implements RegionApi, AutoCloseable {
    public static final int DEFAULT_CACHE_CAPACITY = 256;
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Path worldRoot;
    private final int dataVersion;
    private final int cacheCapacity;
    private final RegionGridRegistry gridRegistry;
    private final RegionLayerRegistry layerRegistry;
    private final RegionPressureTracker pressureTracker;
    private final LongSupplier tickSupplier;
    private final ExecutorService ioExecutor;
    private final LinkedHashMap<RegionCell, ManagedRegion> lruCache;
    private final List<String> systemAlerts = new CopyOnWriteArrayList<>();

    public RegionManager(
            Path worldRoot,
            int dataVersion,
            int cacheCapacity,
            RegionGridRegistry gridRegistry,
            RegionLayerRegistry layerRegistry,
            TcEventBus eventBus,
            LongSupplier tickSupplier
    ) {
        this.worldRoot = Objects.requireNonNull(worldRoot, "worldRoot");
        this.dataVersion = dataVersion;
        this.cacheCapacity = Math.max(1, cacheCapacity);
        this.gridRegistry = Objects.requireNonNull(gridRegistry, "gridRegistry");
        this.layerRegistry = Objects.requireNonNull(layerRegistry, "layerRegistry");
        this.pressureTracker = new RegionPressureTracker(Objects.requireNonNull(eventBus, "eventBus"));
        this.tickSupplier = Objects.requireNonNull(tickSupplier, "tickSupplier");
        this.ioExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread t = new Thread(runnable, "TC-RegionIO");
            t.setDaemon(true);
            return t;
        });
        this.lruCache = new LinkedHashMap<>(64, 0.75f, true);
    }

    public RegionPressureTracker pressureTracker() {
        return pressureTracker;
    }

    public RegionGridRegistry gridRegistry() {
        return gridRegistry;
    }

    public List<String> systemAlerts() {
        return List.copyOf(systemAlerts);
    }

    public synchronized int cachedCount() {
        return lruCache.size();
    }

    public synchronized boolean isCached(RegionCell cell) {
        return lruCache.containsKey(cell);
    }

    @Override
    public Optional<RegionHandle> at(ResourceKey<Level> dimension, BlockPos pos) {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(pos, "pos");
        return atBlock(dimension.identifier().toString(), pos.getX(), pos.getZ());
    }

    public Optional<RegionHandle> atBlock(String dimensionId, int blockX, int blockZ) {
        return gridRegistry.atBlock(dimensionId, blockX, blockZ).map(this::loadOrGet);
    }

    @Override
    public Optional<RegionHandle> get(ResourceKey<Level> dimension, int rx, int rz) {
        Objects.requireNonNull(dimension, "dimension");
        return getByDimensionId(dimension.identifier().toString(), rx, rz);
    }

    public Optional<RegionHandle> getByDimensionId(String dimensionId, int rx, int rz) {
        Optional<RegionGridSpec> specOpt = gridRegistry.get(dimensionId);
        if (specOpt.isEmpty()) {
            return Optional.empty();
        }
        RegionGridSpec spec = specOpt.get();
        int normalizedRx = spec.wrapX()
                ? Math.floorMod(rx + spec.width() / 2, spec.width()) - spec.width() / 2
                : rx;
        if (!spec.contains(normalizedRx, rz)) {
            return Optional.empty();
        }
        return Optional.of(loadOrGet(new RegionCell(dimensionId, normalizedRx, rz)));
    }

    @Override
    public <T> T layer(RegionHandle region, RegionLayer<T> layer) {
        return region.layer(layer);
    }

    @Override
    public <T> void update(RegionHandle region, RegionLayer<T> layer, UnaryOperator<T> operator) {
        TcThreads.assertMain();
        region.update(layer, operator);
    }

    @Override
    public synchronized void pin(RegionHandle region, Object owner) {
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(owner, "owner");
        ManagedRegion managed = loadOrGetInternal(region.cell());
        managed.pins.add(owner);
    }

    @Override
    public synchronized void unpin(RegionHandle region, Object owner) {
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(owner, "owner");
        ManagedRegion managed = lruCache.get(region.cell());
        if (managed != null) {
            managed.pins.remove(owner);
            evictIfNeeded();
        }
    }

    @Override
    public float pressure(RegionHandle region) {
        return region.layer(CivRegionLayer.INSTANCE).pressure();
    }

    private synchronized RegionHandle loadOrGet(RegionCell cell) {
        return loadOrGetInternal(cell);
    }

    private ManagedRegion loadOrGetInternal(RegionCell cell) {
        ManagedRegion existing = lruCache.get(cell);
        if (existing != null) {
            return existing;
        }
        try {
            awaitPendingIo();
        } catch (RuntimeException ignored) {
        }
        Path file = TcrRegionFile.resolvePath(worldRoot, cell.dimension(), cell.fileX(), cell.fileZ());
        TcrRegionFile.RegionBlob blob = TcrRegionFile.readRegion(
                file, cell, tickSupplier.getAsLong(),
                (quarantined, reason) -> recordAlert("region_corrupt_isolated:" + quarantined.getFileName() + ":" + reason)
        );
        ManagedRegion loaded = new ManagedRegion(cell, blob.layers());
        lruCache.put(cell, loaded);
        evictIfNeeded();
        return loaded;
    }

    private void evictIfNeeded() {
        if (lruCache.size() <= cacheCapacity) {
            return;
        }
        List<ManagedRegion> evictedDirty = new ArrayList<>();
        Iterator<Map.Entry<RegionCell, ManagedRegion>> it = lruCache.entrySet().iterator();
        while (lruCache.size() > cacheCapacity && it.hasNext()) {
            ManagedRegion candidate = it.next().getValue();
            if (!candidate.pins.isEmpty()) {
                continue;
            }
            it.remove();
            if (candidate.isDirty()) {
                evictedDirty.add(candidate);
            }
        }
        if (!evictedDirty.isEmpty()) {
            scheduleWriteBatch(evictedDirty);
        }
    }

    public synchronized CompletableFuture<Void> flushDirtyAsync() {
        List<ManagedRegion> dirty = new ArrayList<>();
        for (ManagedRegion region : lruCache.values()) {
            if (region.isDirty()) {
                dirty.add(region);
            }
        }
        if (dirty.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return scheduleWriteBatch(dirty);
    }

    public void flushAllSync() {
        CompletableFuture<Void> future = flushDirtyAsync();
        try {
            future.get();
            awaitPendingIo();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while flushing regions", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("Failed to flush region files", e.getCause());
        }
    }

    private void awaitPendingIo() {
        try {
            CompletableFuture.runAsync(() -> {}, ioExecutor).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
        }
    }

    private CompletableFuture<Void> scheduleWriteBatch(List<ManagedRegion> dirtyRegions) {
        record FileKey(String dimension, int fx, int fz) {}
        Map<FileKey, Map<Integer, TcrRegionFile.RegionBlob>> grouped = new LinkedHashMap<>();
        for (ManagedRegion region : dirtyRegions) {
            TcrRegionFile.RegionBlob snapshot = region.snapshotAndClearDirty();
            FileKey key = new FileKey(region.cell.dimension(), region.cell.fileX(), region.cell.fileZ());
            int slot = TcrRegionFile.slotIndex(region.cell.x(), region.cell.z());
            grouped.computeIfAbsent(key, k -> new LinkedHashMap<>()).put(slot, snapshot);
        }
        long tick = tickSupplier.getAsLong();
        return CompletableFuture.runAsync(() -> {
            if (!"TC-RegionIO".equals(Thread.currentThread().getName())) {
                LOGGER.warn("Unexpected region IO thread name: {}", Thread.currentThread().getName());
            }
            for (Map.Entry<FileKey, Map<Integer, TcrRegionFile.RegionBlob>> entry : grouped.entrySet()) {
                FileKey fk = entry.getKey();
                Path file = TcrRegionFile.resolvePath(worldRoot, fk.dimension(), fk.fx(), fk.fz());
                try {
                    TcrRegionFile.writeRegions(
                            file, dataVersion, entry.getValue(), tick,
                            (quarantine, reason) -> recordAlert("region_corrupt_isolated:" + quarantine.getFileName() + ":" + reason)
                    );
                } catch (IOException ex) {
                    recordAlert("region_write_failed:" + file.getFileName() + ":" + ex.getMessage());
                    throw new IllegalStateException("Failed writing " + file, ex);
                }
            }
        }, ioExecutor);
    }

    private void recordAlert(String alert) {
        systemAlerts.add(alert);
        LOGGER.warn("TC Region Alert: {}", alert);
    }

    @Override
    public void close() {
        try {
            flushAllSync();
        } finally {
            ioExecutor.shutdown();
        }
    }

    private final class ManagedRegion implements RegionHandle {
        private final RegionCell cell;
        private final Map<String, Tag> rawLayers;
        private final Map<Identifier, Object> decodedLayers = new HashMap<>();
        private final Set<Identifier> dirtyLayers = new LinkedHashSet<>();
        private final Set<Object> pins = new HashSet<>();

        private ManagedRegion(RegionCell cell, Map<String, Tag> initialRawLayers) {
            this.cell = cell;
            this.rawLayers = new LinkedHashMap<>(initialRawLayers);
        }

        @Override
        public RegionCell cell() {
            return cell;
        }

        @Override
        @SuppressWarnings("unchecked")
        public synchronized <T> T layer(RegionLayer<T> layer) {
            Objects.requireNonNull(layer, "layer");
            Identifier id = layer.id();
            Object cached = decodedLayers.get(id);
            if (cached != null) {
                return (T) cached;
            }
            Tag raw = rawLayers.get(id.toString());
            if (raw != null) {
                DataResult<T> parsed = layer.codec().parse(NbtOps.INSTANCE, raw);
                Optional<T> result = parsed.resultOrPartial(err ->
                        LOGGER.warn("Failed decoding layer {} in {}: {}", id, cell, err));
                if (result.isPresent()) {
                    T val = result.get();
                    decodedLayers.put(id, val);
                    return val;
                }
            }
            T fallback = layer.createDefault(cell);
            decodedLayers.put(id, fallback);
            return fallback;
        }

        @Override
        public synchronized <T> void setLayer(RegionLayer<T> layer, T value) {
            Objects.requireNonNull(layer, "layer");
            Objects.requireNonNull(value, "value");
            Tag encoded = encodeAndValidateSize(layer, value);
            decodedLayers.put(layer.id(), value);
            rawLayers.put(layer.id().toString(), encoded);
            dirtyLayers.add(layer.id());
        }

        @Override
        public synchronized <T> void update(RegionLayer<T> layer, UnaryOperator<T> operator) {
            Objects.requireNonNull(layer, "layer");
            Objects.requireNonNull(operator, "operator");
            T current = layer(layer);
            T updated = Objects.requireNonNull(operator.apply(current), "updated layer value");
            setLayer(layer, updated);
        }

        @Override
        @SuppressWarnings("unchecked")
        public synchronized void markDirty(Identifier layerId) {
            Objects.requireNonNull(layerId, "layerId");
            RegionLayer<Object> registered = (RegionLayer<Object>) layerRegistry.get(layerId).orElse(null);
            if (registered != null && decodedLayers.containsKey(layerId)) {
                Object val = decodedLayers.get(layerId);
                Tag encoded = encodeAndValidateSize(registered, val);
                rawLayers.put(layerId.toString(), encoded);
            }
            dirtyLayers.add(layerId);
        }

        @Override
        public synchronized boolean isDirty() {
            return !dirtyLayers.isEmpty();
        }

        @Override
        public synchronized Set<Identifier> dirtyLayers() {
            return Collections.unmodifiableSet(new LinkedHashSet<>(dirtyLayers));
        }

        private synchronized TcrRegionFile.RegionBlob snapshotAndClearDirty() {
            Map<String, Tag> snapshot = new LinkedHashMap<>();
            for (Map.Entry<String, Tag> entry : rawLayers.entrySet()) {
                snapshot.put(entry.getKey(), entry.getValue().copy());
            }
            dirtyLayers.clear();
            return new TcrRegionFile.RegionBlob(TcrRegionFile.FORMAT_VERSION, snapshot);
        }

        private <T> Tag encodeAndValidateSize(RegionLayer<T> layer, T value) {
            Tag encoded = layer.codec().encodeStart(NbtOps.INSTANCE, value)
                    .getOrThrow(err -> new IllegalArgumentException("Failed encoding layer " + layer.id() + ": " + err));
            try {
                int size = TcrRegionFile.serializedTagSize(encoded);
                if (size > RegionLayer.MAX_SERIALIZED_BYTES) {
                    recordAlert("region_layer_oversize:" + layer.id() + ":" + size);
                    throw new IllegalArgumentException("Region layer " + layer.id() + " exceeds 16 KiB limit (" + size + " bytes)");
                }
            } catch (IOException ex) {
                throw new IllegalStateException("Failed measuring serialized layer size for " + layer.id(), ex);
            }
            return encoded;
        }
    }
}
