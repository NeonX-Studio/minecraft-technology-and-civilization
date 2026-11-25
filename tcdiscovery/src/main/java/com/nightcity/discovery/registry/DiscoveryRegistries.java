package com.nightcity.discovery.registry;

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

public final class DiscoveryRegistries {
    public static final String MOD_ID = "tcdiscovery";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

    private static final List<String> BLOCK_PATHS = List.of(
        "sieve", "elder_seat", "calendar_stone", "gnomon", "scribe_desk", "tablet_rack",
        "dig_site_stake", "scholars_table", "scroll_shelf", "water_clock", "armillary_sphere", "experiment_bench",
        "scriptorium_desk", "paper_vat", "pulp_stamper", "academy_lectern", "refracting_telescope", "compound_microscope",
        "air_pump", "printing_press", "type_case", "specimen_cabinet", "display_case", "mounted_specimen",
        "engineering_college_desk", "lab_bench", "fume_hood", "camera", "research_director_desk", "spectroscope",
        "oscilloscope", "national_lab_control", "electron_microscope", "cloud_chamber", "radio_telescope_dish", "mainframe_terminal",
        "mainframe_rack", "space_research_console"
    );
    private static final List<String> BLOCK_ITEM_PATHS = List.of(
        "sieve", "elder_seat", "calendar_stone", "gnomon", "scribe_desk", "tablet_rack",
        "dig_site_stake", "scholars_table", "scroll_shelf", "water_clock", "armillary_sphere", "experiment_bench",
        "scriptorium_desk", "paper_vat", "pulp_stamper", "academy_lectern", "refracting_telescope", "compound_microscope",
        "air_pump", "printing_press", "type_case", "specimen_cabinet", "display_case", "mounted_specimen",
        "engineering_college_desk", "lab_bench", "fume_hood", "camera", "research_director_desk", "spectroscope",
        "oscilloscope", "national_lab_control", "electron_microscope", "cloud_chamber", "radio_telescope_dish", "mainframe_terminal",
        "mainframe_rack", "space_research_console"
    );
    private static final List<String> STANDALONE_ITEM_PATHS = List.of(
        "trowel", "fossil", "tally_stick", "stylus", "wet_clay_tablet", "clay_tablet",
        "balance_scale", "bamboo_slips", "parchment", "scroll", "ink", "specimen_jar",
        "herbarium_sheet", "mineral_sample", "streak_plate", "quill", "manuscript", "paper_mould",
        "hand_lens", "lens", "alembic", "printed_book", "movable_type", "thermometer",
        "barometer", "journal_issue", "analytical_balance", "photographic_plate", "technical_report", "microfilm_reel",
        "data_tape", "space_telescope"
    );
    private static final List<String> SOUND_PATHS = List.of(
        "event.observe_complete", "event.discovery_made", "item.quill.scratch", "block.printing_press.stamp", "event.experiment_fail", "ui.page_turn"
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
                "tcdiscovery:sieve", "tcdiscovery:elder_seat", "tcdiscovery:calendar_stone", "tcdiscovery:gnomon", "tcdiscovery:scribe_desk", "tcdiscovery:tablet_rack",
                "tcdiscovery:dig_site_stake", "tcdiscovery:scholars_table", "tcdiscovery:scroll_shelf", "tcdiscovery:water_clock", "tcdiscovery:armillary_sphere", "tcdiscovery:experiment_bench",
                "tcdiscovery:scriptorium_desk", "tcdiscovery:paper_vat", "tcdiscovery:pulp_stamper", "tcdiscovery:academy_lectern", "tcdiscovery:refracting_telescope", "tcdiscovery:compound_microscope",
                "tcdiscovery:air_pump", "tcdiscovery:printing_press", "tcdiscovery:type_case", "tcdiscovery:specimen_cabinet", "tcdiscovery:display_case", "tcdiscovery:mounted_specimen",
                "tcdiscovery:engineering_college_desk", "tcdiscovery:lab_bench", "tcdiscovery:fume_hood", "tcdiscovery:camera", "tcdiscovery:research_director_desk", "tcdiscovery:spectroscope",
                "tcdiscovery:oscilloscope", "tcdiscovery:national_lab_control", "tcdiscovery:electron_microscope", "tcdiscovery:cloud_chamber", "tcdiscovery:radio_telescope_dish", "tcdiscovery:mainframe_terminal",
                "tcdiscovery:mainframe_rack", "tcdiscovery:space_research_console"
        ));
        map.put("item", List.of(
                "tcdiscovery:trowel", "tcdiscovery:fossil", "tcdiscovery:tally_stick", "tcdiscovery:stylus", "tcdiscovery:wet_clay_tablet", "tcdiscovery:clay_tablet",
                "tcdiscovery:balance_scale", "tcdiscovery:bamboo_slips", "tcdiscovery:parchment", "tcdiscovery:scroll", "tcdiscovery:ink", "tcdiscovery:specimen_jar",
                "tcdiscovery:herbarium_sheet", "tcdiscovery:mineral_sample", "tcdiscovery:streak_plate", "tcdiscovery:quill", "tcdiscovery:manuscript", "tcdiscovery:paper_mould",
                "tcdiscovery:hand_lens", "tcdiscovery:lens", "tcdiscovery:alembic", "tcdiscovery:printed_book", "tcdiscovery:movable_type", "tcdiscovery:thermometer",
                "tcdiscovery:barometer", "tcdiscovery:journal_issue", "tcdiscovery:analytical_balance", "tcdiscovery:photographic_plate", "tcdiscovery:technical_report", "tcdiscovery:microfilm_reel",
                "tcdiscovery:data_tape", "tcdiscovery:space_telescope"
        ));
        map.put("block_entity", List.of(
                "tcdiscovery:research_facility", "tcdiscovery:experiment_bench", "tcdiscovery:lab_bench", "tcdiscovery:copying_desk", "tcdiscovery:printing_press", "tcdiscovery:specimen_cabinet",
                "tcdiscovery:display_case", "tcdiscovery:knowledge_shelf", "tcdiscovery:telescope", "tcdiscovery:microscope", "tcdiscovery:dig_site", "tcdiscovery:paper_vat",
                "tcdiscovery:pulp_stamper", "tcdiscovery:mainframe", "tcdiscovery:sieve"
        ));
        map.put("menu", List.of(
                "tcdiscovery:research_facility", "tcdiscovery:experiment", "tcdiscovery:copying", "tcdiscovery:printing_press", "tcdiscovery:dig_site", "tcdiscovery:paper_vat"
        ));
        map.put("keybind", List.of(
                "tcdiscovery:observe", "tcdiscovery:journal"
        ));
        map.put("payload", List.of(
                "tcdiscovery:observe_request", "tcdiscovery:observation_result", "tcdiscovery:known_properties_sync", "tcdiscovery:hypothesis_action", "tcdiscovery:experiment_action", "tcdiscovery:experiment_update",
                "tcdiscovery:book_compose", "tcdiscovery:dig_record", "tcdiscovery:facility_config"
        ));
        map.put("hud_widget", List.of(
                "tcdiscovery:experiment_status", "tcdiscovery:field_notes"
        ));
        map.put("civ_tab", List.of(
                "tcdiscovery:journal"
        ));
        map.put("region_layer", List.of(
                "tcdiscovery:field_record"
        ));
        map.put("sound", List.of(
                "tcdiscovery:event.observe_complete", "tcdiscovery:event.discovery_made", "tcdiscovery:item.quill.scratch", "tcdiscovery:block.printing_press.stamp", "tcdiscovery:event.experiment_fail", "tcdiscovery:ui.page_turn"
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
                    .icon(() -> new ItemStack(REGISTERED_ITEMS.getOrDefault("printing_press", REGISTERED_ITEMS.values().iterator().next()).get()))
                    .displayItems((params, output) -> ITEMS.getEntries().forEach(e -> output.accept(e.get())))
                    .build());

    private DiscoveryRegistries() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);
    }
}
