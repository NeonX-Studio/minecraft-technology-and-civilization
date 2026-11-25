package com.nightcity.nations;

import com.nightcity.nations.api.EconomyCategory;
import com.nightcity.nations.api.NationState;
import com.nightcity.nations.api.NationsApi;
import com.nightcity.nations.compat.tcechoes.TcechoesBridge;
import com.nightcity.nations.compat.tcwildlands.TcwildlandsBridge;
import com.nightcity.nations.data.InfluenceLayerData;
import com.nightcity.nations.data.NationsInfluenceLayer;
import com.nightcity.nations.data.NationsSavedData;
import com.nightcity.nations.seed.EchoesHistorySnapshot;
import com.nightcity.nations.seed.NationSeeder;
import com.nightcity.nations.vanilla.NationsVanillaGuard;
import com.nightcity.tcivilization.CivilizationSavedData;
import com.nightcity.tcivilization.RegionCoordinates;
import com.nightcity.tcivilization.api.CivKind;
import com.nightcity.tcivilization.api.Civilization;
import com.nightcity.tcivilization.api.MonsterPolicy;
import com.nightcity.tcivilization.api.RegionGridRegistry;
import com.nightcity.tcivilization.api.RegionHandle;
import com.nightcity.tcivilization.api.TcMigrators;
import com.nightcity.tcivilization.api.TcThreads;
import com.nightcity.tcivilization.api.VanillaPolicyService;
import com.nightcity.tcivilization.api.VanillaSystem;
import com.nightcity.tcivilization.region.RegionLayerRegistry;
import com.nightcity.tcivilization.region.RegionManager;
import com.nightcity.tcivilization.sim.TcEventBus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class NationsP1SeedingAndMigrationTest {
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        TcThreads.bindMainThread(Thread.currentThread());
        RegionLayerRegistry.global().resetForTests();
        RegionLayerRegistry.global().register(NationsInfluenceLayer.INSTANCE);
        ((VanillaPolicyService.Impl) VanillaPolicyService.get()).clearForTests();
        NationsVanillaGuard.resetCountersForTests();
        NationsConfig.setTestOverrides(null, null);
        TcechoesBridge.setTestSnapshotOverride(null);
        TcwildlandsBridge.setTestSamplerOverride(null);
    }

    @AfterEach
    void tearDown() {
        TcechoesBridge.setTestSnapshotOverride(null);
        TcwildlandsBridge.setTestSamplerOverride(null);
        NationsConfig.setTestOverrides(null, null);
        NationsApi.install(null);
        TcThreads.bindMainThread(null);
    }

    @Test
    void fallbackSeedingIsDeterministicRespectsBreathingRadiusAndWritesInfluenceLayer() throws IOException {
        long seed = 20261123L;
        BlockPos spawnPos = new BlockPos(256, 64, 256);
        BlockPos playerSecondCore = new BlockPos(5_500, 64, -1_200);
        int playerEra = 2; // R_breath = 1500 + 250 * 2 = 2000
        NationsConfig.SeedingSettings settings = NationsConfig.SeedingSettings.defaults();
        int expectedBreath = settings.breathingRadiusForPlayerEra(playerEra);
        assertEquals(2000, expectedBreath);

        NationSeeder.HabitabilityOracle oracle = NationSeeder.proceduralOracle(seed);
        NationSeeder.SeedPlan planA = NationSeeder.computePlan(
                seed, spawnPos, playerEra, List.of(playerSecondCore), settings, EchoesHistorySnapshot.unloaded(), oracle
        );
        NationSeeder.SeedPlan planB = NationSeeder.computePlan(
                seed, spawnPos, playerEra, List.of(playerSecondCore), settings, EchoesHistorySnapshot.unloaded(), oracle
        );

        assertEquals("fallback", planA.source());
        assertEquals(8, planA.blueprints().size(), "Default ai_civ_count = 8 must produce 8 AI civilizations");
        assertEquals(planA, planB, "Two seeding runs with the same worldSeed must produce identical blueprints");

        boolean foundPeacefulNeighbour = false;
        for (int i = 0; i < planA.blueprints().size(); i++) {
            NationSeeder.NationBlueprint bp = planA.blueprints().get(i);
            double dSpawn = NationSeeder.wrappedHorizontalDistance(bp.capitalPos(), spawnPos);
            double dSecondCore = NationSeeder.wrappedHorizontalDistance(bp.capitalPos(), playerSecondCore);
            assertTrue(dSpawn >= settings.spawnExclusionRadius(),
                    "AI capital must respect spawn_exclusion_radius (2500m), got " + dSpawn);
            assertTrue(dSecondCore >= expectedBreath,
                    "AI capital must respect player settlement breathing radius (2000m), got " + dSecondCore);
            assertTrue(bp.habitability() > 0.05D, "AI capital must never be placed in ocean");

            if (dSpawn >= 2500.0D && dSpawn <= 4000.0D && bp.personality().aggression() <= 35) {
                foundPeacefulNeighbour = true;
            }
            for (int j = i + 1; j < planA.blueprints().size(); j++) {
                double dPair = NationSeeder.wrappedHorizontalDistance(bp.capitalPos(), planA.blueprints().get(j).capitalPos());
                assertTrue(dPair >= settings.capitalMinSpacing() * 0.512D, "AI capitals must maintain minimum spacing");
            }
        }
        assertTrue(foundPeacefulNeighbour, "First guaranteed neighbour in [2500, 4000] ring must have aggression <= 35");

        CivilizationSavedData civData = new CivilizationSavedData(new TcEventBus(e -> {}));
        NationsSavedData nationsData = new NationsSavedData();
        Path worldDir = tempDir.resolve("world_seed_test");
        Files.createDirectories(worldDir);

        try (RegionManager regions = new RegionManager(
                worldDir, 1, 128, RegionGridRegistry.defaults(), RegionLayerRegistry.global(), new TcEventBus(e -> {}), () -> 100L
        )) {
            NationSeeder.SeedResult applied = NationSeeder.applyPlan(
                    planA, civData, regions, nationsData, spawnPos, List.of(playerSecondCore), 100L, false
            );
            regions.flushAllSync();

            assertEquals(8, applied.nations().size());
            for (NationState state : applied.nations()) {
                Optional<Civilization> coreCiv = civData.civ(state.civId());
                assertTrue(coreCiv.isPresent(), "CivilizationApi.createAiCivilization must register AI civ");
                assertEquals(CivKind.AI, coreCiv.get().kind());
                assertEquals(1, state.settlements().size());

                int capRx = (int) (state.capitalRegionKey() >> 32);
                int capRz = (int) state.capitalRegionKey();
                RegionHandle capHandle = regions.get(NationSeeder.OVERWORLD_DIM, capRx, capRz).orElseThrow();
                InfluenceLayerData capLayer = regions.layer(capHandle, NationsInfluenceLayer.INSTANCE);
                assertTrue(capLayer.influenceOf(state.civId()) >= 200, "Capital region influence must be >= 200");
                assertEquals(Optional.of(state.civId()), capLayer.ownerCivId());
            }

            // Verify breathing zone regions cap AI influence at <= 199 and can never be owned by AI
            for (Map.Entry<Long, InfluenceLayerData> e : applied.influences().entrySet()) {
                if (e.getValue().breathingZoneRestricted()) {
                    assertTrue(e.getValue().topEntries().getFirst().influence() <= InfluenceLayerData.BREATHING_ZONE_AI_CAP);
                    assertTrue(e.getValue().ownerCivId().isEmpty(), "AI must never own a region inside player breathing radius");
                }
            }

            List<String> statusLines = TNationsEvents.buildStatusLines(nationsData, playerEra, 0, 0);
            assertTrue(statusLines.getFirst().contains("active=8"));
            assertEquals(9, statusLines.size());
        }
    }

    @Test
    void echoesSoftBridgeSeedsSurvivorsSkipsExtinctAndPreservesHistoricRelations() {
        Map<Long, EchoesHistorySnapshot.HistoricRelation> rels = Map.of(
                EchoesHistorySnapshot.pairKey(10, 20), EchoesHistorySnapshot.HistoricRelation.FEUD,
                EchoesHistorySnapshot.pairKey(10, 30), EchoesHistorySnapshot.HistoricRelation.ALLIANCE
        );
        EchoesHistorySnapshot snap = new EchoesHistorySnapshot(true, List.of(
                new EchoesHistorySnapshot.PolityCandidate(10, "Valoria", Identifier.fromNamespaceAndPath("tcechoes", "river"), 12, 0, 4, 920.0, true, List.of(RegionCoordinates.key(12, 0))),
                new EchoesHistorySnapshot.PolityCandidate(99, "Fallen Ur", Identifier.fromNamespaceAndPath("tcechoes", "desert"), 20, 5, 5, 5000.0, false, List.of()),
                new EchoesHistorySnapshot.PolityCandidate(20, "Khorgard", Identifier.fromNamespaceAndPath("tcechoes", "steppe"), -14, 4, 2, 640.0, true, List.of()),
                new EchoesHistorySnapshot.PolityCandidate(30, "Thalassia", Identifier.fromNamespaceAndPath("tcechoes", "coast"), 0, -16, 1, 410.0, true, List.of())
        ), rels);

        TcechoesBridge.setTestSnapshotOverride(snap);
        TcwildlandsBridge.setTestSamplerOverride((rx, rz) -> OptionalDouble.of(0.72D));

        BlockPos spawn = new BlockPos(0, 64, 0);
        NationsConfig.SeedingSettings settings = NationsConfig.SeedingSettings.defaults();
        NationSeeder.SeedPlan plan = NationSeeder.computePlan(
                777L, spawn, 0, List.of(), settings, TcechoesBridge.readHistorySnapshot(), NationSeeder.proceduralOracle(777L)
        );

        assertEquals("echoes+fallback", plan.source());
        assertEquals(8, plan.blueprints().size());
        assertFalse(plan.blueprints().stream().anyMatch(b -> "Fallen Ur".equals(b.name())),
                "Extinct Echoes polities must never become active AI civilizations");
        assertEquals("Valoria", plan.blueprints().get(0).name());
        assertEquals("Khorgard", plan.blueprints().get(1).name());
        assertEquals("Thalassia", plan.blueprints().get(2).name());
        assertTrue(plan.blueprints().get(0).eraIndex() <= 1, "Starting era lead must be truncated to playerEra + start_era_lead_max");

        CivilizationSavedData civData = new CivilizationSavedData(new TcEventBus(e -> {}));
        NationsSavedData savedData = new NationsSavedData();
        NationSeeder.applyPlan(plan, civData, null, savedData, spawn, List.of(), 0L, false);
        NationsApi.install(savedData);

        UUID valoriaId = savedData.allNations().get(0).civId();
        UUID khorgardId = savedData.allNations().get(1).civId();
        UUID thalassiaId = savedData.allNations().get(2).civId();

        assertTrue(NationsApi.get().relation(valoriaId, khorgardId).opinion() <= -30.0D, "Historic feud must apply negative opinion");
        assertTrue(NationsApi.get().relation(valoriaId, thalassiaId).opinion() >= 25.0D, "Historic alliance must apply positive opinion");
    }

    @Test
    void nationsSavedDataFormatVersion1MigrationAndCivsDatRoundTrip() throws IOException {
        CivilizationSavedData civData = new CivilizationSavedData(new TcEventBus(e -> {}));
        NationsSavedData initial = new NationsSavedData();
        BlockPos spawn = new BlockPos(0, 64, 0);
        NationSeeder.SeedPlan plan = NationSeeder.computePlan(
                991122L, spawn, 0, List.of(), NationsConfig.SeedingSettings.defaults(),
                EchoesHistorySnapshot.unloaded(), NationSeeder.proceduralOracle(991122L)
        );
        NationSeeder.applyPlan(plan, civData, null, initial, spawn, List.of(), 2400L, false);
        initial.recordGameruleWrite("minecraft:raids", false);

        // 1. Test legacy v0 migration (missing formatVersion, using legacy ai_civs/seed/source keys)
        CompoundTag v1Encoded = initial.toCompoundTag();
        assertEquals(NationsSavedData.FORMAT_VERSION, v1Encoded.getIntOr(TcMigrators.FORMAT_VERSION_KEY, 0));

        CompoundTag legacyV0 = new CompoundTag();
        legacyV0.putBoolean("seeded", true);
        legacyV0.putLong("seed", 991122L);
        legacyV0.putString("source", "fallback");
        legacyV0.put("ai_civs", v1Encoded.getListOrEmpty("nations").copy());
        legacyV0.put("relations", new ListTag());

        NationsSavedData migratedFromV0 = NationsSavedData.fromCompoundTag(legacyV0);
        assertTrue(migratedFromV0.isSeeded());
        assertEquals(991122L, migratedFromV0.worldSeed());
        assertEquals("fallback", migratedFromV0.seedSource());
        assertEquals(8, migratedFromV0.activeNations().size());

        // 2. Test v1 SavedData + tc/nations/civs.dat disk round-trip (EC-P1-8)
        Path worldRoot = tempDir.resolve("save_roundtrip");
        initial.saveToDirectory(worldRoot);
        assertTrue(Files.isRegularFile(NationsSavedData.civsFilePath(worldRoot)));
        assertTrue(Files.isRegularFile(NationsSavedData.relationsFilePath(worldRoot)));

        NationsSavedData reloaded = NationsSavedData.loadFromDirectory(worldRoot);
        assertEquals(initial.isSeeded(), reloaded.isSeeded());
        assertEquals(initial.worldSeed(), reloaded.worldSeed());
        assertEquals(initial.seedSource(), reloaded.seedSource());
        assertEquals(initial.allNations(), reloaded.allNations());
        assertEquals(initial.ownedRegionCount(), reloaded.ownedRegionCount());
        assertEquals(Optional.of(false), reloaded.recordedGameruleValue("minecraft:raids"));

        NationState first = reloaded.allNations().getFirst();
        assertTrue(first.stock().get(EconomyCategory.FOOD) > 0.0D);
        assertEquals(NationsSavedData.FORMAT_VERSION, reloaded.toCompoundTag().getIntOr("formatVersion", 0));
    }

    @Test
    void vanillaGuardCoordinatesOwnershipChainForPatrolsAndPlanetVillages() {
        VanillaPolicyService policy = VanillaPolicyService.get();
        NationsVanillaGuard.registerPolicyFallbacks();
        assertEquals(Optional.of("tcnations"), policy.owner(VanillaSystem.PATROLS));
        assertEquals(Optional.of("tcnations"), policy.owner(VanillaSystem.RAIDS));

        assertTrue(NationsVanillaGuard.shouldCancelPatrol(
                EntitySpawnReason.PATROL, true, true, MonsterPolicy.RUINS_ONLY, policy
        ));
        assertFalse(NationsVanillaGuard.isStructureAllowed(Identifier.withDefaultNamespace("villages"), true, policy));
        assertTrue(NationsVanillaGuard.isStructureAllowed(Identifier.withDefaultNamespace("villages"), false, policy),
                "Non-planet mode without Settlements must preserve vanilla villages");
        assertTrue(NationsVanillaGuard.shouldCancelVanillaVillageEntity(
                Identifier.withDefaultNamespace("villager"), EntitySpawnReason.STRUCTURE, true, policy
        ));

        // Higher-priority owners (tcfrontiers=100, tcsettlements=100) cleanly supersede tcnations fallback (70)
        policy.registerOwner(VanillaSystem.PATROLS, "tcfrontiers", 100);
        policy.registerOwner(VanillaSystem.VILLAGERS, "tcsettlements", 100);
        assertEquals(Optional.of("tcfrontiers"), policy.owner(VanillaSystem.PATROLS));
        assertFalse(NationsVanillaGuard.shouldCancelPatrol(
                EntitySpawnReason.PATROL, true, true, MonsterPolicy.RUINS_ONLY, policy
        ), "Nations fallback must defer once Frontiers owns PATROLS");
        assertFalse(NationsVanillaGuard.isStructureAllowed(Identifier.withDefaultNamespace("villages"), false, policy),
                "Settlements owner suppresses vanilla villages even in non-planet mode");
    }

    @Test
    void tcechoesBridgeInitDoesNotRegisterAbstractBaseEventOnBus() {
        assertDoesNotThrow(TcechoesBridge::init);
        assertTrue(TcechoesBridge.isInitialized());
    }
}
