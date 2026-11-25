package com.nightcity.tcivilization;

import com.nightcity.tcivilization.api.CivCalendar;
import com.nightcity.tcivilization.api.MemoryApi;
import com.nightcity.tcivilization.api.MemoryEntry;
import com.nightcity.tcivilization.api.MemoryQuery;
import com.nightcity.tcivilization.api.RegionApi;
import com.nightcity.tcivilization.api.event.MemoryRecordEvent;
import com.nightcity.tcivilization.api.event.MemoryRecordedEvent;
import com.nightcity.tcivilization.region.TcrRegionFile;
import com.nightcity.tcivilization.sim.TcEventBus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.stream.Stream;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Per-civilization memory log, query engine, annual compaction, and .tcm persistence (03 §5.4).
 */
public final class MemoryStore implements MemoryApi {
    public static final int DEFAULT_MEMORY_CAP = 50_000;
    public static final long COMPACTION_AGE_TICKS = 2L * CivCalendar.YEAR_DAYS * CivCalendar.DAY_TICKS;

    private final Map<UUID, List<MemoryEntry>> byCiv = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicLong> nextIds = new ConcurrentHashMap<>();
    private final TcEventBus eventBus;
    private final LongSupplier tickSupplier;
    private volatile int memoryCap = DEFAULT_MEMORY_CAP;

    public MemoryStore(TcEventBus eventBus, LongSupplier tickSupplier) {
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        this.tickSupplier = Objects.requireNonNull(tickSupplier, "tickSupplier");
    }

    public void setMemoryCap(int cap) {
        this.memoryCap = Math.max(10, cap);
    }

    public int memoryCap() {
        return memoryCap;
    }

