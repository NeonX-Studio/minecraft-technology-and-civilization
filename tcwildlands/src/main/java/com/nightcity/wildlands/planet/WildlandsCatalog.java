package com.nightcity.wildlands.planet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;

/**
 * Authoritative geological, mineralogical, and biome catalog matching 10 §6, §7, §16.7, and Appendix A.
 * Guarantees zero vanilla stone/deepslate/ores and strictly valid 87 (host, mineral) ore combinations.
 */
public final class WildlandsCatalog {
    public static final String MOD_ID = "tcwildlands";

    public enum RockCategory {
        IGNEOUS,
        SEDIMENTARY,
        METAMORPHIC
    }

    public record RockSpec(
            int paletteIndex,
            Identifier id,
            Identifier cobbleId,
            Identifier bricksId,
            Identifier polishedId,
            String hostPrefix,
            RockCategory category,
            float hardness,
            float blastResistance,
            float kRock,
            int rmrBase,
            float identifyDifficulty,
            float knappingQuality
    ) {
    }

    public enum XpTier {
        NONE(0, 0),
        COAL(0, 2),
        MASSIVE_SULFIDE(0, 1),
        INDUSTRIAL(1, 3),
        DECORATIVE_GEM(2, 5),
        PRECIOUS_GEM(3, 7);

        private final int minXp;
        private final int maxXp;

        XpTier(int minXp, int maxXp) {
            this.minXp = minXp;
            this.maxXp = maxXp;
        }

        public int minXp() {
            return minXp;
        }

        public int maxXp() {
            return maxXp;
        }
    }

    public record MineralSpec(
            String key,
            Identifier id,
            Identifier rawItemId,
            float abundanceFraction,
            float gradeMin,
            float gradeMax,
            boolean needsIronTool,
            XpTier xpTier,
            int burnTimeTicks,
            List<String> validHosts
    ) {
    }

    public enum BiomeGroup {
        OCEAN,
        COAST,
        FRESHWATER,
        WETLAND,
        TROPICAL,
        ARID,
        TEMPERATE,
        CONTINENTAL,
        POLAR,
        HIGHLAND,
        VOLCANIC,
        DEEP_EARTH
    }

    public record BiomeSpec(
            int index,
            String path,
            Identifier id,
            BiomeGroup group,
            float vanillaTemperature,
            float vanillaDownfall,
            Identifier topBlock,
            Identifier underBlock,
            Identifier parentSoil
    ) {
    }

    public static final List<RockSpec> ROCKS;
    public static final Map<Identifier, RockSpec> ROCK_BY_ID;
    public static final Map<String, RockSpec> ROCK_BY_HOST;
    public static final List<MineralSpec> MINERALS;
    public static final Map<Identifier, MineralSpec> MINERAL_BY_ID;
    public static final Map<String, MineralSpec> MINERAL_BY_KEY;
    public static final Map<String, Identifier> LEGAL_ORE_BLOCKS;
    public static final Set<String> ALL_ORE_BLOCK_PATHS;
    public static final List<BiomeSpec> BIOMES;
    public static final Map<Identifier, BiomeSpec> BIOME_BY_ID;
    public static final Map<String, BiomeSpec> BIOME_BY_PATH;

    public static final Set<Identifier> FORBIDDEN_PLANET_BLOCKS = Set.of(
            mc("stone"), mc("deepslate"),
            mc("coal_ore"), mc("deepslate_coal_ore"),
            mc("iron_ore"), mc("deepslate_iron_ore"),
            mc("copper_ore"), mc("deepslate_copper_ore"),
            mc("gold_ore"), mc("deepslate_gold_ore"),
            mc("redstone_ore"), mc("deepslate_redstone_ore"),
            mc("emerald_ore"), mc("deepslate_emerald_ore"),
            mc("lapis_ore"), mc("deepslate_lapis_ore"),
            mc("diamond_ore"), mc("deepslate_diamond_ore"),
            mc("nether_gold_ore"), mc("nether_quartz_ore"), mc("ancient_debris")
    );

