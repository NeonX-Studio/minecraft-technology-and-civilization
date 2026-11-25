package com.nightcity.ecology.spawn;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.nightcity.ecology.TEcology;
import com.nightcity.ecology.api.HabitatCategory;
import com.nightcity.ecology.api.SpeciesEnvelope;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Climate & habitat envelope wildlife distribution table replacing Overworld vanilla passive/ambient/aquatic spawns
 * in P1 while leaving MobCategory.MONSTER untouched (11 §5.2, §5.3, §5.11, §16; 30 VO-SP-03).
 */
public final class BiomeWildlifeTable {
    public static final Set<MobCategory> WILDLIFE_CATEGORIES = Collections.unmodifiableSet(EnumSet.of(
            MobCategory.CREATURE,
            MobCategory.AMBIENT,
            MobCategory.WATER_CREATURE,
            MobCategory.WATER_AMBIENT,
            MobCategory.UNDERGROUND_WATER_CREATURE,
            MobCategory.AXOLOTLS
    ));

    private static final Set<String> NON_OVERWORLD_BIOME_PATHS = Set.of(
            "nether_wastes", "crimson_forest", "warped_forest", "soul_sand_valley", "basalt_deltas",
            "the_end", "end_highlands", "end_midlands", "small_end_islands", "end_barrens", "the_void"
    );

    private static final List<String> CLASSPATH_SPECIES_IDS = List.of(
            "red_deer", "reindeer", "aurochs", "wild_boar", "mouflon", "bezoar_ibex",
            "wild_horse", "wild_ass", "guanaco", "wolf", "polar_bear", "red_junglefowl",
            "salmon", "tropical_fish"
    );

    public record BiomeEnvelopeProfile(
            Identifier biomeId,
            double tempC,
            double precipMm,
            Map<HabitatCategory, Double> cover
    ) {}

    public record ResolvedWildlifeSpawn(
            Identifier speciesId,
            Identifier entityId,
            MobCategory category,
            int weight,
            int minCount,
            int maxCount,
            double suitability
    ) {}

    private static volatile Map<Identifier, SpeciesEnvelope> activeSpecies = buildDefaultRegistry();

    private BiomeWildlifeTable() {
    }

    public static Collection<SpeciesEnvelope> allSpecies() {
        return activeSpecies.values();
    }

    public static Optional<SpeciesEnvelope> species(Identifier id) {
        return Optional.ofNullable(activeSpecies.get(id));
    }

    public static synchronized void reloadFromDirectory(Path speciesJsonDir) throws IOException {
        Map<Identifier, SpeciesEnvelope> merged = new LinkedHashMap<>(createBuiltinEnvelopes());
        if (speciesJsonDir != null && Files.isDirectory(speciesJsonDir)) {
            try (Stream<Path> walk = Files.walk(speciesJsonDir)) {
                for (Path file : walk.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                    JsonElement json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
                    SpeciesEnvelope.CODEC.parse(JsonOps.INSTANCE, json).result()
                            .ifPresent(env -> merged.put(env.id(), env));
                }
            }
        }
        activeSpecies = Collections.unmodifiableMap(merged);
    }

    public static boolean isWildlifeCategory(MobCategory category) {
        return category != null && WILDLIFE_CATEGORIES.contains(category);
    }

    public static boolean isTargetOverworldBiome(Identifier biomeId) {
        if (biomeId == null) {
            return false;
        }
        String ns = biomeId.getNamespace();
        String path = biomeId.getPath().toLowerCase(Locale.ROOT);
        if ("tcbeyond".equals(ns) || "tcorbit".equals(ns)) {
            return false;
        }
        if (NON_OVERWORLD_BIOME_PATHS.contains(path)) {
            return false;
        }
        if ("tcwildlands".equals(ns) && (path.startsWith("deep_") || path.contains("mantle") || path.contains("magma_chamber"))) {
            return false;
        }
        return true;
    }

    public static BiomeEnvelopeProfile classifyBiome(Identifier biomeId, float temperature, float downfall) {
        double approxTempC = Math.max(-20.0D, Math.min(32.0D, temperature * 25.0D));
        double approxPrecipMm = Math.max(20.0D, Math.min(3200.0D, downfall * 1600.0D));
        return classifyBiomeWithClimate(biomeId, approxTempC, approxPrecipMm);
    }

