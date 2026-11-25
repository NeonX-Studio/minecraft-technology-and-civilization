package com.nightcity.nations.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.nations.TNations;
import com.nightcity.nations.api.NationState;
import com.nightcity.nations.api.NationStatus;
import com.nightcity.nations.api.NationsApi;
import com.nightcity.nations.api.OpinionModifier;
import com.nightcity.nations.api.RelationRecord;
import com.nightcity.tcivilization.api.TcMigrators;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Persistent state for Nations (formatVersion = 1) with TcMigrators support and
 * <world>/tc/nations/civs.dat + relations.dat snapshots (23 §5.1, §11.1, 40 EC-P1-8).
 */
public final class NationsSavedData extends SavedData implements NationsApi {
    public static final int FORMAT_VERSION = 1;
    public static final ResourceKey<Level> OVERWORLD_DIM = ResourceKey.create(
            Registries.DIMENSION, Identifier.withDefaultNamespace("overworld")
    );
    public static final Identifier FILE_ID = Identifier.fromNamespaceAndPath(TNations.MOD_ID, "nations_state");
    public static final Identifier CIVS_FILE_ID = Identifier.fromNamespaceAndPath(TNations.MOD_ID, "civs_dat");
    public static final Identifier RELATIONS_FILE_ID = Identifier.fromNamespaceAndPath(TNations.MOD_ID, "relations_dat");

    private static boolean defaultMigratorsRegistered = false;

    static {
        registerDefaultMigrators();
    }

    public static synchronized void registerDefaultMigrators() {
        if (defaultMigratorsRegistered) {
            return;
        }
        defaultMigratorsRegistered = true;
        TcMigrators.register(FILE_ID, 0, (fromVer, tag) -> {
            CompoundTag migrated = new CompoundTag();
            migrated.putInt(TcMigrators.FORMAT_VERSION_KEY, 1);
            migrated.putBoolean("seeded", tag.getBooleanOr("seeded", false));
            migrated.putLong("world_seed", tag.getLongOr("world_seed", tag.getLongOr("seed", 0L)));
            migrated.putString("seed_source", tag.getStringOr("seed_source", tag.getStringOr("source", "none")));
            if (tag.contains("nations")) {
                migrated.put("nations", tag.get("nations").copy());
            } else if (tag.contains("ai_civs")) {
                migrated.put("nations", tag.get("ai_civs").copy());
            } else {
                migrated.put("nations", new ListTag());
            }
            if (tag.contains("relations")) {
                migrated.put("relations", tag.get("relations").copy());
            } else {
                migrated.put("relations", new ListTag());
            }
            if (tag.contains("influences")) {
                migrated.put("influences", tag.get("influences").copy());
            }
            if (tag.contains("applied_rules")) {
                migrated.put("applied_rules", tag.get("applied_rules").copy());
            }
            return migrated;
        });
        TcMigrators.register(CIVS_FILE_ID, 0, (fromVer, tag) -> {
            CompoundTag out = tag.copy();
            out.putInt(TcMigrators.FORMAT_VERSION_KEY, 1);
            if (!out.contains("nations") && out.contains("ai_civs")) {
                out.put("nations", out.get("ai_civs").copy());
            }
            return out;
        });
        TcMigrators.register(RELATIONS_FILE_ID, 0, (fromVer, tag) -> {
            CompoundTag out = tag.copy();
            out.putInt(TcMigrators.FORMAT_VERSION_KEY, 1);
            return out;
        });
    }