    static {
        List<RockSpec> rocks = new ArrayList<>();
        addVanillaRock(rocks, "granite", mc("granite"), wl("granite_cobble"), wl("granite_bricks"), mc("polished_granite"), RockCategory.IGNEOUS, 2.0f, 6f, 0.6f, 75, 0.10f, 0.2f);
        addVanillaRock(rocks, "diorite", mc("diorite"), wl("diorite_cobble"), wl("diorite_bricks"), mc("polished_diorite"), RockCategory.IGNEOUS, 2.0f, 6f, 0.6f, 72, 0.15f, 0.2f);
        addVanillaRock(rocks, "andesite", mc("andesite"), wl("andesite_cobble"), wl("andesite_bricks"), mc("polished_andesite"), RockCategory.IGNEOUS, 1.8f, 6f, 0.8f, 65, 0.15f, 0.4f);
        addModRock(rocks, "gabbro", RockCategory.IGNEOUS, 2.2f, 6f, 0.5f, 78, 0.20f, 0.2f);
        addModRock(rocks, "rhyolite", RockCategory.IGNEOUS, 1.8f, 6f, 0.8f, 62, 0.25f, 0.6f);
        addModRock(rocks, "basalt_rock", RockCategory.IGNEOUS, 2.0f, 6f, 0.7f, 70, 0.05f, 0.5f);
        addVanillaRock(rocks, "tuff", mc("tuff"), wl("tuff_cobble"), mc("tuff_bricks"), mc("polished_tuff"), RockCategory.IGNEOUS, 1.5f, 6f, 1.6f, 45, 0.20f, 0.1f);
        addVanillaRock(rocks, "obsidian", mc("obsidian"), wl("obsidian_cobble"), wl("obsidian_bricks"), wl("polished_obsidian"), RockCategory.IGNEOUS, 50.0f, 1200f, 0.3f, 80, 0.05f, 1.0f);
        addModRock(rocks, "pumice", RockCategory.IGNEOUS, 0.8f, 3f, 2.0f, 30, 0.15f, 0.0f);
        addModRock(rocks, "peridotite", RockCategory.IGNEOUS, 2.2f, 6f, 0.7f, 68, 0.30f, 0.1f);
        addModRock(rocks, "sandstone_rock", RockCategory.SEDIMENTARY, 1.2f, 4f, 1.4f, 55, 0.10f, 0.2f);
        addModRock(rocks, "shale", RockCategory.SEDIMENTARY, 1.0f, 3f, 1.8f, 35, 0.15f, 0.1f);
        addModRock(rocks, "limestone", RockCategory.SEDIMENTARY, 1.3f, 5f, 1.0f, 60, 0.10f, 0.2f);
        addModRock(rocks, "dolomite", RockCategory.SEDIMENTARY, 1.5f, 5f, 0.9f, 62, 0.30f, 0.2f);
        addModRock(rocks, "chalk", RockCategory.SEDIMENTARY, 0.8f, 3f, 2.0f, 35, 0.05f, 0.0f);
        addModRock(rocks, "conglomerate", RockCategory.SEDIMENTARY, 1.4f, 5f, 1.2f, 50, 0.20f, 0.1f);
        addModRock(rocks, "mudstone", RockCategory.SEDIMENTARY, 0.9f, 3f, 2.0f, 30, 0.20f, 0.1f);
        addModRock(rocks, "rock_salt", RockCategory.SEDIMENTARY, 1.0f, 3f, 0.5f, 50, 0.10f, 0.0f);
        addModRock(rocks, "gypsum_rock", RockCategory.SEDIMENTARY, 0.9f, 3f, 1.6f, 40, 0.25f, 0.0f);
        addModRock(rocks, "slate", RockCategory.METAMORPHIC, 1.6f, 6f, 0.8f, 60, 0.15f, 0.3f);
        addModRock(rocks, "schist", RockCategory.METAMORPHIC, 1.5f, 5f, 1.0f, 50, 0.25f, 0.1f);
        addModRock(rocks, "gneiss", RockCategory.METAMORPHIC, 2.2f, 6f, 0.5f, 78, 0.20f, 0.1f);
        addModRock(rocks, "marble", RockCategory.METAMORPHIC, 1.6f, 6f, 0.9f, 65, 0.20f, 0.1f);
        addModRock(rocks, "quartzite", RockCategory.METAMORPHIC, 2.4f, 7f, 0.4f, 82, 0.25f, 0.6f);
        addModRock(rocks, "pegmatite", RockCategory.IGNEOUS, 2.0f, 6f, 0.6f, 70, 0.30f, 0.1f);
        addModRock(rocks, "kimberlite", RockCategory.IGNEOUS, 1.6f, 5f, 1.4f, 40, 0.45f, 0.0f);
        addModRock(rocks, "eclogite", RockCategory.METAMORPHIC, 2.6f, 8f, 0.4f, 82, 0.50f, 0.1f);

        ROCKS = Collections.unmodifiableList(rocks);
        Map<Identifier, RockSpec> rById = new LinkedHashMap<>();
        Map<String, RockSpec> rByHost = new LinkedHashMap<>();
        for (RockSpec r : ROCKS) {
            rById.put(r.id(), r);
            rByHost.put(r.hostPrefix(), r);
        }
        ROCK_BY_ID = Collections.unmodifiableMap(rById);
        ROCK_BY_HOST = Collections.unmodifiableMap(rByHost);

        List<MineralSpec> mins = new ArrayList<>();
        addMin(mins, "native_copper", 0.040f, 0.3f, 0.6f, false, XpTier.NONE, 0, List.of("basalt_rock", "sandstone_rock", "conglomerate"));
        addMin(mins, "malachite", 0.050f, 0.3f, 0.6f, false, XpTier.NONE, 0, List.of("limestone", "sandstone_rock"));
        addMin(mins, "chalcopyrite", 0.070f, 0.3f, 0.7f, true, XpTier.NONE, 0, List.of("diorite", "andesite", "granite", "basalt_rock"));
        addMin(mins, "cassiterite", 0.030f, 0.3f, 0.6f, false, XpTier.NONE, 0, List.of("granite", "pegmatite", "gravel"));
        addMin(mins, "galena", 0.045f, 0.3f, 0.7f, false, XpTier.NONE, 0, List.of("limestone", "dolomite", "granite"));
        addMin(mins, "sphalerite", 0.045f, 0.3f, 0.7f, true, XpTier.NONE, 0, List.of("limestone", "dolomite", "shale"));
        addMin(mins, "calamine", 0.020f, 0.3f, 0.5f, false, XpTier.NONE, 0, List.of("limestone", "dolomite"));
        addMin(mins, "bog_iron", 0.040f, 0.2f, 0.4f, false, XpTier.NONE, 0, List.of("mud", "peat"));
        addMin(mins, "hematite", 0.050f, 0.4f, 0.8f, true, XpTier.NONE, 0, List.of("quartzite", "shale", "sandstone_rock"));
        addMin(mins, "magnetite", 0.040f, 0.4f, 0.8f, true, XpTier.NONE, 0, List.of("gabbro", "gneiss", "marble"));
        addMin(mins, "native_gold", 0.035f, 0.1f, 0.4f, false, XpTier.NONE, 0, List.of("slate", "schist", "gravel", "conglomerate", "andesite"));
        addMin(mins, "native_silver", 0.020f, 0.1f, 0.4f, false, XpTier.NONE, 0, List.of("andesite", "rhyolite", "limestone"));
        addMin(mins, "cinnabar", 0.010f, 0.2f, 0.5f, false, XpTier.NONE, 0, List.of("tuff", "sandstone_rock"));
        addMin(mins, "bauxite", 0.030f, 0.4f, 0.8f, false, XpTier.NONE, 0, List.of("laterite", "limestone"));
        addMin(mins, "chromite", 0.010f, 0.3f, 0.7f, true, XpTier.NONE, 0, List.of("peridotite", "gabbro"));
        addMin(mins, "pentlandite", 0.015f, 0.3f, 0.6f, true, XpTier.NONE, 0, List.of("gabbro", "peridotite"));
        addMin(mins, "wolframite", 0.012f, 0.2f, 0.5f, true, XpTier.NONE, 0, List.of("granite", "pegmatite"));
        addMin(mins, "molybdenite", 0.010f, 0.1f, 0.4f, true, XpTier.NONE, 0, List.of("granite", "diorite"));
        addMin(mins, "uraninite", 0.008f, 0.1f, 0.4f, true, XpTier.NONE, 0, List.of("granite", "sandstone_rock", "conglomerate"));
        addMin(mins, "ilmenite", 0.015f, 0.3f, 0.6f, true, XpTier.NONE, 0, List.of("gabbro", "sand"));
        addMin(mins, "rutile", 0.010f, 0.2f, 0.5f, true, XpTier.NONE, 0, List.of("schist", "sand", "eclogite"));
        addMin(mins, "spodumene", 0.008f, 0.2f, 0.5f, true, XpTier.NONE, 0, List.of("pegmatite"));
        addMin(mins, "monazite", 0.008f, 0.1f, 0.4f, true, XpTier.NONE, 0, List.of("granite", "sand"));
        addMin(mins, "cobaltite", 0.006f, 0.1f, 0.4f, true, XpTier.NONE, 0, List.of("schist", "gneiss"));
        addMin(mins, "sulfur", 0.020f, 0.4f, 0.8f, false, XpTier.INDUSTRIAL, 0, List.of("tuff", "basalt_rock", "gypsum_rock"));
        addMin(mins, "niter", 0.015f, 0.3f, 0.6f, false, XpTier.INDUSTRIAL, 0, List.of("limestone", "sandstone_rock"));
        addMin(mins, "sylvite", 0.015f, 0.3f, 0.7f, false, XpTier.INDUSTRIAL, 0, List.of("rock_salt"));
        addMin(mins, "phosphorite", 0.015f, 0.3f, 0.6f, false, XpTier.INDUSTRIAL, 0, List.of("limestone", "mudstone"));
        addMin(mins, "graphite", 0.015f, 0.3f, 0.7f, false, XpTier.INDUSTRIAL, 0, List.of("schist", "gneiss", "marble"));
        addMin(mins, "kaolinite", 0.025f, 0.5f, 0.9f, false, XpTier.NONE, 0, List.of());
        addMin(mins, "flint", 0.050f, 0.4f, 0.8f, false, XpTier.INDUSTRIAL, 0, List.of("chalk", "limestone"));
        addMin(mins, "peat", 0.0f, 0.5f, 0.8f, false, XpTier.NONE, 600, List.of());
        addMin(mins, "lignite", 0.030f, 0.5f, 0.8f, false, XpTier.COAL, 1200, List.of());
        addMin(mins, "bituminous_coal", 0.035f, 0.5f, 0.9f, false, XpTier.COAL, 1800, List.of());
        addMin(mins, "anthracite", 0.010f, 0.6f, 0.9f, true, XpTier.COAL, 2800, List.of());
        addMin(mins, "diamond", 0.003f, 0.05f, 0.2f, true, XpTier.PRECIOUS_GEM, 0, List.of("kimberlite", "gravel", "eclogite"));
        addMin(mins, "emerald", 0.003f, 0.05f, 0.2f, true, XpTier.PRECIOUS_GEM, 0, List.of("schist", "pegmatite"));
        addMin(mins, "ruby", 0.003f, 0.05f, 0.2f, true, XpTier.PRECIOUS_GEM, 0, List.of("marble", "gravel"));
        addMin(mins, "sapphire", 0.003f, 0.05f, 0.2f, true, XpTier.PRECIOUS_GEM, 0, List.of("gneiss", "gravel"));
        addMin(mins, "jade", 0.004f, 0.1f, 0.3f, false, XpTier.DECORATIVE_GEM, 0, List.of("peridotite", "schist"));
        addMin(mins, "lapis", 0.004f, 0.1f, 0.3f, false, XpTier.DECORATIVE_GEM, 0, List.of("marble"));
        addMin(mins, "amethyst", 0.005f, 0.2f, 0.5f, false, XpTier.DECORATIVE_GEM, 0, List.of("basalt_rock"));

        MINERALS = Collections.unmodifiableList(mins);
        Map<Identifier, MineralSpec> mById = new LinkedHashMap<>();
        Map<String, MineralSpec> mByKey = new LinkedHashMap<>();
        Map<String, Identifier> legalOres = new LinkedHashMap<>();
        LinkedHashSet<String> orePaths = new LinkedHashSet<>();
        for (MineralSpec m : MINERALS) {
            mById.put(m.id(), m);
            mByKey.put(m.key(), m);
            for (String host : m.validHosts()) {
                String path = host + "_" + m.key() + "_ore";
                legalOres.put(host + ":" + m.key(), wl(path));
                orePaths.add(path);
            }
        }
        orePaths.add("lignite_seam");
        orePaths.add("bituminous_coal_seam");
        orePaths.add("anthracite_seam");
        MINERAL_BY_ID = Collections.unmodifiableMap(mById);
        MINERAL_BY_KEY = Collections.unmodifiableMap(mByKey);
        LEGAL_ORE_BLOCKS = Collections.unmodifiableMap(legalOres);
        ALL_ORE_BLOCK_PATHS = Collections.unmodifiableSet(orePaths);

        List<BiomeSpec> biomes = new ArrayList<>();
        addBiome(biomes, "polar_sea_ice", BiomeGroup.OCEAN, 0.15f, 0.25f, mc("gravel"), wl("till"), wl("till"));
        addBiome(biomes, "polar_deep_ocean", BiomeGroup.OCEAN, 0.15f, 0.35f, mc("gravel"), wl("till"), wl("till"));
        addBiome(biomes, "cold_shelf_sea", BiomeGroup.OCEAN, 0.35f, 0.65f, mc("sand"), mc("gravel"), wl("silt"));
        addBiome(biomes, "cold_deep_ocean", BiomeGroup.OCEAN, 0.35f, 0.65f, mc("mud"), wl("silt"), wl("silt"));
        addBiome(biomes, "temperate_shelf_sea", BiomeGroup.OCEAN, 0.60f, 0.70f, mc("sand"), wl("silt"), wl("silt"));
        addBiome(biomes, "temperate_deep_ocean", BiomeGroup.OCEAN, 0.60f, 0.70f, mc("mud"), wl("silt"), wl("silt"));
        addBiome(biomes, "tropical_shelf_sea", BiomeGroup.OCEAN, 0.90f, 0.80f, mc("sand"), wl("limestone"), wl("silt"));
        addBiome(biomes, "tropical_deep_ocean", BiomeGroup.OCEAN, 0.90f, 0.80f, mc("mud"), wl("silt"), wl("silt"));
        addBiome(biomes, "tropical_coral_reef", BiomeGroup.OCEAN, 0.95f, 0.85f, mc("sand"), wl("limestone"), wl("limestone"));
        addBiome(biomes, "abyssal_trench", BiomeGroup.OCEAN, 0.20f, 0.50f, wl("basalt_rock"), mc("mud"), wl("basalt_rock"));
        addBiome(biomes, "tropical_mangrove_coast", BiomeGroup.COAST, 0.95f, 0.90f, mc("mud"), wl("alluvium"), wl("alluvium"));
        addBiome(biomes, "tropical_sandy_beach", BiomeGroup.COAST, 0.90f, 0.60f, mc("sand"), mc("sand"), wl("quartz_sand"));
        addBiome(biomes, "temperate_sandy_beach", BiomeGroup.COAST, 0.60f, 0.60f, mc("sand"), wl("quartz_sand"), wl("quartz_sand"));
        addBiome(biomes, "temperate_salt_marsh", BiomeGroup.COAST, 0.55f, 0.75f, mc("mud"), wl("silt"), wl("silt"));
        addBiome(biomes, "cold_rocky_shore", BiomeGroup.COAST, 0.25f, 0.65f, mc("gravel"), wl("gneiss"), wl("till"));
        addBiome(biomes, "cold_fjord_coast", BiomeGroup.COAST, 0.25f, 0.85f, wl("gneiss"), wl("till"), wl("till"));
        addBiome(biomes, "tropical_river", BiomeGroup.FRESHWATER, 0.95f, 0.85f, mc("sand"), mc("mud"), wl("alluvium"));
        addBiome(biomes, "temperate_river", BiomeGroup.FRESHWATER, 0.60f, 0.65f, mc("gravel"), mc("sand"), wl("alluvium"));
        addBiome(biomes, "cold_river", BiomeGroup.FRESHWATER, 0.20f, 0.55f, mc("gravel"), wl("till"), wl("till"));
        addBiome(biomes, "temperate_lake", BiomeGroup.FRESHWATER, 0.60f, 0.65f, mc("sand"), mc("mud"), wl("silt"));
        addBiome(biomes, "cold_lake", BiomeGroup.FRESHWATER, 0.20f, 0.55f, mc("gravel"), mc("mud"), wl("till"));
        addBiome(biomes, "arid_salt_lake", BiomeGroup.FRESHWATER, 0.80f, 0.10f, wl("salt_crust"), wl("rock_salt"), wl("silt"));
        addBiome(biomes, "tropical_delta", BiomeGroup.FRESHWATER, 0.95f, 0.90f, mc("grass_block"), wl("alluvium"), wl("alluvium"));
        addBiome(biomes, "temperate_delta", BiomeGroup.FRESHWATER, 0.60f, 0.75f, mc("grass_block"), wl("alluvium"), wl("alluvium"));
        addBiome(biomes, "temperate_floodplain", BiomeGroup.FRESHWATER, 0.60f, 0.70f, mc("grass_block"), wl("alluvium"), wl("alluvium"));
        addBiome(biomes, "tropical_floodplain", BiomeGroup.FRESHWATER, 0.95f, 0.85f, mc("grass_block"), wl("alluvium"), wl("alluvium"));
        addBiome(biomes, "tropical_swamp", BiomeGroup.WETLAND, 0.95f, 0.95f, mc("mud"), wl("peat"), wl("peat"));
        addBiome(biomes, "temperate_marsh", BiomeGroup.WETLAND, 0.55f, 0.85f, mc("grass_block"), wl("peat"), wl("peat"));
        addBiome(biomes, "boreal_peat_bog", BiomeGroup.WETLAND, 0.20f, 0.75f, mc("grass_block"), wl("peat"), wl("permafrost"));
        addBiome(biomes, "tropical_rainforest", BiomeGroup.TROPICAL, 1.00f, 1.00f, mc("grass_block"), wl("laterite"), wl("saprolite"));
        addBiome(biomes, "tropical_monsoon_forest", BiomeGroup.TROPICAL, 0.95f, 0.90f, mc("grass_block"), wl("laterite"), wl("saprolite"));
        addBiome(biomes, "tropical_dry_forest", BiomeGroup.TROPICAL, 0.95f, 0.65f, mc("grass_block"), wl("laterite"), wl("saprolite"));
        addBiome(biomes, "tropical_savanna", BiomeGroup.TROPICAL, 0.95f, 0.40f, mc("grass_block"), wl("laterite"), wl("saprolite"));
        addBiome(biomes, "tropical_cloud_forest", BiomeGroup.TROPICAL, 0.65f, 0.95f, mc("grass_block"), wl("saprolite"), wl("saprolite"));
        addBiome(biomes, "hot_sand_desert", BiomeGroup.ARID, 1.20f, 0.02f, mc("sand"), mc("sand"), wl("quartz_sand"));
        addBiome(biomes, "hot_rocky_desert", BiomeGroup.ARID, 1.20f, 0.03f, mc("gravel"), wl("sandstone_rock"), wl("sandstone_rock"));
        addBiome(biomes, "hot_desert_oasis", BiomeGroup.ARID, 1.10f, 0.25f, mc("grass_block"), wl("alluvium"), wl("alluvium"));
        addBiome(biomes, "hot_steppe", BiomeGroup.ARID, 1.00f, 0.20f, mc("grass_block"), wl("loess"), wl("loess"));
        addBiome(biomes, "cold_desert", BiomeGroup.ARID, 0.35f, 0.05f, mc("gravel"), mc("sand"), wl("loess"));
        addBiome(biomes, "cold_steppe", BiomeGroup.ARID, 0.40f, 0.22f, mc("grass_block"), wl("chernozem"), wl("loess"));
        addBiome(biomes, "arid_badlands", BiomeGroup.ARID, 0.95f, 0.08f, mc("terracotta"), wl("mudstone"), wl("mudstone"));
        addBiome(biomes, "arid_salt_flat", BiomeGroup.ARID, 1.00f, 0.02f, wl("salt_crust"), wl("rock_salt"), wl("gypsum_rock"));
        addBiome(biomes, "arid_canyon", BiomeGroup.ARID, 0.95f, 0.08f, wl("sandstone_rock"), wl("sandstone_rock"), wl("sandstone_rock"));
        addBiome(biomes, "humid_subtropical_forest", BiomeGroup.TEMPERATE, 0.78f, 0.80f, mc("grass_block"), mc("dirt"), wl("saprolite"));
        addBiome(biomes, "subtropical_monsoon_forest", BiomeGroup.TEMPERATE, 0.75f, 0.78f, mc("grass_block"), mc("dirt"), wl("saprolite"));
        addBiome(biomes, "subtropical_karst_hills", BiomeGroup.TEMPERATE, 0.75f, 0.82f, mc("grass_block"), wl("terra_rossa"), wl("limestone"));
        addBiome(biomes, "mediterranean_shrubland", BiomeGroup.TEMPERATE, 0.75f, 0.35f, mc("grass_block"), wl("terra_rossa"), wl("limestone"));
        addBiome(biomes, "mediterranean_woodland", BiomeGroup.TEMPERATE, 0.70f, 0.50f, mc("grass_block"), wl("terra_rossa"), wl("limestone"));
        addBiome(biomes, "temperate_broadleaf_forest", BiomeGroup.TEMPERATE, 0.60f, 0.70f, mc("grass_block"), mc("dirt"), wl("loess"));
        addBiome(biomes, "temperate_rainforest", BiomeGroup.TEMPERATE, 0.52f, 0.98f, mc("grass_block"), mc("dirt"), wl("saprolite"));
        addBiome(biomes, "temperate_grassland", BiomeGroup.CONTINENTAL, 0.50f, 0.38f, mc("grass_block"), wl("chernozem"), wl("loess"));
        addBiome(biomes, "temperate_mixed_forest", BiomeGroup.CONTINENTAL, 0.45f, 0.60f, mc("grass_block"), mc("dirt"), wl("till"));
        addBiome(biomes, "boreal_forest", BiomeGroup.CONTINENTAL, 0.25f, 0.55f, mc("grass_block"), mc("dirt"), wl("till"));
        addBiome(biomes, "boreal_larch_forest", BiomeGroup.CONTINENTAL, 0.18f, 0.40f, mc("grass_block"), wl("permafrost"), wl("till"));
        addBiome(biomes, "polar_tundra", BiomeGroup.POLAR, 0.15f, 0.30f, mc("grass_block"), wl("permafrost"), wl("till"));
        addBiome(biomes, "polar_ice_sheet", BiomeGroup.POLAR, 0.15f, 0.15f, mc("snow_block"), mc("packed_ice"), mc("blue_ice"));
        addBiome(biomes, "temperate_montane_forest", BiomeGroup.HIGHLAND, 0.35f, 0.70f, mc("grass_block"), mc("dirt"), wl("till"));
        addBiome(biomes, "alpine_meadow", BiomeGroup.HIGHLAND, 0.25f, 0.65f, mc("grass_block"), mc("dirt"), wl("till"));
        addBiome(biomes, "alpine_tundra", BiomeGroup.HIGHLAND, 0.18f, 0.45f, mc("gravel"), wl("permafrost"), wl("till"));
        addBiome(biomes, "alpine_rocky_peaks", BiomeGroup.HIGHLAND, 0.15f, 0.50f, wl("slate"), wl("gneiss"), wl("gneiss"));
        addBiome(biomes, "alpine_glacier", BiomeGroup.HIGHLAND, 0.15f, 0.75f, mc("snow_block"), mc("packed_ice"), mc("blue_ice"));
        addBiome(biomes, "cold_high_plateau", BiomeGroup.HIGHLAND, 0.22f, 0.25f, mc("gravel"), wl("permafrost"), wl("loess"));
        addBiome(biomes, "volcanic_lava_field", BiomeGroup.VOLCANIC, 0.85f, 0.40f, wl("volcanic_ash"), wl("black_sand"), wl("basalt_rock"));
        addBiome(biomes, "volcanic_geothermal_field", BiomeGroup.VOLCANIC, 0.80f, 0.60f, mc("tuff"), wl("rhyolite"), wl("rhyolite"));
        addBiome(biomes, "deep_granite_batholith", BiomeGroup.DEEP_EARTH, 1.50f, 0.0f, mc("granite"), wl("pegmatite"), wl("gneiss"));
        addBiome(biomes, "deep_hydrothermal_caverns", BiomeGroup.DEEP_EARTH, 1.80f, 0.0f, wl("gneiss"), wl("massive_sulfide"), wl("gneiss"));
        addBiome(biomes, "deep_magma_chambers", BiomeGroup.DEEP_EARTH, 2.00f, 0.0f, wl("basalt_rock"), wl("gabbro"), wl("peridotite"));
        addBiome(biomes, "deep_mantle_plume", BiomeGroup.DEEP_EARTH, 2.00f, 0.0f, wl("peridotite"), wl("gabbro"), wl("peridotite"));
        addBiome(biomes, "deep_kimberlite_roots", BiomeGroup.DEEP_EARTH, 1.90f, 0.0f, wl("kimberlite"), wl("peridotite"), wl("eclogite"));

        BIOMES = Collections.unmodifiableList(biomes);
        Map<Identifier, BiomeSpec> bById = new LinkedHashMap<>();
        Map<String, BiomeSpec> bByPath = new LinkedHashMap<>();
        for (BiomeSpec b : biomes) {
            bById.put(b.id(), b);
            bByPath.put(b.path(), b);
        }
        BIOME_BY_ID = Collections.unmodifiableMap(bById);
        BIOME_BY_PATH = Collections.unmodifiableMap(bByPath);
    }

