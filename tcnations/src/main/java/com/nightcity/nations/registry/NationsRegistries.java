package com.nightcity.nations.registry;

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

public final class NationsRegistries {
    public static final String MOD_ID = "tcnations";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

    private static final List<String> BLOCK_PATHS = List.of(
        "treaty_lectern", "border_stone", "war_table", "embassy_desk", "tribute_chest", "seat_of_power",
        "ai_settlement_core"
    );
    private static final List<String> BLOCK_ITEM_PATHS = List.of(
        "treaty_lectern", "border_stone", "war_table", "embassy_desk", "tribute_chest", "seat_of_power",
        "ai_settlement_core"
    );
    private static final List<String> STANDALONE_ITEM_PATHS = List.of(
        "gift_bundle", "envoy_seal", "treaty_scroll", "white_flag", "regiment_banner", "war_declaration"
    );
    private static final List<String> SOUND_PATHS = List.of(
        "event.first_contact", "event.envoy_horn", "event.treaty_seal", "event.war_declared", "event.peace_signed"
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
                "tcnations:treaty_lectern", "tcnations:border_stone", "tcnations:war_table", "tcnations:embassy_desk", "tcnations:tribute_chest", "tcnations:seat_of_power",
                "tcnations:ai_settlement_core"
        ));
        map.put("item", List.of(
                "tcnations:gift_bundle", "tcnations:envoy_seal", "tcnations:treaty_scroll", "tcnations:white_flag", "tcnations:regiment_banner", "tcnations:war_declaration"
        ));
        map.put("structure", List.of(
                "tcnations:ai_camp", "tcnations:ai_village", "tcnations:ai_town", "tcnations:ai_city", "tcnations:ai_castle", "tcnations:ai_frontier_fort",
                "tcnations:ai_colony", "tcnations:ai_metropolis", "tcnations:embassy_compound"
        ));
        map.put("block_entity", List.of(
                "tcnations:treaty_lectern", "tcnations:war_table", "tcnations:embassy_desk", "tcnations:tribute_chest", "tcnations:ai_settlement_core"
        ));
        map.put("menu", List.of(
                "tcnations:treaty_lectern", "tcnations:war_table", "tcnations:embassy", "tcnations:tribute_chest"
        ));
        map.put("keybind", List.of(
                "tcnations:diplomacy"
        ));
        map.put("payload", List.of(
                "tcnations:diplomacy_snapshot", "tcnations:relation_update", "tcnations:proposal_submit", "tcnations:proposal_response", "tcnations:treaty_sync", "tcnations:war_state_sync",
                "tcnations:army_order", "tcnations:army_snapshot", "tcnations:border_overlay", "tcnations:vote_cast"
        ));
        map.put("hud_widget", List.of(
                "tcnations:war_status", "tcnations:territory_entry"
        ));
        map.put("civ_tab", List.of(
                "tcnations:diplomacy"
        ));
        map.put("region_layer", List.of(
                "tcnations:influence"
        ));
        map.put("sound", List.of(
                "tcnations:event.first_contact", "tcnations:event.envoy_horn", "tcnations:event.treaty_seal", "tcnations:event.war_declared", "tcnations:event.peace_signed"
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
                    .icon(() -> new ItemStack(REGISTERED_ITEMS.getOrDefault("treaty_scroll", REGISTERED_ITEMS.values().iterator().next()).get()))
                    .displayItems((params, output) -> ITEMS.getEntries().forEach(e -> output.accept(e.get())))
                    .build());

    private NationsRegistries() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);
    }
}