    public static BiomeEnvelopeProfile classifyBiomeWithClimate(Identifier biomeId, double tempC, double precipMm) {
        String path = biomeId.getPath().toLowerCase(Locale.ROOT);
        EnumMap<HabitatCategory, Double> cover = new EnumMap<>(HabitatCategory.class);

        if (path.contains("coral") || path.contains("reef") || path.equals("warm_ocean")) {
            cover.put(HabitatCategory.REEF, 1.0D);
            cover.put(HabitatCategory.PELAGIC, 0.6D);
            tempC = Math.max(tempC, 24.0D);
        } else if (path.contains("frozen_ocean") || path.contains("sea_ice") || path.contains("polar_deep_ocean")) {
            cover.put(HabitatCategory.POLAR_SEA, 1.0D);
            cover.put(HabitatCategory.PELAGIC, 0.5D);
            tempC = Math.min(tempC, -4.0D);
        } else if (path.contains("ocean") || path.contains("shelf_sea") || path.contains("trench")) {
            cover.put(HabitatCategory.PELAGIC, 1.0D);
            if (path.contains("shelf") || !path.contains("deep")) {
                cover.put(HabitatCategory.COASTAL, 0.6D);
            }
        } else if (path.contains("beach") || path.contains("shore") || path.contains("fjord_coast")) {
            cover.put(HabitatCategory.COASTAL, 1.0D);
            cover.put(HabitatCategory.FRESHWATER, 0.2D);
        } else if (path.contains("river") || path.contains("lake") || path.contains("delta")) {
            cover.put(HabitatCategory.FRESHWATER, 1.0D);
            cover.put(HabitatCategory.WETLAND, 0.45D);
            cover.put(HabitatCategory.EDGE, 0.35D);
        } else if (path.contains("swamp") || path.contains("marsh") || path.contains("bog") || path.contains("mangrove")) {
            cover.put(HabitatCategory.WETLAND, 1.0D);
            cover.put(HabitatCategory.FRESHWATER, 0.65D);
            cover.put(HabitatCategory.FOREST, 0.35D);
            if (path.contains("mangrove") || path.contains("tropical")) {
                cover.put(HabitatCategory.TROPICAL, 0.8D);
            }
        } else if (path.contains("lush_caves") || path.contains("dripstone") || path.contains("karst")) {
            cover.put(HabitatCategory.UNDERGROUND_WATER, 1.0D);
            cover.put(HabitatCategory.SHRUB, 0.3D);
        } else if (path.contains("desert") || path.contains("badlands") || path.contains("canyon") || path.contains("salt_flat") || path.contains("salt_lake")) {
            cover.put(HabitatCategory.ARID, 1.0D);
            cover.put(HabitatCategory.SHRUB, 0.25D);
            precipMm = Math.min(precipMm, 180.0D);
            if (!path.contains("cold")) {
                tempC = Math.max(tempC, 24.0D);
            }
        } else if (path.contains("savanna") || path.contains("hot_steppe")) {
            cover.put(HabitatCategory.GRASS, 0.85D);
            cover.put(HabitatCategory.TROPICAL, 0.70D);
            cover.put(HabitatCategory.ARID, 0.40D);
            tempC = Math.max(tempC, 22.0D);
            precipMm = Math.min(Math.max(precipMm, 350.0D), 950.0D);
        } else if (path.contains("jungle") || path.contains("tropical_rainforest") || path.contains("monsoon_forest") || path.contains("cloud_forest") || path.contains("tropical_dry_forest")) {
            cover.put(HabitatCategory.TROPICAL, 1.0D);
            cover.put(HabitatCategory.FOREST, 0.90D);
            cover.put(HabitatCategory.EDGE, 0.40D);
            tempC = Math.max(tempC, 23.0D);
            precipMm = Math.max(precipMm, 1500.0D);
        } else if (path.contains("snowy") || path.contains("tundra") || path.contains("ice_sheet") || path.contains("glacier") || path.contains("frozen_peaks")) {
            cover.put(HabitatCategory.TUNDRA, 1.0D);
            if (path.contains("peaks") || path.contains("slopes") || path.contains("alpine") || path.contains("glacier")) {
                cover.put(HabitatCategory.MOUNTAIN, 0.85D);
            }
            tempC = Math.min(tempC, -5.0D);
        } else if (path.contains("taiga") || path.contains("boreal")) {
            cover.put(HabitatCategory.BOREAL, 1.0D);
            cover.put(HabitatCategory.FOREST, 0.80D);
            cover.put(HabitatCategory.EDGE, 0.45D);
            tempC = Math.min(Math.max(tempC, -3.0D), 9.0D);
        } else if (path.contains("peaks") || path.contains("hills") || path.contains("mountain") || path.contains("alpine") || path.contains("plateau")) {
            cover.put(HabitatCategory.MOUNTAIN, 1.0D);
            cover.put(HabitatCategory.GRASS, 0.55D);
            cover.put(HabitatCategory.SHRUB, 0.45D);
            tempC = Math.min(tempC, 11.0D);
        } else if (path.contains("forest") || path.contains("woodland") || path.contains("grove")) {
            cover.put(HabitatCategory.FOREST, 1.0D);
            cover.put(HabitatCategory.EDGE, 0.75D);
            cover.put(HabitatCategory.SHRUB, 0.45D);
        } else {
            // Plains, steppe, meadow, floodplain, shrubland
            cover.put(HabitatCategory.GRASS, 1.0D);
            cover.put(HabitatCategory.EDGE, 0.45D);
            cover.put(HabitatCategory.SHRUB, 0.35D);
        }
        // Subterranean karst/glow-squid baseline in non-arid land/marine biomes
        if (!cover.containsKey(HabitatCategory.ARID)) {
            cover.putIfAbsent(HabitatCategory.UNDERGROUND_WATER, 0.35D);
        }
        return new BiomeEnvelopeProfile(biomeId, tempC, precipMm, Collections.unmodifiableMap(cover));
    }