    public static Identifier wl(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static Identifier mc(String path) {
        return Identifier.withDefaultNamespace(path);
    }

    public static Identifier resolveLegalOreBlock(Identifier mineralId, Identifier candidateHostRock) {
        String minKey = mineralId.getPath().startsWith("raw_")
                ? mineralId.getPath().substring(4)
                : mineralId.getPath();
        return switch (minKey) {
            case "lignite" -> wl("lignite_seam");
            case "bituminous_coal" -> wl("bituminous_coal_seam");
            case "anthracite" -> wl("anthracite_seam");
            case "peat" -> wl("peat");
            case "kaolinite" -> wl("kaolinite");
            default -> {
                String host = candidateHostRock.getPath();
                Identifier exact = LEGAL_ORE_BLOCKS.get(host + ":" + minKey);
                if (exact != null) {
                    yield exact;
                }
                MineralSpec spec = MINERAL_BY_KEY.get(minKey);
                if (spec != null && !spec.validHosts().isEmpty()) {
                    String primaryHost = spec.validHosts().getFirst();
                    yield LEGAL_ORE_BLOCKS.getOrDefault(primaryHost + ":" + minKey, wl("granite_chalcopyrite_ore"));
                }
                yield wl("granite_chalcopyrite_ore");
            }
        };
    }

    public static RockSpec rockByPalette(int idx) {
        return ROCKS.get(Math.clamp(idx, 0, ROCKS.size() - 1));
    }

    public static BiomeSpec biomeByIndex(int idx) {
        return BIOMES.get(Math.clamp(idx, 0, BIOMES.size() - 1));
    }

    private static void addModRock(List<RockSpec> out, String name, RockCategory cat, float hardness, float blast,
                                   float kRock, int rmr, float diff, float knapping) {
        out.add(new RockSpec(
                out.size(),
                wl(name),
                wl(name + "_cobble"),
                wl(name + "_bricks"),
                wl("polished_" + name),
                name,
                cat,
                hardness,
                blast,
                kRock,
                rmr,
                diff,
                knapping
        ));
    }

    private static void addVanillaRock(List<RockSpec> out, String hostPrefix, Identifier raw, Identifier cobble,
                                       Identifier bricks, Identifier polished, RockCategory cat, float hardness,
                                       float blast, float kRock, int rmr, float diff, float knapping) {
        out.add(new RockSpec(out.size(), raw, cobble, bricks, polished, hostPrefix, cat, hardness, blast, kRock, rmr, diff, knapping));
    }

    private static void addMin(List<MineralSpec> out, String key, float abundance, float gMin, float gMax,
                               boolean ironTool, XpTier xp, int burnTicks, List<String> hosts) {
        out.add(new MineralSpec(key, wl(key), wl("raw_" + key), abundance, gMin, gMax, ironTool, xp, burnTicks, hosts));
    }

    private static void addBiome(List<BiomeSpec> out, String path, BiomeGroup group, float temp, float downfall,
                                 Identifier top, Identifier under, Identifier parent) {
        out.add(new BiomeSpec(out.size(), path, wl(path), group, Math.max(0.15f, temp), downfall, top, under, parent));
    }

    private WildlandsCatalog() {
    }
}
