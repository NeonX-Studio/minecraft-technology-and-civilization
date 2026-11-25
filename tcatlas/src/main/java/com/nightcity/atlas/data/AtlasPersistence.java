package com.nightcity.atlas.data;

import com.nightcity.atlas.TAtlas;
import com.nightcity.atlas.api.ChunkKnowledge;
import com.nightcity.atlas.api.MemoryMark;
import com.nightcity.atlas.api.OkabeItoPalette;
import com.nightcity.atlas.api.SurveyLevel;
import com.nightcity.atlas.api.Waypoint;
import com.nightcity.atlas.map.SurveyAtlasStore;
import com.nightcity.atlas.waypoint.WaypointManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Versioned persistence & chained schema migration (formatVersion = 1) for Atlas survey archive,
 * waypoints, and personal fading memories (22 §11.2, 40 §9.1, EC-P1-8).
 */
public final class AtlasPersistence {
    public static final int FORMAT_VERSION = 1;
    public static final String FORMAT_VERSION_KEY = "formatVersion";
    public static final Identifier WAYPOINTS_FILE_ID = Identifier.fromNamespaceAndPath(TAtlas.MOD_ID, "waypoints");
    public static final Identifier SURVEY_FILE_ID = Identifier.fromNamespaceAndPath(TAtlas.MOD_ID, "survey_state");

    @FunctionalInterface
    public interface SchemaStepMigrator {
        CompoundTag migrate(int fromVersion, CompoundTag raw);
    }

    private static final Map<Identifier, NavigableMap<Integer, SchemaStepMigrator>> MIGRATORS = new ConcurrentHashMap<>();

    static {
        registerDefaultMigrators();
    }

    private AtlasPersistence() {
    }

    public static synchronized void registerDefaultMigrators() {
        registerMigrator(WAYPOINTS_FILE_ID, 0, AtlasPersistence::migrateWaypointsV0ToV1);
        registerMigrator(SURVEY_FILE_ID, 0, AtlasPersistence::migrateSurveyV0ToV1);
    }

    public static void registerMigrator(Identifier fileId, int fromVersion, SchemaStepMigrator migrator) {
        Objects.requireNonNull(fileId, "fileId");
        Objects.requireNonNull(migrator, "migrator");
        if (fromVersion < 0) {
            throw new IllegalArgumentException("fromVersion must be >= 0");
        }
        NavigableMap<Integer, SchemaStepMigrator> chain = MIGRATORS.computeIfAbsent(fileId, k -> new TreeMap<>());
        synchronized (chain) {
            chain.put(fromVersion, migrator);
        }
    }

    public static int readFormatVersion(CompoundTag data) {
        if (data.contains(FORMAT_VERSION_KEY)) {
            return data.getIntOr(FORMAT_VERSION_KEY, 0);
        }
        if (data.contains("format_version")) {
            return data.getIntOr("format_version", 0);
        }
        if (data.contains("schemaVersion")) {
            return data.getIntOr("schemaVersion", 0);
        }
        return 0;
    }

    public static CompoundTag migrate(Identifier fileId, int targetVersion, CompoundTag rawData) {
        Objects.requireNonNull(fileId, "fileId");
        Objects.requireNonNull(rawData, "rawData");
        int version = readFormatVersion(rawData);
        if (version > targetVersion) {
            throw new IllegalStateException("Atlas save file " + fileId + " has newer formatVersion (" + version + " > " + targetVersion + ")");
        }
        if (version == targetVersion) {
            CompoundTag copy = rawData.copy();
            copy.putInt(FORMAT_VERSION_KEY, targetVersion);
            return copy;
        }
        CompoundTag working = rawData.copy();
        NavigableMap<Integer, SchemaStepMigrator> chain = MIGRATORS.get(fileId);
        for (int v = version; v < targetVersion; v++) {
            SchemaStepMigrator step = null;
            if (chain != null) {
                synchronized (chain) {
                    step = chain.get(v);
                }
            }
            if (step != null) {
                working = step.migrate(v, working.copy());
            }
            working.putInt(FORMAT_VERSION_KEY, v + 1);
        }
        return working;
    }