    @Override
    @Nullable
    public synchronized MemoryEntry record(
            UUID civ,
            Identifier type,
            int importance,
            @Nullable GlobalPos pos,
            List<UUID> actors,
            CompoundTag payload
    ) {
        Objects.requireNonNull(civ, "civ");
        Objects.requireNonNull(type, "type");
        CompoundTag safePayload = payload == null ? new CompoundTag() : payload.copy();
        try {
            int bytes = TcrRegionFile.serializedTagSize(safePayload);
            if (bytes > MemoryEntry.MAX_PAYLOAD_BYTES) {
                throw new IllegalArgumentException("MemoryEntry payload exceeds 2 KiB limit: " + bytes + " bytes");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed measuring MemoryEntry payload", ex);
        }

        OptionalLong regionKey = OptionalLong.empty();
        if (pos != null) {
            int rx = RegionCoordinates.regionX(pos.pos().getX());
            int rz = RegionCoordinates.regionZ(pos.pos().getZ());
            regionKey = OptionalLong.of(RegionApi.regionKey(rx, rz));
        }

        long id = nextIds.computeIfAbsent(civ, k -> new AtomicLong(0L)).incrementAndGet();
        MemoryEntry entry = new MemoryEntry(
                id,
                tickSupplier.getAsLong(),
                type,
                civ,
                regionKey,
                Optional.ofNullable(pos),
                actors,
                importance,
                safePayload
        );

        MemoryRecordEvent.Pre pre = eventBus.postImmediate(new MemoryRecordEvent.Pre(entry));
        if (pre.isCanceled()) {
            return null;
        }

        List<MemoryEntry> list = byCiv.computeIfAbsent(civ, k -> new ArrayList<>());
        list.add(entry);
        enforceCap(list);
        eventBus.enqueue(new MemoryRecordedEvent(entry));
        return entry;
    }

    @Override
    public synchronized Stream<MemoryEntry> query(UUID civ, MemoryQuery query) {
        Objects.requireNonNull(civ, "civ");
        Objects.requireNonNull(query, "query");
        List<MemoryEntry> list = byCiv.getOrDefault(civ, List.of());
        return list.stream()
                .filter(e -> e.importance() >= query.minImportance())
                .filter(e -> query.type().map(t -> t.equals(e.type())).orElse(true))
                .filter(e -> query.region().isEmpty() || (e.region().isPresent() && e.region().getAsLong() == query.region().getAsLong()))
                .sorted(Comparator.comparingLong(MemoryEntry::tick).thenComparingLong(MemoryEntry::id).reversed())
                .skip(query.offset())
                .limit(query.limit());
    }

    public synchronized int count(UUID civ) {
        return byCiv.getOrDefault(civ, List.of()).size();
    }

    public synchronized void compactAnnual(long currentTick) {
        for (Map.Entry<UUID, List<MemoryEntry>> civEntry : byCiv.entrySet()) {
            UUID civ = civEntry.getKey();
            List<MemoryEntry> entries = civEntry.getValue();
            record GroupKey(Identifier type, OptionalLong region, int year, int season) {}
            Map<GroupKey, List<MemoryEntry>> buckets = new LinkedHashMap<>();
            List<MemoryEntry> kept = new ArrayList<>();

            for (MemoryEntry e : entries) {
                if (e.importance() == 1 && (currentTick - e.tick()) > COMPACTION_AGE_TICKS) {
                    CivCalendar.CalendarDate date = CivCalendar.fromDayTime(e.tick());
                    GroupKey gk = new GroupKey(e.type(), e.region(), date.year(), date.seasonIndex());
                    buckets.computeIfAbsent(gk, k -> new ArrayList<>()).add(e);
                } else {
                    kept.add(e);
                }
            }
            for (Map.Entry<GroupKey, List<MemoryEntry>> bucket : buckets.entrySet()) {
                List<MemoryEntry> grouped = bucket.getValue();
                if (grouped.size() == 1) {
                    kept.add(grouped.getFirst());
                } else {
                    MemoryEntry last = grouped.getLast();
                    CompoundTag summaryPayload = new CompoundTag();
                    summaryPayload.putInt("count", grouped.size());
                    long summaryId = nextIds.computeIfAbsent(civ, k -> new AtomicLong(0L)).incrementAndGet();
                    kept.add(new MemoryEntry(
                            summaryId, last.tick(), last.type(), civ, last.region(),
                            Optional.empty(), List.of(), 2, summaryPayload
                    ));
                }
            }
            kept.sort(Comparator.comparingLong(MemoryEntry::tick).thenComparingLong(MemoryEntry::id));
            enforceCap(kept);
            entries.clear();
            entries.addAll(kept);
        }
    }

    private void enforceCap(List<MemoryEntry> list) {
        if (list.size() <= memoryCap) {
            return;
        }
        int target = Math.max(1, (int) Math.floor(memoryCap * 0.90));
        Iterator<MemoryEntry> it = list.iterator();
        while (list.size() > target && it.hasNext()) {
            MemoryEntry candidate = it.next();
            if (candidate.importance() <= 2) {
                it.remove();
            }
        }
        if (list.size() > memoryCap) {
            Iterator<MemoryEntry> it3 = list.iterator();
            while (list.size() > target && it3.hasNext()) {
                MemoryEntry candidate = it3.next();
                if (candidate.importance() < 4) {
                    it3.remove();
                }
            }
        }
    }

    public synchronized void saveToDirectory(Path worldRoot) throws IOException {
        Path dir = worldRoot.resolve("tc").resolve("memory");
        Files.createDirectories(dir);
        for (Map.Entry<UUID, List<MemoryEntry>> entry : byCiv.entrySet()) {
            CompoundTag root = new CompoundTag();
            root.putInt("formatVersion", 1);
            ListTag items = new ListTag();
            for (MemoryEntry mem : entry.getValue()) {
                Tag encoded = MemoryEntry.CODEC.encodeStart(NbtOps.INSTANCE, mem).getOrThrow();
                items.add(encoded);
            }
            root.put("entries", items);
            Path target = dir.resolve(entry.getKey() + ".tcm");
            NbtIo.writeCompressed(root, target);
        }
    }

    public synchronized void loadCivFromDirectory(Path worldRoot, UUID civ) throws IOException {
        Path target = worldRoot.resolve("tc").resolve("memory").resolve(civ + ".tcm");
        if (!Files.exists(target)) {
            return;
        }
        CompoundTag root = NbtIo.readCompressed(target, NbtAccounter.unlimitedHeap());
        ListTag listTag = root.getListOrEmpty("entries");
        List<MemoryEntry> loaded = new ArrayList<>();
        long maxId = 0L;
        for (Tag element : listTag) {
            MemoryEntry.CODEC.parse(NbtOps.INSTANCE, element).result().ifPresent(e -> loaded.add(e));
        }
        for (MemoryEntry e : loaded) {
            maxId = Math.max(maxId, e.id());
        }
        byCiv.put(civ, loaded);
        nextIds.put(civ, new AtomicLong(maxId));
    }
}
