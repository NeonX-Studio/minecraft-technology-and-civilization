package com.nightcity.industry.registry;

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

public final class IndustryRegistries {
    public static final String MOD_ID = "tcindustry";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

    private static final List<String> BLOCK_PATHS = List.of(
        "panning_station", "sluice_box", "washing_trough", "nitre_bed", "evaporation_pond", "labour_post",
        "works_office", "pit_kiln", "updraft_kiln", "potters_wheel", "pottery_jar", "large_storage_jar",
        "brick_kiln", "dragon_kiln", "lime_kiln", "mixing_vat", "rotary_kiln", "concrete_mixer",
        "mud_brick", "fire_bricks", "silica_bricks", "magnesia_bricks", "alumina_bricks", "clay_lining",
        "graphite_block", "roman_concrete", "reinforced_concrete", "precast_panel", "saddle_quern", "millstone",
        "warp_weighted_loom", "tanning_pit", "fulling_mill", "spinning_mule", "power_loom", "sawmill",
        "powder_mill", "stamp_mill", "charcoal_clamp", "charcoal_kiln", "retort", "crucible_furnace",
        "casting_table", "forge_hearth", "forging_anvil", "hand_bellows", "bloomery", "glass_furnace",
        "water_bellows", "blast_furnace_controller", "hot_blast_stove", "blowing_engine", "beehive_coke_oven", "coke_oven_battery",
        "puddling_furnace", "bessemer_converter", "open_hearth_furnace", "ladle", "electric_arc_furnace", "hall_heroult_cell",
        "basic_oxygen_furnace", "float_glass_line", "solidified_slag", "trip_hammer", "rolling_mill", "wire_drawing_bench",
        "lathe", "planer", "milling_machine", "drill_press", "air_compressor", "canning_machine",
        "refrigeration_unit", "cnc_mill", "additive_printer", "wooden_shaft", "iron_shaft", "steel_shaft",
        "broken_shaft", "lantern_gear", "crown_gear", "cast_iron_gear", "steel_gear", "helical_gear",
        "gearbox", "pulley", "clutch", "slip_coupling", "flywheel", "governor",
        "hand_crank", "animal_gin", "treadwheel", "undershot_waterwheel", "overshot_waterwheel", "sluice_gate",
        "post_windmill", "tower_windmill", "haystack_boiler", "cornish_boiler", "lancashire_boiler", "water_tube_boiler",
        "heat_recovery_boiler", "steam_generator", "newcomen_engine", "watt_engine", "compound_engine", "condenser",
        "cooling_tower", "mechanical_stoker", "gas_engine", "gasoline_engine", "diesel_engine", "stationary_engine",
        "diesel_genset", "gas_turbine", "air_duct", "dc_dynamo", "turbine_generator", "hydro_turbine",
        "geothermal_well", "solar_panel", "inverter", "wind_turbine", "fuel_cell", "rtg",
        "copper_wire", "copper_cable", "mv_line", "hv_line", "ehv_line", "hvdc_line",
        "utility_pole", "transmission_tower", "pole_transformer", "distribution_transformer", "substation", "step_up_station",
        "converter_station", "circuit_breaker", "power_meter", "lead_acid_battery", "flywheel_storage", "lithium_battery_bank",
        "electric_motor_small", "electric_motor_medium", "electric_motor_large", "incandescent_lamp", "fluorescent_lamp", "led_lamp",
        "wooden_chute", "bucket_elevator", "conveyor_belt", "overhead_crane", "sorter", "robot_arm",
        "clay_pipe", "iron_pipe", "steel_pipe", "stainless_pipe", "pump", "tank",
        "fluid_loading_arm", "drilling_rig", "pumpjack", "wellhead", "lead_chamber", "gas_producer",
        "chemical_reactor", "chlor_alkali_cell", "distillation_tower", "haber_reactor", "flotation_cell", "air_separation_unit",
        "catalytic_cracker", "chimney", "cyclone_separator", "electrostatic_precipitator", "wastewater_plant", "fgd_scrubber",
        "scr_unit", "tailings_dam", "incinerator", "uranium_mill", "conversion_plant", "gas_centrifuge",
        "fuel_fabricator", "research_pile", "graphite_water_reactor", "pwr_reactor", "advanced_pwr_reactor", "fast_reactor",
        "compact_fission_reactor", "tokamak_controller", "control_rod_drive", "eccs_pump", "containment_dome", "hydrogen_recombiner",
        "spent_fuel_pool", "dry_cask", "reprocessing_plant", "decontamination_station", "remote_manipulator", "lead_cask",
        "radioactive_debris", "crystal_puller", "lithography_stepper", "cleanroom_panel", "relay_panel", "control_cable",
        "signal_converter", "thermostat", "pressure_switch", "float_switch", "pid_controller", "plc",
        "control_room", "tin_block", "lead_block", "silver_block", "bronze_block", "brass_block",
        "steel_block", "zinc_block", "aluminium_block", "stainless_steel_block", "crude_oil", "fuel_oil",
        "diesel", "gasoline", "kerosene", "naphtha", "lubricating_oil", "ethanol",
        "biodiesel", "wood_tar", "coal_tar", "molten_copper", "molten_gold", "molten_tin",
        "molten_lead", "molten_silver", "molten_bronze", "molten_brass", "molten_zinc", "molten_iron",
        "molten_pig_iron", "molten_steel", "molten_aluminium"
    );
    private static final List<String> BLOCK_ITEM_PATHS = List.of(
        "panning_station", "sluice_box", "washing_trough", "nitre_bed", "evaporation_pond", "labour_post",
        "works_office", "pit_kiln", "updraft_kiln", "potters_wheel", "pottery_jar", "large_storage_jar",
        "brick_kiln", "dragon_kiln", "lime_kiln", "mixing_vat", "rotary_kiln", "concrete_mixer",
        "mud_brick", "fire_bricks", "silica_bricks", "magnesia_bricks", "alumina_bricks", "clay_lining",
        "graphite_block", "roman_concrete", "reinforced_concrete", "precast_panel", "saddle_quern", "millstone",
        "warp_weighted_loom", "tanning_pit", "fulling_mill", "spinning_mule", "power_loom", "sawmill",
        "powder_mill", "stamp_mill", "charcoal_clamp", "charcoal_kiln", "retort", "crucible_furnace",
        "casting_table", "forge_hearth", "forging_anvil", "hand_bellows", "bloomery", "glass_furnace",
        "water_bellows", "blast_furnace_controller", "hot_blast_stove", "blowing_engine", "beehive_coke_oven", "coke_oven_battery",
        "puddling_furnace", "bessemer_converter", "open_hearth_furnace", "ladle", "electric_arc_furnace", "hall_heroult_cell",
        "basic_oxygen_furnace", "float_glass_line", "solidified_slag", "trip_hammer", "rolling_mill", "wire_drawing_bench",
        "lathe", "planer", "milling_machine", "drill_press", "air_compressor", "canning_machine",
        "refrigeration_unit", "cnc_mill", "additive_printer", "wooden_shaft", "iron_shaft", "steel_shaft",
        "broken_shaft", "lantern_gear", "crown_gear", "cast_iron_gear", "steel_gear", "helical_gear",
        "gearbox", "pulley", "clutch", "slip_coupling", "flywheel", "governor",
        "hand_crank", "animal_gin", "treadwheel", "undershot_waterwheel", "overshot_waterwheel", "sluice_gate",
        "post_windmill", "tower_windmill", "haystack_boiler", "cornish_boiler", "lancashire_boiler", "water_tube_boiler",
        "heat_recovery_boiler", "steam_generator", "newcomen_engine", "watt_engine", "compound_engine", "condenser",
        "cooling_tower", "mechanical_stoker", "gas_engine", "gasoline_engine", "diesel_engine", "stationary_engine",
        "diesel_genset", "gas_turbine", "air_duct", "dc_dynamo", "turbine_generator", "hydro_turbine",
        "geothermal_well", "solar_panel", "inverter", "wind_turbine", "fuel_cell", "rtg",
        "copper_wire", "copper_cable", "mv_line", "hv_line", "ehv_line", "hvdc_line",
        "utility_pole", "transmission_tower", "pole_transformer", "distribution_transformer", "substation", "step_up_station",
        "converter_station", "circuit_breaker", "power_meter", "lead_acid_battery", "flywheel_storage", "lithium_battery_bank",
        "electric_motor_small", "electric_motor_medium", "electric_motor_large", "incandescent_lamp", "fluorescent_lamp", "led_lamp",
        "wooden_chute", "bucket_elevator", "conveyor_belt", "overhead_crane", "sorter", "robot_arm",
        "clay_pipe", "iron_pipe", "steel_pipe", "stainless_pipe", "pump", "tank",
        "fluid_loading_arm", "drilling_rig", "pumpjack", "wellhead", "lead_chamber", "gas_producer",
        "chemical_reactor", "chlor_alkali_cell", "distillation_tower", "haber_reactor", "flotation_cell", "air_separation_unit",
        "catalytic_cracker", "chimney", "cyclone_separator", "electrostatic_precipitator", "wastewater_plant", "fgd_scrubber",
        "scr_unit", "tailings_dam", "incinerator", "uranium_mill", "conversion_plant", "gas_centrifuge",
        "fuel_fabricator", "research_pile", "graphite_water_reactor", "pwr_reactor", "advanced_pwr_reactor", "fast_reactor",
        "compact_fission_reactor", "tokamak_controller", "control_rod_drive", "eccs_pump", "containment_dome", "hydrogen_recombiner",
        "spent_fuel_pool", "dry_cask", "reprocessing_plant", "decontamination_station", "remote_manipulator", "lead_cask",
        "radioactive_debris", "crystal_puller", "lithography_stepper", "cleanroom_panel", "relay_panel", "control_cable",
        "signal_converter", "thermostat", "pressure_switch", "float_switch", "pid_controller", "plc",
        "control_room", "tin_block", "lead_block", "silver_block", "bronze_block", "brass_block",
        "steel_block", "zinc_block", "aluminium_block", "stainless_steel_block", "crude_oil", "fuel_oil",
        "diesel", "gasoline", "kerosene", "naphtha", "lubricating_oil", "ethanol",
        "biodiesel", "wood_tar", "coal_tar", "molten_copper", "molten_gold", "molten_tin",
        "molten_lead", "molten_silver", "molten_bronze", "molten_brass", "molten_zinc", "molten_iron",
        "molten_pig_iron", "molten_steel", "molten_aluminium"
    );
    private static final List<String> STANDALONE_ITEM_PATHS = List.of(
        "copper_plate", "tin_ingot", "tin_nugget", "tin_plate", "lead_ingot", "lead_nugget",
        "lead_plate", "silver_ingot", "silver_nugget", "bronze_ingot", "bronze_nugget", "bronze_plate",
        "arsenical_bronze_ingot", "brass_ingot", "brass_nugget", "brass_plate", "brass_rod", "brass_gear",
        "iron_plate", "iron_rod", "pig_iron", "steel_ingot", "steel_nugget", "steel_plate",
        "steel_rod", "zinc_ingot", "aluminium_ingot", "aluminium_plate", "aluminium_wire", "stainless_steel_ingot",
        "stainless_steel_plate", "alloy_steel_ingot", "alloy_steel_plate", "alloy_steel_rod", "titanium_ingot", "titanium_alloy_ingot",
        "titanium_plate", "titanium_alloy_plate", "al_li_alloy_ingot", "al_li_alloy_plate", "tungsten_ingot", "tungsten_wire",
        "nickel_ingot", "chromium_ingot", "molybdenum_ingot", "zirconium_ingot", "lithium_ingot", "cobalt_ingot",
        "magnesium_ingot", "solder", "tinplate", "iron_bloom", "workpiece", "unfired_crucible",
        "clay_crucible", "unfired_clay_mold", "clay_mold", "wax_model", "graphite_electrode", "tin_concentrate",
        "lead_concentrate", "zinc_concentrate", "chromium_concentrate", "nickel_concentrate", "tungsten_concentrate", "molybdenum_concentrate",
        "cobalt_concentrate", "titanium_concentrate", "rare_earth_concentrate", "zircon", "heavy_mineral_sand", "uranium_concentrate",
        "slag", "clinker", "red_mud", "alumina", "cryolite", "coke",
        "peat", "bitumen", "salt", "potash", "quicklime", "pozzolana",
        "gypsum", "saltpeter", "sulfur", "soda_ash", "caustic_soda", "superphosphate",
        "ammonium_sulfate", "ammonium_nitrate", "polysilicon", "lime_mortar", "portland_cement", "unfired_brick",
        "unfired_fire_brick", "fire_clay", "washed_clay", "unfired_pot", "unfired_porcelain", "porcelain",
        "optical_glass", "drop_spindle", "thread", "linen_cloth", "wool_cloth", "cotton_cloth",
        "silk_cloth", "broadcloth", "sail_cloth", "rawhide", "leather_belt", "rubber_belt",
        "tallow", "nylon_thread", "rubber", "synthetic_rubber", "polyethylene", "fiberglass",
        "carbon_fiber", "carbon_fiber_panel", "heat_shield_tile", "wind_turbine_blade", "wooden_gear", "piston_assembly",
        "turbine_core", "fuse", "punched_card", "vacuum_tube", "circuit_board", "lead_acid_cell",
        "transistor", "silicon_boule", "silicon_wafer", "integrated_circuit", "solar_cell", "lithium_cell",
        "yellowcake", "uranium_tailings", "depleted_uranium", "uo2_pellet", "zircaloy_tube", "natural_uranium_rod",
        "fuel_rod", "fuel_assembly", "spent_fuel_assembly", "mox_fuel_assembly", "vitrified_waste", "pu238_heat_source",
        "gas_cylinder", "acid_carboy", "tin_can", "canned_food", "crude_oil_bucket", "fuel_oil_bucket",
        "diesel_bucket", "gasoline_bucket", "kerosene_bucket", "naphtha_bucket", "lubricating_oil_bucket", "ethanol_bucket",
        "biodiesel_bucket", "wood_tar_bucket", "coal_tar_bucket", "maintenance_kit_iron", "maintenance_kit_steel", "maintenance_kit_precision",
        "bronze_pickaxe", "bronze_axe", "bronze_shovel", "bronze_hoe", "steel_pickaxe", "steel_axe",
        "steel_shovel", "steel_hoe", "alloy_steel_pickaxe", "alloy_steel_axe", "alloy_steel_shovel", "alloy_steel_hoe",
        "bronze_hammer", "iron_hammer", "steel_hammer", "bronze_tongs", "iron_tongs", "steel_tongs",
        "bronze_saw", "iron_saw", "steel_saw", "copper_chisel", "bronze_chisel", "iron_chisel",
        "steel_chisel", "copper_sickle", "bronze_sickle", "iron_sickle", "steel_sickle", "iron_file",
        "steel_file", "bronze_shears", "blowpipe", "wooden_pan", "gold_pan", "iron_plough_share",
        "steel_plough_share", "wrench", "engineering_tablet", "pneumatic_drill", "air_tank", "chainsaw",
        "power_drill", "angle_grinder"
    );
    private static final List<String> SOUND_PATHS = List.of(
        "machine.steam_engine.run", "machine.steam_engine.chuff", "machine.gear_whine", "machine.belt_slip", "machine.bearing_squeal", "machine.fault.generic",
        "block.boiler.safety_valve", "block.boiler.hiss", "block.furnace.roar", "block.forge.strike", "block.forge.quench", "block.grid.transformer_hum",
        "event.grid.breaker_trip", "event.reactor.alarm", "event.accident.explosion", "machine.jam", "machine.waterwheel.turn", "machine.windmill.creak",
        "machine.trip_hammer.strike", "machine.loom.clack", "machine.lathe.cut", "machine.centrifuge.whine", "machine.conveyor.run", "machine.pump.run",
        "machine.diesel_engine.run", "machine.gas_turbine.run", "machine.turbine.run", "block.kiln.crackle", "block.converter.blow", "block.pipe.leak",
        "event.process.complete", "event.multiblock.formed"
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
                "tcindustry:panning_station", "tcindustry:sluice_box", "tcindustry:washing_trough", "tcindustry:nitre_bed", "tcindustry:evaporation_pond", "tcindustry:labour_post",
                "tcindustry:works_office", "tcindustry:pit_kiln", "tcindustry:updraft_kiln", "tcindustry:potters_wheel", "tcindustry:pottery_jar", "tcindustry:large_storage_jar",
                "tcindustry:brick_kiln", "tcindustry:dragon_kiln", "tcindustry:lime_kiln", "tcindustry:mixing_vat", "tcindustry:rotary_kiln", "tcindustry:concrete_mixer",
                "tcindustry:mud_brick", "tcindustry:fire_bricks", "tcindustry:silica_bricks", "tcindustry:magnesia_bricks", "tcindustry:alumina_bricks", "tcindustry:clay_lining",
                "tcindustry:graphite_block", "tcindustry:roman_concrete", "tcindustry:reinforced_concrete", "tcindustry:precast_panel", "tcindustry:saddle_quern", "tcindustry:millstone",
                "tcindustry:warp_weighted_loom", "tcindustry:tanning_pit", "tcindustry:fulling_mill", "tcindustry:spinning_mule", "tcindustry:power_loom", "tcindustry:sawmill",
                "tcindustry:powder_mill", "tcindustry:stamp_mill", "tcindustry:charcoal_clamp", "tcindustry:charcoal_kiln", "tcindustry:retort", "tcindustry:crucible_furnace",
                "tcindustry:casting_table", "tcindustry:forge_hearth", "tcindustry:forging_anvil", "tcindustry:hand_bellows", "tcindustry:bloomery", "tcindustry:glass_furnace",
                "tcindustry:water_bellows", "tcindustry:blast_furnace_controller", "tcindustry:hot_blast_stove", "tcindustry:blowing_engine", "tcindustry:beehive_coke_oven", "tcindustry:coke_oven_battery",
                "tcindustry:puddling_furnace", "tcindustry:bessemer_converter", "tcindustry:open_hearth_furnace", "tcindustry:ladle", "tcindustry:electric_arc_furnace", "tcindustry:hall_heroult_cell",
                "tcindustry:basic_oxygen_furnace", "tcindustry:float_glass_line", "tcindustry:solidified_slag", "tcindustry:trip_hammer", "tcindustry:rolling_mill", "tcindustry:wire_drawing_bench",
                "tcindustry:lathe", "tcindustry:planer", "tcindustry:milling_machine", "tcindustry:drill_press", "tcindustry:air_compressor", "tcindustry:canning_machine",
                "tcindustry:refrigeration_unit", "tcindustry:cnc_mill", "tcindustry:additive_printer", "tcindustry:wooden_shaft", "tcindustry:iron_shaft", "tcindustry:steel_shaft",
                "tcindustry:broken_shaft", "tcindustry:lantern_gear", "tcindustry:crown_gear", "tcindustry:cast_iron_gear", "tcindustry:steel_gear", "tcindustry:helical_gear",
                "tcindustry:gearbox", "tcindustry:pulley", "tcindustry:clutch", "tcindustry:slip_coupling", "tcindustry:flywheel", "tcindustry:governor",
                "tcindustry:hand_crank", "tcindustry:animal_gin", "tcindustry:treadwheel", "tcindustry:undershot_waterwheel", "tcindustry:overshot_waterwheel", "tcindustry:sluice_gate",
                "tcindustry:post_windmill", "tcindustry:tower_windmill", "tcindustry:haystack_boiler", "tcindustry:cornish_boiler", "tcindustry:lancashire_boiler", "tcindustry:water_tube_boiler",
                "tcindustry:heat_recovery_boiler", "tcindustry:steam_generator", "tcindustry:newcomen_engine", "tcindustry:watt_engine", "tcindustry:compound_engine", "tcindustry:condenser",
                "tcindustry:cooling_tower", "tcindustry:mechanical_stoker", "tcindustry:gas_engine", "tcindustry:gasoline_engine", "tcindustry:diesel_engine", "tcindustry:stationary_engine",
                "tcindustry:diesel_genset", "tcindustry:gas_turbine", "tcindustry:air_duct", "tcindustry:dc_dynamo", "tcindustry:turbine_generator", "tcindustry:hydro_turbine",
                "tcindustry:geothermal_well", "tcindustry:solar_panel", "tcindustry:inverter", "tcindustry:wind_turbine", "tcindustry:fuel_cell", "tcindustry:rtg",
                "tcindustry:copper_wire", "tcindustry:copper_cable", "tcindustry:mv_line", "tcindustry:hv_line", "tcindustry:ehv_line", "tcindustry:hvdc_line",
                "tcindustry:utility_pole", "tcindustry:transmission_tower", "tcindustry:pole_transformer", "tcindustry:distribution_transformer", "tcindustry:substation", "tcindustry:step_up_station",
                "tcindustry:converter_station", "tcindustry:circuit_breaker", "tcindustry:power_meter", "tcindustry:lead_acid_battery", "tcindustry:flywheel_storage", "tcindustry:lithium_battery_bank",
                "tcindustry:electric_motor_small", "tcindustry:electric_motor_medium", "tcindustry:electric_motor_large", "tcindustry:incandescent_lamp", "tcindustry:fluorescent_lamp", "tcindustry:led_lamp",
                "tcindustry:wooden_chute", "tcindustry:bucket_elevator", "tcindustry:conveyor_belt", "tcindustry:overhead_crane", "tcindustry:sorter", "tcindustry:robot_arm",
                "tcindustry:clay_pipe", "tcindustry:iron_pipe", "tcindustry:steel_pipe", "tcindustry:stainless_pipe", "tcindustry:pump", "tcindustry:tank",
                "tcindustry:fluid_loading_arm", "tcindustry:drilling_rig", "tcindustry:pumpjack", "tcindustry:wellhead", "tcindustry:lead_chamber", "tcindustry:gas_producer",
                "tcindustry:chemical_reactor", "tcindustry:chlor_alkali_cell", "tcindustry:distillation_tower", "tcindustry:haber_reactor", "tcindustry:flotation_cell", "tcindustry:air_separation_unit",
                "tcindustry:catalytic_cracker", "tcindustry:chimney", "tcindustry:cyclone_separator", "tcindustry:electrostatic_precipitator", "tcindustry:wastewater_plant", "tcindustry:fgd_scrubber",
                "tcindustry:scr_unit", "tcindustry:tailings_dam", "tcindustry:incinerator", "tcindustry:uranium_mill", "tcindustry:conversion_plant", "tcindustry:gas_centrifuge",
                "tcindustry:fuel_fabricator", "tcindustry:research_pile", "tcindustry:graphite_water_reactor", "tcindustry:pwr_reactor", "tcindustry:advanced_pwr_reactor", "tcindustry:fast_reactor",
                "tcindustry:compact_fission_reactor", "tcindustry:tokamak_controller", "tcindustry:control_rod_drive", "tcindustry:eccs_pump", "tcindustry:containment_dome", "tcindustry:hydrogen_recombiner",
                "tcindustry:spent_fuel_pool", "tcindustry:dry_cask", "tcindustry:reprocessing_plant", "tcindustry:decontamination_station", "tcindustry:remote_manipulator", "tcindustry:lead_cask",
                "tcindustry:radioactive_debris", "tcindustry:crystal_puller", "tcindustry:lithography_stepper", "tcindustry:cleanroom_panel", "tcindustry:relay_panel", "tcindustry:control_cable",
                "tcindustry:signal_converter", "tcindustry:thermostat", "tcindustry:pressure_switch", "tcindustry:float_switch", "tcindustry:pid_controller", "tcindustry:plc",
                "tcindustry:control_room", "tcindustry:tin_block", "tcindustry:lead_block", "tcindustry:silver_block", "tcindustry:bronze_block", "tcindustry:brass_block",
                "tcindustry:steel_block", "tcindustry:zinc_block", "tcindustry:aluminium_block", "tcindustry:stainless_steel_block", "tcindustry:crude_oil", "tcindustry:fuel_oil",
                "tcindustry:diesel", "tcindustry:gasoline", "tcindustry:kerosene", "tcindustry:naphtha", "tcindustry:lubricating_oil", "tcindustry:ethanol",
                "tcindustry:biodiesel", "tcindustry:wood_tar", "tcindustry:coal_tar", "tcindustry:molten_copper", "tcindustry:molten_gold", "tcindustry:molten_tin",
                "tcindustry:molten_lead", "tcindustry:molten_silver", "tcindustry:molten_bronze", "tcindustry:molten_brass", "tcindustry:molten_zinc", "tcindustry:molten_iron",
                "tcindustry:molten_pig_iron", "tcindustry:molten_steel", "tcindustry:molten_aluminium"
        ));
        map.put("item", List.of(
                "tcindustry:copper_plate", "tcindustry:tin_ingot", "tcindustry:tin_nugget", "tcindustry:tin_plate", "tcindustry:lead_ingot", "tcindustry:lead_nugget",
                "tcindustry:lead_plate", "tcindustry:silver_ingot", "tcindustry:silver_nugget", "tcindustry:bronze_ingot", "tcindustry:bronze_nugget", "tcindustry:bronze_plate",
                "tcindustry:arsenical_bronze_ingot", "tcindustry:brass_ingot", "tcindustry:brass_nugget", "tcindustry:brass_plate", "tcindustry:brass_rod", "tcindustry:brass_gear",
                "tcindustry:iron_plate", "tcindustry:iron_rod", "tcindustry:pig_iron", "tcindustry:steel_ingot", "tcindustry:steel_nugget", "tcindustry:steel_plate",
                "tcindustry:steel_rod", "tcindustry:zinc_ingot", "tcindustry:aluminium_ingot", "tcindustry:aluminium_plate", "tcindustry:aluminium_wire", "tcindustry:stainless_steel_ingot",
                "tcindustry:stainless_steel_plate", "tcindustry:alloy_steel_ingot", "tcindustry:alloy_steel_plate", "tcindustry:alloy_steel_rod", "tcindustry:titanium_ingot", "tcindustry:titanium_alloy_ingot",
                "tcindustry:titanium_plate", "tcindustry:titanium_alloy_plate", "tcindustry:al_li_alloy_ingot", "tcindustry:al_li_alloy_plate", "tcindustry:tungsten_ingot", "tcindustry:tungsten_wire",
                "tcindustry:nickel_ingot", "tcindustry:chromium_ingot", "tcindustry:molybdenum_ingot", "tcindustry:zirconium_ingot", "tcindustry:lithium_ingot", "tcindustry:cobalt_ingot",
                "tcindustry:magnesium_ingot", "tcindustry:solder", "tcindustry:tinplate", "tcindustry:iron_bloom", "tcindustry:workpiece", "tcindustry:unfired_crucible",
                "tcindustry:clay_crucible", "tcindustry:unfired_clay_mold", "tcindustry:clay_mold", "tcindustry:wax_model", "tcindustry:graphite_electrode", "tcindustry:tin_concentrate",
                "tcindustry:lead_concentrate", "tcindustry:zinc_concentrate", "tcindustry:chromium_concentrate", "tcindustry:nickel_concentrate", "tcindustry:tungsten_concentrate", "tcindustry:molybdenum_concentrate",
                "tcindustry:cobalt_concentrate", "tcindustry:titanium_concentrate", "tcindustry:rare_earth_concentrate", "tcindustry:zircon", "tcindustry:heavy_mineral_sand", "tcindustry:uranium_concentrate",
                "tcindustry:slag", "tcindustry:clinker", "tcindustry:red_mud", "tcindustry:alumina", "tcindustry:cryolite", "tcindustry:coke",
                "tcindustry:peat", "tcindustry:bitumen", "tcindustry:salt", "tcindustry:potash", "tcindustry:quicklime", "tcindustry:pozzolana",
                "tcindustry:gypsum", "tcindustry:saltpeter", "tcindustry:sulfur", "tcindustry:soda_ash", "tcindustry:caustic_soda", "tcindustry:superphosphate",
                "tcindustry:ammonium_sulfate", "tcindustry:ammonium_nitrate", "tcindustry:polysilicon", "tcindustry:lime_mortar", "tcindustry:portland_cement", "tcindustry:unfired_brick",
                "tcindustry:unfired_fire_brick", "tcindustry:fire_clay", "tcindustry:washed_clay", "tcindustry:unfired_pot", "tcindustry:unfired_porcelain", "tcindustry:porcelain",
                "tcindustry:optical_glass", "tcindustry:drop_spindle", "tcindustry:thread", "tcindustry:linen_cloth", "tcindustry:wool_cloth", "tcindustry:cotton_cloth",
                "tcindustry:silk_cloth", "tcindustry:broadcloth", "tcindustry:sail_cloth", "tcindustry:rawhide", "tcindustry:leather_belt", "tcindustry:rubber_belt",
                "tcindustry:tallow", "tcindustry:nylon_thread", "tcindustry:rubber", "tcindustry:synthetic_rubber", "tcindustry:polyethylene", "tcindustry:fiberglass",
                "tcindustry:carbon_fiber", "tcindustry:carbon_fiber_panel", "tcindustry:heat_shield_tile", "tcindustry:wind_turbine_blade", "tcindustry:wooden_gear", "tcindustry:piston_assembly",
                "tcindustry:turbine_core", "tcindustry:fuse", "tcindustry:punched_card", "tcindustry:vacuum_tube", "tcindustry:circuit_board", "tcindustry:lead_acid_cell",
                "tcindustry:transistor", "tcindustry:silicon_boule", "tcindustry:silicon_wafer", "tcindustry:integrated_circuit", "tcindustry:solar_cell", "tcindustry:lithium_cell",
                "tcindustry:yellowcake", "tcindustry:uranium_tailings", "tcindustry:depleted_uranium", "tcindustry:uo2_pellet", "tcindustry:zircaloy_tube", "tcindustry:natural_uranium_rod",
                "tcindustry:fuel_rod", "tcindustry:fuel_assembly", "tcindustry:spent_fuel_assembly", "tcindustry:mox_fuel_assembly", "tcindustry:vitrified_waste", "tcindustry:pu238_heat_source",
                "tcindustry:gas_cylinder", "tcindustry:acid_carboy", "tcindustry:tin_can", "tcindustry:canned_food", "tcindustry:crude_oil_bucket", "tcindustry:fuel_oil_bucket",
                "tcindustry:diesel_bucket", "tcindustry:gasoline_bucket", "tcindustry:kerosene_bucket", "tcindustry:naphtha_bucket", "tcindustry:lubricating_oil_bucket", "tcindustry:ethanol_bucket",
                "tcindustry:biodiesel_bucket", "tcindustry:wood_tar_bucket", "tcindustry:coal_tar_bucket", "tcindustry:maintenance_kit_iron", "tcindustry:maintenance_kit_steel", "tcindustry:maintenance_kit_precision",
                "tcindustry:bronze_pickaxe", "tcindustry:bronze_axe", "tcindustry:bronze_shovel", "tcindustry:bronze_hoe", "tcindustry:steel_pickaxe", "tcindustry:steel_axe",
                "tcindustry:steel_shovel", "tcindustry:steel_hoe", "tcindustry:alloy_steel_pickaxe", "tcindustry:alloy_steel_axe", "tcindustry:alloy_steel_shovel", "tcindustry:alloy_steel_hoe",
                "tcindustry:bronze_hammer", "tcindustry:iron_hammer", "tcindustry:steel_hammer", "tcindustry:bronze_tongs", "tcindustry:iron_tongs", "tcindustry:steel_tongs",
                "tcindustry:bronze_saw", "tcindustry:iron_saw", "tcindustry:steel_saw", "tcindustry:copper_chisel", "tcindustry:bronze_chisel", "tcindustry:iron_chisel",
                "tcindustry:steel_chisel", "tcindustry:copper_sickle", "tcindustry:bronze_sickle", "tcindustry:iron_sickle", "tcindustry:steel_sickle", "tcindustry:iron_file",
                "tcindustry:steel_file", "tcindustry:bronze_shears", "tcindustry:blowpipe", "tcindustry:wooden_pan", "tcindustry:gold_pan", "tcindustry:iron_plough_share",
                "tcindustry:steel_plough_share", "tcindustry:wrench", "tcindustry:engineering_tablet", "tcindustry:pneumatic_drill", "tcindustry:air_tank", "tcindustry:chainsaw",
                "tcindustry:power_drill", "tcindustry:angle_grinder"
        ));
        map.put("fluid", List.of(
                "tcindustry:crude_oil", "tcindustry:natural_gas", "tcindustry:fuel_oil", "tcindustry:diesel", "tcindustry:gasoline", "tcindustry:kerosene",
                "tcindustry:naphtha", "tcindustry:lubricating_oil", "tcindustry:biodiesel", "tcindustry:ethanol", "tcindustry:wood_tar", "tcindustry:pyroligneous_acid",
                "tcindustry:coal_gas", "tcindustry:coal_tar", "tcindustry:ammonia_liquor", "tcindustry:steam", "tcindustry:hydrogen", "tcindustry:oxygen",
                "tcindustry:nitrogen", "tcindustry:liquid_hydrogen", "tcindustry:liquid_oxygen", "tcindustry:liquefied_natural_gas", "tcindustry:argon", "tcindustry:hypergolic_propellant",
                "tcindustry:ammonia", "tcindustry:chlorine", "tcindustry:ethylene", "tcindustry:sulfuric_acid", "tcindustry:nitric_acid", "tcindustry:hydrochloric_acid",
                "tcindustry:phosphoric_acid", "tcindustry:hydrofluoric_acid", "tcindustry:uranium_hexafluoride", "tcindustry:deuterium", "tcindustry:tritium", "tcindustry:mercury",
                "tcindustry:molten_copper", "tcindustry:molten_gold", "tcindustry:molten_tin", "tcindustry:molten_lead", "tcindustry:molten_silver", "tcindustry:molten_bronze",
                "tcindustry:molten_brass", "tcindustry:molten_zinc", "tcindustry:molten_iron", "tcindustry:molten_pig_iron", "tcindustry:molten_steel", "tcindustry:molten_aluminium",
                "tcindustry:flowing_crude_oil", "tcindustry:flowing_fuel_oil", "tcindustry:flowing_diesel", "tcindustry:flowing_gasoline", "tcindustry:flowing_kerosene", "tcindustry:flowing_naphtha",
                "tcindustry:flowing_lubricating_oil", "tcindustry:flowing_ethanol", "tcindustry:flowing_biodiesel", "tcindustry:flowing_wood_tar", "tcindustry:flowing_coal_tar", "tcindustry:flowing_molten_copper",
                "tcindustry:flowing_molten_gold", "tcindustry:flowing_molten_tin", "tcindustry:flowing_molten_lead", "tcindustry:flowing_molten_silver", "tcindustry:flowing_molten_bronze", "tcindustry:flowing_molten_brass",
                "tcindustry:flowing_molten_zinc", "tcindustry:flowing_molten_iron", "tcindustry:flowing_molten_pig_iron", "tcindustry:flowing_molten_steel", "tcindustry:flowing_molten_aluminium"
        ));
        map.put("block_entity", List.of(
                "tcindustry:kiln", "tcindustry:smelter", "tcindustry:forge", "tcindustry:casting_table", "tcindustry:process_machine", "tcindustry:handcraft_station",
                "tcindustry:power_source", "tcindustry:kinetic_part", "tcindustry:steam_engine", "tcindustry:boiler", "tcindustry:combustion_engine", "tcindustry:generator",
                "tcindustry:electric_motor", "tcindustry:grid_node", "tcindustry:energy_storage", "tcindustry:electric_consumer", "tcindustry:fluid_pipe", "tcindustry:pump",
                "tcindustry:tank", "tcindustry:conveyor", "tcindustry:extraction_well", "tcindustry:chemical_plant", "tcindustry:emission_control", "tcindustry:reactor",
                "tcindustry:nuclear_facility", "tcindustry:semiconductor_fab", "tcindustry:automation_device", "tcindustry:works_office"
        ));
        map.put("menu", List.of(
                "tcindustry:machine", "tcindustry:furnace", "tcindustry:forging", "tcindustry:boiler", "tcindustry:generator", "tcindustry:reactor",
                "tcindustry:grid_node", "tcindustry:tank", "tcindustry:works_office", "tcindustry:plc", "tcindustry:sorter", "tcindustry:control_room"
        ));
        map.put("keybind", List.of(
                "tcindustry:network_overlay"
        ));
        map.put("payload", List.of(
                "tcindustry:machine_sync", "tcindustry:network_annotations", "tcindustry:multiblock_preview", "tcindustry:accident_fx", "tcindustry:machine_anim", "tcindustry:production_snapshot",
                "tcindustry:machine_config", "tcindustry:forge_strike", "tcindustry:plc_upload", "tcindustry:sorter_filter", "tcindustry:reactor_command", "tcindustry:grid_dispatch",
                "tcindustry:production_action"
        ));
        map.put("hud_widget", List.of(
                "tcindustry:network_status"
        ));
        map.put("civ_tab", List.of(
                "tcindustry:production"
        ));
        map.put("region_layer", List.of(
                "tcindustry:emission"
        ));
        map.put("sound", List.of(
                "tcindustry:machine.steam_engine.run", "tcindustry:machine.steam_engine.chuff", "tcindustry:machine.gear_whine", "tcindustry:machine.belt_slip", "tcindustry:machine.bearing_squeal", "tcindustry:machine.fault.generic",
                "tcindustry:block.boiler.safety_valve", "tcindustry:block.boiler.hiss", "tcindustry:block.furnace.roar", "tcindustry:block.forge.strike", "tcindustry:block.forge.quench", "tcindustry:block.grid.transformer_hum",
                "tcindustry:event.grid.breaker_trip", "tcindustry:event.reactor.alarm", "tcindustry:event.accident.explosion", "tcindustry:machine.jam", "tcindustry:machine.waterwheel.turn", "tcindustry:machine.windmill.creak",
                "tcindustry:machine.trip_hammer.strike", "tcindustry:machine.loom.clack", "tcindustry:machine.lathe.cut", "tcindustry:machine.centrifuge.whine", "tcindustry:machine.conveyor.run", "tcindustry:machine.pump.run",
                "tcindustry:machine.diesel_engine.run", "tcindustry:machine.gas_turbine.run", "tcindustry:machine.turbine.run", "tcindustry:block.kiln.crackle", "tcindustry:block.converter.blow", "tcindustry:block.pipe.leak",
                "tcindustry:event.process.complete", "tcindustry:event.multiblock.formed"
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
                    .icon(() -> new ItemStack(REGISTERED_ITEMS.getOrDefault("forging_anvil", REGISTERED_ITEMS.values().iterator().next()).get()))
                    .displayItems((params, output) -> ITEMS.getEntries().forEach(e -> output.accept(e.get())))
                    .build());

    private IndustryRegistries() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);
    }
}
