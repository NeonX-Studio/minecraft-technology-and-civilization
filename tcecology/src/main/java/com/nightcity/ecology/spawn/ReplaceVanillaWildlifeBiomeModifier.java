package com.nightcity.ecology.spawn;

import com.mojang.serialization.MapCodec;
import com.nightcity.ecology.EcologyConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.MobSpawnSettingsBuilder;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * NeoForge BiomeModifier tcecology:remove_vanilla_wildlife (11 §5.11, §16; 30 VO-SP-03).
 * Removes Overworld passive/ambient/aquatic vanilla spawns and injects climate-habitat envelope entries,
 * leaving MobCategory.MONSTER completely untouched.
 */
public final class ReplaceVanillaWildlifeBiomeModifier implements BiomeModifier {
    public static final ReplaceVanillaWildlifeBiomeModifier INSTANCE = new ReplaceVanillaWildlifeBiomeModifier();
    public static final MapCodec<ReplaceVanillaWildlifeBiomeModifier> CODEC = MapCodec.unit(() -> INSTANCE);

    private static final Map<Identifier, Map<MobCategory, List<Weighted<MobSpawnSettings.SpawnerData>>>> ORIGINAL_VANILLA_CACHE =
            new ConcurrentHashMap<>();

    @Override
    public void modify(
            RegistryAccess registries,
            Holder<Biome> biome,
            Phase phase,
            ModifiableBiomeInfo.BiomeInfo.Builder builder
    ) {
        if (biome == null || phase == null || builder == null || !EcologyConfig.isEnabled()) {
            return;
        }
        Identifier biomeId = biome.unwrapKey().map(k -> k.identifier()).orElse(null);
        if (biomeId == null || !BiomeWildlifeTable.isTargetOverworldBiome(biomeId)) {
            return;
        }

        MobSpawnSettingsBuilder spawnBuilder = builder.getMobSpawnSettings();
        if (spawnBuilder == null) {
            return;
        }

        switch (phase) {
            case BEFORE_EVERYTHING -> cacheOriginalSpawnsIfAbsent(biomeId, spawnBuilder);
            case REMOVE -> {
                cacheOriginalSpawnsIfAbsent(biomeId, spawnBuilder);
                if (!EcologyConfig.isReplaceVanillaWildlife()) {
                    return;
                }
                for (MobCategory cat : BiomeWildlifeTable.WILDLIFE_CATEGORIES) {
                    if (cat != null) {
                        spawnBuilder.noSpawns(cat);
                    }
                }
            }
            case ADD -> {
                if (!EcologyConfig.isReplaceVanillaWildlife()) {
                    return;
                }
                var climate = builder.getClimateSettings();
                float temp = climate != null ? climate.getTemperature() : 0.5f;
                float downfall = climate != null ? climate.getDownfall() : 0.5f;
                applyEnvelopeSpawnsToBuilder(biomeId, temp, downfall, EcologyConfig.isRelictSpeciesEnabled(), spawnBuilder);
            }
            case MODIFY, AFTER_EVERYTHING -> {
            }
        }
    }

    public static void applyEnvelopeSpawnsToBuilder(
            Identifier biomeId,
            float temperature,
            float downfall,
            boolean includeRelicts,
            MobSpawnSettingsBuilder spawnBuilder
    ) {
        if (biomeId == null || spawnBuilder == null) {
            return;
        }
        float safeTemp = Float.isFinite(temperature) ? temperature : 0.5f;
        float safeDownfall = Float.isFinite(downfall) ? downfall : 0.5f;
        BiomeWildlifeTable.BiomeEnvelopeProfile profile = BiomeWildlifeTable.classifyBiome(biomeId, safeTemp, safeDownfall);
        List<BiomeWildlifeTable.ResolvedWildlifeSpawn> resolved = BiomeWildlifeTable.resolveSpawns(profile, includeRelicts);
        if (resolved == null || resolved.isEmpty()) {
            return;
        }

        record Key(EntityType<?> type, MobCategory category) {}
        record Agg(int weight, int min, int max) {}
        Map<Key, Agg> deduplicated = new LinkedHashMap<>();

        for (BiomeWildlifeTable.ResolvedWildlifeSpawn s : resolved) {
            if (s == null || s.entityId() == null || s.category() == null || s.weight() <= 0) {
                continue;
            }
            Optional<EntityType<?>> typeOpt = BiomeWildlifeTable.lookupEntityType(s.entityId());
            if (typeOpt.isEmpty()) {
                continue;
            }
            int safeMin = Math.max(1, s.minCount());
            int safeMax = Math.max(safeMin, s.maxCount());
            Key key = new Key(typeOpt.get(), s.category());
            deduplicated.compute(key, (k, prev) -> prev == null
                    ? new Agg(s.weight(), safeMin, safeMax)
                    : new Agg(prev.weight() + s.weight(), Math.min(prev.min(), safeMin), Math.max(prev.max(), safeMax)));
        }

        for (Map.Entry<Key, Agg> entry : deduplicated.entrySet()) {
            Key key = entry.getKey();
            Agg agg = entry.getValue();
            if (agg.weight() > 0) {
                spawnBuilder.addSpawn(key.type(), key.category(), agg.weight(), UniformInt.of(agg.min(), agg.max()));
            }
        }
    }

    public static void cacheOriginalSpawnsIfAbsent(Identifier biomeId, MobSpawnSettingsBuilder spawnBuilder) {
        if (biomeId == null || spawnBuilder == null) {
            return;
        }
        ORIGINAL_VANILLA_CACHE.computeIfAbsent(biomeId, id -> {
            EnumMap<MobCategory, List<Weighted<MobSpawnSettings.SpawnerData>>> byCat = new EnumMap<>(MobCategory.class);
            for (MobCategory cat : BiomeWildlifeTable.WILDLIFE_CATEGORIES) {
                if (cat == null) {
                    continue;
                }
                var spawner = spawnBuilder.getSpawner(cat);
                if (spawner == null) {
                    byCat.put(cat, List.of());
                    continue;
                }
                var built = spawner.build();
                var unwrapped = built == null ? List.<Weighted<MobSpawnSettings.SpawnerData>>of() : built.unwrap();
                byCat.put(cat, unwrapped == null || unwrapped.isEmpty() ? List.of() : List.copyOf(unwrapped));
            }
            return Collections.unmodifiableMap(byCat);
        });
    }

    public static List<Weighted<MobSpawnSettings.SpawnerData>> cachedOriginalVanillaSpawns(Identifier biomeId, MobCategory category) {
        if (biomeId == null || category == null) {
            return List.of();
        }
        Map<MobCategory, List<Weighted<MobSpawnSettings.SpawnerData>>> byCat = ORIGINAL_VANILLA_CACHE.get(biomeId);
        if (byCat == null) {
            return List.of();
        }
        List<Weighted<MobSpawnSettings.SpawnerData>> list = byCat.get(category);
        return list == null ? List.of() : new ArrayList<>(list);
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