    public static List<ResolvedWildlifeSpawn> resolveSpawns(
            BiomeEnvelopeProfile profile,
            boolean includeRelicts
    ) {
        Objects.requireNonNull(profile, "profile");
        if (!isTargetOverworldBiome(profile.biomeId())) {
            return List.of();
        }
        List<ResolvedWildlifeSpawn> result = new ArrayList<>();
        for (SpeciesEnvelope sp : activeSpecies.values()) {
            if (sp.relict() && !includeRelicts) {
                continue;
            }
            double hq = sp.suitability(profile.tempC(), profile.precipMm(), profile.cover());
            int weight = sp.spawnWeight(hq);
            if (weight <= 0) {
                continue;
            }
            Identifier resolvedEntity = resolveRuntimeEntityId(sp);
            result.add(new ResolvedWildlifeSpawn(
                    sp.id(),
                    resolvedEntity,
                    sp.mobCategory(),
                    weight,
                    sp.minGroup(),
                    sp.maxGroup(),
                    hq
            ));
        }
        result.sort(Comparator.comparing(ResolvedWildlifeSpawn::category)
                .thenComparing(Comparator.comparingInt(ResolvedWildlifeSpawn::weight).reversed())
                .thenComparing(s -> s.speciesId().toString()));
        return List.copyOf(result);
    }

    public static List<ResolvedWildlifeSpawn> resolveSpawnsForCategory(
            BiomeEnvelopeProfile profile,
            MobCategory category,
            boolean includeRelicts
    ) {
        if (!isWildlifeCategory(category)) {
            return List.of();
        }
        List<ResolvedWildlifeSpawn> all = resolveSpawns(profile, includeRelicts);
        List<ResolvedWildlifeSpawn> filtered = new ArrayList<>();
        for (ResolvedWildlifeSpawn s : all) {
            if (s.category() == category) {
                filtered.add(s);
            }
        }
        return List.copyOf(filtered);
    }

    private static Identifier resolveRuntimeEntityId(SpeciesEnvelope sp) {
        try {
            if (BuiltInRegistries.ENTITY_TYPE.containsKey(sp.entityId())) {
                return sp.entityId();
            }
        } catch (Throwable ignored) {
        }
        return sp.fallbackVanillaEntityId();
    }

