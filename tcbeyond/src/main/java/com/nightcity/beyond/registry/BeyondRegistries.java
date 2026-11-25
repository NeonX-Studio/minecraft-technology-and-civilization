package com.nightcity.beyond.registry;

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

public final class BeyondRegistries {
    public static final String MOD_ID = "tcbeyond";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

    private static final List<String> BLOCK_PATHS = List.of(
        "anomalous_crystal", "glimmer_lichen", "whisper_reed", "veil_bloom", "resonant_cluster", "ember_heart",
        "rift_gate", "rift_portal", "gate_spire_core", "pact_stone", "chalk_ward", "ritual_circle",
        "ward_stone", "attunement_altar", "alembic", "verdant_charm", "waystone", "ember_crucible",
        "resonance_relay", "resonance_gate", "field_stabilizer", "anomaly_sensor", "crystal_synthesizer"
    );
    private static final List<String> BLOCK_ITEM_PATHS = List.of(
        "anomalous_crystal", "glimmer_lichen", "whisper_reed", "veil_bloom", "resonant_cluster", "ember_heart",
        "rift_gate", "rift_portal", "gate_spire_core", "pact_stone", "chalk_ward", "ritual_circle",
        "ward_stone", "attunement_altar", "alembic", "verdant_charm", "waystone", "ember_crucible",
        "resonance_relay", "resonance_gate", "field_stabilizer", "anomaly_sensor", "crystal_synthesizer"
    );
    private static final List<String> STANDALONE_ITEM_PATHS = List.of(
        "anomalous_shard", "anomalous_dust", "refined_crystal", "attuned_crystal", "stabilized_essence", "synthetic_crystal",
        "veil_essence", "twisted_produce", "ember_core", "resonant_crystal", "sigil_fragment", "warding_chalk",
        "anomaly_lens", "rift_key", "ashen_sigil", "pale_sigil", "silent_sigil", "elixir_of_sight",
        "elixir_of_warding", "elixir_of_embers", "elixir_of_hush", "verdant_tincture"
    );
    private static final List<String> SOUND_PATHS = List.of(
        "ambient.anomaly_whisper", "block.crystal.resonance", "event.surge_horn", "block.gate.hum"
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
                "tcbeyond:anomalous_crystal", "tcbeyond:glimmer_lichen", "tcbeyond:whisper_reed", "tcbeyond:veil_bloom", "tcbeyond:resonant_cluster", "tcbeyond:ember_heart",
                "tcbeyond:rift_gate", "tcbeyond:rift_portal", "tcbeyond:gate_spire_core", "tcbeyond:pact_stone", "tcbeyond:chalk_ward", "tcbeyond:ritual_circle",
                "tcbeyond:ward_stone", "tcbeyond:attunement_altar", "tcbeyond:alembic", "tcbeyond:verdant_charm", "tcbeyond:waystone", "tcbeyond:ember_crucible",
                "tcbeyond:resonance_relay", "tcbeyond:resonance_gate", "tcbeyond:field_stabilizer", "tcbeyond:anomaly_sensor", "tcbeyond:crystal_synthesizer"
        ));
        map.put("item", List.of(
                "tcbeyond:anomalous_shard", "tcbeyond:anomalous_dust", "tcbeyond:refined_crystal", "tcbeyond:attuned_crystal", "tcbeyond:stabilized_essence", "tcbeyond:synthetic_crystal",
                "tcbeyond:veil_essence", "tcbeyond:twisted_produce", "tcbeyond:ember_core", "tcbeyond:resonant_crystal", "tcbeyond:sigil_fragment", "tcbeyond:warding_chalk",
                "tcbeyond:anomaly_lens", "tcbeyond:rift_key", "tcbeyond:ashen_sigil", "tcbeyond:pale_sigil", "tcbeyond:silent_sigil", "tcbeyond:elixir_of_sight",
                "tcbeyond:elixir_of_warding", "tcbeyond:elixir_of_embers", "tcbeyond:elixir_of_hush", "tcbeyond:verdant_tincture"
        ));
        map.put("entity", List.of(
                "tcbeyond:spire_sentinel", "tcbeyond:rift_wisp", "tcbeyond:veil_stalker", "tcbeyond:crystal_mite"
        ));
        map.put("dimension", List.of(
                "tcbeyond:ashen_reach", "tcbeyond:pale_hollow", "tcbeyond:silent_deep"
        ));
        map.put("biome", List.of(
                "tcbeyond:pale_thicket", "tcbeyond:hollow_moor", "tcbeyond:mansion_wilds", "tcbeyond:raider_steppe", "tcbeyond:silent_dark", "tcbeyond:resonant_caverns",
                "tcbeyond:drowned_vault"
        ));
        map.put("structure", List.of(
                "tcbeyond:shattered_spire", "tcbeyond:starfall_crater", "tcbeyond:aurora_ring", "tcbeyond:vent_throat", "tcbeyond:rift_gate_ruin", "tcbeyond:ember_altar",
                "tcbeyond:threshold_landing", "tcbeyond:pact_circle", "tcbeyond:gate_spire", "tcbeyond:return_spire", "tcbeyond:drowned_reliquary"
        ));
        map.put("block_entity", List.of(
                "tcbeyond:rift_gate", "tcbeyond:gate_spire_core", "tcbeyond:pact_stone", "tcbeyond:ember_heart", "tcbeyond:ward_stone", "tcbeyond:ritual_circle",
                "tcbeyond:attunement_altar", "tcbeyond:alembic", "tcbeyond:waystone", "tcbeyond:ember_crucible", "tcbeyond:resonance_relay", "tcbeyond:resonance_gate",
                "tcbeyond:field_stabilizer", "tcbeyond:anomaly_sensor", "tcbeyond:crystal_synthesizer"
        ));
        map.put("menu", List.of(
                "tcbeyond:attunement_altar", "tcbeyond:alembic", "tcbeyond:waystone", "tcbeyond:gate_spire", "tcbeyond:crystal_synthesizer"
        ));
        map.put("payload", List.of(
                "tcbeyond:beyond_mode_sync", "tcbeyond:meter_sync", "tcbeyond:zone_event", "tcbeyond:rite_preview_request", "tcbeyond:rite_preview", "tcbeyond:waystone_action",
                "tcbeyond:relay_action", "tcbeyond:gate_action"
        ));
        map.put("hud_widget", List.of(
                "tcbeyond:anomaly_meter"
        ));
        map.put("civ_tab", List.of(
                "tcbeyond:anomalies"
        ));
        map.put("region_layer", List.of(
                "tcbeyond:anomaly"
        ));
        map.put("sound", List.of(
                "tcbeyond:ambient.anomaly_whisper", "tcbeyond:block.crystal.resonance", "tcbeyond:event.surge_horn", "tcbeyond:block.gate.hum"
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
                    .icon(() -> new ItemStack(REGISTERED_ITEMS.getOrDefault("rift_key", REGISTERED_ITEMS.values().iterator().next()).get()))
                    .displayItems((params, output) -> ITEMS.getEntries().forEach(e -> output.accept(e.get())))
                    .build());

    private BeyondRegistries() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);
    }
}
