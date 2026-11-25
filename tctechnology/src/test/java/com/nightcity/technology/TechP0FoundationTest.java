package com.nightcity.technology;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.nightcity.technology.api.DifficultyApi;
import com.nightcity.technology.api.KnowledgeSourceType;
import com.nightcity.technology.api.TechApi;
import com.nightcity.technology.api.TechEra;
import com.nightcity.technology.api.TechState;
import com.nightcity.technology.data.CivTechState;
import com.nightcity.technology.data.RecipeGateService;
import com.nightcity.technology.data.TechDefinitionLoader;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TechP0FoundationTest {
    private static Path workspaceRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return Files.isDirectory(cwd.resolve("tctechnology")) ? cwd : cwd.getParent();
    }

    private static Map<Identifier, JsonElement> scanAllModuleTechs(Path root) throws IOException {
        Map<Identifier, JsonElement> raw = new LinkedHashMap<>();
        try (Stream<Path> dirs = Files.list(root)) {
            for (Path sub : dirs.filter(Files::isDirectory).toList()) {
                Path techDir = sub.resolve("src/main/resources/data/tctechnology/tc_tech");
                if (!Files.isDirectory(techDir)) {
                    continue;
                }
                try (Stream<Path> walk = Files.walk(techDir)) {
                    for (Path f : walk.filter(p -> p.toString().endsWith(".json")).toList()) {
                        String rel = techDir.relativize(f).toString().replace('\\', '/');
                        String idPath = rel.substring(0, rel.length() - 5);
                        raw.put(Identifier.fromNamespaceAndPath("tctechnology", idPath),
                                JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)));
                    }
                }
            }
        }
        return raw;
    }

    private static Map<Identifier, JsonElement> scanJsonSubdir(Path dir) throws IOException {
        Map<Identifier, JsonElement> raw = new LinkedHashMap<>();
        if (!Files.isDirectory(dir)) {
            return raw;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path f : walk.filter(p -> p.toString().endsWith(".json")).toList()) {
                String rel = dir.relativize(f).toString().replace('\\', '/');
                String idPath = rel.substring(0, rel.length() - 5);
                raw.put(Identifier.fromNamespaceAndPath("tctechnology", idPath),
                        JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)));
            }
        }
        return raw;
    }

    @Test
    void all370PlaceholderTechsAnd11ErasLoadWithZeroErrors() throws IOException {
        Path root = workspaceRoot();
        Map<Identifier, JsonElement> techs = scanAllModuleTechs(root);
        Map<Identifier, JsonElement> eras = scanJsonSubdir(root.resolve("tctechnology/src/main/resources/data/tctechnology/tc_era"));
        Map<Identifier, JsonElement> fallbacks = scanJsonSubdir(root.resolve("tctechnology/src/main/resources/data/tctechnology/tc_tech_fallback"));

        assertTrue(techs.size() >= 360, "Expected >= 360 placeholder techs across modules, found " + techs.size());
        assertEquals(11, eras.size(), "Expected 11 era definitions");
        assertEquals(41, fallbacks.size(), "Expected 41 fallback keystone stubs");

        TechDefinitionLoader.TechGraphSnapshot snap = TechDefinitionLoader.compile(techs, eras, fallbacks, m -> true, true);
        assertTrue(snap.selfTestPassed(), () -> "Tech selftest failed with issues: " + snap.issues());
        assertEquals(0, snap.blockingErrorCount());
        assertEquals(0, snap.fallbackCount(), "When all mods are loaded, 0 fallback stubs should be needed");
        assertEquals(techs.size(), snap.definitions().size());
        assertEquals(techs.size(), snap.layouts().size());
    }

    @Test
    void missingThemeModsInjectFallbackKeystonesAndKeepAll11ErasValid() throws IOException {
        Path root = workspaceRoot();
        Map<Identifier, JsonElement> techs = scanAllModuleTechs(root);
        Map<Identifier, JsonElement> eras = scanJsonSubdir(root.resolve("tctechnology/src/main/resources/data/tctechnology/tc_era"));
        Map<Identifier, JsonElement> fallbacks = scanJsonSubdir(root.resolve("tctechnology/src/main/resources/data/tctechnology/tc_tech_fallback"));

        // Simulate only tcivilization + tctechnology loaded (all 15 theme mods absent!)
        Set<String> loadedOnlyCore = Set.of("tcivilization", "tctechnology");
        TechDefinitionLoader.TechGraphSnapshot snap = TechDefinitionLoader.compile(
                techs, eras, fallbacks, loadedOnlyCore::contains, true
        );
        assertTrue(snap.selfTestPassed(), () -> "Core-only fallback graph failed validation: " + snap.issues());
        assertEquals(41, snap.fallbackCount(), "All 41 non-core keystones should resolve to fallback stubs");

        for (TechEra era : TechEra.values()) {
            var eraDef = snap.eras().get(era);
            assertNotNull(eraDef);
            for (Identifier ks : eraDef.keystones()) {
                assertTrue(snap.definitions().containsKey(ks),
                        "Keystone " + ks + " in era " + era.id() + " must remain present via fallback stub");
            }
        }
    }

    @Test
    void tarjanCycleDetectionAndValidationErrorCodesWork() {
        Map<Identifier, JsonElement> raw = new LinkedHashMap<>();
        raw.put(Identifier.fromNamespaceAndPath("tctechnology", "stone/valid_root"), JsonParser.parseString("""
                {"owner":"tctechnology","era":"stone","rp":20,"keystone":true,
                 "knowledge":{"threshold":10,"sources":[]},"prerequisites":[]}
                """));
        raw.put(Identifier.fromNamespaceAndPath("tctechnology", "stone/cycle_a"), JsonParser.parseString("""
                {"owner":"tctechnology","era":"stone","rp":20,
                 "knowledge":{"threshold":10,"sources":[]},"prerequisites":["tctechnology:stone/cycle_b"]}
                """));
        raw.put(Identifier.fromNamespaceAndPath("tctechnology", "stone/cycle_b"), JsonParser.parseString("""
                {"owner":"tctechnology","era":"stone","rp":20,
                 "knowledge":{"threshold":10,"sources":[]},"prerequisites":["tctechnology:stone/cycle_a"]}
                """));
        raw.put(Identifier.fromNamespaceAndPath("tctechnology", "stone/bad_era"), JsonParser.parseString("""
                {"owner":"tctechnology","era":"bronze","rp":150,
                 "knowledge":{"threshold":45,"sources":[]},"prerequisites":[]}
                """));
        raw.put(Identifier.fromNamespaceAndPath("tctechnology", "stone/bad_edges"), JsonParser.parseString("""
                {"owner":"tctechnology","era":"stone","rp":20,"keystone":false,
                 "knowledge":{"threshold":10,"sources":[]},
                 "prerequisites":["tctechnology:stone/nonexistent","tctechnology:bronze/future_tech"]}
                """));
        raw.put(Identifier.fromNamespaceAndPath("tctechnology", "bronze/future_tech"), JsonParser.parseString("""
                {"owner":"tctechnology","era":"bronze","rp":150,"keystone":false,
                 "knowledge":{"threshold":45,"sources":[]},"prerequisites":[]}
                """));
        raw.put(Identifier.fromNamespaceAndPath("tctechnology", "bronze/cross_bad"), JsonParser.parseString("""
                {"owner":"tcmetallurgy","era":"bronze","rp":150,
                 "knowledge":{"threshold":45,"sources":[]},"prerequisites":["tctechnology:bronze/future_tech"]}
                """));

        TechDefinitionLoader.TechGraphSnapshot snap = TechDefinitionLoader.compile(raw, Map.of(), Map.of(), m -> true, false);
        var counts = snap.issueCounts();
        assertEquals(1, counts.get(TechDefinitionLoader.ValidationCode.TECH_PARSE));
        assertEquals(1, counts.get(TechDefinitionLoader.ValidationCode.TECH_CYCLE));
        assertEquals(1, counts.get(TechDefinitionLoader.ValidationCode.TECH_PREREQ_MISSING));
        assertEquals(1, counts.get(TechDefinitionLoader.ValidationCode.TECH_PREREQ_ERA));
        assertEquals(1, counts.get(TechDefinitionLoader.ValidationCode.TECH_PREREQ_CROSS));
        assertFalse(snap.definitions().containsKey(Identifier.fromNamespaceAndPath("tctechnology", "stone/cycle_a")));
        assertFalse(snap.definitions().containsKey(Identifier.fromNamespaceAndPath("tctechnology", "stone/cycle_b")));
        assertTrue(snap.definitions().containsKey(Identifier.fromNamespaceAndPath("tctechnology", "stone/valid_root")));
    }

    @Test
    void civStateLifecycleSavedDataMigrationAndRecipeGatesWork() throws IOException {
        Path root = workspaceRoot();
        Map<Identifier, JsonElement> gateJson = scanJsonSubdir(root.resolve("tctechnology/src/main/resources/data/tctechnology/tc_recipe_gate"));
        RecipeGateService gates = new RecipeGateService();
        gates.loadFromJsonMap(gateJson);

        UUID civId = TechApi.PLAYER_CIV_ID;
        CivTechState state = new CivTechState(civId);
        Identifier smeltingStub = Identifier.fromNamespaceAndPath("tctechnology", "bronze/copper_smelting");
        Identifier ironPick = Identifier.withDefaultNamespace("iron_pickaxe");

        // Vanilla iron_pickaxe is removed when tcindustry is loaded, or gated behind iron/bloomery fallback when absent
        assertFalse(gates.isRecipeUnlocked(civId, ironPick, (c, t) -> state.isUnlocked(t), "tcindustry"::equals, true, true));
        Identifier ironBloomery = Identifier.fromNamespaceAndPath("tctechnology", "iron/bloomery");
        state.completeTech(ironBloomery);
        assertTrue(gates.isRecipeUnlocked(civId, ironPick, (c, t) -> state.isUnlocked(t), m -> false, true, true));
        assertFalse(gates.isRecipeUnlocked(civId, ironPick, (c, t) -> state.isUnlocked(t), "tcindustry"::equals, true, true));

        // State machine & Pool 50% cap
        Identifier stoneTest = Identifier.fromNamespaceAndPath("tctechnology", "stone/fire_test");
        Map<Identifier, JsonElement> raw = Map.of(
                smeltingStub, JsonParser.parseString("""
                        {"owner":"tctechnology","era":"bronze","rp":20,"keystone":true,
                         "knowledge":{"threshold":10,"sources":[]},"prerequisites":[]}
                        """),
                stoneTest, JsonParser.parseString("""
                        {"owner":"tctechnology","era":"stone","rp":20,"keystone":true,
                         "knowledge":{"threshold":10,"sources":[]},"prerequisites":[]}
                        """)
        );
        var graph = TechDefinitionLoader.compile(raw, Map.of(), Map.of(), m -> true, false).definitions();
        assertEquals(TechState.HIDDEN, state.stateOf(graph.get(smeltingStub), graph, 3));
        var def = graph.get(stoneTest);
        assertEquals(TechState.UNDISCOVERED, state.stateOf(def, graph, 3));
        state.addKnowledge(def, KnowledgeSourceType.PRACTICE, 5, 1.0, DifficultyApi.Difficulty.fromPreset("standard"), 0.5, 1.0, 3.0);
        assertEquals(TechState.KNOWN, state.stateOf(def, graph, 3));
        var gain2 = state.addKnowledge(def, KnowledgeSourceType.PRACTICE, 5, 1.0, DifficultyApi.Difficulty.fromPreset("standard"), 0.5, 1.0, 3.0);
        assertTrue(gain2.thresholdJustReached());
        assertEquals(TechState.RESEARCHABLE, state.stateOf(def, graph, 3));

        state.addPoolRp(50, 3.0);
        int spent = state.investFromPool(def, 20, 20, 0.50, graph, 3);
        assertEquals(10, spent, "Pool investment must cap at 50% of cost");
        assertTrue(state.contributeSiteRp(def, 10, 20, graph, 3, 3.0));
        assertEquals(TechState.COMPLETED, state.stateOf(def, graph, 3));

        // Legacy v0 SavedData migration check
        JsonElement legacyJson = JsonParser.parseString("{\"research_points\": 25}");
        TechnologySavedData migrated = TechnologySavedData.CODEC.parse(JsonOps.INSTANCE, legacyJson).getOrThrow();
        assertEquals(1, migrated.formatVersion());
        assertEquals(25, migrated.researchPoints());
    }
}
