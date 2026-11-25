package com.nightcity.commons.registry;

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

public final class CommonsRegistries {
    public static final String MOD_ID = "tccommons";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

    private static final List<String> BLOCK_PATHS = List.of(
        "claim_post", "zone_sign", "council_seat", "ballot_box", "works_marker", "donation_box",
        "works_scaffold", "covenant_chest", "council_table", "law_stele", "notice_board", "treasury_chest",
        "courthouse_bench", "polling_booth", "civic_terminal"
    );
    private static final List<String> BLOCK_ITEM_PATHS = List.of(
        "claim_post", "zone_sign", "council_seat", "ballot_box", "works_marker", "donation_box",
        "works_scaffold", "covenant_chest", "council_table", "law_stele", "notice_board", "treasury_chest",
        "courthouse_bench", "polling_booth", "civic_terminal"
    );
    private static final List<String> STANDALONE_ITEM_PATHS = List.of(
        "survey_cord", "contract_scroll", "law_code", "land_deed", "town_charter", "colony_charter"
    );
    private static final List<String> SOUND_PATHS = List.of(
        "event.vote.open", "event.vote.passed", "event.vote.failed", "block.stele.engrave", "event.works_complete"
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
                "tccommons:claim_post", "tccommons:zone_sign", "tccommons:council_seat", "tccommons:ballot_box", "tccommons:works_marker", "tccommons:donation_box",
                "tccommons:works_scaffold", "tccommons:covenant_chest", "tccommons:council_table", "tccommons:law_stele", "tccommons:notice_board", "tccommons:treasury_chest",
                "tccommons:courthouse_bench", "tccommons:polling_booth", "tccommons:civic_terminal"
        ));
        map.put("item", List.of(
                "tccommons:survey_cord", "tccommons:contract_scroll", "tccommons:law_code", "tccommons:land_deed", "tccommons:town_charter", "tccommons:colony_charter"
        ));
        map.put("block_entity", List.of(
                "tccommons:ballot_box", "tccommons:works_marker", "tccommons:donation_box", "tccommons:covenant_chest", "tccommons:council_table", "tccommons:law_stele",
                "tccommons:notice_board", "tccommons:treasury_chest", "tccommons:courthouse_bench", "tccommons:polling_booth", "tccommons:civic_terminal"
        ));
        map.put("menu", List.of(
                "tccommons:donation_box", "tccommons:covenant_chest", "tccommons:council_table", "tccommons:treasury_chest"
        ));
        map.put("payload", List.of(
                "tccommons:claim_edit", "tccommons:parcel_settings", "tccommons:role_assign", "tccommons:office_assign", "tccommons:proposal_submit", "tccommons:ballot_cast",
                "tccommons:statute_decree", "tccommons:works_edit", "tccommons:covenant_edit", "tccommons:covenant_sign", "tccommons:case_rule", "tccommons:stele_dedicate",
                "tccommons:claim_sync", "tccommons:vote_update"
        ));
        map.put("hud_widget", List.of(
                "tccommons:claim_status"
        ));
        map.put("civ_tab", List.of(
                "tccommons:governance"
        ));
        map.put("region_layer", List.of(
                "tccommons:claim"
        ));
        map.put("sound", List.of(
                "tccommons:event.vote.open", "tccommons:event.vote.passed", "tccommons:event.vote.failed", "tccommons:block.stele.engrave", "tccommons:event.works_complete"
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
                    .icon(() -> new ItemStack(REGISTERED_ITEMS.getOrDefault("council_table", REGISTERED_ITEMS.values().iterator().next()).get()))
                    .displayItems((params, output) -> ITEMS.getEntries().forEach(e -> output.accept(e.get())))
                    .build());

    private CommonsRegistries() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);
    }
}
