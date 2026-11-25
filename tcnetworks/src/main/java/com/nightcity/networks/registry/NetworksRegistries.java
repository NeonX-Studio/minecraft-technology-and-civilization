package com.nightcity.networks.registry;

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

public final class NetworksRegistries {
    public static final String MOD_ID = "tcnetworks";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

    private static final List<String> BLOCK_PATHS = List.of(
        "trodden_trail", "trail_cairn", "packed_earth_road", "gravel_road", "stone_paved_road", "road_curb",
        "milestone", "road_sign", "macadam_road", "asphalt_road", "road_marking", "traffic_light",
        "concrete_road", "guard_rail", "timber_trestle", "truss_bridge_segment", "tunnel_lining", "tunnel_portal",
        "suspension_cable", "wooden_wagonway", "rail_iron", "switch_stand", "buffer_stop", "semaphore_signal",
        "signal_box", "level_crossing", "platform_edge", "rail_steel", "color_light_signal", "catenary_mast",
        "switch_motor", "rail_concrete_sleeper", "ctc_panel", "rail_slab_track", "cab_signal_beacon", "maglev_guideway",
        "crate", "freight_depot", "loading_bay", "dispatch_board", "cart_shed", "crew_office",
        "waystation", "quay", "harbor_master", "bollard", "slipway", "road_depot",
        "warehouse_core", "storage_rack", "treadwheel_crane", "canal_lock_gate", "lock_controller", "railway_station",
        "freight_station", "engine_shed", "steam_crane", "dry_dock", "water_tower", "coal_stage",
        "bus_stop", "truck_depot", "distribution_center", "fuel_pump", "pallet_rack", "traction_substation",
        "airfield_core", "control_tower", "runway_light", "hangar", "pipeline", "pipeline_pump_station",
        "pipeline_terminal", "container_terminal", "gantry_crane", "asrs_rack", "telegraph_pole", "telegraph_office",
        "telegraph_key", "telephone_exchange", "telephone", "radio_mast", "network_switch", "fiber_optic_cable",
        "network_router", "dispatch_terminal"
    );
    private static final List<String> BLOCK_ITEM_PATHS = List.of(
        "trodden_trail", "trail_cairn", "packed_earth_road", "gravel_road", "stone_paved_road", "road_curb",
        "milestone", "road_sign", "macadam_road", "asphalt_road", "road_marking", "traffic_light",
        "concrete_road", "guard_rail", "timber_trestle", "truss_bridge_segment", "tunnel_lining", "tunnel_portal",
        "suspension_cable", "wooden_wagonway", "rail_iron", "switch_stand", "buffer_stop", "semaphore_signal",
        "signal_box", "level_crossing", "platform_edge", "rail_steel", "color_light_signal", "catenary_mast",
        "switch_motor", "rail_concrete_sleeper", "ctc_panel", "rail_slab_track", "cab_signal_beacon", "maglev_guideway",
        "crate", "freight_depot", "loading_bay", "dispatch_board", "cart_shed", "crew_office",
        "waystation", "quay", "harbor_master", "bollard", "slipway", "road_depot",
        "warehouse_core", "storage_rack", "treadwheel_crane", "canal_lock_gate", "lock_controller", "railway_station",
        "freight_station", "engine_shed", "steam_crane", "dry_dock", "water_tower", "coal_stage",
        "bus_stop", "truck_depot", "distribution_center", "fuel_pump", "pallet_rack", "traction_substation",
        "airfield_core", "control_tower", "runway_light", "hangar", "pipeline", "pipeline_pump_station",
        "pipeline_terminal", "container_terminal", "gantry_crane", "asrs_rack", "telegraph_pole", "telegraph_office",
        "telegraph_key", "telephone_exchange", "telephone", "radio_mast", "network_switch", "fiber_optic_cable",
        "network_router", "dispatch_terminal"
    );
    private static final List<String> STANDALONE_ITEM_PATHS = List.of(
        "pack_saddle", "cargo_sack", "road_planner", "rammer", "harness", "wooden_wheel",
        "hull_section", "sail", "mast", "cargo_barrel", "waybill", "survey_level",
        "spoked_wheel", "track_layer", "repair_kit", "iron_tyred_wheel", "railway_wheelset", "bogie",
        "steam_powertrain", "crucible_steel", "ticket", "pneumatic_tyre", "combustion_powertrain", "electric_traction_motor",
        "pallet", "fuel_canister", "telegram", "aero_engine", "turbofan", "shipping_container"
    );
    private static final List<String> SOUND_PATHS = List.of(
        "vehicle.cart.roll", "vehicle.locomotive.whistle", "vehicle.locomotive.chuff", "vehicle.ship.creak", "vehicle.ship.horn", "vehicle.car.engine",
        "vehicle.car.horn", "vehicle.aircraft.propeller", "vehicle.aircraft.jet", "vehicle.tram.bell", "block.signal.change", "block.telegraph.key",
        "block.crane.operate", "block.lock.water", "ambient.station.bustle", "ambient.port.gulls", "ui.dispatch.alert", "event.milestone.first_train"
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
                "tcnetworks:trodden_trail", "tcnetworks:trail_cairn", "tcnetworks:packed_earth_road", "tcnetworks:gravel_road", "tcnetworks:stone_paved_road", "tcnetworks:road_curb",
                "tcnetworks:milestone", "tcnetworks:road_sign", "tcnetworks:macadam_road", "tcnetworks:asphalt_road", "tcnetworks:road_marking", "tcnetworks:traffic_light",
                "tcnetworks:concrete_road", "tcnetworks:guard_rail", "tcnetworks:timber_trestle", "tcnetworks:truss_bridge_segment", "tcnetworks:tunnel_lining", "tcnetworks:tunnel_portal",
                "tcnetworks:suspension_cable", "tcnetworks:wooden_wagonway", "tcnetworks:rail_iron", "tcnetworks:switch_stand", "tcnetworks:buffer_stop", "tcnetworks:semaphore_signal",
                "tcnetworks:signal_box", "tcnetworks:level_crossing", "tcnetworks:platform_edge", "tcnetworks:rail_steel", "tcnetworks:color_light_signal", "tcnetworks:catenary_mast",
                "tcnetworks:switch_motor", "tcnetworks:rail_concrete_sleeper", "tcnetworks:ctc_panel", "tcnetworks:rail_slab_track", "tcnetworks:cab_signal_beacon", "tcnetworks:maglev_guideway",
                "tcnetworks:crate", "tcnetworks:freight_depot", "tcnetworks:loading_bay", "tcnetworks:dispatch_board", "tcnetworks:cart_shed", "tcnetworks:crew_office",
                "tcnetworks:waystation", "tcnetworks:quay", "tcnetworks:harbor_master", "tcnetworks:bollard", "tcnetworks:slipway", "tcnetworks:road_depot",
                "tcnetworks:warehouse_core", "tcnetworks:storage_rack", "tcnetworks:treadwheel_crane", "tcnetworks:canal_lock_gate", "tcnetworks:lock_controller", "tcnetworks:railway_station",
                "tcnetworks:freight_station", "tcnetworks:engine_shed", "tcnetworks:steam_crane", "tcnetworks:dry_dock", "tcnetworks:water_tower", "tcnetworks:coal_stage",
                "tcnetworks:bus_stop", "tcnetworks:truck_depot", "tcnetworks:distribution_center", "tcnetworks:fuel_pump", "tcnetworks:pallet_rack", "tcnetworks:traction_substation",
                "tcnetworks:airfield_core", "tcnetworks:control_tower", "tcnetworks:runway_light", "tcnetworks:hangar", "tcnetworks:pipeline", "tcnetworks:pipeline_pump_station",
                "tcnetworks:pipeline_terminal", "tcnetworks:container_terminal", "tcnetworks:gantry_crane", "tcnetworks:asrs_rack", "tcnetworks:telegraph_pole", "tcnetworks:telegraph_office",
                "tcnetworks:telegraph_key", "tcnetworks:telephone_exchange", "tcnetworks:telephone", "tcnetworks:radio_mast", "tcnetworks:network_switch", "tcnetworks:fiber_optic_cable",
                "tcnetworks:network_router", "tcnetworks:dispatch_terminal"
        ));
        map.put("item", List.of(
                "tcnetworks:pack_saddle", "tcnetworks:cargo_sack", "tcnetworks:road_planner", "tcnetworks:rammer", "tcnetworks:harness", "tcnetworks:wooden_wheel",
                "tcnetworks:hull_section", "tcnetworks:sail", "tcnetworks:mast", "tcnetworks:cargo_barrel", "tcnetworks:waybill", "tcnetworks:survey_level",
                "tcnetworks:spoked_wheel", "tcnetworks:track_layer", "tcnetworks:repair_kit", "tcnetworks:iron_tyred_wheel", "tcnetworks:railway_wheelset", "tcnetworks:bogie",
                "tcnetworks:steam_powertrain", "tcnetworks:crucible_steel", "tcnetworks:ticket", "tcnetworks:pneumatic_tyre", "tcnetworks:combustion_powertrain", "tcnetworks:electric_traction_motor",
                "tcnetworks:pallet", "tcnetworks:fuel_canister", "tcnetworks:telegram", "tcnetworks:aero_engine", "tcnetworks:turbofan", "tcnetworks:shipping_container"
        ));
        map.put("entity", List.of(
                "tcnetworks:raft", "tcnetworks:dugout_canoe", "tcnetworks:sledge", "tcnetworks:handcart", "tcnetworks:ox_cart", "tcnetworks:plank_boat",
                "tcnetworks:sailing_boat", "tcnetworks:galley", "tcnetworks:horse_wagon", "tcnetworks:coach", "tcnetworks:canal_barge", "tcnetworks:cog",
                "tcnetworks:caravel", "tcnetworks:galleon", "tcnetworks:mine_wagon", "tcnetworks:steam_locomotive", "tcnetworks:freight_wagon", "tcnetworks:hopper_wagon",
                "tcnetworks:tank_wagon", "tcnetworks:passenger_coach", "tcnetworks:steamship", "tcnetworks:electric_tram", "tcnetworks:electric_locomotive", "tcnetworks:automobile",
                "tcnetworks:truck", "tcnetworks:bus", "tcnetworks:diesel_locomotive", "tcnetworks:propeller_aircraft", "tcnetworks:propeller_airliner", "tcnetworks:oil_tanker",
                "tcnetworks:bulk_carrier", "tcnetworks:semi_truck", "tcnetworks:jet_airliner", "tcnetworks:cargo_jet", "tcnetworks:helicopter", "tcnetworks:container_ship",
                "tcnetworks:high_speed_train", "tcnetworks:container_wagon", "tcnetworks:automated_guided_vehicle", "tcnetworks:delivery_drone", "tcnetworks:maglev_train"
        ));
        map.put("structure", List.of(
                "tcnetworks:wildlife_overpass"
        ));
        map.put("block_entity", List.of(
                "tcnetworks:track", "tcnetworks:signal", "tcnetworks:station", "tcnetworks:warehouse", "tcnetworks:crate", "tcnetworks:dispatch_board",
                "tcnetworks:telegraph_office", "tcnetworks:lock_controller", "tcnetworks:crane", "tcnetworks:pipeline_node", "tcnetworks:airfield", "tcnetworks:telephone_exchange"
        ));
        map.put("menu", List.of(
                "tcnetworks:vehicle", "tcnetworks:crate", "tcnetworks:warehouse", "tcnetworks:station", "tcnetworks:telegraph", "tcnetworks:lock_controller"
        ));
        map.put("keybind", List.of(
                "tcnetworks:horn", "tcnetworks:reverser"
        ));
        map.put("payload", List.of(
                "tcnetworks:vehicle_control", "tcnetworks:vehicle_state", "tcnetworks:track_graph_delta", "tcnetworks:route_plan_preview", "tcnetworks:route_plan_edit", "tcnetworks:dispatch_action",
                "tcnetworks:timetable_edit", "tcnetworks:signal_sync", "tcnetworks:telegraph_message", "tcnetworks:proxy_positions"
        ));
        map.put("hud_widget", List.of(
                "tcnetworks:vehicle_status", "tcnetworks:route_survey"
        ));
        map.put("civ_tab", List.of(
                "tcnetworks:logistics"
        ));
        map.put("region_layer", List.of(
                "tcnetworks:transport"
        ));
        map.put("sound", List.of(
                "tcnetworks:vehicle.cart.roll", "tcnetworks:vehicle.locomotive.whistle", "tcnetworks:vehicle.locomotive.chuff", "tcnetworks:vehicle.ship.creak", "tcnetworks:vehicle.ship.horn", "tcnetworks:vehicle.car.engine",
                "tcnetworks:vehicle.car.horn", "tcnetworks:vehicle.aircraft.propeller", "tcnetworks:vehicle.aircraft.jet", "tcnetworks:vehicle.tram.bell", "tcnetworks:block.signal.change", "tcnetworks:block.telegraph.key",
                "tcnetworks:block.crane.operate", "tcnetworks:block.lock.water", "tcnetworks:ambient.station.bustle", "tcnetworks:ambient.port.gulls", "tcnetworks:ui.dispatch.alert", "tcnetworks:event.milestone.first_train"
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
                    .icon(() -> new ItemStack(REGISTERED_ITEMS.getOrDefault("railway_station", REGISTERED_ITEMS.values().iterator().next()).get()))
                    .displayItems((params, output) -> ITEMS.getEntries().forEach(e -> output.accept(e.get())))
                    .build());

    private NetworksRegistries() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);
    }
}