    private record RegionInfluenceEntry(long regionKey, InfluenceLayerData data) {
        static final Codec<RegionInfluenceEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("region").forGetter(RegionInfluenceEntry::regionKey),
                InfluenceLayerData.CODEC.fieldOf("data").forGetter(RegionInfluenceEntry::data)
        ).apply(i, RegionInfluenceEntry::new));
    }

    private record V1Payload(
            int formatVersion,
            boolean seeded,
            long worldSeed,
            String seedSource,
            List<NationState> nations,
            List<RelationRecord> relations,
            List<RegionInfluenceEntry> influences,
            Map<String, Boolean> appliedRules
    ) {
        static final Codec<V1Payload> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf(TcMigrators.FORMAT_VERSION_KEY).forGetter(V1Payload::formatVersion),
                Codec.BOOL.optionalFieldOf("seeded", false).forGetter(V1Payload::seeded),
                Codec.LONG.optionalFieldOf("world_seed", 0L).forGetter(V1Payload::worldSeed),
                Codec.STRING.optionalFieldOf("seed_source", "none").forGetter(V1Payload::seedSource),
                NationState.CODEC.listOf().optionalFieldOf("nations", List.of()).forGetter(V1Payload::nations),
                RelationRecord.CODEC.listOf().optionalFieldOf("relations", List.of()).forGetter(V1Payload::relations),
                RegionInfluenceEntry.CODEC.listOf().optionalFieldOf("influences", List.of()).forGetter(V1Payload::influences),
                Codec.unboundedMap(Codec.STRING, Codec.BOOL).optionalFieldOf("applied_rules", Map.of()).forGetter(V1Payload::appliedRules)
        ).apply(i, V1Payload::new));
    }

    public static final Codec<NationsSavedData> CODEC = CompoundTag.CODEC.xmap(
            NationsSavedData::fromCompoundTag,
            NationsSavedData::toCompoundTag
    );

    public static final SavedDataType<NationsSavedData> TYPE = new SavedDataType<>(
            FILE_ID,
            NationsSavedData::new,
            CODEC
    );

    private record PairKey(UUID first, UUID second) {
        static PairKey of(UUID a, UUID b) {
            return a.compareTo(b) <= 0 ? new PairKey(a, b) : new PairKey(b, a);
        }
    }

    private final Map<UUID, NationState> nations = new LinkedHashMap<>();
    private final Map<PairKey, RelationRecord> relations = new LinkedHashMap<>();
    private final Map<Long, InfluenceLayerData> regionInfluences = new LinkedHashMap<>();
    private final Map<String, Boolean> appliedRules = new LinkedHashMap<>();
    private final Set<String> warnedAdminRuleOverrides = new LinkedHashSet<>();
    private boolean seeded;
    private long worldSeed;
    private String seedSource = "none";
    private LongSupplier clockSupplier = () -> 0L;

    public NationsSavedData() {
    }

    public static NationsSavedData get(ServerLevel overworld) {
        NationsSavedData data = overworld.getDataStorage().computeIfAbsent(TYPE);
        data.bindClock(overworld::getOverworldClockTime);
        return data;
    }

    public void bindClock(LongSupplier supplier) {
        this.clockSupplier = Objects.requireNonNull(supplier, "supplier");
    }

    public static NationsSavedData fromCompoundTag(CompoundTag rawTag) {
        registerDefaultMigrators();
        CompoundTag migrated = TcMigrators.migrate(FILE_ID, FORMAT_VERSION, rawTag);
        V1Payload payload = V1Payload.CODEC.parse(NbtOps.INSTANCE, migrated)
                .getOrThrow(err -> new IllegalStateException("Failed decoding NationsSavedData v1: " + err));
        NationsSavedData data = new NationsSavedData();
        data.seeded = payload.seeded();
        data.worldSeed = payload.worldSeed();
        data.seedSource = payload.seedSource();
        for (NationState state : payload.nations()) {
            data.nations.put(state.civId(), state);
        }
        for (RelationRecord rel : payload.relations()) {
            data.relations.put(PairKey.of(rel.a(), rel.b()), rel);
        }
        for (RegionInfluenceEntry inf : payload.influences()) {
            data.regionInfluences.put(inf.regionKey(), inf.data());
        }
        data.appliedRules.putAll(payload.appliedRules());
        return data;
    }

    public CompoundTag toCompoundTag() {
        List<RegionInfluenceEntry> infList = new ArrayList<>(regionInfluences.size());
        for (Map.Entry<Long, InfluenceLayerData> e : regionInfluences.entrySet()) {
            infList.add(new RegionInfluenceEntry(e.getKey(), e.getValue()));
        }
        V1Payload payload = new V1Payload(
                FORMAT_VERSION,
                seeded,
                worldSeed,
                seedSource,
                List.copyOf(nations.values()),
                List.copyOf(relations.values()),
                infList,
                Map.copyOf(appliedRules)
        );
        Tag encoded = V1Payload.CODEC.encodeStart(NbtOps.INSTANCE, payload)
                .getOrThrow(err -> new IllegalStateException("Failed encoding NationsSavedData v1: " + err));
        return (CompoundTag) encoded;
    }

    public static Path civsFilePath(Path worldRoot) {
        return worldRoot.resolve("tc").resolve("nations").resolve("civs.dat");
    }

    public static Path relationsFilePath(Path worldRoot) {
        return worldRoot.resolve("tc").resolve("nations").resolve("relations.dat");
    }

    public synchronized void saveToDirectory(Path worldRoot) throws IOException {
        Path dir = worldRoot.resolve("tc").resolve("nations");
        Files.createDirectories(dir);
        NbtIo.writeCompressed(toCompoundTag(), civsFilePath(worldRoot));

        CompoundTag relRoot = new CompoundTag();
        relRoot.putInt(TcMigrators.FORMAT_VERSION_KEY, FORMAT_VERSION);
        ListTag relList = new ListTag();
        for (RelationRecord rel : relations.values()) {
            relList.add(RelationRecord.CODEC.encodeStart(NbtOps.INSTANCE, rel).getOrThrow());
        }
        relRoot.put("relations", relList);
        NbtIo.writeCompressed(relRoot, relationsFilePath(worldRoot));
    }

    public static NationsSavedData loadFromDirectory(Path worldRoot) throws IOException {
        Path civsFile = civsFilePath(worldRoot);
        if (!Files.isRegularFile(civsFile)) {
            return new NationsSavedData();
        }
        CompoundTag raw = NbtIo.readCompressed(civsFile, NbtAccounter.unlimitedHeap());
        CompoundTag migratedCivs = TcMigrators.migrate(CIVS_FILE_ID, FORMAT_VERSION, raw);
        return fromCompoundTag(migratedCivs);
    }

    public boolean isSeeded() {
        return seeded;
    }

    public long worldSeed() {
        return worldSeed;
    }

    public String seedSource() {
        return seedSource;
    }

    public synchronized void recordSeeded(
            long seed,
            String source,
            List<NationState> seededNations,
            List<RelationRecord> seededRelations,
            Map<Long, InfluenceLayerData> influences
    ) {
        this.seeded = true;
        this.worldSeed = seed;
        this.seedSource = Objects.requireNonNull(source, "source");
        this.nations.clear();
        for (NationState state : seededNations) {
            this.nations.put(state.civId(), state);
        }
        this.relations.clear();
        for (RelationRecord rel : seededRelations) {
            this.relations.put(PairKey.of(rel.a(), rel.b()), rel);
        }
        this.regionInfluences.clear();
        this.regionInfluences.putAll(influences);
        setDirty();
    }

    public synchronized Map<Long, InfluenceLayerData> regionInfluences() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(regionInfluences));
    }

    public synchronized int ownedRegionCount() {
        int count = 0;
        for (InfluenceLayerData d : regionInfluences.values()) {
            if (d.ownerCivId().isPresent()) {
                count++;
            }
        }
        return count;
    }

    public synchronized Optional<Boolean> recordedGameruleValue(String ruleKey) {
        return Optional.ofNullable(appliedRules.get(ruleKey));
    }

    public synchronized void recordGameruleWrite(String ruleKey, boolean writtenValue) {
        appliedRules.put(ruleKey, writtenValue);
        setDirty();
    }

    public synchronized boolean markAdminOverrideWarned(String ruleKey) {
        return warnedAdminRuleOverrides.add(ruleKey);
    }

    @Override
    public synchronized Collection<NationState> activeNations() {
        List<NationState> active = new ArrayList<>();
        for (NationState s : nations.values()) {
            if (s.status() == NationStatus.ACTIVE) {
                active.add(s);
            }
        }
        return List.copyOf(active);
    }

    public synchronized List<NationState> allNations() {
        return List.copyOf(nations.values());
    }

    @Override
    public synchronized Optional<NationState> nation(UUID civId) {
        return Optional.ofNullable(nations.get(civId));
    }

    @Override
    public synchronized Optional<UUID> regionOwner(ResourceKey<Level> dim, long regionKey) {
        if (dim != null && !OVERWORLD_DIM.equals(dim)) {
            return Optional.empty();
        }
        InfluenceLayerData layer = regionInfluences.get(regionKey);
        return layer == null ? Optional.empty() : layer.ownerCivId();
    }

    @Override
    public synchronized RelationView relation(UUID a, UUID b) {
        if (a == null || b == null || a.equals(b)) {
            return RelationView.neutral();
        }
        RelationRecord rec = relations.get(PairKey.of(a, b));
        if (rec == null) {
            return RelationView.neutral();
        }
        double opinion = rec.opinionAt(clockSupplier.getAsLong());
        double fear = a.equals(rec.a()) ? rec.fearAB() : rec.fearBA();
        return new RelationView(opinion, rec.trust(), fear, OpinionTier.fromOpinion(opinion));
    }

    public synchronized List<RelationRecord> allRelations() {
        return List.copyOf(relations.values());
    }

    @Override
    public synchronized boolean hasContact(UUID a, UUID b) {
        if (a == null || b == null || a.equals(b)) {
            return false;
        }
        RelationRecord rec = relations.get(PairKey.of(a, b));
        return rec != null && rec.contacted();
    }

    @Override
    public synchronized Set<UUID> contactedCivs(UUID civ) {
        Set<UUID> out = new LinkedHashSet<>();
        for (RelationRecord rec : relations.values()) {
            if (!rec.contacted()) {
                continue;
            }
            if (rec.a().equals(civ)) {
                out.add(rec.b());
            } else if (rec.b().equals(civ)) {
                out.add(rec.a());
            }
        }
        return Collections.unmodifiableSet(out);
    }

    @Override
    public synchronized void addOpinionModifier(UUID from, UUID to, Identifier id, double value, int halfLifeDays) {
        if (from == null || to == null || from.equals(to)) {
            return;
        }
        PairKey key = PairKey.of(from, to);
        RelationRecord existing = relations.get(key);
        long now = clockSupplier.getAsLong();
        List<OpinionModifier> mods = new ArrayList<>(existing == null ? List.of() : existing.modifiers());
        mods.add(new OpinionModifier(id, value, now, halfLifeDays));
        RelationRecord updated = existing == null
                ? new RelationRecord(key.first(), key.second(), 50.0D, 0.0D, 0.0D, mods, false, -1L)
                : new RelationRecord(existing.a(), existing.b(), existing.trust(), existing.fearAB(), existing.fearBA(), mods, existing.contacted(), existing.firstContactTick());
        relations.put(key, updated);
        setDirty();
    }
}
