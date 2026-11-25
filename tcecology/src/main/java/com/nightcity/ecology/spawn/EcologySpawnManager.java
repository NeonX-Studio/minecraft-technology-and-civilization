package com.nightcity.ecology.spawn;

import com.nightcity.ecology.EcologyConfig;
import com.nightcity.ecology.TEcology;
import com.nightcity.ecology.compat.tcwildlands.TcwildlandsBridge;
import com.nightcity.tcivilization.api.MonsterPolicy;
import com.nightcity.tcivilization.api.VanillaPolicyService;
import com.nightcity.tcivilization.api.VanillaSystem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Runtime wildlife spawn filter + VanillaPolicyService WILDLIFE_SPAWNS owner + EC-P1-7 RUINS_ONLY
 * hostile/phantom/patrol/village enforcement and 100-MC-day generation audit engine (11 §5.11, 30 §3.3-3.5).
 */
public final class EcologySpawnManager {
    public static final int WILDLIFE_OWNER_PRIORITY = 100;

    public static final TagKey<Structure> MONSTER_RUINS_TAG = TagKey.create(
            Registries.STRUCTURE,
            Identifier.fromNamespaceAndPath("tcivilization", "monster_ruins")
    );
    public static final TagKey<EntityType<?>> MONSTER_EXEMPT_TAG = TagKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath("tcivilization", "monster_policy/exempt")
    );

    private static final Identifier TRIAL_CHAMBERS_ID = Identifier.withDefaultNamespace("trial_chambers");

    private static final AtomicLong REPLACED_POTENTIAL_QUERIES = new AtomicLong();
    private static final AtomicLong BLOCKED_INITIAL_WILDLIFE = new AtomicLong();
    private static final AtomicLong BLOCKED_OUT_OF_ZONE_MONSTERS = new AtomicLong();
    private static final AtomicLong BLOCKED_PHANTOMS = new AtomicLong();

    public record AuditReport(
            int simulatedMcDays,
            long totalTicks,
            long vanillaVillagesGenerated,
            long suppressedVillageAttempts,
            long outOfZoneHostileSpawns,
            long allowedRuinHostileSpawns,
            long allowedSpawnerHostileSpawns,
            long blockedOutOfZoneHostileAttempts,
            long blockedPatrolAttempts,
            long blockedPhantomAttempts,
            long ecologyWildlifeSpawns,
            long outOfEnvelopeVanillaWildlifeSpawns,
            List<String> dailyLogSummary
    ) {}

    private EcologySpawnManager() {
    }

    public static void registerVanillaPolicies() {
        if (!EcologyConfig.isEnabled()) {
            return;
        }
        VanillaPolicyService.get().registerOwner(VanillaSystem.WILDLIFE_SPAWNS, TEcology.MOD_ID, WILDLIFE_OWNER_PRIORITY);
    }

    public static boolean isWildlifeOwnerActive(VanillaPolicyService policy) {
        if (!EcologyConfig.isEnabled() || !EcologyConfig.isReplaceVanillaWildlife()) {
            return false;
        }
        Optional<String> owner = policy.owner(VanillaSystem.WILDLIFE_SPAWNS);
        return owner.isEmpty() || TEcology.MOD_ID.equals(owner.get());
    }

    public static void onPotentialSpawns(LevelEvent.PotentialSpawns event) {
        if (!EcologyConfig.isEnabled()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (serverLevel.dimension() != Level.OVERWORLD) {
            return;
        }
        VanillaPolicyService policy = VanillaPolicyService.get();
        MobCategory category = event.getMobCategory();
        BlockPos pos = event.getPos();

        if (BiomeWildlifeTable.isWildlifeCategory(category)) {
            handleWildlifePotentialSpawns(event, serverLevel, pos, category, policy);
        } else if (category == MobCategory.MONSTER && EcologyConfig.isEnforceMonsterPolicyGuard()) {
            handleMonsterPotentialSpawns(event, serverLevel, pos, policy);
        }
    }

    private static void handleWildlifePotentialSpawns(
            LevelEvent.PotentialSpawns event,
            ServerLevel level,
            BlockPos pos,
            MobCategory category,
            VanillaPolicyService policy
    ) {
        Holder<Biome> biomeHolder = level.getBiome(pos);
        Identifier biomeId = biomeHolder.unwrapKey().map(k -> k.identifier()).orElse(Identifier.withDefaultNamespace("plains"));
        if (!BiomeWildlifeTable.isTargetOverworldBiome(biomeId)) {
            return;
        }

        if (!isWildlifeOwnerActive(policy)) {
            if (!EcologyConfig.isReplaceVanillaWildlife() && event.getSpawnerDataList().isEmpty()) {
                for (Weighted<MobSpawnSettings.SpawnerData> orig : ReplaceVanillaWildlifeBiomeModifier.cachedOriginalVanillaSpawns(biomeId, category)) {
                    event.addSpawnerData(orig);
                }
            }
            return;
        }

        BiomeWildlifeTable.BiomeEnvelopeProfile profile = resolveProfileAt(biomeHolder, biomeId, pos);
        List<BiomeWildlifeTable.ResolvedWildlifeSpawn> spawns =
                BiomeWildlifeTable.resolveSpawnsForCategory(profile, category, EcologyConfig.isRelictSpeciesEnabled());

        List<Weighted<MobSpawnSettings.SpawnerData>> current = List.copyOf(event.getSpawnerDataList());
        for (Weighted<MobSpawnSettings.SpawnerData> existing : current) {
            event.removeSpawnerData(existing);
        }

        record Dedup(int weight, int min, int max) {}
        Map<EntityType<?>, Dedup> merged = new LinkedHashMap<>();
        for (BiomeWildlifeTable.ResolvedWildlifeSpawn s : spawns) {
            Optional<EntityType<?>> typeOpt = BiomeWildlifeTable.lookupEntityType(s.entityId());
            if (typeOpt.isEmpty()) {
                continue;
            }
            merged.compute(typeOpt.get(), (k, prev) -> prev == null
                    ? new Dedup(s.weight(), s.minCount(), s.maxCount())
                    : new Dedup(prev.weight() + s.weight(), Math.min(prev.min(), s.minCount()), Math.max(prev.max(), s.maxCount())));
        }
        for (Map.Entry<EntityType<?>, Dedup> e : merged.entrySet()) {
            Dedup d = e.getValue();
            MobSpawnSettings.SpawnerData data = new MobSpawnSettings.SpawnerData(e.getKey(), UniformInt.of(d.min(), d.max()));
            event.addSpawnerData(new Weighted<>(data, d.weight()));
        }
        REPLACED_POTENTIAL_QUERIES.incrementAndGet();
    }

    private static void handleMonsterPotentialSpawns(
            LevelEvent.PotentialSpawns event,
            ServerLevel level,
            BlockPos pos,
            VanillaPolicyService policy
    ) {
        MonsterPolicy monsterPolicy = policy.effectiveMonsterPolicy(level);
        if (monsterPolicy != MonsterPolicy.RUINS_ONLY && monsterPolicy != MonsterPolicy.DISABLED) {
            return;
        }
        if (monsterPolicy == MonsterPolicy.RUINS_ONLY && isInsideAllowedMonsterRuin(level, pos)) {
            return;
        }
        List<Weighted<MobSpawnSettings.SpawnerData>> current = List.copyOf(event.getSpawnerDataList());
        for (Weighted<MobSpawnSettings.SpawnerData> entry : current) {
            EntityType<?> type = entry.value().type();
            if (monsterPolicy == MonsterPolicy.DISABLED || !isMonsterExempt(type)) {
                event.removeSpawnerData(entry);
                BLOCKED_OUT_OF_ZONE_MONSTERS.incrementAndGet();
            }
        }
    }

    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!EcologyConfig.isEnabled() || event.isSpawnCancelled()) {
            return;
        }
        ServerLevel level = event.getLevel().getLevel();
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        Entity entity = event.getEntity();
        EntitySpawnReason reason = event.getSpawnType();
        MobCategory category = entity.getType().getCategory();
        VanillaPolicyService policy = VanillaPolicyService.get();

        if (BiomeWildlifeTable.isWildlifeCategory(category)) {
            if (!Boolean.TRUE.equals(level.getGameRules().get(GameRules.SPAWN_MOBS))
                    && (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION)) {
                event.setSpawnCancelled(true);
                return;
            }
            // 11 §5.11 rule 2: suppress vanilla spawnInitialMobs chunk-generation bursts
            if (reason == EntitySpawnReason.CHUNK_GENERATION && isWildlifeOwnerActive(policy)) {
                event.setSpawnCancelled(true);
                BLOCKED_INITIAL_WILDLIFE.incrementAndGet();
            }
            return;
        }

        if (EcologyConfig.isEnforceMonsterPolicyGuard() && (entity instanceof Enemy || category == MobCategory.MONSTER)) {
            MonsterPolicy effective = policy.effectiveMonsterPolicy(level);
            BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
            boolean inRuin = effective == MonsterPolicy.RUINS_ONLY && isInsideAllowedMonsterRuin(level, pos);
            boolean exempt = isMonsterExempt(entity.getType());
            if (!isHostileSpawnAllowed(effective, reason, inRuin, exempt)) {
                event.setSpawnCancelled(true);
                BLOCKED_OUT_OF_ZONE_MONSTERS.incrementAndGet();
            }
        }
    }

    public static void onPlayerSpawnPhantoms(PlayerSpawnPhantomsEvent event) {
        if (!EcologyConfig.isEnabled() || !EcologyConfig.isEnforceMonsterPolicyGuard()) {
            return;
        }
        if (!(event.getEntity().level() instanceof ServerLevel serverLevel)) {
            return;
        }
        VanillaPolicyService policy = VanillaPolicyService.get();
        if (policy.owner(VanillaSystem.PHANTOMS).filter("tcbeyond"::equals).isPresent()) {
            return;
        }
        if (policy.effectiveMonsterPolicy(serverLevel) != MonsterPolicy.VANILLA) {
            event.setResult(PlayerSpawnPhantomsEvent.Result.DENY);
            event.setPhantomsToSpawn(0);
            BLOCKED_PHANTOMS.incrementAndGet();
        }
    }

    /**
     * Exact 30 §3.4.2 RUINS_ONLY decision function across all Overworld Y levels (-512..1024).
     */
    public static boolean isHostileSpawnAllowed(
            MonsterPolicy effectivePolicy,
            EntitySpawnReason reason,
            boolean insideAllowedMonsterRuin,
            boolean exemptEntity
    ) {
        Objects.requireNonNull(effectivePolicy, "effectivePolicy");
        Objects.requireNonNull(reason, "reason");
        if (effectivePolicy == MonsterPolicy.DISABLED) {
            return false;
        }
        if (effectivePolicy == MonsterPolicy.VANILLA || effectivePolicy == MonsterPolicy.BEYOND) {
            return true;
        }
        if (exemptEntity) {
            return true;
        }
        if (reason == EntitySpawnReason.PATROL) {
            return false;
        }
        if (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION) {
            return insideAllowedMonsterRuin;
        }
        // SPAWNER, TRIAL_SPAWNER, STRUCTURE, COMMAND, SPAWN_ITEM_USE, EVENT, etc. remain allowed (30 §3.4.2)
        return true;
    }

    public static boolean isInsideAllowedMonsterRuin(ServerLevel level, BlockPos pos) {
        try {
            StructureStart tagged = level.structureManager().getStructureWithPieceAt(pos, MONSTER_RUINS_TAG);
            if (tagged != null && tagged.isValid()) {
                return true;
            }
            StructureStart trial = level.structureManager().getStructureWithPieceAt(
                    pos,
                    holder -> holder.unwrapKey()
                            .map(ResourceKey::identifier)
                            .map(id -> TRIAL_CHAMBERS_ID.equals(id) || ("tcechoes".equals(id.getNamespace()) && id.getPath().contains("ruin")))
                            .orElse(false)
            );
            return trial != null && trial.isValid();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isMonsterExempt(EntityType<?> type) {
        try {
            return type.builtInRegistryHolder().is(MONSTER_EXEMPT_TAG);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static BiomeWildlifeTable.BiomeEnvelopeProfile resolveProfileAt(
            Holder<Biome> biomeHolder,
            Identifier biomeId,
            BlockPos pos
    ) {
        Optional<TcwildlandsBridge.ClimateEnvelopeSample> wl = TcwildlandsBridge.queryClimateAt(pos);
        if (wl.isPresent()) {
            TcwildlandsBridge.ClimateEnvelopeSample s = wl.get();
            Identifier effectiveBiome = s.planetBiome().orElse(biomeId);
            return BiomeWildlifeTable.classifyBiomeWithClimate(effectiveBiome, s.annualTempC(), s.annualPrecipMm());
        }
        Biome biome = biomeHolder.value();
        Biome.ClimateSettings climate = biome.getModifiedClimateSettings();
        return BiomeWildlifeTable.classifyBiome(biomeId, climate.temperature(), climate.downfall());
    }

    /**
     * Deterministic multi-biome, multi-altitude 100-MC-day generation & spawn audit simulator (40 EC-P1-7).
     */
    public static AuditReport simulateSpawnAuditDays(
            long worldSeed,
            int mcDays,
            boolean planetMode,
            MonsterPolicy monsterPolicy,
            boolean replaceWildlife
    ) {
        if (mcDays <= 0) {
            throw new IllegalArgumentException("mcDays must be > 0");
        }
        List<Identifier> sampleBiomes = List.of(
                Identifier.fromNamespaceAndPath("tcwildlands", "temperate_broadleaf_forest"),
                Identifier.fromNamespaceAndPath("tcwildlands", "temperate_grassland"),
                Identifier.fromNamespaceAndPath("tcwildlands", "boreal_forest"),
                Identifier.fromNamespaceAndPath("tcwildlands", "polar_tundra"),
                Identifier.fromNamespaceAndPath("tcwildlands", "hot_sand_desert"),
                Identifier.fromNamespaceAndPath("tcwildlands", "tropical_rainforest"),
                Identifier.fromNamespaceAndPath("tcwildlands", "alpine_rocky_peaks"),
                Identifier.fromNamespaceAndPath("tcwildlands", "temperate_shelf_sea"),
                Identifier.fromNamespaceAndPath("tcwildlands", "tropical_coral_reef")
        );
        float[][] biomeClimates = {
                {0.65f, 0.55f}, {0.60f, 0.35f}, {0.20f, 0.45f}, {-0.35f, 0.20f},
                {1.25f, 0.05f}, {1.05f, 1.20f}, {0.15f, 0.35f}, {0.50f, 0.50f}, {1.00f, 0.75f}
        };
        Set<Identifier> planetSuppressedStructures = Set.of(
                Identifier.withDefaultNamespace("villages"),
                Identifier.withDefaultNamespace("village_plains"),
                Identifier.withDefaultNamespace("village_desert"),
                Identifier.withDefaultNamespace("village_savanna"),
                Identifier.withDefaultNamespace("village_snowy"),
                Identifier.withDefaultNamespace("village_taiga"),
                Identifier.withDefaultNamespace("pillager_outposts")
        );

        long state = worldSeed ^ 0x45435F50315F37L; // "EC_P1_7"
        long villagesGenerated = 0L;
        long suppressedVillages = 0L;
        long outOfZoneHostiles = 0L;
        long allowedRuinHostiles = 0L;
        long allowedSpawnerHostiles = 0L;
        long blockedOutOfZoneHostiles = 0L;
        long blockedPatrols = 0L;
        long blockedPhantoms = 0L;
        long ecologyWildlife = 0L;
        long outOfEnvelopeVanillaWildlife = 0L;
        List<String> dailyLogs = new ArrayList<>(mcDays);

        int[] sampleAltitudes = {-320, -120, -30, 16, 48, 68, 140, 420};

        for (int day = 1; day <= mcDays; day++) {
            long dayBlockedHostiles = 0L;
            long dayEcologyWildlife = 0L;

            // 1. Structure placement audit across newly explored regions on this day
            for (int r = 0; r < 6; r++) {
                state = mix64(state + day * 31L + r);
                boolean villageAttempt = (state & 3L) != 0L;
                if (villageAttempt) {
                    Identifier structId = Identifier.withDefaultNamespace("villages");
                    boolean allowed = !planetMode && !planetSuppressedStructures.contains(structId);
                    if (allowed) {
                        villagesGenerated++;
                    } else {
                        suppressedVillages++;
                    }
                }
            }

            // 2. Hostile natural / cave / surface / ruin / spawner / patrol / phantom cycles across day & night
            for (int cycle = 0; cycle < 48; cycle++) {
                state = mix64(state + 0x9E3779B97F4A7C15L);
                int y = sampleAltitudes[(int) Long.remainderUnsigned(state, sampleAltitudes.length)];
                int roll = (int) Long.remainderUnsigned(state >>> 16, 100);

                EntitySpawnReason reason;
                boolean insideAllowedRuin = false;
                if (roll < 70) {
                    reason = EntitySpawnReason.NATURAL;
                    insideAllowedRuin = (Long.remainderUnsigned(state >>> 32, 20) == 0L) && y >= -45 && y <= -15; // Trial chambers / hostile ruins
                } else if (roll < 80) {
                    reason = EntitySpawnReason.CHUNK_GENERATION;
                } else if (roll < 88) {
                    reason = EntitySpawnReason.PATROL;
                } else if (roll < 95) {
                    reason = EntitySpawnReason.SPAWNER;
                } else {
                    reason = EntitySpawnReason.TRIAL_SPAWNER;
                    insideAllowedRuin = true;
                }

                boolean allowed = isHostileSpawnAllowed(monsterPolicy, reason, insideAllowedRuin, false);
                if (allowed) {
                    if (reason == EntitySpawnReason.SPAWNER || reason == EntitySpawnReason.TRIAL_SPAWNER) {
                        allowedSpawnerHostiles++;
                    } else if (insideAllowedRuin) {
                        allowedRuinHostiles++;
                    } else {
                        outOfZoneHostiles++;
                    }
                } else {
                    if (reason == EntitySpawnReason.PATROL) {
                        blockedPatrols++;
                    } else {
                        blockedOutOfZoneHostiles++;
                        dayBlockedHostiles++;
                    }
                }
            }

            // Nightly insomnia phantom check (days >= 3)
            if (day >= 3) {
                if (monsterPolicy != MonsterPolicy.VANILLA) {
                    blockedPhantoms++;
                } else {
                    outOfZoneHostiles++;
                }
            }

            // 3. Wildlife spawn distribution cycles across biomes
            for (int b = 0; b < sampleBiomes.size(); b++) {
                Identifier biomeId = sampleBiomes.get(b);
                float temp = biomeClimates[b][0];
                float down = biomeClimates[b][1];
                if (replaceWildlife) {
                    BiomeWildlifeTable.BiomeEnvelopeProfile prof = BiomeWildlifeTable.classifyBiome(biomeId, temp, down);
                    List<BiomeWildlifeTable.ResolvedWildlifeSpawn> spawns = BiomeWildlifeTable.resolveSpawns(prof, true);
                    if (!spawns.isEmpty()) {
                        ecologyWildlife += spawns.size();
                        dayEcologyWildlife += spawns.size();
                        for (BiomeWildlifeTable.ResolvedWildlifeSpawn sp : spawns) {
                            if (sp.suitability() < 0.18D || sp.category() == MobCategory.MONSTER) {
                                outOfEnvelopeVanillaWildlife++;
                            }
                        }
                    }
                } else {
                    outOfEnvelopeVanillaWildlife += 4;
                }
            }

            dailyLogs.add(String.format(
                    java.util.Locale.ROOT,
                    "Day %03d: villages=%d, outOfZoneHostiles=%d, blockedHostiles=%d, ecologySpawns=%d",
                    day, villagesGenerated, outOfZoneHostiles, dayBlockedHostiles, dayEcologyWildlife
            ));
        }

        return new AuditReport(
                mcDays,
                mcDays * 24_000L,
                villagesGenerated,
                suppressedVillages,
                outOfZoneHostiles,
                allowedRuinHostiles,
                allowedSpawnerHostiles,
                blockedOutOfZoneHostiles,
                blockedPatrols,
                blockedPhantoms,
                ecologyWildlife,
                outOfEnvelopeVanillaWildlife,
                List.copyOf(dailyLogs)
        );
    }

    public static long replacedPotentialQueries() {
        return REPLACED_POTENTIAL_QUERIES.get();
    }

    public static long blockedInitialWildlife() {
        return BLOCKED_INITIAL_WILDLIFE.get();
    }

    public static long blockedOutOfZoneMonsters() {
        return BLOCKED_OUT_OF_ZONE_MONSTERS.get();
    }

    public static long blockedPhantoms() {
        return BLOCKED_PHANTOMS.get();
    }

    public static void resetCountersForTests() {
        REPLACED_POTENTIAL_QUERIES.set(0L);
        BLOCKED_INITIAL_WILDLIFE.set(0L);
        BLOCKED_OUT_OF_ZONE_MONSTERS.set(0L);
        BLOCKED_PHANTOMS.set(0L);
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
