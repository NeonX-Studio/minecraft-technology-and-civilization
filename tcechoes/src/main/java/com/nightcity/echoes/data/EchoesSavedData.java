package com.nightcity.echoes.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.echoes.TEchoes;
import com.nightcity.echoes.api.HeritageEntry;
import com.nightcity.echoes.api.LegendView;
import com.nightcity.echoes.api.LiveHistoryEntry;
import com.nightcity.echoes.api.SimulationMode;
import com.nightcity.echoes.sim.WorldHistoryData;
import com.nightcity.tcivilization.api.TcMigrators;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Global SavedData for tcechoes (data/tcechoes.dat, formatVersion = 1) with TcMigrators support (18 §11.2, 33 §8.1).
 */
public final class EchoesSavedData extends SavedData {
    public static final int FORMAT_VERSION = 1;
    public static final Identifier FILE_ID = Identifier.fromNamespaceAndPath(TEchoes.MOD_ID, "tcechoes");

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
            CompoundTag out = tag.copy();
            out.putInt(TcMigrators.FORMAT_VERSION_KEY, 1);
            if (!out.contains("historyInitialized")) {
                out.putBoolean("historyInitialized", tag.getBooleanOr("initialized", false));
            }
            if (!out.contains("regionsPopulated")) {
                out.putBoolean("regionsPopulated", false);
            }
            if (!out.contains("generatorVersion")) {
                out.putInt("generatorVersion", WorldHistoryData.GENERATOR_VERSION);
            }
            if (!out.contains("mode")) {
                out.putString("mode", SimulationMode.STANDALONE.name().toLowerCase());
            }
            return out;
        });
    }

    public record HistoricCivBinding(int polityId, UUID civId) {
        public static final Codec<HistoricCivBinding> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("polity_id").forGetter(HistoricCivBinding::polityId),
                UUIDUtil.CODEC.fieldOf("civ_id").forGetter(HistoricCivBinding::civId)
        ).apply(i, HistoricCivBinding::new));
    }

    public record ScriptMasteryEntry(UUID civId, int cultureId, int mastery, List<Integer> countedInscriptions) {
        public static final Codec<ScriptMasteryEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("civ_id").forGetter(ScriptMasteryEntry::civId),
                Codec.INT.fieldOf("culture_id").forGetter(ScriptMasteryEntry::cultureId),
                Codec.intRange(0, 100).fieldOf("mastery").forGetter(ScriptMasteryEntry::mastery),
                Codec.INT.listOf().optionalFieldOf("inscriptions", List.of()).forGetter(ScriptMasteryEntry::countedInscriptions)
        ).apply(i, ScriptMasteryEntry::new));

        public ScriptMasteryEntry {
            Objects.requireNonNull(civId, "civId");
            mastery = Math.clamp(mastery, 0, 100);
            countedInscriptions = countedInscriptions == null ? List.of() : List.copyOf(countedInscriptions);
        }
    }

    public record EchoClueEntry(UUID civId, Identifier tech, float discount, int artefactSiteId) {
        public static final Codec<EchoClueEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("civ_id").forGetter(EchoClueEntry::civId),
                Identifier.CODEC.fieldOf("tech").forGetter(EchoClueEntry::tech),
                Codec.FLOAT.fieldOf("discount").forGetter(EchoClueEntry::discount),
                Codec.INT.fieldOf("site_id").forGetter(EchoClueEntry::artefactSiteId)
        ).apply(i, EchoClueEntry::new));

        public EchoClueEntry {
            Objects.requireNonNull(civId, "civId");
            Objects.requireNonNull(tech, "tech");
            discount = Math.clamp(discount, 0.0F, 0.50F);
        }
    }

    private record V1Payload(
            int formatVersion,
            boolean historyInitialized,
            boolean regionsPopulated,
            long historySeed,
            int generatorVersion,
            SimulationMode mode,
            List<HistoricCivBinding> historicCivs,
            List<LegendView> legends,
            List<ScriptMasteryEntry> scriptMasteries,
            List<HeritageEntry> heritages,
            List<EchoClueEntry> clues,
            List<LiveHistoryEntry> liveQueue
    ) {
        static final Codec<V1Payload> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf(TcMigrators.FORMAT_VERSION_KEY).forGetter(V1Payload::formatVersion),
                Codec.BOOL.optionalFieldOf("historyInitialized", false).forGetter(V1Payload::historyInitialized),
                Codec.BOOL.optionalFieldOf("regionsPopulated", false).forGetter(V1Payload::regionsPopulated),
                Codec.LONG.optionalFieldOf("historySeed", 0L).forGetter(V1Payload::historySeed),
                Codec.INT.optionalFieldOf("generatorVersion", WorldHistoryData.GENERATOR_VERSION).forGetter(V1Payload::generatorVersion),
                SimulationMode.CODEC.optionalFieldOf("mode", SimulationMode.STANDALONE).forGetter(V1Payload::mode),
                HistoricCivBinding.CODEC.listOf().optionalFieldOf("historicCivs", List.of()).forGetter(V1Payload::historicCivs),
                LegendView.CODEC.listOf().optionalFieldOf("legends", List.of()).forGetter(V1Payload::legends),
                ScriptMasteryEntry.CODEC.listOf().optionalFieldOf("scriptMasteries", List.of()).forGetter(V1Payload::scriptMasteries),
                HeritageEntry.CODEC.listOf().optionalFieldOf("heritages", List.of()).forGetter(V1Payload::heritages),
                EchoClueEntry.CODEC.listOf().optionalFieldOf("clues", List.of()).forGetter(V1Payload::clues),
                LiveHistoryEntry.CODEC.listOf().optionalFieldOf("liveQueue", List.of()).forGetter(V1Payload::liveQueue)
        ).apply(i, V1Payload::new));
    }

    public static final Codec<EchoesSavedData> CODEC = CompoundTag.CODEC.xmap(
            EchoesSavedData::fromCompoundTag,
            EchoesSavedData::toCompoundTag
    );

    public static final SavedDataType<EchoesSavedData> TYPE = new SavedDataType<>(
            FILE_ID,
            EchoesSavedData::new,
            CODEC
    );

    private int formatVersion = FORMAT_VERSION;
    private boolean historyInitialized = false;
    private boolean regionsPopulated = false;
    private long historySeed = 0L;
    private int generatorVersion = WorldHistoryData.GENERATOR_VERSION;
    private SimulationMode mode = SimulationMode.STANDALONE;

    private final Map<Integer, UUID> historicCivIds = new LinkedHashMap<>();
    private final List<LegendView> legends = new ArrayList<>();
    private final Map<String, ScriptMasteryEntry> scriptMasteries = new LinkedHashMap<>();
    private final Map<Integer, HeritageEntry> heritages = new LinkedHashMap<>();
    private final List<EchoClueEntry> clues = new ArrayList<>();
    private final List<LiveHistoryEntry> liveQueue = new ArrayList<>();

    public EchoesSavedData() {
    }

    public static EchoesSavedData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public static Path resolveDatPath(Path worldRoot) {
        return worldRoot.resolve("data").resolve("tcechoes.dat");
    }

    public static EchoesSavedData fromCompoundTag(CompoundTag rawTag) {
        return fromCompoundTag(rawTag, FORMAT_VERSION);
    }

    public static EchoesSavedData fromCompoundTag(CompoundTag rawTag, int targetVersion) {
        CompoundTag migrated = TcMigrators.migrate(FILE_ID, targetVersion, rawTag);
        V1Payload p = V1Payload.CODEC.parse(NbtOps.INSTANCE, migrated)
                .getOrThrow(err -> new IllegalStateException("Failed decoding EchoesSavedData: " + err));
        EchoesSavedData data = new EchoesSavedData();
        data.formatVersion = p.formatVersion();
        data.historyInitialized = p.historyInitialized();
        data.regionsPopulated = p.regionsPopulated();
        data.historySeed = p.historySeed();
        data.generatorVersion = p.generatorVersion();
        data.mode = p.mode();
        for (HistoricCivBinding b : p.historicCivs()) {
            data.historicCivIds.put(b.polityId(), b.civId());
        }
        data.legends.addAll(p.legends());
        for (ScriptMasteryEntry sm : p.scriptMasteries()) {
            data.scriptMasteries.put(masteryKey(sm.civId(), sm.cultureId()), sm);
        }
        for (HeritageEntry h : p.heritages()) {
            data.heritages.put(h.siteId(), h);
        }
        data.clues.addAll(p.clues());
        data.liveQueue.addAll(p.liveQueue());
        return data;
    }

    public CompoundTag toCompoundTag() {
        List<HistoricCivBinding> bindings = new ArrayList<>(historicCivIds.size());
        for (Map.Entry<Integer, UUID> e : historicCivIds.entrySet()) {
            bindings.add(new HistoricCivBinding(e.getKey(), e.getValue()));
        }
        V1Payload payload = new V1Payload(
                formatVersion,
                historyInitialized,
                regionsPopulated,
                historySeed,
                generatorVersion,
                mode,
                bindings,
                List.copyOf(legends),
                List.copyOf(scriptMasteries.values()),
                List.copyOf(heritages.values()),
                List.copyOf(clues),
                List.copyOf(liveQueue)
        );
        Tag encoded = V1Payload.CODEC.encodeStart(NbtOps.INSTANCE, payload)
                .getOrThrow(err -> new IllegalStateException("Failed encoding EchoesSavedData: " + err));
        return (CompoundTag) encoded;
    }

    public void saveToDatFile(Path datFile) throws IOException {
        if (datFile.getParent() != null) {
            Files.createDirectories(datFile.getParent());
        }
        CompoundTag root = new CompoundTag();
        root.put("data", toCompoundTag());
        NbtIo.writeCompressed(root, datFile);
    }

    public static EchoesSavedData loadFromDatFile(Path datFile) throws IOException {
        return loadFromDatFile(datFile, FORMAT_VERSION);
    }

    public static EchoesSavedData loadFromDatFile(Path datFile, int targetVersion) throws IOException {
        CompoundTag root = NbtIo.readCompressed(datFile, NbtAccounter.unlimitedHeap());
        CompoundTag inner = root.contains("data") ? root.getCompoundOrEmpty("data") : root;
        return fromCompoundTag(inner, targetVersion);
    }

    public int formatVersion() {
        return formatVersion;
    }

    public boolean isHistoryInitialized() {
        return historyInitialized;
    }

    public void markHistoryInitialized(long seed, int genVer, SimulationMode simMode) {
        this.historyInitialized = true;
        this.historySeed = seed;
        this.generatorVersion = genVer;
        this.mode = Objects.requireNonNull(simMode, "simMode");
        setDirty();
    }

    public boolean isRegionsPopulated() {
        return regionsPopulated;
    }

    public void setRegionsPopulated(boolean populated) {
        this.regionsPopulated = populated;
        setDirty();
    }

    public long historySeed() {
        return historySeed;
    }

    public int generatorVersion() {
        return generatorVersion;
    }

    public SimulationMode mode() {
        return mode;
    }

    public Map<Integer, UUID> historicCivIds() {
        return Collections.unmodifiableMap(historicCivIds);
    }

    public Optional<UUID> historicCivId(int polityId) {
        return Optional.ofNullable(historicCivIds.get(polityId));
    }

    public void putHistoricCivId(int polityId, UUID civId) {
        historicCivIds.put(polityId, Objects.requireNonNull(civId, "civId"));
        setDirty();
    }

    public List<LegendView> legendsOf(UUID civId) {
        List<LegendView> out = new ArrayList<>();
        for (LegendView lv : legends) {
            if (lv.civId().equals(civId)) {
                out.add(lv);
            }
        }
        return List.copyOf(out);
    }

    public void addOrUpdateLegend(LegendView view) {
        Objects.requireNonNull(view, "view");
        legends.removeIf(l -> l.civId().equals(view.civId()) && l.seedId() == view.seedId());
        legends.add(view);
        setDirty();
    }

    public int scriptMastery(UUID civId, int cultureId) {
        ScriptMasteryEntry entry = scriptMasteries.get(masteryKey(civId, cultureId));
        return entry != null ? entry.mastery() : 0;
    }

    public void setScriptMastery(ScriptMasteryEntry entry) {
        scriptMasteries.put(masteryKey(entry.civId(), entry.cultureId()), entry);
        setDirty();
    }

    public Optional<HeritageEntry> heritage(int siteId) {
        return Optional.ofNullable(heritages.get(siteId));
    }

    public boolean putHeritageOptimistic(HeritageEntry candidate, long expectedVersion) {
        HeritageEntry existing = heritages.get(candidate.siteId());
        if (existing == null) {
            if (expectedVersion != 0L) {
                return false;
            }
            heritages.put(candidate.siteId(), candidate);
            setDirty();
            return true;
        }
        if (existing.version() != expectedVersion) {
            return false;
        }
        heritages.put(candidate.siteId(), new HeritageEntry(
                candidate.siteId(), candidate.civId(), candidate.level(),
                candidate.since(), candidate.baselineBlocks(), existing.version() + 1L
        ));
        setDirty();
        return true;
    }

    public float clueDiscount(UUID civId, Identifier techId) {
        float max = 0.0F;
        for (EchoClueEntry c : clues) {
            if (c.civId().equals(civId) && c.tech().equals(techId) && c.discount() > max) {
                max = c.discount();
            }
        }
        return max;
    }

    public void addClue(EchoClueEntry clue) {
        clues.add(Objects.requireNonNull(clue, "clue"));
        setDirty();
    }

    public List<LiveHistoryEntry> liveHistoryQueue() {
        return List.copyOf(liveQueue);
    }

    public void appendLiveEntry(LiveHistoryEntry entry, int maxTotalCap) {
        liveQueue.add(Objects.requireNonNull(entry, "entry"));
        if (liveQueue.size() > maxTotalCap) {
            liveQueue.removeFirst();
        }
        setDirty();
    }

    private static String masteryKey(UUID civId, int cultureId) {
        return civId + ":" + cultureId;
    }
}
