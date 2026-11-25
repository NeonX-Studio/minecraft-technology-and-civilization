package com.nightcity.settlements.registry;

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

public final class SettlementsRegistries {
    public static final String MOD_ID = "tcsettlements";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

    private static final List<String> BLOCK_PATHS = List.of(
        "settlement_core", "thatch", "thatch_slab", "hide_cover", "sleeping_mat", "job_board",
        "construction_stake", "wattle", "wattle_and_daub", "granary", "well", "latrine",
        "herb_drying_rack", "healer_mat", "shrine", "drain_channel", "census_desk", "town_bell",
        "drafting_table", "school_desk", "teachers_lectern", "cistern", "aqueduct_channel", "public_fountain",
        "bath_basin", "planning_table", "house_number_plate", "timber_frame", "infirmary_bed", "quarantine_flag",
        "rat_trap", "oil_street_lamp", "surgery_table", "sewer_pipe", "sewage_outlet", "standpipe",
        "vaccination_station", "classroom_blackboard", "water_treatment_controller", "electric_street_lamp", "clinic_cabinet", "health_monitor_terminal",
        "habitat_bunk"
    );
    private static final List<String> BLOCK_ITEM_PATHS = List.of(
        "settlement_core", "thatch", "thatch_slab", "hide_cover", "sleeping_mat", "job_board",
        "construction_stake", "wattle", "wattle_and_daub", "granary", "well", "latrine",
        "herb_drying_rack", "healer_mat", "shrine", "drain_channel", "census_desk", "town_bell",
        "drafting_table", "school_desk", "teachers_lectern", "cistern", "aqueduct_channel", "public_fountain",
        "bath_basin", "planning_table", "house_number_plate", "timber_frame", "infirmary_bed", "quarantine_flag",
        "rat_trap", "oil_street_lamp", "surgery_table", "sewer_pipe", "sewage_outlet", "standpipe",
        "vaccination_station", "classroom_blackboard", "water_treatment_controller", "electric_street_lamp", "clinic_cabinet", "health_monitor_terminal",
        "habitat_bunk"
    );
    private static final List<String> STANDALONE_ITEM_PATHS = List.of(
        "herbal_poultice", "building_plan", "zoning_brush", "surgical_kit", "vaccine_vial", "antibiotic_dose"
    );
    private static final List<String> SOUND_PATHS = List.of(
        "ambient.crowd.small", "ambient.crowd.medium", "ambient.crowd.large", "ambient.village.day", "event.birth", "event.settlement_upgrade",
        "block.town_bell.ring", "event.epidemic"
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
                "tcsettlements:settlement_core", "tcsettlements:thatch", "tcsettlements:thatch_slab", "tcsettlements:hide_cover", "tcsettlements:sleeping_mat", "tcsettlements:job_board",
                "tcsettlements:construction_stake", "tcsettlements:wattle", "tcsettlements:wattle_and_daub", "tcsettlements:granary", "tcsettlements:well", "tcsettlements:latrine",
                "tcsettlements:herb_drying_rack", "tcsettlements:healer_mat", "tcsettlements:shrine", "tcsettlements:drain_channel", "tcsettlements:census_desk", "tcsettlements:town_bell",
                "tcsettlements:drafting_table", "tcsettlements:school_desk", "tcsettlements:teachers_lectern", "tcsettlements:cistern", "tcsettlements:aqueduct_channel", "tcsettlements:public_fountain",
                "tcsettlements:bath_basin", "tcsettlements:planning_table", "tcsettlements:house_number_plate", "tcsettlements:timber_frame", "tcsettlements:infirmary_bed", "tcsettlements:quarantine_flag",
                "tcsettlements:rat_trap", "tcsettlements:oil_street_lamp", "tcsettlements:surgery_table", "tcsettlements:sewer_pipe", "tcsettlements:sewage_outlet", "tcsettlements:standpipe",
                "tcsettlements:vaccination_station", "tcsettlements:classroom_blackboard", "tcsettlements:water_treatment_controller", "tcsettlements:electric_street_lamp", "tcsettlements:clinic_cabinet", "tcsettlements:health_monitor_terminal",
                "tcsettlements:habitat_bunk"
        ));
        map.put("item", List.of(
                "tcsettlements:herbal_poultice", "tcsettlements:building_plan", "tcsettlements:zoning_brush", "tcsettlements:surgical_kit", "tcsettlements:vaccine_vial", "tcsettlements:antibiotic_dose"
        ));
        map.put("block_entity", List.of(
                "tcsettlements:settlement_core", "tcsettlements:job_board", "tcsettlements:granary", "tcsettlements:well", "tcsettlements:census_desk", "tcsettlements:town_bell",
                "tcsettlements:drafting_table", "tcsettlements:planning_table", "tcsettlements:water_treatment_controller", "tcsettlements:health_monitor_terminal"
        ));
        map.put("menu", List.of(
                "tcsettlements:settlement_core", "tcsettlements:job_board", "tcsettlements:granary", "tcsettlements:drafting_table", "tcsettlements:planning_table"
        ));
        map.put("payload", List.of(
                "tcsettlements:settlement_sync", "tcsettlements:resident_info_request", "tcsettlements:resident_info", "tcsettlements:job_assign", "tcsettlements:room_scan_result", "tcsettlements:zone_paint",
                "tcsettlements:directive_toggle", "tcsettlements:building_plan_place", "tcsettlements:health_alert"
        ));
        map.put("hud_widget", List.of(
                "tcsettlements:settlement_status"
        ));
        map.put("civ_tab", List.of(
                "tcsettlements:settlements"
        ));
        map.put("region_layer", List.of(
                "tcsettlements:demography"
        ));
        map.put("sound", List.of(
                "tcsettlements:ambient.crowd.small", "tcsettlements:ambient.crowd.medium", "tcsettlements:ambient.crowd.large", "tcsettlements:ambient.village.day", "tcsettlements:event.birth", "tcsettlements:event.settlement_upgrade",
                "tcsettlements:block.town_bell.ring", "tcsettlements:event.epidemic"
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
                    .icon(() -> new ItemStack(REGISTERED_ITEMS.getOrDefault("settlement_core", REGISTERED_ITEMS.values().iterator().next()).get()))
                    .displayItems((params, output) -> ITEMS.getEntries().forEach(e -> output.accept(e.get())))
                    .build());

    private SettlementsRegistries() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);
    }
}
