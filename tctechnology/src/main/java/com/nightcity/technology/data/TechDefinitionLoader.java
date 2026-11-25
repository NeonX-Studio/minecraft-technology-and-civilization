package com.nightcity.technology.data;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.nightcity.technology.TTechnology;
import com.nightcity.technology.TechConfig;
import com.nightcity.technology.api.EraDefinition;
import com.nightcity.technology.api.TechDefinition;
import com.nightcity.technology.api.TechEra;
import com.nightcity.technology.api.event.TechDefinitionsReloadedEvent;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public final class TechDefinitionLoader extends SimpleJsonResourceReloadListener<JsonElement> {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Codec<JsonElement> JSON_CODEC = Codec.PASSTHROUGH.xmap(
            dynamic -> dynamic.convert(JsonOps.INSTANCE).getValue(),
            element -> new Dynamic<>(JsonOps.INSTANCE, element)
    );
    private static final FileToIdConverter TECH_LISTER = FileToIdConverter.json("tc_tech");
    private static final FileToIdConverter ERA_LISTER = FileToIdConverter.json("tc_era");
    private static final FileToIdConverter FALLBACK_LISTER = FileToIdConverter.json("tc_tech_fallback");

    public enum ValidationCode {
        TECH_PARSE(true),
        TECH_OWNER_ABSENT(false),
        TECH_PREREQ_MISSING(true),
        TECH_PREREQ_ERA(true),
        TECH_PREREQ_CROSS(true),
        TECH_CYCLE(true),
        TECH_COST_RANGE(false),
        TECH_NAMESPACE_IGNORED(false);

        private final boolean error;

        ValidationCode(boolean error) {
            this.error = error;
        }

        public boolean isError() {
            return error;
        }
    }

    public record ValidationIssue(ValidationCode code, Identifier techId, String detail) {
    }

    public record TechNodeLayout(int eraColumn, String lane, int depth, int row) {
    }

    public record TechGraphSnapshot(
            Map<Identifier, TechDefinition> definitions,
            Map<TechEra, EraDefinition> eras,
            Map<Identifier, TechNodeLayout> layouts,
            List<ValidationIssue> issues,
            int fallbackCount
    ) {
        public static final TechGraphSnapshot EMPTY = new TechGraphSnapshot(
                Map.of(), defaultEras(), Map.of(), List.of(), 0
        );

        public Map<TechEra, Integer> countByEra() {
            Map<TechEra, Integer> counts = new EnumMap<>(TechEra.class);
            for (TechEra era : TechEra.values()) {
                counts.put(era, 0);
            }
            for (TechDefinition def : definitions.values()) {
                counts.merge(def.era(), 1, Integer::sum);
            }
            return Collections.unmodifiableMap(counts);
        }

        public Map<String, Integer> countByOwner() {
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (TechDefinition def : definitions.values()) {
                counts.merge(def.owner(), 1, Integer::sum);
            }
            return Collections.unmodifiableMap(counts);
        }

        public Map<ValidationCode, Integer> issueCounts() {
            Map<ValidationCode, Integer> counts = new EnumMap<>(ValidationCode.class);
            for (ValidationCode code : ValidationCode.values()) {
                counts.put(code, 0);
            }
            for (ValidationIssue issue : issues) {
                counts.merge(issue.code(), 1, Integer::sum);
            }
            return Collections.unmodifiableMap(counts);
        }

        public int blockingErrorCount() {
            int total = 0;
            for (ValidationIssue issue : issues) {
                if (issue.code() == ValidationCode.TECH_PARSE || issue.code() == ValidationCode.TECH_CYCLE) {
                    total++;
                }
            }
            return total;
        }

        public boolean selfTestPassed() {
            for (ValidationIssue issue : issues) {
                if (issue.code().isError()) {
                    return false;
                }
            }
            return !definitions.isEmpty();
        }
    }

    private volatile Map<Identifier, JsonElement> pendingEraJson = Map.of();
    private volatile Map<Identifier, JsonElement> pendingFallbackJson = Map.of();
    private volatile TechGraphSnapshot currentSnapshot = TechGraphSnapshot.EMPTY;

    public TechDefinitionLoader() {
        super(JSON_CODEC, TECH_LISTER);
    }

    public TechGraphSnapshot snapshot() {
        return currentSnapshot;
    }

    public void setSnapshotForTesting(TechGraphSnapshot snapshot) {
        this.currentSnapshot = snapshot;
    }

    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, JsonElement> eras = new LinkedHashMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(resourceManager, ERA_LISTER, JsonOps.INSTANCE, JSON_CODEC, eras);
        this.pendingEraJson = eras;

        Map<Identifier, JsonElement> fallbacks = new LinkedHashMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(resourceManager, FALLBACK_LISTER, JsonOps.INSTANCE, JSON_CODEC, fallbacks);
        this.pendingFallbackJson = fallbacks;

        return super.prepare(resourceManager, profiler);
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> rawTechs, ResourceManager resourceManager, ProfilerFiller profiler) {
        boolean enableStubs = !TechConfig.SERVER_SPEC.isLoaded() || TechConfig.ENABLE_STUBS.get();
        Predicate<String> modCheck = modId -> ModList.get() != null && ModList.get().isLoaded(modId);
        TechGraphSnapshot compiled = compile(rawTechs, pendingEraJson, pendingFallbackJson, modCheck, enableStubs);
        this.currentSnapshot = compiled;
        LOGGER.info("[tctechnology] Loaded {} technologies ({} fallback stubs), {} validation notices",
                compiled.definitions().size(), compiled.fallbackCount(), compiled.issues().size());
        NeoForge.EVENT_BUS.post(new TechDefinitionsReloadedEvent(compiled));
    }

    public static TechGraphSnapshot compile(
            Map<Identifier, JsonElement> rawTechs,
            Map<Identifier, JsonElement> rawEras,
            Map<Identifier, JsonElement> rawFallbacks,
            Predicate<String> isModLoaded,
            boolean enableStubs
    ) {
        List<ValidationIssue> issues = new ArrayList<>();
        Map<TechEra, EraDefinition> eras = parseEras(rawEras);
        Set<Identifier> registeredKeystones = new LinkedHashSet<>();
        for (EraDefinition eraDef : eras.values()) {
            registeredKeystones.addAll(eraDef.keystones());
        }

        Map<Identifier, TechDefinition> candidates = new LinkedHashMap<>();
        List<Map.Entry<Identifier, JsonElement>> sortedRaw = new ArrayList<>(rawTechs.entrySet());
        sortedRaw.sort(Map.Entry.comparingByKey());

        for (Map.Entry<Identifier, JsonElement> entry : sortedRaw) {
            Identifier fileId = entry.getKey();
            if (!TTechnology.MOD_ID.equals(fileId.getNamespace())) {
                issues.add(new ValidationIssue(ValidationCode.TECH_NAMESPACE_IGNORED, fileId,
                        "Ignored tc_tech outside tctechnology namespace: " + fileId));
                continue;
            }
            Optional<TechDefinition> parsedOpt = parseSingleTech(fileId, entry.getValue(), false, issues);
            if (parsedOpt.isEmpty()) {
                continue;
            }
            TechDefinition def = parsedOpt.get();
            if (def.disabled()) {
                continue;
            }
            // V2: owner mod must be loaded
            if (!isModLoaded.test(def.owner())) {
                issues.add(new ValidationIssue(ValidationCode.TECH_OWNER_ABSENT, def.id(),
                        "Owner mod not loaded: " + def.owner()));
                continue;
            }
            if (def.keystone()) {
                registeredKeystones.add(def.id());
            }
            candidates.put(def.id(), def);
        }

        // V3-A / 04 §5.8: Resolve fallback keystone stubs when owner is absent
        Map<Identifier, TechDefinition> parsedFallbacks = new LinkedHashMap<>();
        if (enableStubs && rawFallbacks != null) {
            for (Map.Entry<Identifier, JsonElement> fbEntry : rawFallbacks.entrySet()) {
                if (!TTechnology.MOD_ID.equals(fbEntry.getKey().getNamespace())) {
                    continue;
                }
                parseSingleTech(fbEntry.getKey(), fbEntry.getValue(), true, issues)
                        .ifPresent(fb -> parsedFallbacks.put(fb.id(), fb));
            }
        }

        int fallbackCount = 0;
        if (enableStubs) {
            Set<Identifier> neededStubs = new LinkedHashSet<>(registeredKeystones);
            neededStubs.addAll(parsedFallbacks.keySet());
            for (Identifier ksId : neededStubs) {
                if (!candidates.containsKey(ksId) && parsedFallbacks.containsKey(ksId)) {
                    TechDefinition stub = parsedFallbacks.get(ksId);
                    if (stub.originalOwner().isPresent() && isModLoaded.test(stub.originalOwner().get())) {
                        continue;
                    }
                    candidates.put(ksId, stub);
                    registeredKeystones.add(ksId);
                    fallbackCount++;
                }
            }
        }

        // V3, V4, V5 + RP range check
        Map<Identifier, TechDefinition> validated = new LinkedHashMap<>();
        for (TechDefinition def : candidates.values()) {
            if (def.rp() < def.era().minAllowedRp() || def.rp() > def.era().maxAllowedRp()) {
                issues.add(new ValidationIssue(ValidationCode.TECH_COST_RANGE, def.id(),
                        "RP " + def.rp() + " outside era reference interval"));
            }
            List<Identifier> validPrereqs = new ArrayList<>();
            for (Identifier prereqId : def.prerequisites()) {
                TechDefinition prereq = candidates.get(prereqId);
                if (prereq == null) {
                    issues.add(new ValidationIssue(ValidationCode.TECH_PREREQ_MISSING, def.id(),
                            "Missing prerequisite: " + prereqId));
                    continue;
                }
                if (prereq.era().index() > def.era().index()) {
                    issues.add(new ValidationIssue(ValidationCode.TECH_PREREQ_ERA, def.id(),
                            "Prerequisite era " + prereq.era().id() + " is later than " + def.era().id()));
                    continue;
                }
                boolean crossOwner = !prereq.owner().equals(def.owner()) && !def.fallback();
                if (crossOwner && !registeredKeystones.contains(prereqId) && !prereq.keystone()) {
                    issues.add(new ValidationIssue(ValidationCode.TECH_PREREQ_CROSS, def.id(),
                            "Cross-mod prerequisite must be an era keystone: " + prereqId));
                    continue;
                }
                if (!validPrereqs.contains(prereqId)) {
                    validPrereqs.add(prereqId);
                }
            }
            validated.put(def.id(), def.withPrerequisites(validPrereqs));
        }

        // V6: Tarjan Strongly Connected Components (cycle detection)
        Set<Identifier> cyclicNodes = findCyclicNodesTarjan(validated, issues);
        Map<Identifier, TechDefinition> acyclic = new LinkedHashMap<>();
        for (TechDefinition def : validated.values()) {
            if (cyclicNodes.contains(def.id())) {
                continue;
            }
            List<Identifier> cleaned = def.prerequisites().stream()
                    .filter(p -> !cyclicNodes.contains(p))
                    .toList();
            acyclic.put(def.id(), def.withPrerequisites(cleaned));
        }

        // V7: Tree layout calculation
        Map<Identifier, TechNodeLayout> layouts = computeLayouts(acyclic);
        return new TechGraphSnapshot(
                Collections.unmodifiableMap(acyclic),
                Collections.unmodifiableMap(eras),
                Collections.unmodifiableMap(layouts),
                List.copyOf(issues),
                fallbackCount
        );
    }

    private static Optional<TechDefinition> parseSingleTech(
            Identifier fileId,
            JsonElement json,
            boolean forceFallback,
            List<ValidationIssue> issues
    ) {
        String path = fileId.getPath();
        int slash = path.indexOf('/');
        if (slash <= 0 || slash == path.length() - 1 || path.indexOf('/', slash + 1) >= 0) {
            issues.add(new ValidationIssue(ValidationCode.TECH_PARSE, fileId,
                    "Invalid tc_tech path '<era>/<tech>': " + path));
            return Optional.empty();
        }
        String dirEraStr = path.substring(0, slash);
        Optional<TechEra> dirEraOpt = TechEra.byId(dirEraStr);
        if (dirEraOpt.isEmpty()) {
            issues.add(new ValidationIssue(ValidationCode.TECH_PARSE, fileId,
                    "Unknown directory era: " + dirEraStr));
            return Optional.empty();
        }
        if (json == null || !json.isJsonObject()) {
            issues.add(new ValidationIssue(ValidationCode.TECH_PARSE, fileId, "Root JSON must be an object"));
            return Optional.empty();
        }
        DataResult<TechDefinition.RawTechJson> decoded = TechDefinition.RawTechJson.CODEC.parse(JsonOps.INSTANCE, json);
        if (decoded.error().isPresent() || decoded.result().isEmpty()) {
            String errMsg = decoded.error().map(DataResult.Error::message).orElse("Codec decode failed");
            issues.add(new ValidationIssue(ValidationCode.TECH_PARSE, fileId, errMsg));
            return Optional.empty();
        }
        TechDefinition.RawTechJson raw = decoded.result().get();
        Optional<TechEra> jsonEraOpt = TechEra.byId(raw.rawEra());
        if (jsonEraOpt.isEmpty() || jsonEraOpt.get() != dirEraOpt.get()) {
            issues.add(new ValidationIssue(ValidationCode.TECH_PARSE, fileId,
                    "Era mismatch: JSON '" + raw.rawEra() + "' != directory '" + dirEraStr + "'"));
            return Optional.empty();
        }
        Identifier canonicalId = Identifier.fromNamespaceAndPath(TTechnology.MOD_ID, path);
        return Optional.of(new TechDefinition(
                canonicalId,
                raw.owner(),
                dirEraOpt.get(),
                raw.rp(),
                raw.keystone(),
                raw.knowledge().threshold(),
                raw.knowledge().sources(),
                raw.prerequisites(),
                raw.unlocks(),
                raw.tree(),
                raw.icon(),
                raw.diplomatic(),
                raw.fallbackEffect(),
                forceFallback || raw.fallback(),
                raw.disabled(),
                raw.originalOwner()
        ));
    }

    private static Set<Identifier> findCyclicNodesTarjan(
            Map<Identifier, TechDefinition> graph,
            List<ValidationIssue> issues
    ) {
        Map<Identifier, Integer> indexMap = new HashMap<>();
        Map<Identifier, Integer> lowlinkMap = new HashMap<>();
        Deque<Identifier> stack = new ArrayDeque<>();
        Set<Identifier> onStack = new HashSet<>();
        Set<Identifier> cyclic = new LinkedHashSet<>();
        int[] currentIndex = {0};

        for (Identifier node : graph.keySet()) {
            if (!indexMap.containsKey(node)) {
                strongConnect(node, graph, indexMap, lowlinkMap, stack, onStack, cyclic, issues, currentIndex);
            }
        }
        return cyclic;
    }

    private static void strongConnect(
            Identifier v,
            Map<Identifier, TechDefinition> graph,
            Map<Identifier, Integer> indexMap,
            Map<Identifier, Integer> lowlinkMap,
            Deque<Identifier> stack,
            Set<Identifier> onStack,
            Set<Identifier> cyclic,
            List<ValidationIssue> issues,
            int[] currentIndex
    ) {
        indexMap.put(v, currentIndex[0]);
        lowlinkMap.put(v, currentIndex[0]);
        currentIndex[0]++;
        stack.push(v);
        onStack.add(v);

        TechDefinition def = graph.get(v);
        boolean selfLoop = false;
        if (def != null) {
            for (Identifier w : def.prerequisites()) {
                if (w.equals(v)) {
                    selfLoop = true;
                }
                if (!graph.containsKey(w)) {
                    continue;
                }
                if (!indexMap.containsKey(w)) {
                    strongConnect(w, graph, indexMap, lowlinkMap, stack, onStack, cyclic, issues, currentIndex);
                    lowlinkMap.put(v, Math.min(lowlinkMap.get(v), lowlinkMap.get(w)));
                } else if (onStack.contains(w)) {
                    lowlinkMap.put(v, Math.min(lowlinkMap.get(v), indexMap.get(w)));
                }
            }
        }

        if (lowlinkMap.get(v).equals(indexMap.get(v))) {
            List<Identifier> scc = new ArrayList<>();
            Identifier w;
            do {
                w = stack.pop();
                onStack.remove(w);
                scc.add(w);
            } while (!w.equals(v));

            if (scc.size() > 1 || selfLoop) {
                scc.sort(Comparator.naturalOrder());
                cyclic.addAll(scc);
                String cycleSummary = String.join(" -> ", scc.stream().map(Identifier::toString).toList());
                issues.add(new ValidationIssue(ValidationCode.TECH_CYCLE, scc.getFirst(),
                        "Cycle detected and disabled: " + cycleSummary));
            }
        }
    }

    private static Map<Identifier, TechNodeLayout> computeLayouts(Map<Identifier, TechDefinition> acyclic) {
        Map<Identifier, Integer> depthMemo = new HashMap<>();
        for (Identifier id : acyclic.keySet()) {
            computeDepth(id, acyclic, depthMemo);
        }
        Map<String, List<TechDefinition>> buckets = new LinkedHashMap<>();
        for (TechDefinition def : acyclic.values()) {
            int depth = depthMemo.getOrDefault(def.id(), 0);
            String key = def.era().index() + "|" + def.tree().lane() + "|" + depth;
            buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(def);
        }
        Map<Identifier, TechNodeLayout> layouts = new LinkedHashMap<>();
        for (List<TechDefinition> group : buckets.values()) {
            group.sort(Comparator.comparingInt((TechDefinition d) -> d.tree().rowHint())
                    .thenComparing(TechDefinition::id));
            Set<Integer> usedRows = new HashSet<>();
            for (TechDefinition def : group) {
                int row = Math.max(0, def.tree().rowHint());
                while (!usedRows.add(row)) {
                    row++;
                }
                int depth = depthMemo.getOrDefault(def.id(), 0);
                layouts.put(def.id(), new TechNodeLayout(def.era().index(), def.tree().lane(), depth, row));
            }
        }
        return layouts;
    }

    private static int computeDepth(
            Identifier id,
            Map<Identifier, TechDefinition> acyclic,
            Map<Identifier, Integer> memo
    ) {
        Integer cached = memo.get(id);
        if (cached != null) {
            return cached;
        }
        TechDefinition def = acyclic.get(id);
        if (def == null || def.prerequisites().isEmpty()) {
            memo.put(id, 0);
            return 0;
        }
        int maxPrereq = 0;
        for (Identifier p : def.prerequisites()) {
            maxPrereq = Math.max(maxPrereq, computeDepth(p, acyclic, memo) + 1);
        }
        memo.put(id, maxPrereq);
        return maxPrereq;
    }

    private static Map<TechEra, EraDefinition> parseEras(Map<Identifier, JsonElement> rawEras) {
        Map<TechEra, EraDefinition> map = defaultEras();
        if (rawEras == null) {
            return map;
        }
        for (Map.Entry<Identifier, JsonElement> entry : rawEras.entrySet()) {
            if (!TTechnology.MOD_ID.equals(entry.getKey().getNamespace())) {
                continue;
            }
            EraDefinition.CODEC.parse(JsonOps.INSTANCE, entry.getValue()).result().ifPresent(eraDef ->
                    TechEra.byId(entry.getKey().getPath()).ifPresent(te -> map.put(te, eraDef))
            );
        }
        return map;
    }

    private static Map<TechEra, EraDefinition> defaultEras() {
        Map<TechEra, EraDefinition> defaults = new EnumMap<>(TechEra.class);
        int[] popMin = {6, 20, 50, 120, 300, 700, 1500, 3000, 5000, 8000, 12000};
        int[] fbPopMin = {6, 16, 24, 28, 33, 38, 43, 48, 53, 58, 63};
        for (TechEra era : TechEra.values()) {
            int idx = era.index();
            defaults.put(era, new EraDefinition(
                    era.identifier(),
                    idx,
                    List.of(),
                    Identifier.fromNamespaceAndPath(TTechnology.MOD_ID, "milestone/" + era.id()),
                    popMin[idx],
                    fbPopMin[idx],
                    List.of(),
                    Identifier.fromNamespaceAndPath(TTechnology.MOD_ID, "music.era." + era.id()),
                    "tcatlas:skin/" + era.id()
            ));
        }
        return defaults;
    }
}
