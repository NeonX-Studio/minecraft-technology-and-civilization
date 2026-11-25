package com.nightcity.ecology;

import com.nightcity.ecology.compat.tcwildlands.TcwildlandsBridge;
import com.nightcity.ecology.spawn.BiomeWildlifeTable;
import com.nightcity.ecology.spawn.EcologySpawnManager;
import com.nightcity.tcivilization.api.MonsterPolicy;
import com.nightcity.tcivilization.api.VanillaPolicyService;
import com.nightcity.tcivilization.api.VanillaSystem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MobCategory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EcologyP1WildlifeAndSpawnAuditTest {
    @BeforeEach
    void setUp() {
        ((VanillaPolicyService.Impl) VanillaPolicyService.get()).clearForTests();
        EcologyConfig.setTestOverrides(null, null);
        TcwildlandsBridge.setTestOracleOverride(null);
        EcologySpawnManager.resetCountersForTests();
    }

    @AfterEach
    void tearDown() {
        EcologyConfig.setTestOverrides(null, null);
        TcwildlandsBridge.setTestOracleOverride(null);
        ((VanillaPolicyService.Impl) VanillaPolicyService.get()).clearForTests();
    }

    @Test
    void replacesOverworldWildlifeByClimateEnvelopeRegistersPolicyAndPreservesMonsterCategory() throws Exception {
        var redDeerUrl = BiomeWildlifeTable.class.getResource("/data/tcecology/tcecology/species/red_deer.json");
        assertNotNull(redDeerUrl, "Packaged species JSON resource red_deer.json must exist on classpath");
        Path speciesDir = Path.of(redDeerUrl.toURI()).getParent();
        assertTrue(Files.isDirectory(speciesDir), "Species data directory must exist");
        BiomeWildlifeTable.reloadFromDirectory(speciesDir);

        VanillaPolicyService policy = VanillaPolicyService.get();
        EcologySpawnManager.registerVanillaPolicies();
        assertEquals(Optional.of("tcecology"), policy.owner(VanillaSystem.WILDLIFE_SPAWNS));
        assertTrue(EcologySpawnManager.isWildlifeOwnerActive(policy));

        // Config toggle disabling wildlife replacement
        EcologyConfig.setTestOverrides(false, true);
        assertFalse(EcologySpawnManager.isWildlifeOwnerActive(policy));
        EcologyConfig.setTestOverrides(true, true);
        assertTrue(EcologySpawnManager.isWildlifeOwnerActive(policy));

        // 1. Polar tundra: cold-adapted species only
        var tundraProfile = BiomeWildlifeTable.classifyBiome(
                Identifier.fromNamespaceAndPath("tcwildlands", "polar_tundra"), -0.35f, 0.22f
        );
        Set<String> tundraSpecies = speciesPaths(BiomeWildlifeTable.resolveSpawns(tundraProfile, true));
        assertTrue(tundraSpecies.contains("reindeer"));
        assertTrue(tundraSpecies.contains("polar_bear"));
        assertFalse(tundraSpecies.contains("red_junglefowl"));
        assertFalse(tundraSpecies.contains("camel"));
        assertFalse(tundraSpecies.contains("tropical_fish"));

        // 2. Temperate broadleaf forest: deer, boar, aurochs, wolf/fox
        var forestProfile = BiomeWildlifeTable.classifyBiome(
                Identifier.fromNamespaceAndPath("tcwildlands", "temperate_broadleaf_forest"), 0.62f, 0.55f
        );
        Set<String> forestSpecies = speciesPaths(BiomeWildlifeTable.resolveSpawns(forestProfile, true));
        assertTrue(forestSpecies.contains("red_deer"));
        assertTrue(forestSpecies.contains("wild_boar"));
        assertTrue(forestSpecies.contains("aurochs"));
        assertFalse(forestSpecies.contains("polar_bear"));
        assertFalse(forestSpecies.contains("camel"));

        // 3. Hot desert: camel & wild_ass, zero pigs/cows/polar bears
        var desertProfile = BiomeWildlifeTable.classifyBiome(
                Identifier.fromNamespaceAndPath("tcwildlands", "hot_sand_desert"), 1.3f, 0.05f
        );
        Set<String> desertSpecies = speciesPaths(BiomeWildlifeTable.resolveSpawns(desertProfile, true));
        assertTrue(desertSpecies.contains("camel"));
        assertTrue(desertSpecies.contains("wild_ass"));
        assertFalse(desertSpecies.contains("wild_boar"));
        assertFalse(desertSpecies.contains("aurochs"));

        // 4. Tropical rainforest: red junglefowl, parrot, ocelot
        var jungleProfile = BiomeWildlifeTable.classifyBiome(
                Identifier.fromNamespaceAndPath("tcwildlands", "tropical_rainforest"), 1.0f, 1.25f
        );
        Set<String> jungleSpecies = speciesPaths(BiomeWildlifeTable.resolveSpawns(jungleProfile, true));
        assertTrue(jungleSpecies.contains("red_junglefowl"));
        assertTrue(jungleSpecies.contains("parrot"));
        assertTrue(jungleSpecies.contains("ocelot"));
        assertFalse(jungleSpecies.contains("polar_bear"));
        assertFalse(jungleSpecies.contains("reindeer"));

        // 5. Marine contrast: coral reef vs polar sea
        var reefProfile = BiomeWildlifeTable.classifyBiome(
                Identifier.fromNamespaceAndPath("tcwildlands", "tropical_coral_reef"), 1.0f, 0.8f
        );
        Set<String> reefSpecies = speciesPaths(BiomeWildlifeTable.resolveSpawns(reefProfile, true));
        assertTrue(reefSpecies.contains("tropical_fish"));
        assertTrue(reefSpecies.contains("pufferfish"));
        assertTrue(reefSpecies.contains("dolphin"));

        // 6. Non-Overworld & Deep Earth biomes remain untouched, and MONSTER category is never produced
        assertFalse(BiomeWildlifeTable.isTargetOverworldBiome(Identifier.withDefaultNamespace("nether_wastes")));
        assertFalse(BiomeWildlifeTable.isTargetOverworldBiome(Identifier.withDefaultNamespace("the_end")));
        assertFalse(BiomeWildlifeTable.isTargetOverworldBiome(Identifier.fromNamespaceAndPath("tcwildlands", "deep_crust")));
        assertFalse(BiomeWildlifeTable.isWildlifeCategory(MobCategory.MONSTER));
        assertTrue(BiomeWildlifeTable.allSpecies().stream().noneMatch(s -> s.mobCategory() == MobCategory.MONSTER));
    }

    @Test
    void hundredMcDaySimulationLogsZeroVanillaVillagesAndZeroOutOfZoneHostiles() {
        VanillaPolicyService policy = VanillaPolicyService.get();
        policy.registerOwner(VanillaSystem.WORLDGEN, "tcwildlands", 100);
        EcologySpawnManager.registerVanillaPolicies();

        assertTrue(policy.isPlanetMode(null));

        // Verify direct RUINS_ONLY rule across surface AND underground caves (-512..1024)
        assertFalse(EcologySpawnManager.isHostileSpawnAllowed(MonsterPolicy.RUINS_ONLY, EntitySpawnReason.NATURAL, false, false));
        assertFalse(EcologySpawnManager.isHostileSpawnAllowed(MonsterPolicy.RUINS_ONLY, EntitySpawnReason.CHUNK_GENERATION, false, false));
        assertFalse(EcologySpawnManager.isHostileSpawnAllowed(MonsterPolicy.RUINS_ONLY, EntitySpawnReason.PATROL, true, false));
        assertTrue(EcologySpawnManager.isHostileSpawnAllowed(MonsterPolicy.RUINS_ONLY, EntitySpawnReason.NATURAL, true, false),
                "Natural spawns inside #tcivilization:monster_ruins (e.g. trial_chambers) must be allowed");
        assertTrue(EcologySpawnManager.isHostileSpawnAllowed(MonsterPolicy.RUINS_ONLY, EntitySpawnReason.SPAWNER, false, false),
                "Dungeon spawners must remain functional under RUINS_ONLY");
        assertTrue(EcologySpawnManager.isHostileSpawnAllowed(MonsterPolicy.RUINS_ONLY, EntitySpawnReason.TRIAL_SPAWNER, false, false));
        assertTrue(EcologySpawnManager.isHostileSpawnAllowed(MonsterPolicy.RUINS_ONLY, EntitySpawnReason.NATURAL, false, true),
                "Entities in #tcivilization:monster_policy/exempt must remain allowed");

        // Simulate 100 full MC days (2,400,000 ticks) of generation & spawn logs (EC-P1-7)
        EcologySpawnManager.AuditReport report = EcologySpawnManager.simulateSpawnAuditDays(
                20260301L,
                100,
                policy.isPlanetMode(null),
                MonsterPolicy.RUINS_ONLY,
                true
        );

        assertEquals(100, report.simulatedMcDays());
        assertEquals(2_400_000L, report.totalTicks());
        assertEquals(100, report.dailyLogSummary().size());

        // Core EC-P1-7 exit assertions
        assertEquals(0L, report.vanillaVillagesGenerated(),
                "EC-P1-7: Vanilla villages generated across 100 MC days must be strictly 0");
        assertEquals(0L, report.outOfZoneHostileSpawns(),
                "EC-P1-7: Out-of-allowed-zone vanilla hostile spawns across 100 MC days must be strictly 0");
        assertEquals(0L, report.outOfEnvelopeVanillaWildlifeSpawns(),
                "Out-of-envelope vanilla wildlife spawns must be 0 when Ecology wildlife replacement is active");

        // Confirm active filtering & legitimate sources occurred during the 100-day run
        assertTrue(report.suppressedVillageAttempts() > 0L);
        assertTrue(report.blockedOutOfZoneHostileAttempts() > 0L);
        assertTrue(report.blockedPatrolAttempts() > 0L);
        assertTrue(report.blockedPhantomAttempts() > 0L);
        assertTrue(report.allowedRuinHostileSpawns() > 0L, "Hostile ruins / trial chambers must still spawn monsters");
        assertTrue(report.allowedSpawnerHostileSpawns() > 0L, "Spawners must still spawn monsters");
        assertTrue(report.ecologyWildlifeSpawns() > 0L, "Ecology biome envelope wildlife must populate Overworld biomes");
    }

    @Test
    void biomeModifierHandlesBiomesWithUndefinedSpawnerCategories() {
        var emptyBuilder = new net.neoforged.neoforge.common.world.MobSpawnSettingsBuilder(
                net.minecraft.world.level.biome.MobSpawnSettings.EMPTY
        );
        Identifier biomeId = Identifier.fromNamespaceAndPath("tcwildlands", "temperate_broadleaf_forest");
        assertDoesNotThrow(() -> com.nightcity.ecology.spawn.ReplaceVanillaWildlifeBiomeModifier.cacheOriginalSpawnsIfAbsent(biomeId, emptyBuilder));
        assertTrue(com.nightcity.ecology.spawn.ReplaceVanillaWildlifeBiomeModifier.cachedOriginalVanillaSpawns(biomeId, MobCategory.CREATURE).isEmpty());
        assertDoesNotThrow(() -> {
            for (MobCategory cat : BiomeWildlifeTable.WILDLIFE_CATEGORIES) {
                emptyBuilder.noSpawns(cat);
            }
            com.nightcity.ecology.spawn.ReplaceVanillaWildlifeBiomeModifier.applyEnvelopeSpawnsToBuilder(biomeId, 0.65f, 0.5f, true, emptyBuilder);
        });
        assertNotNull(emptyBuilder.getSpawner(MobCategory.CREATURE));

        var mod = com.nightcity.ecology.spawn.ReplaceVanillaWildlifeBiomeModifier.INSTANCE;
        assertDoesNotThrow(() -> com.nightcity.ecology.spawn.ReplaceVanillaWildlifeBiomeModifier.cacheOriginalSpawnsIfAbsent(null, emptyBuilder));
        assertDoesNotThrow(() -> com.nightcity.ecology.spawn.ReplaceVanillaWildlifeBiomeModifier.cacheOriginalSpawnsIfAbsent(biomeId, null));
        assertTrue(com.nightcity.ecology.spawn.ReplaceVanillaWildlifeBiomeModifier.cachedOriginalVanillaSpawns(null, MobCategory.CREATURE).isEmpty());
        assertTrue(com.nightcity.ecology.spawn.ReplaceVanillaWildlifeBiomeModifier.cachedOriginalVanillaSpawns(biomeId, null).isEmpty());
        assertDoesNotThrow(() -> com.nightcity.ecology.spawn.ReplaceVanillaWildlifeBiomeModifier.applyEnvelopeSpawnsToBuilder(null, 0.5f, 0.5f, true, emptyBuilder));
        assertDoesNotThrow(() -> com.nightcity.ecology.spawn.ReplaceVanillaWildlifeBiomeModifier.applyEnvelopeSpawnsToBuilder(biomeId, Float.NaN, Float.POSITIVE_INFINITY, true, null));

        var dummyBiome = new net.minecraft.world.level.biome.Biome.BiomeBuilder()
                .hasPrecipitation(true)
                .temperature(0.7f)
                .downfall(0.5f)
                .specialEffects(new net.minecraft.world.level.biome.BiomeSpecialEffects.Builder().waterColor(0x3F76E4).build())
                .generationSettings(net.minecraft.world.level.biome.BiomeGenerationSettings.EMPTY)
                .build();
        var biomeKey = net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.BIOME,
                Identifier.fromNamespaceAndPath("tcwildlands", "temperate_mixed_forest")
        );
        var biomeInfoBuilder = net.neoforged.neoforge.common.world.ModifiableBiomeInfo.BiomeInfo.Builder.copyOf(
                biomeKey,
                dummyBiome.modifiableBiomeInfo().getOriginalBiomeInfo()
        );
        var holder = net.minecraft.core.Holder.Reference.createStandAlone(
                new net.minecraft.core.HolderOwner<net.minecraft.world.level.biome.Biome>() {},
                biomeKey
        );
        for (var phase : net.neoforged.neoforge.common.world.BiomeModifier.Phase.values()) {
            assertDoesNotThrow(() -> mod.modify(null, holder, phase, biomeInfoBuilder));
        }
        assertDoesNotThrow(() -> mod.modify(null, null, net.neoforged.neoforge.common.world.BiomeModifier.Phase.REMOVE, biomeInfoBuilder));
        assertDoesNotThrow(() -> mod.modify(null, holder, null, biomeInfoBuilder));
        assertDoesNotThrow(() -> mod.modify(null, holder, net.neoforged.neoforge.common.world.BiomeModifier.Phase.ADD, null));
    }

    private static Set<String> speciesPaths(List<BiomeWildlifeTable.ResolvedWildlifeSpawn> spawns) {
        return spawns.stream().map(s -> s.speciesId().getPath()).collect(Collectors.toSet());
    }
}
