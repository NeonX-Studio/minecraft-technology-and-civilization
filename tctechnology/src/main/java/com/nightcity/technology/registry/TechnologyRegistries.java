package com.nightcity.technology.registry;

import com.nightcity.technology.TTechnology;
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

public final class TechnologyRegistries {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TTechnology.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TTechnology.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, TTechnology.MOD_ID);

    private static final List<String> BLOCK_PATHS = List.of(
            "fire_pit", "storage_basket", "grinding_slab", "research_table",
            "milestone_marker", "milestone_cache", "comm_station"
    );

    private static final List<String> ITEM_PATHS = List.of(
            "hammerstone", "flint_flake", "plant_fiber", "wooden_spear", "digging_stick", "fire_drill",
            "flint_knife", "flint_scraper", "hand_axe", "flint_axe_head", "flint_spear_point",
            "tinder", "cordage", "bow_drill", "flint_axe", "flint_spear", "spear_thrower",
            "bone_harpoon", "antler_billet", "pressure_flaker", "gathering_basket",
            "microlith", "flint_arrowhead", "simple_bow", "flint_arrow",
            "stone_axe_blank", "polished_axe_head", "polished_stone_axe", "stone_adze",
            "flint_sickle", "stone_hoe", "antler_pick", "firebrand", "flatbow"
    );

    private static final List<String> SOUND_PATHS = List.of(
            "event.era_advance", "event.tech_unlocked", "ui.insight",
            "block.knapping.strike", "block.fire_pit.crackle", "item.fire_drill.spin",
            "music.era.stone", "music.era.neolithic", "music.era.bronze", "music.era.iron",
            "music.era.medieval", "music.era.renaissance", "music.era.industrial",
            "music.era.electric", "music.era.atomic", "music.era.information", "music.era.space"
    );

    public static final Map<String, DeferredBlock<Block>> REGISTERED_BLOCKS;
    public static final Map<String, DeferredItem<? extends Item>> REGISTERED_ITEMS;
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> REGISTERED_SOUNDS;

    static {
        Map<String, DeferredBlock<Block>> bMap = new LinkedHashMap<>();
        Map<String, DeferredItem<? extends Item>> iMap = new LinkedHashMap<>();
        for (String bp : BLOCK_PATHS) {
            DeferredBlock<Block> blk = BLOCKS.registerSimpleBlock(bp);
            bMap.put(bp, blk);
            DeferredItem<BlockItem> bi = ITEMS.registerSimpleBlockItem(bp, blk);
            iMap.put(bp, bi);
        }
        for (String ip : ITEM_PATHS) {
            iMap.put(ip, ITEMS.registerSimpleItem(ip));
        }
        Map<String, DeferredHolder<SoundEvent, SoundEvent>> sMap = new LinkedHashMap<>();
        for (String sp : SOUND_PATHS) {
            Identifier id = Identifier.fromNamespaceAndPath(TTechnology.MOD_ID, sp);
            sMap.put(sp, SOUNDS.register(sp, () -> SoundEvent.createVariableRangeEvent(id)));
        }
        REGISTERED_BLOCKS = Collections.unmodifiableMap(bMap);
        REGISTERED_ITEMS = Collections.unmodifiableMap(iMap);
        REGISTERED_SOUNDS = Collections.unmodifiableMap(sMap);
    }

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TTechnology.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + TTechnology.MOD_ID))
                    .icon(() -> new ItemStack(REGISTERED_ITEMS.getOrDefault("research_table", REGISTERED_ITEMS.values().iterator().next()).get()))
                    .displayItems((params, output) -> ITEMS.getEntries().forEach(e -> output.accept(e.get())))
                    .build());

    private TechnologyRegistries() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);
    }
}
