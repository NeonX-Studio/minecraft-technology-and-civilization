package com.nightcity.cultivation.registry;

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

public final class CultivationRegistries {
    public static final String MOD_ID = "tccultivation";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

    private static final List<String> BLOCK_PATHS = List.of(
        "wheat", "potato", "carrot", "beetroot", "pumpkin_stem", "melon_stem",
        "cocoa", "sweet_berry_bush", "sugar_cane", "emmer_wheat", "einkorn", "barley",
        "rye", "oats", "rice", "foxtail_millet", "maize", "sorghum",
        "lentil", "pea", "soybean", "common_bean", "sweet_potato", "cassava",
        "cabbage", "onion", "sunflower", "flax", "hemp", "cotton",
        "grape", "olive", "tea", "coffee", "mulberry", "paddy_field",
        "raised_bed", "saline_soil", "guano_deposit", "compost_bin", "manure_heap", "scarecrow",
        "hedgerow", "field_sensor", "irrigation_ditch", "sluice_gate", "diversion_weir", "shadoof",
        "noria", "chain_pump", "tile_drain", "tube_well", "sprinkler", "drip_line",
        "seed_sorting_tray", "seed_vault", "acclimatization_garden", "gene_editing_station", "feed_trough", "fish_pond_marker",
        "silkworm_tray", "grain_pit", "rat_proof_bin", "drying_rack", "smokehouse", "churn",
        "cheese_press", "ice_chest", "refrigerator", "deep_freezer", "tree_tap", "grow_rack",
        "nutrient_tank"
    );
    private static final List<String> BLOCK_ITEM_PATHS = List.of(
        "wheat", "potato", "carrot", "beetroot", "pumpkin_stem", "melon_stem",
        "cocoa", "sweet_berry_bush", "sugar_cane", "paddy_field", "raised_bed", "saline_soil",
        "guano_deposit", "compost_bin", "manure_heap", "scarecrow", "hedgerow", "field_sensor",
        "irrigation_ditch", "sluice_gate", "diversion_weir", "shadoof", "noria", "chain_pump",
        "tile_drain", "tube_well", "sprinkler", "drip_line", "seed_sorting_tray", "seed_vault",
        "acclimatization_garden", "gene_editing_station", "feed_trough", "fish_pond_marker", "silkworm_tray", "grain_pit",
        "rat_proof_bin", "drying_rack", "smokehouse", "churn", "cheese_press", "ice_chest",
        "refrigerator", "deep_freezer", "tree_tap", "grow_rack", "nutrient_tank"
    );
    private static final List<String> STANDALONE_ITEM_PATHS = List.of(
        "emmer_wheat", "einkorn", "barley", "rye", "oats", "rice",
        "foxtail_millet", "maize", "sorghum", "lentil", "pea", "soybean",
        "common_bean", "sweet_potato", "cassava", "cabbage", "onion", "sunflower",
        "flax", "hemp", "cotton", "grape", "olive", "tea",
        "coffee", "mulberry", "emmer_wheat_seeds", "einkorn_seeds", "barley_seeds", "rye_seeds",
        "oats_seeds", "rice_seeds", "foxtail_millet_seeds", "maize_seeds", "sorghum_seeds", "lentil_seeds",
        "pea_seeds", "soybean_seeds", "common_bean_seeds", "cabbage_seeds", "flax_seeds", "cotton_seeds",
        "hemp_seeds", "grape_cutting", "tea_cutting", "mulberry_cutting", "olive_sapling", "coffee_seedling",
        "cured_tea", "coffee_beans", "implement", "yoke", "herding_crook", "herd_book",
        "hay", "pasture_seed", "carp", "cooked_carp", "pond_silt", "silk_cocoon",
        "manure", "compost", "agricultural_lime", "guano", "npk_fertilizer", "soil_test_kit",
        "bordeaux_mixture", "pesticide", "herbicide", "soil_inoculant", "venison", "cooked_venison",
        "game_bird", "cooked_game_bird", "dried_meat", "dried_fish", "smoked_meat", "smoked_fish",
        "salted_meat", "salted_fish", "dried_fruit", "fruit_preserves", "sauerkraut", "cheese_wheel",
        "butter", "yogurt", "rennet", "beer", "wine", "rice_wine",
        "soy_sauce", "miso", "fish_sauce", "flour", "dough", "maple_sap",
        "maple_syrup", "spoiled_food"
    );
    private static final List<String> SOUND_PATHS = List.of();

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
                "tccultivation:wheat", "tccultivation:potato", "tccultivation:carrot", "tccultivation:beetroot", "tccultivation:pumpkin_stem", "tccultivation:melon_stem",
                "tccultivation:cocoa", "tccultivation:sweet_berry_bush", "tccultivation:sugar_cane", "tccultivation:emmer_wheat", "tccultivation:einkorn", "tccultivation:barley",
                "tccultivation:rye", "tccultivation:oats", "tccultivation:rice", "tccultivation:foxtail_millet", "tccultivation:maize", "tccultivation:sorghum",
                "tccultivation:lentil", "tccultivation:pea", "tccultivation:soybean", "tccultivation:common_bean", "tccultivation:sweet_potato", "tccultivation:cassava",
                "tccultivation:cabbage", "tccultivation:onion", "tccultivation:sunflower", "tccultivation:flax", "tccultivation:hemp", "tccultivation:cotton",
                "tccultivation:grape", "tccultivation:olive", "tccultivation:tea", "tccultivation:coffee", "tccultivation:mulberry", "tccultivation:paddy_field",
                "tccultivation:raised_bed", "tccultivation:saline_soil", "tccultivation:guano_deposit", "tccultivation:compost_bin", "tccultivation:manure_heap", "tccultivation:scarecrow",
                "tccultivation:hedgerow", "tccultivation:field_sensor", "tccultivation:irrigation_ditch", "tccultivation:sluice_gate", "tccultivation:diversion_weir", "tccultivation:shadoof",
                "tccultivation:noria", "tccultivation:chain_pump", "tccultivation:tile_drain", "tccultivation:tube_well", "tccultivation:sprinkler", "tccultivation:drip_line",
                "tccultivation:seed_sorting_tray", "tccultivation:seed_vault", "tccultivation:acclimatization_garden", "tccultivation:gene_editing_station", "tccultivation:feed_trough", "tccultivation:fish_pond_marker",
                "tccultivation:silkworm_tray", "tccultivation:grain_pit", "tccultivation:rat_proof_bin", "tccultivation:drying_rack", "tccultivation:smokehouse", "tccultivation:churn",
                "tccultivation:cheese_press", "tccultivation:ice_chest", "tccultivation:refrigerator", "tccultivation:deep_freezer", "tccultivation:tree_tap", "tccultivation:grow_rack",
                "tccultivation:nutrient_tank"
        ));
        map.put("item", List.of(
                "tccultivation:emmer_wheat", "tccultivation:einkorn", "tccultivation:barley", "tccultivation:rye", "tccultivation:oats", "tccultivation:rice",
                "tccultivation:foxtail_millet", "tccultivation:maize", "tccultivation:sorghum", "tccultivation:lentil", "tccultivation:pea", "tccultivation:soybean",
                "tccultivation:common_bean", "tccultivation:sweet_potato", "tccultivation:cassava", "tccultivation:cabbage", "tccultivation:onion", "tccultivation:sunflower",
                "tccultivation:flax", "tccultivation:hemp", "tccultivation:cotton", "tccultivation:grape", "tccultivation:olive", "tccultivation:tea",
                "tccultivation:coffee", "tccultivation:mulberry", "tccultivation:emmer_wheat_seeds", "tccultivation:einkorn_seeds", "tccultivation:barley_seeds", "tccultivation:rye_seeds",
                "tccultivation:oats_seeds", "tccultivation:rice_seeds", "tccultivation:foxtail_millet_seeds", "tccultivation:maize_seeds", "tccultivation:sorghum_seeds", "tccultivation:lentil_seeds",
                "tccultivation:pea_seeds", "tccultivation:soybean_seeds", "tccultivation:common_bean_seeds", "tccultivation:cabbage_seeds", "tccultivation:flax_seeds", "tccultivation:cotton_seeds",
                "tccultivation:hemp_seeds", "tccultivation:grape_cutting", "tccultivation:tea_cutting", "tccultivation:mulberry_cutting", "tccultivation:olive_sapling", "tccultivation:coffee_seedling",
                "tccultivation:cured_tea", "tccultivation:coffee_beans", "tccultivation:implement", "tccultivation:yoke", "tccultivation:herding_crook", "tccultivation:herd_book",
                "tccultivation:hay", "tccultivation:pasture_seed", "tccultivation:carp", "tccultivation:cooked_carp", "tccultivation:pond_silt", "tccultivation:silk_cocoon",
                "tccultivation:manure", "tccultivation:compost", "tccultivation:agricultural_lime", "tccultivation:guano", "tccultivation:npk_fertilizer", "tccultivation:soil_test_kit",
                "tccultivation:bordeaux_mixture", "tccultivation:pesticide", "tccultivation:herbicide", "tccultivation:soil_inoculant", "tccultivation:venison", "tccultivation:cooked_venison",
                "tccultivation:game_bird", "tccultivation:cooked_game_bird", "tccultivation:dried_meat", "tccultivation:dried_fish", "tccultivation:smoked_meat", "tccultivation:smoked_fish",
                "tccultivation:salted_meat", "tccultivation:salted_fish", "tccultivation:dried_fruit", "tccultivation:fruit_preserves", "tccultivation:sauerkraut", "tccultivation:cheese_wheel",
                "tccultivation:butter", "tccultivation:yogurt", "tccultivation:rennet", "tccultivation:beer", "tccultivation:wine", "tccultivation:rice_wine",
                "tccultivation:soy_sauce", "tccultivation:miso", "tccultivation:fish_sauce", "tccultivation:flour", "tccultivation:dough", "tccultivation:maple_sap",
                "tccultivation:maple_syrup", "tccultivation:spoiled_food"
        ));
        map.put("entity", List.of(
                "tccultivation:goat", "tccultivation:water_buffalo", "tccultivation:yak", "tccultivation:duck", "tccultivation:goose", "tccultivation:turkey",
                "tccultivation:implement"
        ));
        map.put("block_entity", List.of(
                "tccultivation:crop", "tccultivation:storage", "tccultivation:processor", "tccultivation:water_works", "tccultivation:husbandry_station"
        ));
        map.put("menu", List.of(
                "tccultivation:seed_sorting", "tccultivation:storage", "tccultivation:processing", "tccultivation:feed_trough", "tccultivation:gene_editing"
        ));
        map.put("payload", List.of(
                "tccultivation:field_status", "tccultivation:soil_probe", "tccultivation:animal_inspect", "tccultivation:seed_select", "tccultivation:name_variety", "tccultivation:set_rotation",
                "tccultivation:allocate_water", "tccultivation:herd_action", "tccultivation:hitch"
        ));
        map.put("hud_widget", List.of(
                "tccultivation:field_status"
        ));
        map.put("civ_tab", List.of(
                "tccultivation:agriculture"
        ));
        map.put("region_layer", List.of(
                "tccultivation:agri"
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
                    .icon(() -> new ItemStack(REGISTERED_ITEMS.getOrDefault("emmer_wheat", REGISTERED_ITEMS.values().iterator().next()).get()))
                    .displayItems((params, output) -> ITEMS.getEntries().forEach(e -> output.accept(e.get())))
                    .build());

    private CultivationRegistries() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);
    }
}