    public static CompoundTag migrateWaypointsV0ToV1(int fromVersion, CompoundTag v0) {
        CompoundTag out = new CompoundTag();
        out.putInt(FORMAT_VERSION_KEY, 1);
        ListTag v1List = new ListTag();
        if (v0.contains("waypoints")) {
            v1List = v0.getListOrEmpty("waypoints").copy();
        } else if (v0.contains("legacy_waypoints")) {
            ListTag legacy = v0.getListOrEmpty("legacy_waypoints");
            for (int i = 0; i < legacy.size(); i++) {
                CompoundTag item = legacy.getCompoundOrEmpty(i);
                String name = item.getStringOr("title", item.getStringOr("name", "Waypoint"));
                int x = item.getIntOr("x", 0);
                int y = item.getIntOr("y", 64);
                int z = item.getIntOr("z", 0);
                String catRaw = item.getStringOr("category", "custom");
                OkabeItoPalette.WaypointCategory cat = OkabeItoPalette.WaypointCategory.CODEC
                        .parse(NbtOps.INSTANCE, net.minecraft.nbt.StringTag.valueOf(catRaw))
                        .result().orElse(OkabeItoPalette.WaypointCategory.CUSTOM);
                UUID creator = parseUuidOrDeterministic(item.getStringOr("owner", ""), "legacy-owner-" + i);
                UUID id = parseUuidOrDeterministic(item.getStringOr("id", ""), "legacy-wp-" + name + "-" + x + "-" + z);
                Waypoint migrated = new Waypoint(
                        id, name, cat, cat.identifier(),
                        item.getIntOr("color", cat.defaultArgb()),
                        cat.defaultShape(),
                        Waypoint.Share.PRIVATE,
                        Identifier.parse(item.getStringOr("dim", "minecraft:overworld")),
                        new BlockPos(x, y, z),
                        item.getIntOr("eps", 16),
                        creator,
                        item.getLongOr("day", 0L),
                        Optional.empty(),
                        Optional.empty()
                );
                Tag encoded = Waypoint.CODEC.encodeStart(NbtOps.INSTANCE, migrated).getOrThrow();
                v1List.add(encoded);
            }
        }
        out.put("waypoints", v1List);
        if (v0.contains("memoryMarks")) {
            out.put("memoryMarks", v0.getCompoundOrEmpty("memoryMarks").copy());
        }
        return out;
    }

    public static CompoundTag migrateSurveyV0ToV1(int fromVersion, CompoundTag v0) {
        CompoundTag out = new CompoundTag();
        out.putInt(FORMAT_VERSION_KEY, 1);
        out.putInt("standaloneTier", v0.getIntOr("tier", v0.getIntOr("standaloneTier", 0)));
        out.putLong("worldSeed", v0.getLongOr("worldSeed", 0x53757065724D43L));
        ListTag chunksOut = new ListTag();
        if (v0.contains("archiveChunks")) {
            chunksOut = v0.getListOrEmpty("archiveChunks").copy();
        } else if (v0.contains("explored_chunks")) {
            ListTag legacy = v0.getListOrEmpty("explored_chunks");
            for (int i = 0; i < legacy.size(); i++) {
                CompoundTag entry = legacy.getCompoundOrEmpty(i);
                int cx = entry.getIntOr("cx", 0);
                int cz = entry.getIntOr("cz", 0);
                int slCode = entry.getIntOr("sl", SurveyLevel.SKETCH.code());
                int epsClass = entry.getIntOr("errClass", 4);
                ChunkKnowledge ck = new ChunkKnowledge(SurveyLevel.fromCode(slCode), entry.getBooleanOr("height", false), epsClass, entry.getIntOr("day", 0));
                CompoundTag v1Entry = new CompoundTag();
                v1Entry.putString("dim", entry.getStringOr("dim", "minecraft:overworld"));
                v1Entry.putInt("cx", cx);
                v1Entry.putInt("cz", cz);
                v1Entry.putInt("packed", ck.pack());
                chunksOut.add(v1Entry);
            }
        }
        out.put("archiveChunks", chunksOut);
        if (v0.contains("playerMemories")) {
            out.put("playerMemories", v0.getCompoundOrEmpty("playerMemories").copy());
        }
        if (v0.contains("cairns")) {
            out.put("cairns", v0.getListOrEmpty("cairns").copy());
        }
        return out;
    }

