package com.nightcity.atlas.registry;

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

public final class AtlasRegistries {
    public static final String MOD_ID = "tcatlas";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    private static final AtomicBoolean REGISTERED_ON_BUS = new AtomicBoolean(false);

    private static final List<String> BLOCK_PATHS = List.of(
        "cairn", "map_table", "waypoint_flag", "wall_map", "signpost", "boundary_stone",
        "map_stele", "survey_stake", "ranging_pole", "chart_table", "globe", "theodolite",
        "plane_table", "trig_pillar", "dumpy_level", "reporting_station", "radio_beacon", "situation_board",
        "field_wire", "developing_tank", "stereo_plotter", "radar_antenna", "radar_station", "radar_console",
        "gis_terminal", "ground_station", "triangulation_pillar", "cartography_table_tc"
    );
    private static final List<String> BLOCK_ITEM_PATHS = List.of(
        "cairn", "map_table", "waypoint_flag", "wall_map", "signpost", "boundary_stone",
        "map_stele", "survey_stake", "ranging_pole", "chart_table", "globe", "theodolite",
        "plane_table", "trig_pillar", "dumpy_level", "reporting_station", "radio_beacon", "situation_board",
        "field_wire", "developing_tank", "stereo_plotter", "radar_antenna", "radar_station", "radar_console",
        "gis_terminal", "ground_station", "triangulation_pillar", "cartography_table_tc"
    );
    private static final List<String> STANDALONE_ITEM_PATHS = List.of(
        "map_hide", "charcoal_stylus", "hand_drawn_map", "clay_tablet_map", "scroll_map", "measuring_rope",
        "groma", "surveyors_wheel", "lodestone_needle", "magnetic_compass", "portolan_chart", "astrolabe",
        "sextant", "marine_chronometer", "log_line", "nautical_chart", "bound_atlas", "survey_chain",
        "levelling_staff", "topographic_map", "radio_direction_finder", "aerial_camera", "aerial_film", "aerial_photograph",
        "total_station", "gps_receiver", "imaging_payload", "navigation_payload", "ancient_map",
        "charcoal_sketch", "compass_needle", "camera_aerial", "gnss_receiver", "parchment_chart",
        "printed_map", "atlas_book", "survey_notebook", "chalk", "signal_flare"
    );
    private static final List<String> SOUND_PATHS = List.of(
        "ui.map.open", "ui.map.close", "ui.waypoint.add", "ui.layout.snap", "block.theodolite.adjust", "block.radar.sweep",
        "item.compass.needle", "item.camera.shutter", "item.gps.fix",
        "ui.map_open", "ui.map_close", "ui.pin_place", "ui.era_skin_transition",
        "ui.toast.stone", "ui.alert.drum", "block.theodolite.sight"
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
                "tcatlas:cairn", "tcatlas:map_table", "tcatlas:waypoint_flag", "tcatlas:wall_map", "tcatlas:signpost", "tcatlas:boundary_stone",
                "tcatlas:map_stele", "tcatlas:survey_stake", "tcatlas:ranging_pole", "tcatlas:chart_table", "tcatlas:globe", "tcatlas:theodolite",
                "tcatlas:plane_table", "tcatlas:trig_pillar", "tcatlas:dumpy_level", "tcatlas:reporting_station", "tcatlas:radio_beacon", "tcatlas:situation_board",
                "tcatlas:field_wire", "tcatlas:developing_tank", "tcatlas:stereo_plotter", "tcatlas:radar_antenna", "tcatlas:radar_station", "tcatlas:radar_console",
                "tcatlas:gis_terminal", "tcatlas:ground_station"
        ));
        map.put("item", List.of(
                "tcatlas:map_hide", "tcatlas:charcoal_stylus", "tcatlas:hand_drawn_map", "tcatlas:clay_tablet_map", "tcatlas:scroll_map", "tcatlas:measuring_rope",
                "tcatlas:groma", "tcatlas:surveyors_wheel", "tcatlas:lodestone_needle", "tcatlas:magnetic_compass", "tcatlas:portolan_chart", "tcatlas:astrolabe",
                "tcatlas:sextant", "tcatlas:marine_chronometer", "tcatlas:log_line", "tcatlas:nautical_chart", "tcatlas:bound_atlas", "tcatlas:survey_chain",
                "tcatlas:levelling_staff", "tcatlas:topographic_map", "tcatlas:radio_direction_finder", "tcatlas:aerial_camera", "tcatlas:aerial_film", "tcatlas:aerial_photograph",
                "tcatlas:total_station", "tcatlas:gps_receiver", "tcatlas:imaging_payload", "tcatlas:navigation_payload", "tcatlas:ancient_map"
        ));
        map.put("block_entity", List.of(
                "tcatlas:map_table", "tcatlas:chart_table", "tcatlas:wall_map", "tcatlas:map_stele", "tcatlas:signpost", "tcatlas:theodolite",
                "tcatlas:plane_table", "tcatlas:trig_pillar", "tcatlas:reporting_station", "tcatlas:radio_beacon", "tcatlas:situation_board", "tcatlas:developing_tank",
                "tcatlas:stereo_plotter", "tcatlas:radar_station", "tcatlas:gis_terminal", "tcatlas:ground_station"
        ));
        map.put("menu", List.of(
                "tcatlas:map_table", "tcatlas:signpost", "tcatlas:theodolite", "tcatlas:plane_table", "tcatlas:developing_tank", "tcatlas:stereo_plotter",
                "tcatlas:radar_console", "tcatlas:gis_terminal", "tcatlas:ground_station", "tcatlas:bound_atlas"
        ));
        map.put("keybind", List.of(
                "tcatlas:world_map", "tcatlas:minimap_mode", "tcatlas:add_waypoint", "tcatlas:add_waypoint_quick", "tcatlas:hud_level", "tcatlas:layout_editor",
                "tcatlas:minimap_zoom_in", "tcatlas:minimap_zoom_out", "tcatlas:ping"
        ));
        map.put("payload", List.of(
                "tcatlas:capability_sync", "tcatlas:knowledge_mask_sync", "tcatlas:map_tile_request", "tcatlas:map_tile_sync", "tcatlas:memory_sync", "tcatlas:nav_state_sync",
                "tcatlas:waypoint_sync", "tcatlas:waypoint_edit", "tcatlas:annotation_sync", "tcatlas:annotation_edit", "tcatlas:layer_data_request", "tcatlas:layer_data_sync",
                "tcatlas:live_track_sync", "tcatlas:survey_action", "tcatlas:survey_result", "tcatlas:map_sheet_sync", "tcatlas:ping"
        ));
        map.put("hud_widget", List.of(
                "tcatlas:minimap", "tcatlas:compass_bar", "tcatlas:waypoint_nav", "tcatlas:coordinates"
        ));
        map.put("civ_tab", List.of(
                "tcatlas:cartography"
        ));
        map.put("region_layer", List.of(
                "tcatlas:survey"
        ));
        map.put("sound", List.of(
                "tcatlas:ui.map.open", "tcatlas:ui.map.close", "tcatlas:ui.waypoint.add", "tcatlas:ui.layout.snap", "tcatlas:block.theodolite.adjust", "tcatlas:block.radar.sweep",
                "tcatlas:item.compass.needle", "tcatlas:item.camera.shutter", "tcatlas:item.gps.fix"
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
                    .icon(() -> new ItemStack(REGISTERED_ITEMS.getOrDefault("bound_atlas", REGISTERED_ITEMS.values().iterator().next()).get()))
                    .displayItems((params, output) -> ITEMS.getEntries().forEach(e -> output.accept(e.get())))
                    .build());

    private AtlasRegistries() {
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
