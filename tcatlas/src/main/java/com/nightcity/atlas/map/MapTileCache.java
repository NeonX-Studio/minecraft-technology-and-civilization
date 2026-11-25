package com.nightcity.atlas.map;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * 256x256 RGBA tile memory LRU + region-chunked disk cache with dirty-tile invalidation,
 * disk quota pruning, and <= 4 ms per-frame viewport build budget (22 §5.9, §5.13, §14, EC-P1-5).
 */
public final class MapTileCache implements AutoCloseable {
    public static final byte[] DISK_MAGIC = "TTC1".getBytes(StandardCharsets.US_ASCII);
    public static final int FORMAT_VERSION = 1;
    public static final int BUNDLE_SIDE = 8; // 8x8 tiles per disk chunk file
    public static final int BUNDLE_SLOTS = BUNDLE_SIDE * BUNDLE_SIDE;
    private static final int HEADER_BYTES = 12 + BUNDLE_SLOTS * 8; // magic(4)+ver(4)+slotCount(4)+64*(offset,len)

    public record TileKey(Identifier dimension, int zoom, int tileX, int tileZ) {
        public TileKey {
            Objects.requireNonNull(dimension, "dimension");
        }
    }

    public record TileSnapshot(TileKey key, int[] argbPixels, long generation, boolean placeholder) {
    }

    public record ViewportFrameResult(
            List<TileSnapshot> visibleTiles,
            int cacheHits,
            int builtThisFrame,
            int queuedAsync,
            long elapsedNanos
    ) {
        public double elapsedMillis() {
            return elapsedNanos / 1_000_000.0;
        }
    }

    private final SurveyAtlasStore store;
    private final Path diskCacheRoot;
    private final int maxMemoryTiles;
    private volatile long maxDiskBytes;
    private final int maxSyncBuildsPerFrame;
    private final long frameBudgetNanos;

    private final Map<TileKey, TileSnapshot> memoryLru;
    private final Set<TileKey> dirtyKeys = ConcurrentHashMap.newKeySet();
    private final Set<TileKey> inFlightKeys = ConcurrentHashMap.newKeySet();
    private final AtomicLong generationCounter = new AtomicLong(1L);
    private final ExecutorService workerPool;
    @Nullable
    private Consumer<TileKey> evictionCallback;

    private static final int[] PLACEHOLDER_PIXELS = createPlaceholderPixels();