    public static CompoundTag encodeWaypointsState(WaypointManager manager) {
        CompoundTag root = new CompoundTag();
        root.putInt(FORMAT_VERSION_KEY, FORMAT_VERSION);
        ListTag wpList = new ListTag();
        for (Waypoint wp : manager.allWaypointsSnapshot()) {
            if (wp.category() == OkabeItoPalette.WaypointCategory.PING) {
                continue; // transient 60s pings are not persisted
            }
            wpList.add(Waypoint.CODEC.encodeStart(NbtOps.INSTANCE, wp).getOrThrow());
        }
        root.put("waypoints", wpList);

        CompoundTag marksTag = new CompoundTag();
        for (Map.Entry<UUID, List<MemoryMark>> entry : manager.exportAllMemoryMarks().entrySet()) {
            ListTag playerMarks = new ListTag();
            for (MemoryMark m : entry.getValue()) {
                playerMarks.add(MemoryMark.CODEC.encodeStart(NbtOps.INSTANCE, m).getOrThrow());
            }
            marksTag.put(entry.getKey().toString(), playerMarks);
        }
        root.put("memoryMarks", marksTag);
        return root;
    }

    public static void decodeWaypointsState(CompoundTag raw, WaypointManager manager, boolean wrapX) {
        CompoundTag migrated = migrate(WAYPOINTS_FILE_ID, FORMAT_VERSION, raw);
        ListTag wpList = migrated.getListOrEmpty("waypoints");
        List<Waypoint> loaded = new ArrayList<>(wpList.size());
        for (int i = 0; i < wpList.size(); i++) {
            Tag el = wpList.get(i);
            Waypoint.CODEC.parse(NbtOps.INSTANCE, el).result().ifPresent(wp -> {
                BlockPos pos = wrapX
                        ? new BlockPos(com.nightcity.atlas.map.PlanetProjection.wrapBlockX(wp.pos().getX()), wp.pos().getY(), wp.pos().getZ())
                        : wp.pos();
                loaded.add(new Waypoint(
                        wp.id(), wp.name(), wp.category(), wp.icon(), wp.argb(), wp.shape(), wp.share(),
                        wp.dimension(), pos, wp.epsilon(), wp.creator(), wp.createdDay(), wp.note(), wp.routeOrder()
                ));
            });
        }
        manager.replaceAllWaypoints(loaded);

        CompoundTag marksTag = migrated.getCompoundOrEmpty("memoryMarks");
        for (String uuidKey : marksTag.keySet()) {
            try {
                UUID playerId = UUID.fromString(uuidKey);
                ListTag list = marksTag.getListOrEmpty(uuidKey);
                List<MemoryMark> marks = new ArrayList<>(list.size());
                for (int i = 0; i < list.size(); i++) {
                    MemoryMark.CODEC.parse(NbtOps.INSTANCE, list.get(i)).result().ifPresent(marks::add);
                }
                manager.loadMemoryMarks(playerId, marks);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public static CompoundTag encodeSurveyState(SurveyAtlasStore store, List<UUID> knownPlayers, List<Identifier> dimensions) {
        CompoundTag root = new CompoundTag();
        root.putInt(FORMAT_VERSION_KEY, FORMAT_VERSION);
        root.putInt("standaloneTier", store.standaloneTier());
        root.putLong("worldSeed", store.worldSeed());

        ListTag archiveList = new ListTag();
        ListTag cairnsList = new ListTag();
        for (Identifier dim : dimensions) {
            for (SurveyAtlasStore.ChunkEntry ce : store.exportArchiveEntries(dim)) {
                CompoundTag tag = new CompoundTag();
                tag.putString("dim", dim.toString());
                tag.putInt("cx", ce.chunkX());
                tag.putInt("cz", ce.chunkZ());
                tag.putInt("packed", ce.knowledge().pack());
                archiveList.add(tag);
            }
            for (BlockPos c : store.cairns(dim)) {
                CompoundTag ct = new CompoundTag();
                ct.putString("dim", dim.toString());
                ct.putInt("x", c.getX());
                ct.putInt("y", c.getY());
                ct.putInt("z", c.getZ());
                cairnsList.add(ct);
            }
        }
        root.put("archiveChunks", archiveList);
        root.put("cairns", cairnsList);

        CompoundTag memByPlayer = new CompoundTag();
        for (UUID playerId : knownPlayers) {
            ListTag pList = new ListTag();
            for (Identifier dim : dimensions) {
                for (SurveyAtlasStore.MemoryEntry me : store.exportPlayerMemories(playerId, dim)) {
                    CompoundTag mt = new CompoundTag();
                    mt.putString("dim", dim.toString());
                    mt.putInt("cx", me.chunkX());
                    mt.putInt("cz", me.chunkZ());
                    mt.putLong("seen", me.seenGameSeconds());
                    pList.add(mt);
                }
            }
            if (!pList.isEmpty()) {
                memByPlayer.put(playerId.toString(), pList);
            }
        }
        root.put("playerMemories", memByPlayer);
        return root;
    }

    public static void decodeSurveyState(CompoundTag raw, SurveyAtlasStore store, boolean wrapX) {
        CompoundTag migrated = migrate(SURVEY_FILE_ID, FORMAT_VERSION, raw);
        store.setStandaloneTier(migrated.getIntOr("standaloneTier", 0));
        if (migrated.contains("worldSeed")) {
            store.setWorldSeed(migrated.getLongOr("worldSeed", store.worldSeed()));
        }
        ListTag cairns = migrated.getListOrEmpty("cairns");
        for (int i = 0; i < cairns.size(); i++) {
            CompoundTag ct = cairns.getCompoundOrEmpty(i);
            Identifier dim = Identifier.parse(ct.getStringOr("dim", "minecraft:overworld"));
            store.registerCairn(dim, new BlockPos(ct.getIntOr("x", 0), ct.getIntOr("y", 64), ct.getIntOr("z", 0)));
        }
        ListTag archiveList = migrated.getListOrEmpty("archiveChunks");
        for (int i = 0; i < archiveList.size(); i++) {
            CompoundTag ct = archiveList.getCompoundOrEmpty(i);
            Identifier dim = Identifier.parse(ct.getStringOr("dim", "minecraft:overworld"));
            int cx = ct.getIntOr("cx", 0);
            int cz = ct.getIntOr("cz", 0);
            int packed = ct.getIntOr("packed", 0);
            store.mergeArchiveKnowledge(dim, cx, cz, ChunkKnowledge.unpack(packed), wrapX);
        }
        CompoundTag memByPlayer = migrated.getCompoundOrEmpty("playerMemories");
        for (String uuidKey : memByPlayer.keySet()) {
            try {
                UUID pid = UUID.fromString(uuidKey);
                ListTag pList = memByPlayer.getListOrEmpty(uuidKey);
                for (int i = 0; i < pList.size(); i++) {
                    CompoundTag mt = pList.getCompoundOrEmpty(i);
                    Identifier dim = Identifier.parse(mt.getStringOr("dim", "minecraft:overworld"));
                    store.recordPersonalSighting(pid, dim, mt.getIntOr("cx", 0), mt.getIntOr("cz", 0), mt.getLongOr("seen", 0L), wrapX);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public static void writeCompressedAtomically(Path file, CompoundTag tag) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
        NbtIo.writeCompressed(tag, tmp);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static CompoundTag readCompressed(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            CompoundTag empty = new CompoundTag();
            empty.putInt(FORMAT_VERSION_KEY, FORMAT_VERSION);
            return empty;
        }
        return NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
    }

    private static UUID parseUuidOrDeterministic(String raw, String fallbackKey) {
        if (raw != null && !raw.isBlank()) {
            try {
                return UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return UUID.nameUUIDFromBytes(fallbackKey.getBytes(StandardCharsets.UTF_8));
    }
}
