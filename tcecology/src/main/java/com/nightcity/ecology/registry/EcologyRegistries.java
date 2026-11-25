package com.nightcity.ecology.registry;

import com.mojang.serialization.MapCodec;
import com.nightcity.ecology.spawn.ReplaceVanillaWildlifeBiomeModifier;
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
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EcologyRegistries {
    public static final String MOD_ID = "tcecology";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, MOD_ID);

    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<ReplaceVanillaWildlifeBiomeModifier>> REMOVE_VANILLA_WILDLIFE =
            BIOME_MODIFIER_SERIALIZERS.register("remove_vanilla_wildlife", () -> ReplaceVanillaWildlifeBiomeModifier.CODEC);

    private static final List<String> BLOCK_PATHS = List.of(
        "wild_emmer", "wild_einkorn", "wild_barley", "wild_lentil", "wild_rice", "green_foxtail",
        "teosinte", "wild_potato", "wild_bean", "wild_squash", "wild_sorghum", "wild_flax",
        "wild_grape", "wild_cabbage", "wild_carrot", "sea_beet", "wild_cotton", "fireweed",
        "hazel_shrub", "bramble", "wild_garlic", "nettle", "cattail", "forest_floor_moss",
        "beech_log", "beech_leaves", "beech_sapling", "beech_planks", "chestnut_log", "chestnut_leaves",
        "chestnut_sapling", "chestnut_planks", "walnut_log", "walnut_leaves", "walnut_sapling", "walnut_planks",
        "maple_log", "maple_leaves", "maple_sapling", "maple_planks", "pine_log", "pine_leaves",
        "pine_sapling", "pine_planks", "larch_log", "larch_leaves", "larch_sapling", "larch_planks",
        "cedar_log", "cedar_leaves", "cedar_sapling", "cedar_planks", "eucalyptus_log", "eucalyptus_leaves",
        "eucalyptus_sapling", "eucalyptus_planks", "teak_log", "teak_leaves", "teak_sapling", "teak_planks",
        "rubber_tree_log", "rubber_tree_leaves", "rubber_tree_sapling", "rubber_tree_planks", "willow_log", "willow_leaves",
        "willow_sapling", "willow_planks", "ash_log", "ash_leaves", "ash_sapling", "ash_planks",
        "snare", "pit_trap", "fish_weir", "beaver_dam", "salt_lick", "nest_box",
        "reserve_marker", "coppice_stool", "bird_blind", "forest_stand_marker", "tree_nursery", "fish_ladder",
        "wildlife_crossing_marker", "captive_breeding_pen", "camera_trap", "telemetry_receiver", "constructed_wetland", "gene_bank",
        "terrarium"
    );
    private static final List<String> BLOCK_ITEM_PATHS = List.of(
        "wild_emmer", "wild_einkorn", "wild_barley", "wild_lentil", "wild_rice", "green_foxtail",
        "teosinte", "wild_potato", "wild_bean", "wild_squash", "wild_sorghum", "wild_flax",
        "wild_grape", "wild_cabbage", "wild_carrot", "sea_beet", "wild_cotton", "fireweed",
        "hazel_shrub", "bramble", "wild_garlic", "nettle", "cattail", "forest_floor_moss",
        "beech_log", "beech_leaves", "beech_sapling", "beech_planks", "chestnut_log", "chestnut_leaves",
        "chestnut_sapling", "chestnut_planks", "walnut_log", "walnut_leaves", "walnut_sapling", "walnut_planks",
        "maple_log", "maple_leaves", "maple_sapling", "maple_planks", "pine_log", "pine_leaves",
        "pine_sapling", "pine_planks", "larch_log", "larch_leaves", "larch_sapling", "larch_planks",
        "cedar_log", "cedar_leaves", "cedar_sapling", "cedar_planks", "eucalyptus_log", "eucalyptus_leaves",
        "eucalyptus_sapling", "eucalyptus_planks", "teak_log", "teak_leaves", "teak_sapling", "teak_planks",
        "rubber_tree_log", "rubber_tree_leaves", "rubber_tree_sapling", "rubber_tree_planks", "willow_log", "willow_leaves",
        "willow_sapling", "willow_planks", "ash_log", "ash_leaves", "ash_sapling", "ash_planks",
        "snare", "pit_trap", "fish_weir", "beaver_dam", "salt_lick", "nest_box",
        "reserve_marker", "coppice_stool", "bird_blind", "forest_stand_marker", "tree_nursery", "fish_ladder",
        "wildlife_crossing_marker", "captive_breeding_pen", "camera_trap", "telemetry_receiver", "constructed_wetland", "gene_bank",
        "terrarium"
    );
    private static final List<String> STANDALONE_ITEM_PATHS = List.of(
        "capture_net", "spoor_sample", "fur_pelt", "antler", "ivory", "coppice_pole",
        "blubber", "latex", "radio_collar", "biopsy_kit", "tissue_sample", "native_seed_mix",
        "liquid_nitrogen_flask"
    );
    private static final List<String> SOUND_PATHS = List.of(
        "ambient.dawn_chorus", "ambient.frog_chorus", "ambient.wolf_howl", "ambient.deer_rut", "ambient.crane_call", "entity.generic_wild.flee"
    );

    public static final Map<String, DeferredBlock<Block>> REGISTERED_BLOCKS;
    public static final Map<String, DeferredItem<? extends Item>> REGISTERED_ITEMS;
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> REGISTERED_SOUNDS;
    public static final Map<String, List<Identifier>> APPENDIX_A_IDS;

    static {
        Map<String, DeferredBlock<Block>> bMap = new LinkedHashMap<>();
        for (String path : BLOCK_PATHS) {
            bMap.put(path, BLOCKS.registerSimpleBlock(path));
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
                "tcecology:wild_emmer", "tcecology:wild_einkorn", "tcecology:wild_barley", "tcecology:wild_lentil", "tcecology:wild_rice", "tcecology:green_foxtail",
                "tcecology:teosinte", "tcecology:wild_potato", "tcecology:wild_bean", "tcecology:wild_squash", "tcecology:wild_sorghum", "tcecology:wild_flax",
                "tcecology:wild_grape", "tcecology:wild_cabbage", "tcecology:wild_carrot", "tcecology:sea_beet", "tcecology:wild_cotton", "tcecology:fireweed",
                "tcecology:hazel_shrub", "tcecology:bramble", "tcecology:wild_garlic", "tcecology:nettle", "tcecology:cattail", "tcecology:forest_floor_moss",
                "tcecology:beech_log", "tcecology:beech_leaves", "tcecology:beech_sapling", "tcecology:beech_planks", "tcecology:chestnut_log", "tcecology:chestnut_leaves",
                "tcecology:chestnut_sapling", "tcecology:chestnut_planks", "tcecology:walnut_log", "tcecology:walnut_leaves", "tcecology:walnut_sapling", "tcecology:walnut_planks",
                "tcecology:maple_log", "tcecology:maple_leaves", "tcecology:maple_sapling", "tcecology:maple_planks", "tcecology:pine_log", "tcecology:pine_leaves",
                "tcecology:pine_sapling", "tcecology:pine_planks", "tcecology:larch_log", "tcecology:larch_leaves", "tcecology:larch_sapling", "tcecology:larch_planks",
                "tcecology:cedar_log", "tcecology:cedar_leaves", "tcecology:cedar_sapling", "tcecology:cedar_planks", "tcecology:eucalyptus_log", "tcecology:eucalyptus_leaves",
                "tcecology:eucalyptus_sapling", "tcecology:eucalyptus_planks", "tcecology:teak_log", "tcecology:teak_leaves", "tcecology:teak_sapling", "tcecology:teak_planks",
                "tcecology:rubber_tree_log", "tcecology:rubber_tree_leaves", "tcecology:rubber_tree_sapling", "tcecology:rubber_tree_planks", "tcecology:willow_log", "tcecology:willow_leaves",
                "tcecology:willow_sapling", "tcecology:willow_planks", "tcecology:ash_log", "tcecology:ash_leaves", "tcecology:ash_sapling", "tcecology:ash_planks",
                "tcecology:snare", "tcecology:pit_trap", "tcecology:fish_weir", "tcecology:beaver_dam", "tcecology:salt_lick", "tcecology:nest_box",
                "tcecology:reserve_marker", "tcecology:coppice_stool", "tcecology:bird_blind", "tcecology:forest_stand_marker", "tcecology:tree_nursery", "tcecology:fish_ladder",
                "tcecology:wildlife_crossing_marker", "tcecology:captive_breeding_pen", "tcecology:camera_trap", "tcecology:telemetry_receiver", "tcecology:constructed_wetland", "tcecology:gene_bank",
                "tcecology:terrarium"
        ));
        map.put("item", List.of(
                "tcecology:capture_net", "tcecology:spoor_sample", "tcecology:fur_pelt", "tcecology:antler", "tcecology:ivory", "tcecology:coppice_pole",
                "tcecology:blubber", "tcecology:latex", "tcecology:radio_collar", "tcecology:biopsy_kit", "tcecology:tissue_sample", "tcecology:native_seed_mix",
                "tcecology:liquid_nitrogen_flask"
        ));
        map.put("entity", List.of(
                "tcecology:red_deer", "tcecology:roe_deer", "tcecology:reindeer", "tcecology:moose", "tcecology:bison", "tcecology:aurochs",
                "tcecology:wild_boar", "tcecology:mouflon", "tcecology:bezoar_ibex", "tcecology:wild_horse", "tcecology:wild_ass", "tcecology:guanaco",
                "tcecology:wild_yak", "tcecology:wild_water_buffalo", "tcecology:saiga", "tcecology:wildebeest", "tcecology:zebra", "tcecology:gazelle",
                "tcecology:elephant", "tcecology:woolly_mammoth", "tcecology:beaver", "tcecology:red_squirrel", "tcecology:hedgehog", "tcecology:brown_bear",
                "tcecology:lynx", "tcecology:lion", "tcecology:tiger", "tcecology:pine_marten", "tcecology:otter", "tcecology:marsupial_wolf",
                "tcecology:brown_rat", "tcecology:red_junglefowl", "tcecology:wild_turkey", "tcecology:mallard", "tcecology:greylag_goose", "tcecology:pheasant",
                "tcecology:grouse", "tcecology:common_crane", "tcecology:barn_swallow", "tcecology:passenger_pigeon", "tcecology:great_auk", "tcecology:island_dodo",
                "tcecology:golden_eagle", "tcecology:peregrine_falcon", "tcecology:osprey", "tcecology:grey_heron", "tcecology:brown_trout", "tcecology:sturgeon",
                "tcecology:european_eel", "tcecology:common_carp", "tcecology:herring", "tcecology:grey_seal", "tcecology:right_whale"
        ));
        map.put("structure", List.of(
                "tcecology:beaver_lodge", "tcecology:animal_den"
        ));
        map.put("block_entity", List.of(
                "tcecology:snare", "tcecology:pit_trap", "tcecology:fish_weir", "tcecology:forest_stand_marker", "tcecology:tree_nursery", "tcecology:captive_breeding_pen",
                "tcecology:camera_trap", "tcecology:telemetry_receiver", "tcecology:gene_bank"
        ));
        map.put("menu", List.of(
                "tcecology:fish_weir", "tcecology:tree_nursery", "tcecology:captive_breeding_pen", "tcecology:gene_bank"
        ));
        map.put("payload", List.of(
                "tcecology:track_request", "tcecology:track_sync", "tcecology:codex_sighting", "tcecology:codex_update", "tcecology:burn_confirm", "tcecology:reserve_edit",
                "tcecology:collar_update", "tcecology:pen_action"
        ));
        map.put("hud_widget", List.of(
                "tcecology:ecosystem_health", "tcecology:tracking"
        ));
        map.put("civ_tab", List.of(
                "tcecology:ecology"
        ));
        map.put("region_layer", List.of(
                "tcecology:ecosystem"
        ));
        map.put("sound", List.of(
                "tcecology:ambient.dawn_chorus", "tcecology:ambient.frog_chorus", "tcecology:ambient.wolf_howl", "tcecology:ambient.deer_rut", "tcecology:ambient.crane_call", "tcecology:entity.generic_wild.flee"
        ));
        Map<String, List<Identifier>> idMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            idMap.put(entry.getKey(), entry.getValue().stream().map(Identifier::parse).toList());
        }
        APPENDIX_A_IDS = Collections.unmodifiableMap(idMap);
    }

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MOD_ID))
                    .icon(() -> new ItemStack(REGISTERED_ITEMS.getOrDefault("wild_emmer", REGISTERED_ITEMS.values().iterator().next()).get()))
                    .displayItems((params, output) -> ITEMS.getEntries().forEach(e -> output.accept(e.get())))
                    .build());

    private EcologyRegistries() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        BIOME_MODIFIER_SERIALIZERS.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);
    }
}