    public static Optional<EntityType<?>> lookupEntityType(Identifier entityId) {
        try {
            if (BuiltInRegistries.ENTITY_TYPE.containsKey(entityId)) {
                return Optional.of(BuiltInRegistries.ENTITY_TYPE.getValue(entityId));
            }
        } catch (Throwable ignored) {
        }
        return Optional.empty();
    }

    private static Map<Identifier, SpeciesEnvelope> buildDefaultRegistry() {
        Map<Identifier, SpeciesEnvelope> map = new LinkedHashMap<>(createBuiltinEnvelopes());
        for (String id : CLASSPATH_SPECIES_IDS) {
            String resPath = "/data/tcecology/tcecology/species/" + id + ".json";
            try (InputStream in = BiomeWildlifeTable.class.getResourceAsStream(resPath)) {
                if (in != null) {
                    JsonElement el = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    SpeciesEnvelope.CODEC.parse(JsonOps.INSTANCE, el).result()
                            .ifPresent(env -> map.put(env.id(), env));
                }
            } catch (IOException ignored) {
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private static Map<Identifier, SpeciesEnvelope> createBuiltinEnvelopes() {
        Map<Identifier, SpeciesEnvelope> m = new LinkedHashMap<>();
        // Temperate & boreal forest / edge ungulates & carnivores
        put(m, "red_deer", "tcecology:red_deer", "minecraft:goat", MobCategory.CREATURE, 96, 3, 8, 13.0, 10.0, 850.0, 650.0,
                Map.of(HabitatCategory.FOREST, 0.85, HabitatCategory.EDGE, 1.0, HabitatCategory.SHRUB, 0.5), false);
        put(m, "aurochs", "tcecology:aurochs", "minecraft:cow", MobCategory.CREATURE, 40, 3, 6, 15.0, 8.0, 750.0, 500.0,
                Map.of(HabitatCategory.FOREST, 0.75, HabitatCategory.EDGE, 0.90), true);
        put(m, "wild_boar", "tcecology:wild_boar", "minecraft:pig", MobCategory.CREATURE, 80, 3, 7, 15.0, 9.0, 850.0, 600.0,
                Map.of(HabitatCategory.FOREST, 1.0, HabitatCategory.WETLAND, 0.55), false);
        put(m, "wolf", "minecraft:wolf", "minecraft:wolf", MobCategory.CREATURE, 24, 3, 6, 5.0, 14.0, 650.0, 600.0,
                Map.of(HabitatCategory.BOREAL, 1.0, HabitatCategory.FOREST, 0.70, HabitatCategory.TUNDRA, 0.55), false);
        put(m, "fox", "minecraft:fox", "minecraft:fox", MobCategory.CREATURE, 36, 2, 4, 6.0, 14.0, 700.0, 600.0,
                Map.of(HabitatCategory.BOREAL, 1.0, HabitatCategory.FOREST, 0.75, HabitatCategory.EDGE, 0.70, HabitatCategory.TUNDRA, 0.50), false);
        put(m, "rabbit", "minecraft:rabbit", "minecraft:rabbit", MobCategory.CREATURE, 80, 2, 5, 11.0, 18.0, 550.0, 650.0,
                Map.of(HabitatCategory.GRASS, 0.85, HabitatCategory.EDGE, 0.80, HabitatCategory.SHRUB, 0.75, HabitatCategory.TUNDRA, 0.65, HabitatCategory.ARID, 0.45), false);

        // Polar & boreal
        put(m, "reindeer", "tcecology:reindeer", "minecraft:goat", MobCategory.CREATURE, 120, 4, 10, -6.0, 9.0, 400.0, 450.0,
                Map.of(HabitatCategory.TUNDRA, 1.0, HabitatCategory.BOREAL, 0.75), false);
        put(m, "polar_bear", "minecraft:polar_bear", "minecraft:polar_bear", MobCategory.CREATURE, 16, 1, 2, -10.0, 8.0, 300.0, 450.0,
                Map.of(HabitatCategory.TUNDRA, 0.85, HabitatCategory.POLAR_SEA, 1.0), false);
        put(m, "woolly_mammoth", "tcecology:woolly_mammoth", "minecraft:camel", MobCategory.CREATURE, 12, 2, 4, -8.0, 6.0, 320.0, 350.0,
                Map.of(HabitatCategory.TUNDRA, 0.90), true);

        // Steppe, grassland & savanna
        put(m, "wild_horse", "tcecology:wild_horse", "minecraft:horse", MobCategory.CREATURE, 60, 4, 8, 16.0, 10.0, 520.0, 420.0,
                Map.of(HabitatCategory.GRASS, 1.0, HabitatCategory.SHRUB, 0.35), false);
        put(m, "bison", "tcecology:bison", "minecraft:cow", MobCategory.CREATURE, 50, 4, 9, 14.0, 10.0, 550.0, 420.0,
                Map.of(HabitatCategory.GRASS, 0.95, HabitatCategory.EDGE, 0.40), false);
        put(m, "armadillo", "minecraft:armadillo", "minecraft:armadillo", MobCategory.CREATURE, 44, 1, 3, 25.0, 9.0, 420.0, 420.0,
                Map.of(HabitatCategory.ARID, 0.75, HabitatCategory.GRASS, 0.65, HabitatCategory.SHRUB, 0.70, HabitatCategory.TROPICAL, 0.55), false);

        // Mountain & highland
        put(m, "mouflon", "tcecology:mouflon", "minecraft:sheep", MobCategory.CREATURE, 56, 3, 6, 8.0, 9.0, 600.0, 500.0,
                Map.of(HabitatCategory.MOUNTAIN, 1.0, HabitatCategory.SHRUB, 0.50), false);
        put(m, "bezoar_ibex", "tcecology:bezoar_ibex", "minecraft:goat", MobCategory.CREATURE, 52, 2, 5, 4.0, 10.0, 500.0, 500.0,
                Map.of(HabitatCategory.MOUNTAIN, 1.0), false);
        put(m, "guanaco", "tcecology:guanaco", "minecraft:llama", MobCategory.CREATURE, 48, 3, 6, 7.0, 9.0, 450.0, 450.0,
                Map.of(HabitatCategory.MOUNTAIN, 0.95, HabitatCategory.GRASS, 0.30), false);

        // Arid & desert
        put(m, "wild_ass", "tcecology:wild_ass", "minecraft:donkey", MobCategory.CREATURE, 40, 2, 5, 25.0, 9.0, 180.0, 260.0,
                Map.of(HabitatCategory.ARID, 1.0, HabitatCategory.SHRUB, 0.45), false);
        put(m, "camel", "minecraft:camel", "minecraft:camel", MobCategory.CREATURE, 28, 1, 3, 27.0, 8.0, 120.0, 220.0,
                Map.of(HabitatCategory.ARID, 1.0), false);

        // Tropical forest
        put(m, "red_junglefowl", "tcecology:red_junglefowl", "minecraft:chicken", MobCategory.CREATURE, 72, 3, 6, 25.0, 6.0, 1800.0, 900.0,
                Map.of(HabitatCategory.TROPICAL, 1.0, HabitatCategory.FOREST, 0.50), false);
        put(m, "parrot", "minecraft:parrot", "minecraft:parrot", MobCategory.CREATURE, 64, 2, 5, 25.0, 6.0, 1900.0, 900.0,
                Map.of(HabitatCategory.TROPICAL, 1.0), false);
        put(m, "ocelot", "minecraft:ocelot", "minecraft:ocelot", MobCategory.CREATURE, 24, 1, 2, 25.0, 6.0, 1900.0, 900.0,
                Map.of(HabitatCategory.TROPICAL, 1.0), false);
        put(m, "panda", "minecraft:panda", "minecraft:panda", MobCategory.CREATURE, 20, 1, 2, 20.0, 6.0, 1600.0, 700.0,
                Map.of(HabitatCategory.TROPICAL, 0.80, HabitatCategory.MOUNTAIN, 0.50), false);

        // Wetland & coastal
        put(m, "frog", "minecraft:frog", "minecraft:frog", MobCategory.CREATURE, 68, 2, 5, 17.0, 12.0, 1100.0, 800.0,
                Map.of(HabitatCategory.WETLAND, 1.0, HabitatCategory.FRESHWATER, 0.60), false);
        put(m, "turtle", "minecraft:turtle", "minecraft:turtle", MobCategory.CREATURE, 36, 2, 5, 21.0, 9.0, 900.0, 800.0,
                Map.of(HabitatCategory.COASTAL, 1.0, HabitatCategory.WETLAND, 0.45), false);

        // Ambient
        put(m, "bat", "minecraft:bat", "minecraft:bat", MobCategory.AMBIENT, 40, 2, 6, 13.0, 18.0, 800.0, 1000.0,
                Map.of(HabitatCategory.FOREST, 0.7, HabitatCategory.MOUNTAIN, 0.7, HabitatCategory.UNDERGROUND_WATER, 0.9), false);

        // Aquatic & marine
        put(m, "salmon", "minecraft:salmon", "minecraft:salmon", MobCategory.WATER_AMBIENT, 120, 3, 7, 8.0, 11.0, 850.0, 900.0,
                Map.of(HabitatCategory.FRESHWATER, 1.0, HabitatCategory.COASTAL, 0.70, HabitatCategory.POLAR_SEA, 0.65), false);
        put(m, "cod", "minecraft:cod", "minecraft:cod", MobCategory.WATER_AMBIENT, 120, 3, 8, 9.0, 12.0, 800.0, 900.0,
                Map.of(HabitatCategory.PELAGIC, 1.0, HabitatCategory.COASTAL, 0.80, HabitatCategory.POLAR_SEA, 0.75), false);
        put(m, "tropical_fish", "minecraft:tropical_fish", "minecraft:tropical_fish", MobCategory.WATER_AMBIENT, 140, 4, 8, 25.0, 6.0, 1200.0, 1100.0,
                Map.of(HabitatCategory.REEF, 1.0, HabitatCategory.PELAGIC, 0.65, HabitatCategory.FRESHWATER, 0.45), false);
        put(m, "pufferfish", "minecraft:pufferfish", "minecraft:pufferfish", MobCategory.WATER_AMBIENT, 48, 1, 3, 25.0, 5.0, 1200.0, 1100.0,
                Map.of(HabitatCategory.REEF, 1.0), false);
        put(m, "tadpole", "minecraft:tadpole", "minecraft:tadpole", MobCategory.WATER_AMBIENT, 48, 2, 5, 17.0, 10.0, 1100.0, 800.0,
                Map.of(HabitatCategory.WETLAND, 1.0), false);
        put(m, "squid", "minecraft:squid", "minecraft:squid", MobCategory.WATER_CREATURE, 64, 2, 4, 13.0, 16.0, 800.0, 1100.0,
                Map.of(HabitatCategory.PELAGIC, 1.0, HabitatCategory.COASTAL, 0.75, HabitatCategory.FRESHWATER, 0.45), false);
        put(m, "dolphin", "minecraft:dolphin", "minecraft:dolphin", MobCategory.WATER_CREATURE, 36, 1, 3, 20.0, 9.0, 900.0, 1100.0,
                Map.of(HabitatCategory.PELAGIC, 0.90, HabitatCategory.COASTAL, 0.85, HabitatCategory.REEF, 0.95), false);
        put(m, "glow_squid", "minecraft:glow_squid", "minecraft:glow_squid", MobCategory.UNDERGROUND_WATER_CREATURE, 44, 2, 4, 13.0, 18.0, 800.0, 1200.0,
                Map.of(HabitatCategory.UNDERGROUND_WATER, 1.0), false);
        put(m, "axolotl", "minecraft:axolotl", "minecraft:axolotl", MobCategory.AXOLOTLS, 32, 2, 4, 18.0, 9.0, 1200.0, 900.0,
                Map.of(HabitatCategory.UNDERGROUND_WATER, 1.0), false);
        return m;
    }

    private static void put(
            Map<Identifier, SpeciesEnvelope> map,
            String shortId,
            String entity,
            String fallbackEntity,
            MobCategory cat,
            int kBase,
            int minGroup,
            int maxGroup,
            double tOpt,
            double tWidth,
            double pOpt,
            double pWidth,
            Map<HabitatCategory, Double> prefs,
            boolean relict
    ) {
        Identifier id = Identifier.fromNamespaceAndPath(TEcology.MOD_ID, shortId);
        map.put(id, new SpeciesEnvelope(
                id,
                Identifier.parse(entity),
                Identifier.parse(fallbackEntity),
                cat,
                kBase,
                minGroup,
                maxGroup,
                tOpt,
                tWidth,
                pOpt,
                pWidth,
                prefs,
                relict
        ));
    }
}
