package com.nightcity.echoes.registry;

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

public final class EchoesRegistries {
    public static final String MOD_ID = "tcechoes";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

    private static final List<String> BLOCK_PATHS = List.of(
        "cultural_layer", "artefact_deposit", "midden_soil", "ancient_slag", "rock_art", "ancient_tablet",
        "frozen_remains", "relic_shrine", "epigraphy_table", "stele", "conservation_bench"
    );
    private static final List<String> BLOCK_ITEM_PATHS = List.of(
        "cultural_layer", "artefact_deposit", "midden_soil", "ancient_slag", "rock_art", "ancient_tablet",
        "frozen_remains", "relic_shrine", "epigraphy_table", "stele", "conservation_bench"
    );
    private static final List<String> STANDALONE_ITEM_PATHS = List.of(
        "flint_scraper", "bone_harpoon", "carved_figurine", "painted_pottery", "ancient_tool", "votive_offering",
        "seal_stone", "glass_beads", "ancient_blade", "inscribed_fragment", "bronze_ritual_vessel", "funerary_mask",
        "coin_hoard", "scroll_case", "astrolabe_fragment", "royal_regalia", "rubbing", "ancient_cargo_bundle",
        "captains_log", "ancient_seed_jar", "tomb_key", "chronicle_book", "ancient_chart"
    );
    private static final List<String> SOUND_PATHS = List.of(
        "event.legend_revealed", "item.rubbing.take", "block.relic_shrine.place"
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
                "tcechoes:cultural_layer", "tcechoes:artefact_deposit", "tcechoes:midden_soil", "tcechoes:ancient_slag", "tcechoes:rock_art", "tcechoes:ancient_tablet",
                "tcechoes:frozen_remains", "tcechoes:relic_shrine", "tcechoes:epigraphy_table", "tcechoes:stele", "tcechoes:conservation_bench"
        ));
        map.put("item", List.of(
                "tcechoes:flint_scraper", "tcechoes:bone_harpoon", "tcechoes:carved_figurine", "tcechoes:painted_pottery", "tcechoes:ancient_tool", "tcechoes:votive_offering",
                "tcechoes:seal_stone", "tcechoes:glass_beads", "tcechoes:ancient_blade", "tcechoes:inscribed_fragment", "tcechoes:bronze_ritual_vessel", "tcechoes:funerary_mask",
                "tcechoes:coin_hoard", "tcechoes:scroll_case", "tcechoes:astrolabe_fragment", "tcechoes:royal_regalia", "tcechoes:rubbing", "tcechoes:ancient_cargo_bundle",
                "tcechoes:captains_log", "tcechoes:ancient_seed_jar", "tcechoes:tomb_key", "tcechoes:chronicle_book", "tcechoes:ancient_chart"
        ));
        map.put("structure", List.of(
                "tcechoes:camp_remains", "tcechoes:rock_shelter", "tcechoes:frozen_camp", "tcechoes:megalith", "tcechoes:tell", "tcechoes:village_ruin",
                "tcechoes:city_ruin", "tcechoes:waystation_ruin", "tcechoes:barrow", "tcechoes:necropolis", "tcechoes:temple_ruin", "tcechoes:terrace_temple",
                "tcechoes:cult_sanctum", "tcechoes:fortress_ruin", "tcechoes:battlefield", "tcechoes:ancient_mine", "tcechoes:ancient_quarry", "tcechoes:furnace_site",
                "tcechoes:ancient_road", "tcechoes:ancient_harbor", "tcechoes:ancient_wreck", "tcechoes:aqueduct_ruin", "tcechoes:observatory_ruin", "tcechoes:library_ruin",
                "tcechoes:sunken_city"
        ));
        map.put("block_entity", List.of(
                "tcechoes:ancient_tablet", "tcechoes:relic_shrine", "tcechoes:epigraphy_table", "tcechoes:stele", "tcechoes:conservation_bench"
        ));
        map.put("menu", List.of(
                "tcechoes:epigraphy_table", "tcechoes:conservation_bench", "tcechoes:stele_inscription"
        ));
        map.put("keybind", List.of(
                "tcechoes:journal"
        ));
        map.put("payload", List.of(
                "tcechoes:history_progress", "tcechoes:history_preview", "tcechoes:legend_sync", "tcechoes:site_dossier", "tcechoes:appraise_request", "tcechoes:appraisal_result",
                "tcechoes:script_sync", "tcechoes:heritage_edit", "tcechoes:chronicle_compile", "tcechoes:stele_text"
        ));
        map.put("hud_widget", List.of(
                "tcechoes:clue_compass"
        ));
        map.put("civ_tab", List.of(
                "tcechoes:legends"
        ));
        map.put("region_layer", List.of(
                "tcechoes:history"
        ));
        map.put("sound", List.of(
                "tcechoes:event.legend_revealed", "tcechoes:item.rubbing.take", "tcechoes:block.relic_shrine.place"
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
                    .icon(() -> new ItemStack(REGISTERED_ITEMS.getOrDefault("chronicle_book", REGISTERED_ITEMS.values().iterator().next()).get()))
                    .displayItems((params, output) -> ITEMS.getEntries().forEach(e -> output.accept(e.get())))
                    .build());

    private EchoesRegistries() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);
    }
}
