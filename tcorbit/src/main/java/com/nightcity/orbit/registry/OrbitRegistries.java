package com.nightcity.orbit.registry;

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

public final class OrbitRegistries {
    public static final String MOD_ID = "tcorbit";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

    private static final List<String> BLOCK_PATHS = List.of(
        "regolith", "mare_regolith", "ilmenite_regolith", "anorthosite", "lunar_basalt", "lunar_ice",
        "kreep_basalt", "mars_regolith", "hematite_regolith", "mars_basalt", "mars_ice", "dry_ice",
        "mars_clay", "co2_frost", "asteroid_rock", "carbonaceous_rock", "asteroid_metal", "sounding_rail",
        "solid_motor_segment", "solid_motor_nozzle", "rocket_engine", "propellant_tank", "stage_separator", "nose_cone",
        "avionics_unit", "kerolox_engine", "kerolox_vacuum_engine", "hypergolic_engine", "rcs_thruster", "payload_fairing",
        "radial_decoupler", "satellite_bus", "solar_array", "beacon_payload", "cryogenic_tank", "hydrolox_engine",
        "hydrolox_upper_engine", "comm_payload", "heat_shield", "parachute_pack", "earth_observation_payload", "science_payload",
        "solar_monitor_payload", "assembly_controller", "mobile_launch_platform", "assembly_crane", "crawlerway", "launch_pad_deck",
        "scorched_pad_deck", "flame_trench", "umbilical_tower", "propellant_loading_arm", "sound_suppression_tower", "launch_pad_controller",
        "mission_control_console", "tracking_antenna", "capsule_hull", "crew_seat", "capsule_hatch", "launch_escape_tower",
        "hull_plating", "core_module", "life_support_unit", "oxygen_tank", "water_tank", "co2_scrubber",
        "water_recycler", "oxygen_generator", "habitat_module_frame", "greenhouse_module", "crew_berth", "pressure_window",
        "pressure_hatch", "airlock_controller", "sealant_coating", "storm_shelter", "suit_port", "airlock_module",
        "docking_port", "lab_module", "node_module", "truss_segment", "propellant_depot", "orbital_dock",
        "station_arm", "radiator_panel", "whipple_shield", "debris_collector_payload", "landing_leg", "grid_fin",
        "landing_barge", "methalox_engine", "methalox_vacuum_engine", "nuclear_thermal_engine", "ion_thruster", "relay_payload",
        "lander_base", "deep_space_antenna", "outpost_core", "regolith_excavator", "regolith_sinterer", "sintered_regolith_brick",
        "ice_drill", "regolith_reducer", "molten_regolith_electrolyzer", "propellant_liquefier", "sabatier_reactor", "co2_collector",
        "solid_oxide_electrolyzer", "helium3_extractor", "perchlorate_washer", "washed_mars_soil", "colony_beacon"
    );
    private static final List<String> BLOCK_ITEM_PATHS = List.of(
        "regolith", "mare_regolith", "ilmenite_regolith", "anorthosite", "lunar_basalt", "lunar_ice",
        "kreep_basalt", "mars_regolith", "hematite_regolith", "mars_basalt", "mars_ice", "dry_ice",
        "mars_clay", "co2_frost", "asteroid_rock", "carbonaceous_rock", "asteroid_metal", "sounding_rail",
        "solid_motor_segment", "solid_motor_nozzle", "rocket_engine", "propellant_tank", "stage_separator", "nose_cone",
        "avionics_unit", "kerolox_engine", "kerolox_vacuum_engine", "hypergolic_engine", "rcs_thruster", "payload_fairing",
        "radial_decoupler", "satellite_bus", "solar_array", "beacon_payload", "cryogenic_tank", "hydrolox_engine",
        "hydrolox_upper_engine", "comm_payload", "heat_shield", "parachute_pack", "earth_observation_payload", "science_payload",
        "solar_monitor_payload", "assembly_controller", "mobile_launch_platform", "assembly_crane", "crawlerway", "launch_pad_deck",
        "scorched_pad_deck", "flame_trench", "umbilical_tower", "propellant_loading_arm", "sound_suppression_tower", "launch_pad_controller",
        "mission_control_console", "tracking_antenna", "capsule_hull", "crew_seat", "capsule_hatch", "launch_escape_tower",
        "hull_plating", "core_module", "life_support_unit", "oxygen_tank", "water_tank", "co2_scrubber",
        "water_recycler", "oxygen_generator", "habitat_module_frame", "greenhouse_module", "crew_berth", "pressure_window",
        "pressure_hatch", "airlock_controller", "sealant_coating", "storm_shelter", "suit_port", "airlock_module",
        "docking_port", "lab_module", "node_module", "truss_segment", "propellant_depot", "orbital_dock",
        "station_arm", "radiator_panel", "whipple_shield", "debris_collector_payload", "landing_leg", "grid_fin",
        "landing_barge", "methalox_engine", "methalox_vacuum_engine", "nuclear_thermal_engine", "ion_thruster", "relay_payload",
        "lander_base", "deep_space_antenna", "outpost_core", "regolith_excavator", "regolith_sinterer", "sintered_regolith_brick",
        "ice_drill", "regolith_reducer", "molten_regolith_electrolyzer", "propellant_liquefier", "sabatier_reactor", "co2_collector",
        "solid_oxide_electrolyzer", "helium3_extractor", "perchlorate_washer", "washed_mars_soil", "colony_beacon"
    );
    private static final List<String> STANDALONE_ITEM_PATHS = List.of(
        "rocket_blueprint", "pad_repair_kit", "repair_kit", "space_ration", "eva_suit", "engine_refurbish_kit",
        "lunar_sample", "mars_sample", "asteroid_sample", "helium3_canister", "lunar_metal_slag", "nickel_iron_chunk",
        "pgm_concentrate"
    );
    private static final List<String> SOUND_PATHS = List.of(
        "vehicle.rocket.ignition", "vehicle.rocket.countdown", "vehicle.rocket.stage_separation", "vehicle.docking.lock", "vehicle.reentry.plasma", "item.eva_suit.warning"
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
                "tcorbit:regolith", "tcorbit:mare_regolith", "tcorbit:ilmenite_regolith", "tcorbit:anorthosite", "tcorbit:lunar_basalt", "tcorbit:lunar_ice",
                "tcorbit:kreep_basalt", "tcorbit:mars_regolith", "tcorbit:hematite_regolith", "tcorbit:mars_basalt", "tcorbit:mars_ice", "tcorbit:dry_ice",
                "tcorbit:mars_clay", "tcorbit:co2_frost", "tcorbit:asteroid_rock", "tcorbit:carbonaceous_rock", "tcorbit:asteroid_metal", "tcorbit:sounding_rail",
                "tcorbit:solid_motor_segment", "tcorbit:solid_motor_nozzle", "tcorbit:rocket_engine", "tcorbit:propellant_tank", "tcorbit:stage_separator", "tcorbit:nose_cone",
                "tcorbit:avionics_unit", "tcorbit:kerolox_engine", "tcorbit:kerolox_vacuum_engine", "tcorbit:hypergolic_engine", "tcorbit:rcs_thruster", "tcorbit:payload_fairing",
                "tcorbit:radial_decoupler", "tcorbit:satellite_bus", "tcorbit:solar_array", "tcorbit:beacon_payload", "tcorbit:cryogenic_tank", "tcorbit:hydrolox_engine",
                "tcorbit:hydrolox_upper_engine", "tcorbit:comm_payload", "tcorbit:heat_shield", "tcorbit:parachute_pack", "tcorbit:earth_observation_payload", "tcorbit:science_payload",
                "tcorbit:solar_monitor_payload", "tcorbit:assembly_controller", "tcorbit:mobile_launch_platform", "tcorbit:assembly_crane", "tcorbit:crawlerway", "tcorbit:launch_pad_deck",
                "tcorbit:scorched_pad_deck", "tcorbit:flame_trench", "tcorbit:umbilical_tower", "tcorbit:propellant_loading_arm", "tcorbit:sound_suppression_tower", "tcorbit:launch_pad_controller",
                "tcorbit:mission_control_console", "tcorbit:tracking_antenna", "tcorbit:capsule_hull", "tcorbit:crew_seat", "tcorbit:capsule_hatch", "tcorbit:launch_escape_tower",
                "tcorbit:hull_plating", "tcorbit:core_module", "tcorbit:life_support_unit", "tcorbit:oxygen_tank", "tcorbit:water_tank", "tcorbit:co2_scrubber",
                "tcorbit:water_recycler", "tcorbit:oxygen_generator", "tcorbit:habitat_module_frame", "tcorbit:greenhouse_module", "tcorbit:crew_berth", "tcorbit:pressure_window",
                "tcorbit:pressure_hatch", "tcorbit:airlock_controller", "tcorbit:sealant_coating", "tcorbit:storm_shelter", "tcorbit:suit_port", "tcorbit:airlock_module",
                "tcorbit:docking_port", "tcorbit:lab_module", "tcorbit:node_module", "tcorbit:truss_segment", "tcorbit:propellant_depot", "tcorbit:orbital_dock",
                "tcorbit:station_arm", "tcorbit:radiator_panel", "tcorbit:whipple_shield", "tcorbit:debris_collector_payload", "tcorbit:landing_leg", "tcorbit:grid_fin",
                "tcorbit:landing_barge", "tcorbit:methalox_engine", "tcorbit:methalox_vacuum_engine", "tcorbit:nuclear_thermal_engine", "tcorbit:ion_thruster", "tcorbit:relay_payload",
                "tcorbit:lander_base", "tcorbit:deep_space_antenna", "tcorbit:outpost_core", "tcorbit:regolith_excavator", "tcorbit:regolith_sinterer", "tcorbit:sintered_regolith_brick",
                "tcorbit:ice_drill", "tcorbit:regolith_reducer", "tcorbit:molten_regolith_electrolyzer", "tcorbit:propellant_liquefier", "tcorbit:sabatier_reactor", "tcorbit:co2_collector",
                "tcorbit:solid_oxide_electrolyzer", "tcorbit:helium3_extractor", "tcorbit:perchlorate_washer", "tcorbit:washed_mars_soil", "tcorbit:colony_beacon"
        ));
        map.put("item", List.of(
                "tcorbit:rocket_blueprint", "tcorbit:pad_repair_kit", "tcorbit:repair_kit", "tcorbit:space_ration", "tcorbit:eva_suit", "tcorbit:engine_refurbish_kit",
                "tcorbit:lunar_sample", "tcorbit:mars_sample", "tcorbit:asteroid_sample", "tcorbit:helium3_canister", "tcorbit:lunar_metal_slag", "tcorbit:nickel_iron_chunk",
                "tcorbit:pgm_concentrate"
        ));
        map.put("entity", List.of(
                "tcorbit:rocket", "tcorbit:spent_stage", "tcorbit:crawler_transporter", "tcorbit:spacecraft", "tcorbit:lunar_rover", "tcorbit:pressurized_rover"
        ));
        map.put("fluid", List.of(
                "tcorbit:helium_3", "tcorbit:carbon_dioxide"
        ));
        map.put("dimension", List.of(
                "tcorbit:low_orbit", "tcorbit:moon", "tcorbit:mars"
        ));
        map.put("biome", List.of(
                "tcorbit:orbit_void", "tcorbit:lunar_mare", "tcorbit:lunar_highlands", "tcorbit:lunar_crater", "tcorbit:lunar_polar", "tcorbit:mars_lowlands",
                "tcorbit:mars_highlands", "tcorbit:mars_volcano", "tcorbit:mars_canyon", "tcorbit:mars_polar"
        ));
        map.put("block_entity", List.of(
                "tcorbit:assembly_controller", "tcorbit:launch_pad_controller", "tcorbit:mission_control_console", "tcorbit:life_support_unit", "tcorbit:airlock_controller", "tcorbit:isru_machine",
                "tcorbit:outpost_core", "tcorbit:colony_beacon"
        ));
        map.put("menu", List.of(
                "tcorbit:rocket_assembly", "tcorbit:launch_pad", "tcorbit:mission_control", "tcorbit:life_support", "tcorbit:isru_machine", "tcorbit:airlock",
                "tcorbit:suit_port", "tcorbit:propellant_depot", "tcorbit:colony_beacon"
        ));
        map.put("keybind", List.of(
                "tcorbit:toggle_rcs_mode", "tcorbit:cruise_confirm", "tcorbit:vehicle_headlights"
        ));
        map.put("payload", List.of(
                "tcorbit:assembly_state", "tcorbit:assembly_action", "tcorbit:mission_plan", "tcorbit:mission_sync", "tcorbit:launch_command", "tcorbit:orbit_view",
                "tcorbit:life_support_hud", "tcorbit:flight_telemetry", "tcorbit:rcs_input", "tcorbit:wrap_preload", "tcorbit:sky_snapshot"
        ));
        map.put("hud_widget", List.of(
                "tcorbit:life_support", "tcorbit:flight_telemetry"
        ));
        map.put("civ_tab", List.of(
                "tcorbit:space_program"
        ));
        map.put("region_layer", List.of(
                "tcorbit:remote_sensing"
        ));
        map.put("sound", List.of(
                "tcorbit:vehicle.rocket.ignition", "tcorbit:vehicle.rocket.countdown", "tcorbit:vehicle.rocket.stage_separation", "tcorbit:vehicle.docking.lock", "tcorbit:vehicle.reentry.plasma", "tcorbit:item.eva_suit.warning"
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
                    .icon(() -> new ItemStack(REGISTERED_ITEMS.getOrDefault("rocket_engine", REGISTERED_ITEMS.values().iterator().next()).get()))
                    .displayItems((params, output) -> ITEMS.getEntries().forEach(e -> output.accept(e.get())))
                    .build());

    private OrbitRegistries() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);
    }
}
