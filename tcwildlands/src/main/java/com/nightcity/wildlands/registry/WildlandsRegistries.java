package com.nightcity.wildlands.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WildlandsRegistries {
    public static final String MOD_ID = "tcwildlands";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    private static final AtomicBoolean REGISTERED_ON_BUS = new AtomicBoolean(false);

    private static final List<String> BLOCK_PATHS = List.of(
        "gabbro", "gabbro_cobble", "gabbro_bricks", "polished_gabbro", "rhyolite", "rhyolite_cobble",
        "rhyolite_bricks", "polished_rhyolite", "basalt_rock", "basalt_rock_cobble", "basalt_rock_bricks", "polished_basalt_rock",
        "pumice", "pumice_cobble", "pumice_bricks", "polished_pumice", "peridotite", "peridotite_cobble",
        "peridotite_bricks", "polished_peridotite", "sandstone_rock", "sandstone_rock_cobble", "sandstone_rock_bricks", "polished_sandstone_rock",
        "shale", "shale_cobble", "shale_bricks", "polished_shale", "limestone", "limestone_cobble",
        "limestone_bricks", "polished_limestone", "dolomite", "dolomite_cobble", "dolomite_bricks", "polished_dolomite",
        "chalk", "chalk_cobble", "chalk_bricks", "polished_chalk", "conglomerate", "conglomerate_cobble",
        "conglomerate_bricks", "polished_conglomerate", "mudstone", "mudstone_cobble", "mudstone_bricks", "polished_mudstone",
        "rock_salt", "rock_salt_cobble", "rock_salt_bricks", "polished_rock_salt", "gypsum_rock", "gypsum_rock_cobble",
        "gypsum_rock_bricks", "polished_gypsum_rock", "slate", "slate_cobble", "slate_bricks", "polished_slate",
        "schist", "schist_cobble", "schist_bricks", "polished_schist", "gneiss", "gneiss_cobble",
        "gneiss_bricks", "polished_gneiss", "marble", "marble_cobble", "marble_bricks", "polished_marble",
        "quartzite", "quartzite_cobble", "quartzite_bricks", "polished_quartzite", "pegmatite", "pegmatite_cobble",
        "pegmatite_bricks", "polished_pegmatite", "kimberlite", "kimberlite_cobble", "kimberlite_bricks", "polished_kimberlite",
        "eclogite", "eclogite_cobble", "eclogite_bricks", "polished_eclogite", "granite_cobble", "granite_bricks",
        "diorite_cobble", "diorite_bricks", "andesite_cobble", "andesite_bricks", "tuff_cobble", "obsidian_cobble",
        "obsidian_bricks", "polished_obsidian", "basalt_rock_native_copper_ore", "sandstone_rock_native_copper_ore", "conglomerate_native_copper_ore", "limestone_malachite_ore",
        "sandstone_rock_malachite_ore", "diorite_chalcopyrite_ore", "andesite_chalcopyrite_ore", "granite_chalcopyrite_ore", "basalt_rock_chalcopyrite_ore", "granite_cassiterite_ore",
        "pegmatite_cassiterite_ore", "gravel_cassiterite_ore", "limestone_galena_ore", "dolomite_galena_ore", "granite_galena_ore", "limestone_sphalerite_ore",
        "dolomite_sphalerite_ore", "shale_sphalerite_ore", "limestone_calamine_ore", "dolomite_calamine_ore", "mud_bog_iron_ore", "peat_bog_iron_ore",
        "quartzite_hematite_ore", "shale_hematite_ore", "sandstone_rock_hematite_ore", "gabbro_magnetite_ore", "gneiss_magnetite_ore", "marble_magnetite_ore",
        "slate_native_gold_ore", "schist_native_gold_ore", "gravel_native_gold_ore", "conglomerate_native_gold_ore", "andesite_native_gold_ore", "andesite_native_silver_ore",
        "rhyolite_native_silver_ore", "limestone_native_silver_ore", "tuff_cinnabar_ore", "sandstone_rock_cinnabar_ore", "laterite_bauxite_ore", "limestone_bauxite_ore",
        "peridotite_chromite_ore", "gabbro_chromite_ore", "gabbro_pentlandite_ore", "peridotite_pentlandite_ore", "granite_wolframite_ore", "pegmatite_wolframite_ore",
        "granite_molybdenite_ore", "diorite_molybdenite_ore", "granite_uraninite_ore", "sandstone_rock_uraninite_ore", "conglomerate_uraninite_ore", "gabbro_ilmenite_ore",
        "sand_ilmenite_ore", "schist_rutile_ore", "sand_rutile_ore", "eclogite_rutile_ore", "pegmatite_spodumene_ore", "granite_monazite_ore",
        "sand_monazite_ore", "schist_cobaltite_ore", "gneiss_cobaltite_ore", "tuff_sulfur_ore", "basalt_rock_sulfur_ore", "gypsum_rock_sulfur_ore",
        "limestone_niter_ore", "sandstone_rock_niter_ore", "rock_salt_sylvite_ore", "limestone_phosphorite_ore", "mudstone_phosphorite_ore", "schist_graphite_ore",
        "gneiss_graphite_ore", "marble_graphite_ore", "chalk_flint_ore", "limestone_flint_ore", "kimberlite_diamond_ore", "gravel_diamond_ore",
        "eclogite_diamond_ore", "schist_emerald_ore", "pegmatite_emerald_ore", "marble_ruby_ore", "gravel_ruby_ore", "gneiss_sapphire_ore",
        "gravel_sapphire_ore", "peridotite_jade_ore", "schist_jade_ore", "marble_lapis_ore", "basalt_rock_amethyst_ore", "lignite_seam",
        "bituminous_coal_seam", "anthracite_seam", "loess", "alluvium", "silt", "chernozem",
        "laterite", "terra_rossa", "marl", "till", "permafrost", "volcanic_ash",
        "black_sand", "quartz_sand", "peat", "kaolinite", "flood_silt", "saprolite",
        "loose_stones", "ore_float", "gossan", "fumarole", "geyser", "bitumen_seep",
        "gas_seep", "lava_crust", "steam_vent", "massive_sulfide", "deep_threshold", "salt_crust",
        "timber_prop", "steel_mine_arch", "rock_bolts"
    );
    private static final List<String> BLOCK_ITEM_PATHS = List.of(
        "gabbro", "gabbro_cobble", "gabbro_bricks", "polished_gabbro", "rhyolite", "rhyolite_cobble",
        "rhyolite_bricks", "polished_rhyolite", "basalt_rock", "basalt_rock_cobble", "basalt_rock_bricks", "polished_basalt_rock",
        "pumice", "pumice_cobble", "pumice_bricks", "polished_pumice", "peridotite", "peridotite_cobble",
        "peridotite_bricks", "polished_peridotite", "sandstone_rock", "sandstone_rock_cobble", "sandstone_rock_bricks", "polished_sandstone_rock",
        "shale", "shale_cobble", "shale_bricks", "polished_shale", "limestone", "limestone_cobble",
        "limestone_bricks", "polished_limestone", "dolomite", "dolomite_cobble", "dolomite_bricks", "polished_dolomite",
        "chalk", "chalk_cobble", "chalk_bricks", "polished_chalk", "conglomerate", "conglomerate_cobble",
        "conglomerate_bricks", "polished_conglomerate", "mudstone", "mudstone_cobble", "mudstone_bricks", "polished_mudstone",
        "rock_salt", "rock_salt_cobble", "rock_salt_bricks", "polished_rock_salt", "gypsum_rock", "gypsum_rock_cobble",
        "gypsum_rock_bricks", "polished_gypsum_rock", "slate", "slate_cobble", "slate_bricks", "polished_slate",
        "schist", "schist_cobble", "schist_bricks", "polished_schist", "gneiss", "gneiss_cobble",
        "gneiss_bricks", "polished_gneiss", "marble", "marble_cobble", "marble_bricks", "polished_marble",
        "quartzite", "quartzite_cobble", "quartzite_bricks", "polished_quartzite", "pegmatite", "pegmatite_cobble",
        "pegmatite_bricks", "polished_pegmatite", "kimberlite", "kimberlite_cobble", "kimberlite_bricks", "polished_kimberlite",
        "eclogite", "eclogite_cobble", "eclogite_bricks", "polished_eclogite", "granite_cobble", "granite_bricks",
        "diorite_cobble", "diorite_bricks", "andesite_cobble", "andesite_bricks", "tuff_cobble", "obsidian_cobble",
        "obsidian_bricks", "polished_obsidian", "basalt_rock_native_copper_ore", "sandstone_rock_native_copper_ore", "conglomerate_native_copper_ore", "limestone_malachite_ore",
        "sandstone_rock_malachite_ore", "diorite_chalcopyrite_ore", "andesite_chalcopyrite_ore", "granite_chalcopyrite_ore", "basalt_rock_chalcopyrite_ore", "granite_cassiterite_ore",
        "pegmatite_cassiterite_ore", "gravel_cassiterite_ore", "limestone_galena_ore", "dolomite_galena_ore", "granite_galena_ore", "limestone_sphalerite_ore",
        "dolomite_sphalerite_ore", "shale_sphalerite_ore", "limestone_calamine_ore", "dolomite_calamine_ore", "mud_bog_iron_ore", "peat_bog_iron_ore",
        "quartzite_hematite_ore", "shale_hematite_ore", "sandstone_rock_hematite_ore", "gabbro_magnetite_ore", "gneiss_magnetite_ore", "marble_magnetite_ore",
        "slate_native_gold_ore", "schist_native_gold_ore", "gravel_native_gold_ore", "conglomerate_native_gold_ore", "andesite_native_gold_ore", "andesite_native_silver_ore",
        "rhyolite_native_silver_ore", "limestone_native_silver_ore", "tuff_cinnabar_ore", "sandstone_rock_cinnabar_ore", "laterite_bauxite_ore", "limestone_bauxite_ore",
        "peridotite_chromite_ore", "gabbro_chromite_ore", "gabbro_pentlandite_ore", "peridotite_pentlandite_ore", "granite_wolframite_ore", "pegmatite_wolframite_ore",
        "granite_molybdenite_ore", "diorite_molybdenite_ore", "granite_uraninite_ore", "sandstone_rock_uraninite_ore", "conglomerate_uraninite_ore", "gabbro_ilmenite_ore",
        "sand_ilmenite_ore", "schist_rutile_ore", "sand_rutile_ore", "eclogite_rutile_ore", "pegmatite_spodumene_ore", "granite_monazite_ore",
        "sand_monazite_ore", "schist_cobaltite_ore", "gneiss_cobaltite_ore", "tuff_sulfur_ore", "basalt_rock_sulfur_ore", "gypsum_rock_sulfur_ore",
        "limestone_niter_ore", "sandstone_rock_niter_ore", "rock_salt_sylvite_ore", "limestone_phosphorite_ore", "mudstone_phosphorite_ore", "schist_graphite_ore",
        "gneiss_graphite_ore", "marble_graphite_ore", "chalk_flint_ore", "limestone_flint_ore", "kimberlite_diamond_ore", "gravel_diamond_ore",
        "eclogite_diamond_ore", "schist_emerald_ore", "pegmatite_emerald_ore", "marble_ruby_ore", "gravel_ruby_ore", "gneiss_sapphire_ore",
        "gravel_sapphire_ore", "peridotite_jade_ore", "schist_jade_ore", "marble_lapis_ore", "basalt_rock_amethyst_ore", "lignite_seam",
        "bituminous_coal_seam", "anthracite_seam", "loess", "alluvium", "silt", "chernozem",
        "laterite", "terra_rossa", "marl", "till", "permafrost", "volcanic_ash",
        "black_sand", "quartz_sand", "peat", "kaolinite", "flood_silt", "saprolite",
        "loose_stones", "ore_float", "gossan", "fumarole", "geyser", "bitumen_seep",
        "gas_seep", "lava_crust", "steam_vent", "massive_sulfide", "deep_threshold", "salt_crust",
        "timber_prop", "steel_mine_arch", "rock_bolts"
    );
    private static final List<String> STANDALONE_ITEM_PATHS = List.of(
        "raw_native_copper", "raw_malachite", "raw_chalcopyrite", "raw_cassiterite", "raw_galena", "raw_sphalerite",
        "raw_calamine", "raw_bog_iron", "raw_hematite", "raw_magnetite", "raw_native_gold", "raw_native_silver",
        "raw_cinnabar", "raw_bauxite", "raw_chromite", "raw_pentlandite", "raw_wolframite", "raw_molybdenite",
        "raw_uraninite", "raw_ilmenite", "raw_rutile", "raw_spodumene", "raw_monazite", "raw_cobaltite",
        "raw_sulfur", "raw_niter", "raw_sylvite", "raw_phosphorite", "raw_graphite", "raw_kaolinite",
        "raw_flint", "raw_peat", "raw_lignite", "raw_bituminous_coal", "raw_anthracite", "raw_diamond",
        "raw_emerald", "raw_ruby", "raw_sapphire", "raw_jade", "raw_lapis", "raw_amethyst",
        "raw_bitumen", "loose_rock", "brine_bucket", "thermal_water_bucket"
    );
    private static final List<String> SOUND_PATHS = List.of(
        "block.rock_tap", "block.panning", "event.landslide"
    );

    public static final Map<String, DeferredBlock<Block>> REGISTERED_BLOCKS;
    public static final Map<String, DeferredItem<? extends Item>> REGISTERED_ITEMS;
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> REGISTERED_SOUNDS;
    public static final Map<String, List<Identifier>> APPENDIX_A_IDS;

    static {
        Map<String, DeferredBlock<Block>> bMap = new LinkedHashMap<>();
        for (String path : BLOCK_PATHS) {
            if (com.nightcity.wildlands.planet.WildlandsCatalog.ALL_ORE_BLOCK_PATHS.contains(path)) {
                String minKey = com.nightcity.wildlands.block.WildlandsOreBlock.mineralKeyFromPath(path);
                bMap.put(path, BLOCKS.registerBlock(
                        path,
                        props -> new com.nightcity.wildlands.block.WildlandsOreBlock(
                                minKey, com.nightcity.wildlands.block.WildlandsOreBlock.xpProviderForPath(path), props),
                        () -> net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(2.4f, 6.0f).requiresCorrectToolForDrops()
                ));
            } else if ("deep_threshold".equals(path)) {
                bMap.put(path, BLOCKS.registerSimpleBlock(path,
                        () -> net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(-1.0f, 3_600_000.0f).lightLevel(s -> 7)));
            } else if ("loose_stones".equals(path)) {
                bMap.put(path, BLOCKS.registerSimpleBlock(path,
                        () -> net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().noCollision().instabreak()));
            } else {
                bMap.put(path, BLOCKS.registerSimpleBlock(path,
                        () -> net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(1.8f, 6.0f)));
            }
        }
        REGISTERED_BLOCKS = Collections.unmodifiableMap(bMap);

        Map<String, DeferredItem<? extends Item>> iMap = new LinkedHashMap<>();
        for (String path : BLOCK_ITEM_PATHS) {
            DeferredBlock<Block> block = bMap.get(path);
            if (block != null) {
                DeferredItem<BlockItem> bi = ITEMS.registerSimpleBlockItem(path, block);
                iMap.put(path, bi);
            }
        }
        for (String path : STANDALONE_ITEM_PATHS) {
            DeferredBlock<Block> block = bMap.get(path);
            if (block != null) {
                DeferredItem<BlockItem> bi = ITEMS.registerSimpleBlockItem(path, block);
                iMap.put(path, bi);
            } else {
                iMap.put(path, ITEMS.registerSimpleItem(path));
            }
        }
        REGISTERED_ITEMS = Collections.unmodifiableMap(iMap);

        Map<String, DeferredHolder<SoundEvent, SoundEvent>> sMap = new LinkedHashMap<>();
        for (String path : SOUND_PATHS) {
            Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, path);
            sMap.put(path, SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(id)));
        }
        REGISTERED_SOUNDS = Collections.unmodifiableMap(sMap);

        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("block", List.of(
                "tcwildlands:gabbro", "tcwildlands:gabbro_cobble", "tcwildlands:gabbro_bricks", "tcwildlands:polished_gabbro", "tcwildlands:rhyolite", "tcwildlands:rhyolite_cobble",
                "tcwildlands:rhyolite_bricks", "tcwildlands:polished_rhyolite", "tcwildlands:basalt_rock", "tcwildlands:basalt_rock_cobble", "tcwildlands:basalt_rock_bricks", "tcwildlands:polished_basalt_rock",
                "tcwildlands:pumice", "tcwildlands:pumice_cobble", "tcwildlands:pumice_bricks", "tcwildlands:polished_pumice", "tcwildlands:peridotite", "tcwildlands:peridotite_cobble",
                "tcwildlands:peridotite_bricks", "tcwildlands:polished_peridotite", "tcwildlands:sandstone_rock", "tcwildlands:sandstone_rock_cobble", "tcwildlands:sandstone_rock_bricks", "tcwildlands:polished_sandstone_rock",
                "tcwildlands:shale", "tcwildlands:shale_cobble", "tcwildlands:shale_bricks", "tcwildlands:polished_shale", "tcwildlands:limestone", "tcwildlands:limestone_cobble",
                "tcwildlands:limestone_bricks", "tcwildlands:polished_limestone", "tcwildlands:dolomite", "tcwildlands:dolomite_cobble", "tcwildlands:dolomite_bricks", "tcwildlands:polished_dolomite",
                "tcwildlands:chalk", "tcwildlands:chalk_cobble", "tcwildlands:chalk_bricks", "tcwildlands:polished_chalk", "tcwildlands:conglomerate", "tcwildlands:conglomerate_cobble",
                "tcwildlands:conglomerate_bricks", "tcwildlands:polished_conglomerate", "tcwildlands:mudstone", "tcwildlands:mudstone_cobble", "tcwildlands:mudstone_bricks", "tcwildlands:polished_mudstone",
                "tcwildlands:rock_salt", "tcwildlands:rock_salt_cobble", "tcwildlands:rock_salt_bricks", "tcwildlands:polished_rock_salt", "tcwildlands:gypsum_rock", "tcwildlands:gypsum_rock_cobble",
                "tcwildlands:gypsum_rock_bricks", "tcwildlands:polished_gypsum_rock", "tcwildlands:slate", "tcwildlands:slate_cobble", "tcwildlands:slate_bricks", "tcwildlands:polished_slate",
                "tcwildlands:schist", "tcwildlands:schist_cobble", "tcwildlands:schist_bricks", "tcwildlands:polished_schist", "tcwildlands:gneiss", "tcwildlands:gneiss_cobble",
                "tcwildlands:gneiss_bricks", "tcwildlands:polished_gneiss", "tcwildlands:marble", "tcwildlands:marble_cobble", "tcwildlands:marble_bricks", "tcwildlands:polished_marble",
                "tcwildlands:quartzite", "tcwildlands:quartzite_cobble", "tcwildlands:quartzite_bricks", "tcwildlands:polished_quartzite", "tcwildlands:pegmatite", "tcwildlands:pegmatite_cobble",
                "tcwildlands:pegmatite_bricks", "tcwildlands:polished_pegmatite", "tcwildlands:kimberlite", "tcwildlands:kimberlite_cobble", "tcwildlands:kimberlite_bricks", "tcwildlands:polished_kimberlite",
                "tcwildlands:eclogite", "tcwildlands:eclogite_cobble", "tcwildlands:eclogite_bricks", "tcwildlands:polished_eclogite", "tcwildlands:granite_cobble", "tcwildlands:granite_bricks",
                "tcwildlands:diorite_cobble", "tcwildlands:diorite_bricks", "tcwildlands:andesite_cobble", "tcwildlands:andesite_bricks", "tcwildlands:tuff_cobble", "tcwildlands:obsidian_cobble",
                "tcwildlands:obsidian_bricks", "tcwildlands:polished_obsidian", "tcwildlands:basalt_rock_native_copper_ore", "tcwildlands:sandstone_rock_native_copper_ore", "tcwildlands:conglomerate_native_copper_ore", "tcwildlands:limestone_malachite_ore",
                "tcwildlands:sandstone_rock_malachite_ore", "tcwildlands:diorite_chalcopyrite_ore", "tcwildlands:andesite_chalcopyrite_ore", "tcwildlands:granite_chalcopyrite_ore", "tcwildlands:basalt_rock_chalcopyrite_ore", "tcwildlands:granite_cassiterite_ore",
                "tcwildlands:pegmatite_cassiterite_ore", "tcwildlands:gravel_cassiterite_ore", "tcwildlands:limestone_galena_ore", "tcwildlands:dolomite_galena_ore", "tcwildlands:granite_galena_ore", "tcwildlands:limestone_sphalerite_ore",
                "tcwildlands:dolomite_sphalerite_ore", "tcwildlands:shale_sphalerite_ore", "tcwildlands:limestone_calamine_ore", "tcwildlands:dolomite_calamine_ore", "tcwildlands:mud_bog_iron_ore", "tcwildlands:peat_bog_iron_ore",
                "tcwildlands:quartzite_hematite_ore", "tcwildlands:shale_hematite_ore", "tcwildlands:sandstone_rock_hematite_ore", "tcwildlands:gabbro_magnetite_ore", "tcwildlands:gneiss_magnetite_ore", "tcwildlands:marble_magnetite_ore",
                "tcwildlands:slate_native_gold_ore", "tcwildlands:schist_native_gold_ore", "tcwildlands:gravel_native_gold_ore", "tcwildlands:conglomerate_native_gold_ore", "tcwildlands:andesite_native_gold_ore", "tcwildlands:andesite_native_silver_ore",
                "tcwildlands:rhyolite_native_silver_ore", "tcwildlands:limestone_native_silver_ore", "tcwildlands:tuff_cinnabar_ore", "tcwildlands:sandstone_rock_cinnabar_ore", "tcwildlands:laterite_bauxite_ore", "tcwildlands:limestone_bauxite_ore",
                "tcwildlands:peridotite_chromite_ore", "tcwildlands:gabbro_chromite_ore", "tcwildlands:gabbro_pentlandite_ore", "tcwildlands:peridotite_pentlandite_ore", "tcwildlands:granite_wolframite_ore", "tcwildlands:pegmatite_wolframite_ore",
                "tcwildlands:granite_molybdenite_ore", "tcwildlands:diorite_molybdenite_ore", "tcwildlands:granite_uraninite_ore", "tcwildlands:sandstone_rock_uraninite_ore", "tcwildlands:conglomerate_uraninite_ore", "tcwildlands:gabbro_ilmenite_ore",
                "tcwildlands:sand_ilmenite_ore", "tcwildlands:schist_rutile_ore", "tcwildlands:sand_rutile_ore", "tcwildlands:eclogite_rutile_ore", "tcwildlands:pegmatite_spodumene_ore", "tcwildlands:granite_monazite_ore",
                "tcwildlands:sand_monazite_ore", "tcwildlands:schist_cobaltite_ore", "tcwildlands:gneiss_cobaltite_ore", "tcwildlands:tuff_sulfur_ore", "tcwildlands:basalt_rock_sulfur_ore", "tcwildlands:gypsum_rock_sulfur_ore",
                "tcwildlands:limestone_niter_ore", "tcwildlands:sandstone_rock_niter_ore", "tcwildlands:rock_salt_sylvite_ore", "tcwildlands:limestone_phosphorite_ore", "tcwildlands:mudstone_phosphorite_ore", "tcwildlands:schist_graphite_ore",
                "tcwildlands:gneiss_graphite_ore", "tcwildlands:marble_graphite_ore", "tcwildlands:chalk_flint_ore", "tcwildlands:limestone_flint_ore", "tcwildlands:kimberlite_diamond_ore", "tcwildlands:gravel_diamond_ore",
                "tcwildlands:eclogite_diamond_ore", "tcwildlands:schist_emerald_ore", "tcwildlands:pegmatite_emerald_ore", "tcwildlands:marble_ruby_ore", "tcwildlands:gravel_ruby_ore", "tcwildlands:gneiss_sapphire_ore",
                "tcwildlands:gravel_sapphire_ore", "tcwildlands:peridotite_jade_ore", "tcwildlands:schist_jade_ore", "tcwildlands:marble_lapis_ore", "tcwildlands:basalt_rock_amethyst_ore", "tcwildlands:lignite_seam",
                "tcwildlands:bituminous_coal_seam", "tcwildlands:anthracite_seam", "tcwildlands:loess", "tcwildlands:alluvium", "tcwildlands:silt", "tcwildlands:chernozem",
                "tcwildlands:laterite", "tcwildlands:terra_rossa", "tcwildlands:marl", "tcwildlands:till", "tcwildlands:permafrost", "tcwildlands:volcanic_ash",
                "tcwildlands:black_sand", "tcwildlands:quartz_sand", "tcwildlands:peat", "tcwildlands:kaolinite", "tcwildlands:flood_silt", "tcwildlands:saprolite",
                "tcwildlands:loose_stones", "tcwildlands:ore_float", "tcwildlands:gossan", "tcwildlands:fumarole", "tcwildlands:geyser", "tcwildlands:bitumen_seep",
                "tcwildlands:gas_seep", "tcwildlands:lava_crust", "tcwildlands:steam_vent", "tcwildlands:massive_sulfide", "tcwildlands:deep_threshold", "tcwildlands:salt_crust",
                "tcwildlands:timber_prop", "tcwildlands:steel_mine_arch", "tcwildlands:rock_bolts"
        ));
        map.put("item", List.of(
                "tcwildlands:raw_native_copper", "tcwildlands:raw_malachite", "tcwildlands:raw_chalcopyrite", "tcwildlands:raw_cassiterite", "tcwildlands:raw_galena", "tcwildlands:raw_sphalerite",
                "tcwildlands:raw_calamine", "tcwildlands:raw_bog_iron", "tcwildlands:raw_hematite", "tcwildlands:raw_magnetite", "tcwildlands:raw_native_gold", "tcwildlands:raw_native_silver",
                "tcwildlands:raw_cinnabar", "tcwildlands:raw_bauxite", "tcwildlands:raw_chromite", "tcwildlands:raw_pentlandite", "tcwildlands:raw_wolframite", "tcwildlands:raw_molybdenite",
                "tcwildlands:raw_uraninite", "tcwildlands:raw_ilmenite", "tcwildlands:raw_rutile", "tcwildlands:raw_spodumene", "tcwildlands:raw_monazite", "tcwildlands:raw_cobaltite",
                "tcwildlands:raw_sulfur", "tcwildlands:raw_niter", "tcwildlands:raw_sylvite", "tcwildlands:raw_phosphorite", "tcwildlands:raw_graphite", "tcwildlands:raw_kaolinite",
                "tcwildlands:raw_flint", "tcwildlands:raw_peat", "tcwildlands:raw_lignite", "tcwildlands:raw_bituminous_coal", "tcwildlands:raw_anthracite", "tcwildlands:raw_diamond",
                "tcwildlands:raw_emerald", "tcwildlands:raw_ruby", "tcwildlands:raw_sapphire", "tcwildlands:raw_jade", "tcwildlands:raw_lapis", "tcwildlands:raw_amethyst",
                "tcwildlands:raw_bitumen", "tcwildlands:loose_rock", "tcwildlands:brine_bucket", "tcwildlands:thermal_water_bucket"
        ));
        map.put("entity", List.of(
                "tcwildlands:volcanic_bomb", "tcwildlands:tornado"
        ));
        map.put("fluid", List.of(
                "tcwildlands:brine", "tcwildlands:thermal_water", "tcwildlands:floodwater"
        ));
        map.put("dimension", List.of(
                "tcwildlands:earthlike", "tcwildlands:deep_earth"
        ));
        map.put("biome", List.of(
                "tcwildlands:polar_sea_ice", "tcwildlands:polar_deep_ocean", "tcwildlands:cold_shelf_sea", "tcwildlands:cold_deep_ocean", "tcwildlands:temperate_shelf_sea", "tcwildlands:temperate_deep_ocean",
                "tcwildlands:tropical_shelf_sea", "tcwildlands:tropical_deep_ocean", "tcwildlands:tropical_coral_reef", "tcwildlands:abyssal_trench", "tcwildlands:tropical_mangrove_coast", "tcwildlands:tropical_sandy_beach",
                "tcwildlands:temperate_sandy_beach", "tcwildlands:temperate_salt_marsh", "tcwildlands:cold_rocky_shore", "tcwildlands:cold_fjord_coast", "tcwildlands:tropical_river", "tcwildlands:temperate_river",
                "tcwildlands:cold_river", "tcwildlands:temperate_lake", "tcwildlands:cold_lake", "tcwildlands:arid_salt_lake", "tcwildlands:tropical_delta", "tcwildlands:temperate_delta",
                "tcwildlands:temperate_floodplain", "tcwildlands:tropical_floodplain", "tcwildlands:tropical_swamp", "tcwildlands:temperate_marsh", "tcwildlands:boreal_peat_bog", "tcwildlands:tropical_rainforest",
                "tcwildlands:tropical_monsoon_forest", "tcwildlands:tropical_dry_forest", "tcwildlands:tropical_savanna", "tcwildlands:tropical_cloud_forest", "tcwildlands:hot_sand_desert", "tcwildlands:hot_rocky_desert",
                "tcwildlands:hot_desert_oasis", "tcwildlands:hot_steppe", "tcwildlands:cold_desert", "tcwildlands:cold_steppe", "tcwildlands:arid_badlands", "tcwildlands:arid_salt_flat",
                "tcwildlands:arid_canyon", "tcwildlands:humid_subtropical_forest", "tcwildlands:subtropical_monsoon_forest", "tcwildlands:subtropical_karst_hills", "tcwildlands:mediterranean_shrubland", "tcwildlands:mediterranean_woodland",
                "tcwildlands:temperate_broadleaf_forest", "tcwildlands:temperate_rainforest", "tcwildlands:temperate_grassland", "tcwildlands:temperate_mixed_forest", "tcwildlands:boreal_forest", "tcwildlands:boreal_larch_forest",
                "tcwildlands:polar_tundra", "tcwildlands:polar_ice_sheet", "tcwildlands:temperate_montane_forest", "tcwildlands:alpine_meadow", "tcwildlands:alpine_tundra", "tcwildlands:alpine_rocky_peaks",
                "tcwildlands:alpine_glacier", "tcwildlands:cold_high_plateau", "tcwildlands:volcanic_lava_field", "tcwildlands:volcanic_geothermal_field", "tcwildlands:deep_granite_batholith", "tcwildlands:deep_hydrothermal_caverns",
                "tcwildlands:deep_magma_chambers", "tcwildlands:deep_mantle_plume", "tcwildlands:deep_kimberlite_roots"
        ));
        map.put("structure", List.of(
                "tcwildlands:amethyst_geode", "tcwildlands:kimberlite_pipe", "tcwildlands:karst_cave_system", "tcwildlands:lava_tube", "tcwildlands:geyser_field", "tcwildlands:hot_spring",
                "tcwildlands:cenote", "tcwildlands:natural_arch", "tcwildlands:impact_crater", "tcwildlands:fossil_bed", "tcwildlands:ice_cave", "tcwildlands:deep_conduit",
                "tcwildlands:hoodoo_field", "tcwildlands:volcanic_vent", "tcwildlands:sea_stack_cluster"
        ));
        map.put("block_entity", List.of(
                "tcwildlands:geyser", "tcwildlands:deep_threshold"
        ));
        map.put("payload", List.of(
                "tcwildlands:calendar_sync", "tcwildlands:weather_cells", "tcwildlands:local_climate", "tcwildlands:hazard_state", "tcwildlands:quake_shake", "tcwildlands:rock_tap_result",
                "tcwildlands:prospect_result", "tcwildlands:start_site_options", "tcwildlands:start_site_choice", "tcwildlands:planet_progress", "tcwildlands:planet_preview", "tcwildlands:region_summary_request",
                "tcwildlands:region_summary", "tcwildlands:wrap_preload", "tcwildlands:flow_field"
        ));
        map.put("hud_widget", List.of(
                "tcwildlands:local_conditions"
        ));
        map.put("civ_tab", List.of(
                "tcwildlands:geography"
        ));
        map.put("region_layer", List.of(
                "tcwildlands:geo", "tcwildlands:climate", "tcwildlands:hydro"
        ));
        map.put("sound", List.of(
                "tcwildlands:block.rock_tap", "tcwildlands:block.panning", "tcwildlands:event.landslide"
        ));
        Map<String, List<Identifier>> idMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            idMap.put(entry.getKey(), entry.getValue().stream().map(Identifier::parse).toList());
        }
        APPENDIX_A_IDS = Collections.unmodifiableMap(idMap);
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MOD_ID))
                    .icon(() -> new ItemStack(REGISTERED_ITEMS.getOrDefault(
                            "basalt_rock_native_copper_ore",
                            REGISTERED_ITEMS.values().iterator().next()
                    ).get()))
                    .displayItems((params, output) -> ITEMS.getEntries().forEach(e -> output.accept(e.get())))
                    .build());

    private WildlandsRegistries() {
    }

    public static void register(IEventBus modBus) {
        if (REGISTERED_ON_BUS.compareAndSet(false, true)) {
            BLOCKS.register(modBus);
            ITEMS.register(modBus);
            SOUNDS.register(modBus);
            CREATIVE_MODE_TABS.register(modBus);
        }
    }
}