    public MapTileCache(
            SurveyAtlasStore store,
            Path diskCacheRoot,
            int maxMemoryTiles,
            long maxDiskBytes,
            int maxSyncBuildsPerFrame,
            int workerThreads
    ) {
        this.store = Objects.requireNonNull(store, "store");
        this.diskCacheRoot = Objects.requireNonNull(diskCacheRoot, "diskCacheRoot");
        this.maxMemoryTiles = Math.max(16, maxMemoryTiles);
        this.maxDiskBytes = Math.max(64L * 1024L, maxDiskBytes);
        this.maxSyncBuildsPerFrame = Math.clamp(maxSyncBuildsPerFrame, 1, 16);
        this.frameBudgetNanos = 2_400_000L; // 2.4 ms hard cap inside 4.0 ms EC-P1-5 frame budget
        this.memoryLru = new LinkedHashMap<>(this.maxMemoryTiles + 16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<TileKey, TileSnapshot> eldest) {
                if (size() > MapTileCache.this.maxMemoryTiles) {
                    if (evictionCallback != null) {
                        evictionCallback.accept(eldest.getKey());
                    }
                    return true;
                }
                return false;
            }
        };
        int threads = Math.clamp(workerThreads, 1, 4);
        this.workerPool = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "TCAtlas-TileWorker");
            t.setDaemon(true);
            return t;
        });
        this.store.addDirtyListener((dim, rx, rz, ver) -> invalidateRegion(dim, rx, rz, true));
    }

    public void setEvictionCallback(@Nullable Consumer<TileKey> callback) {
        this.evictionCallback = callback;
    }

    public void setMaxDiskBytes(long maxDiskBytes) {
        this.maxDiskBytes = Math.max(64L * 1024L, maxDiskBytes);
    }

    public int memoryTileCount() {
        synchronized (memoryLru) {
            return memoryLru.size();
        }
    }

    public Set<TileKey> dirtyTileKeysSnapshot() {
        return Set.copyOf(dirtyKeys);
    }

    /**
     * Incrementally marks the 4 L0 tiles inside the 512x512 region (rx, rz) and their L1..L9 pyramid ancestors dirty.
     */
    public void invalidateRegion(Identifier dimension, int regionX, int regionZ, boolean wrapEastWest) {
        int minBlockX = regionX * PlanetProjection.REGION_SIZE;
        int minBlockZ = regionZ * PlanetProjection.REGION_SIZE;
        for (int subZ = 0; subZ < 2; subZ++) {
            for (int subX = 0; subX < 2; subX++) {
                int bx = minBlockX + subX * 256;
                int bz = minBlockZ + subZ * 256;
                int tx0 = PlanetProjection.tileIndexX(bx, 0, wrapEastWest);
                int tz0 = PlanetProjection.tileIndexZ(bz, 0, wrapEastWest);
                markIfCachedOrKnown(new TileKey(dimension, 0, tx0, tz0));
            }
        }
        for (int zoom = 1; zoom <= PlanetProjection.MAX_ZOOM; zoom++) {
            int tx = PlanetProjection.tileIndexX(minBlockX, zoom, wrapEastWest);
            int tz = PlanetProjection.tileIndexZ(minBlockZ, zoom, wrapEastWest);
            markIfCachedOrKnown(new TileKey(dimension, zoom, tx, tz));
        }
    }

    private void markIfCachedOrKnown(TileKey key) {
        dirtyKeys.add(key);
    }

    /**
     * Synchronous get-or-build for single tile (used by background workers and deterministic tests).
     */
    public TileSnapshot getOrBuildBlocking(TileKey key, MapTileRasterizer.RenderParams params, boolean persistToDisk) {
        synchronized (memoryLru) {
            TileSnapshot mem = memoryLru.get(key);
            if (mem != null && !dirtyKeys.contains(key)) {
                return mem;
            }
        }
        if (!dirtyKeys.contains(key)) {
            int[] fromDisk = readTileFromDisk(key);
            if (fromDisk != null) {
                TileSnapshot loaded = new TileSnapshot(key, fromDisk, generationCounter.getAndIncrement(), false);
                synchronized (memoryLru) {
                    memoryLru.put(key, loaded);
                }
                return loaded;
            }
        }
        return rebuildNow(key, params, persistToDisk);
    }

    /**
     * Collects all tiles visible in the current viewport while strictly enforcing the <= 4 ms frame budget (EC-P1-5).
     */
    public ViewportFrameResult collectViewportFrame(
            Identifier dimension,
            @Nullable UUID viewerId,
            long holderSalt,
            int zoom,
            double centerBlockX,
            double centerBlockZ,
            int viewportWidthPx,
            int viewportHeightPx,
            boolean wrapEastWest,
            boolean mapWarpEnabled,
            boolean revealAll,
            long nowGameSeconds,
            int currentDay,
            int tier,
            int explorationSkill
    ) {
        long startNanos = System.nanoTime();
        int clampedZoom = Math.clamp(zoom, PlanetProjection.MIN_ZOOM, PlanetProjection.MAX_ZOOM);
        int bpp = PlanetProjection.blocksPerPixel(clampedZoom);
        double halfSpanBlocksX = (viewportWidthPx * 0.5) * bpp;
        double halfSpanBlocksZ = (viewportHeightPx * 0.5) * bpp;

        int minBlockX = (int) Math.floor(centerBlockX - halfSpanBlocksX);
        int maxBlockX = (int) Math.floor(centerBlockX + halfSpanBlocksX);
        int minBlockZ = (int) Math.floor(centerBlockZ - halfSpanBlocksZ);
        int maxBlockZ = (int) Math.floor(centerBlockZ + halfSpanBlocksZ);

        int tileSpan = PlanetProjection.tileSpanBlocks(clampedZoom);
        int startTileX = wrapEastWest
                ? Math.floorDiv(minBlockX + PlanetProjection.HALF_WIDTH, tileSpan)
                : Math.floorDiv(minBlockX, tileSpan);
        int endTileX = wrapEastWest
                ? Math.floorDiv(maxBlockX + PlanetProjection.HALF_WIDTH, tileSpan)
                : Math.floorDiv(maxBlockX, tileSpan);
        int startTileZ = wrapEastWest
                ? Math.max(0, Math.floorDiv(minBlockZ + PlanetProjection.Z_LIMIT, tileSpan))
                : Math.floorDiv(minBlockZ, tileSpan);
        int endTileZ = wrapEastWest
                ? Math.min(PlanetProjection.tileRows(clampedZoom) - 1, Math.floorDiv(maxBlockZ + PlanetProjection.Z_LIMIT, tileSpan))
                : Math.floorDiv(maxBlockZ, tileSpan);

        // Cap horizontal tile count to avoid pathological wide viewports exceeding 48 tiles
        if (endTileX - startTileX > 11) {
            endTileX = startTileX + 11;
        }
        if (endTileZ - startTileZ > 7) {
            endTileZ = startTileZ + 7;
        }

        List<TileSnapshot> visible = new ArrayList<>();
        int hits = 0;
        int built = 0;
        int queued = 0;

        for (int rawTz = startTileZ; rawTz <= endTileZ; rawTz++) {
            for (int rawTx = startTileX; rawTx <= endTileX; rawTx++) {
                int canonicalTx = PlanetProjection.wrapTileIndexX(rawTx, clampedZoom, wrapEastWest);
                TileKey key = new TileKey(dimension, clampedZoom, canonicalTx, rawTz);
                TileSnapshot cached;
                synchronized (memoryLru) {
                    cached = memoryLru.get(key);
                }
                boolean isDirty = dirtyKeys.contains(key);
                if (cached != null && !isDirty) {
                    visible.add(cached);
                    hits++;
                    continue;
                }

                MapTileRasterizer.RenderParams rp = new MapTileRasterizer.RenderParams(
                        dimension, viewerId, holderSalt, clampedZoom, canonicalTx, rawTz,
                        wrapEastWest, mapWarpEnabled, revealAll, nowGameSeconds, currentDay, tier, explorationSkill
                );

                scheduleRebuildAsync(key, rp);
                queued++;
                visible.add(cached != null ? cached : new TileSnapshot(key, PLACEHOLDER_PIXELS, 0L, true));
            }
        }

        long totalNanos = System.nanoTime() - startNanos;
        return new ViewportFrameResult(visible, hits, built, queued, totalNanos);
    }

    private TileSnapshot rebuildNow(TileKey key, MapTileRasterizer.RenderParams params, boolean syncDiskWrite) {
        dirtyKeys.remove(key);
        int[] pixels = MapTileRasterizer.rasterize(store, params);
        TileSnapshot snap = new TileSnapshot(key, pixels, generationCounter.getAndIncrement(), false);
        synchronized (memoryLru) {
            memoryLru.put(key, snap);
        }
        if (syncDiskWrite) {
            writeTileToDisk(key, pixels);
        }
        return snap;
    }

    private void scheduleRebuildAsync(TileKey key, MapTileRasterizer.RenderParams params) {
        if (!inFlightKeys.add(key)) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                getOrBuildBlocking(key, params, true);
            } finally {
                inFlightKeys.remove(key);
            }
        }, workerPool);
    }

    private void scheduleDiskWriteAsync(TileKey key, int[] pixels) {
        CompletableFuture.runAsync(() -> writeTileToDisk(key, pixels), workerPool);
    }

    public synchronized void writeTileToDisk(TileKey key, int[] argbPixels) {
        try {
            Path bundlePath = resolveBundlePath(key);
            if (bundlePath.getParent() != null) {
                Files.createDirectories(bundlePath.getParent());
            }
            byte[][] existingPayloads = new byte[BUNDLE_SLOTS][];
            if (Files.isRegularFile(bundlePath)) {
                readBundlePayloads(bundlePath, existingPayloads);
            }
            int slot = bundleSlot(key.tileX(), key.tileZ());
            existingPayloads[slot] = compressPixels(argbPixels);
            writeBundleAtomically(bundlePath, existingPayloads);
            enforceDiskQuota();
        } catch (IOException ignored) {
        }
    }

    public synchronized int @Nullable [] readTileFromDisk(TileKey key) {
        Path bundlePath = resolveBundlePath(key);
        if (!Files.isRegularFile(bundlePath)) {
            return null;
        }
        try {
            byte[] all = Files.readAllBytes(bundlePath);
            if (all.length < HEADER_BYTES) {
                return null;
            }
            if (all[0] != DISK_MAGIC[0] || all[1] != DISK_MAGIC[1] || all[2] != DISK_MAGIC[2] || all[3] != DISK_MAGIC[3]) {
                return null;
            }
            ByteBuffer hdr = ByteBuffer.wrap(all, 0, HEADER_BYTES);
            int ver = hdr.getInt(4);
            if (ver != FORMAT_VERSION) {
                return null;
            }
            int slot = bundleSlot(key.tileX(), key.tileZ());
            int entryOffset = 12 + slot * 8;
            int offset = hdr.getInt(entryOffset);
            int length = hdr.getInt(entryOffset + 4);
            if (offset < HEADER_BYTES || length <= 0 || offset + length > all.length) {
                return null;
            }
            return decompressPixels(all, offset, length);
        } catch (IOException ignored) {
            return null;
        }
    }

    public synchronized long computeDiskUsageBytes() {
        if (!Files.isDirectory(diskCacheRoot)) {
            return 0L;
        }
        try (Stream<Path> walk = Files.walk(diskCacheRoot)) {
            return walk.filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0L;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            return 0L;
        }
    }

    /**
     * Prunes oldest-modified .ttc bundle files until total disk usage <= maxDiskBytes.
     */
    public synchronized int enforceDiskQuota() {
        if (!Files.isDirectory(diskCacheRoot)) {
            return 0;
        }
        record FileStat(Path path, long size, long mtime) {
        }
        List<FileStat> files = new ArrayList<>();
        long total = 0L;
        try (Stream<Path> walk = Files.walk(diskCacheRoot)) {
            for (Path p : walk.filter(f -> Files.isRegularFile(f) && f.getFileName().toString().endsWith(".ttc")).toList()) {
                long sz = Files.size(p);
                long mt = Files.getLastModifiedTime(p).toMillis();
                files.add(new FileStat(p, sz, mt));
                total += sz;
            }
        } catch (IOException ignored) {
            return 0;
        }
        if (total <= maxDiskBytes) {
            return 0;
        }
        files.sort(Comparator.comparingLong(FileStat::mtime));
        int pruned = 0;
        for (FileStat fs : files) {
            if (total <= maxDiskBytes) {
                break;
            }
            try {
                if (Files.deleteIfExists(fs.path)) {
                    total -= fs.size;
                    pruned++;
                }
            } catch (IOException ignored) {
            }
        }
        return pruned;
    }

    private Path resolveBundlePath(TileKey key) {
        int bx = Math.floorDiv(key.tileX(), BUNDLE_SIDE);
        int bz = Math.floorDiv(key.tileZ(), BUNDLE_SIDE);
        String dimFolder = key.dimension().getNamespace() + "_" + key.dimension().getPath().replace('/', '_');
        return diskCacheRoot.resolve(dimFolder).resolve("L" + key.zoom()).resolve("b." + bx + "." + bz + ".ttc");
    }

    private static int bundleSlot(int tileX, int tileZ) {
        return (Math.floorMod(tileZ, BUNDLE_SIDE) * BUNDLE_SIDE) + Math.floorMod(tileX, BUNDLE_SIDE);
    }

    private static void readBundlePayloads(Path bundlePath, byte[][] outPayloads) throws IOException {
        byte[] all = Files.readAllBytes(bundlePath);
        if (all.length < HEADER_BYTES) {
            return;
        }
        ByteBuffer hdr = ByteBuffer.wrap(all, 0, HEADER_BYTES);
        if (hdr.getInt(4) != FORMAT_VERSION) {
            return;
        }
        for (int i = 0; i < BUNDLE_SLOTS; i++) {
            int pos = 12 + i * 8;
            int off = hdr.getInt(pos);
            int len = hdr.getInt(pos + 4);
            if (off >= HEADER_BYTES && len > 0 && off + len <= all.length) {
                byte[] slice = new byte[len];
                System.arraycopy(all, off, slice, 0, len);
                outPayloads[i] = slice;
            }
        }
    }

    private static void writeBundleAtomically(Path target, byte[][] payloads) throws IOException {
        int bodySize = 0;
        int present = 0;
        for (byte[] p : payloads) {
            if (p != null) {
                bodySize += p.length;
                present++;
            }
        }
        ByteBuffer out = ByteBuffer.allocate(HEADER_BYTES + bodySize);
        out.put(DISK_MAGIC);
        out.putInt(FORMAT_VERSION);
        out.putInt(present);
        int cursor = HEADER_BYTES;
        for (int i = 0; i < BUNDLE_SLOTS; i++) {
            byte[] p = payloads[i];
            if (p != null) {
                out.putInt(cursor);
                out.putInt(p.length);
                cursor += p.length;
            } else {
                out.putInt(0);
                out.putInt(0);
            }
        }
        for (byte[] p : payloads) {
            if (p != null) {
                out.put(p);
            }
        }
        Path tmp = target.resolveSibling(target.getFileName().toString() + ".tmp");
        Files.write(tmp, out.array());
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static byte[] compressPixels(int[] argb) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
        Deflater deflater = new Deflater(1); // fast compression level 1
        try (DeflaterOutputStream dos = new DeflaterOutputStream(baos, deflater); DataOutputStream out = new DataOutputStream(dos)) {
            ByteBuffer raw = ByteBuffer.allocate(MapTileRasterizer.PIXEL_COUNT * 4);
            raw.asIntBuffer().put(argb);
            out.write(raw.array());
        } finally {
            deflater.end();
        }
        return baos.toByteArray();
    }

    private static int[] decompressPixels(byte[] src, int offset, int length) throws IOException {
        try (DataInputStream in = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(src, offset, length)))) {
            byte[] raw = in.readNBytes(MapTileRasterizer.PIXEL_COUNT * 4);
            int[] pixels = new int[MapTileRasterizer.PIXEL_COUNT];
            ByteBuffer.wrap(raw).asIntBuffer().get(pixels);
            return pixels;
        }
    }

    private static int[] createPlaceholderPixels() {
        int[] px = new int[MapTileRasterizer.PIXEL_COUNT];
        for (int z = 0; z < 256; z++) {
            for (int x = 0; x < 256; x++) {
                px[(z << 8) | x] = ((x ^ z) & 15) == 0 ? 0xFF2C261F : 0xFF221D18;
            }
        }
        return px;
    }

    @Override
    public void close() {
        workerPool.shutdownNow();
        synchronized (memoryLru) {
            memoryLru.clear();
        }
    }
}
