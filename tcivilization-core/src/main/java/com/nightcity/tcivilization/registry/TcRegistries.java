package com.nightcity.tcivilization.registry;

import com.nightcity.tcivilization.TCivilization;
import com.nightcity.tcivilization.person.PersonEntity;
import com.nightcity.tcivilization.world.LegacyItem;
import com.nightcity.tcivilization.world.PlaceholderBlock;
import com.nightcity.tcivilization.world.PlaceholderBlockEntity;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Core DeferredRegisters for blocks, block entities, items, data components, and tcivilization:person (03 §7, Appendix A).
 */
public final class TcRegistries {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, TCivilization.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TCivilization.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, TCivilization.MOD_ID);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, TCivilization.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, TCivilization.MOD_ID);

    public static final DeferredHolder<Block, PlaceholderBlock> PLACEHOLDER_BLOCK = BLOCKS.register(
            "placeholder",
            id -> new PlaceholderBlock(
                    BlockBehaviour.Properties.of()
                            .strength(1.5F, 6.0F)
                            .noLootTable()
                            .setId(ResourceKey.create(Registries.BLOCK, id))
            )
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> PLACEHOLDER_BLOCK_ENTITY = BLOCK_ENTITIES.register(
            "placeholder",
            () -> new BlockEntityType<>(PlaceholderBlockEntity::new, PLACEHOLDER_BLOCK.get())
    );

    public static final DeferredHolder<Item, BlockItem> PLACEHOLDER_BLOCK_ITEM = ITEMS.register(
            "placeholder",
            id -> new BlockItem(
                    PLACEHOLDER_BLOCK.get(),
                    new Item.Properties()
                            .useBlockDescriptionPrefix()
                            .setId(ResourceKey.create(Registries.ITEM, id))
            )
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LegacyItem.LegacyData>> LEGACY_DATA_COMPONENT = DATA_COMPONENTS.register(
            "legacy_data",
            () -> DataComponentType.<LegacyItem.LegacyData>builder()
                    .persistent(LegacyItem.LegacyData.CODEC)
                    .networkSynchronized(LegacyItem.LegacyData.STREAM_CODEC)
                    .build()
    );

    public static final DeferredHolder<Item, LegacyItem> LEGACY_ITEM = ITEMS.register(
            "legacy_item",
            id -> new LegacyItem(
                    new Item.Properties()
                            .stacksTo(64)
                            .component(LEGACY_DATA_COMPONENT.get(), LegacyItem.LegacyData.DEFAULT)
                            .setId(ResourceKey.create(Registries.ITEM, id))
            )
    );

    public static final DeferredHolder<Item, Item> WOOD_ASH = ITEMS.register(
            "wood_ash",
            id -> new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)))
    );

    public static final DeferredHolder<EntityType<?>, EntityType<PersonEntity>> PERSON_ENTITY = ENTITY_TYPES.register(
            "person",
            id -> EntityType.Builder.<PersonEntity>of(PersonEntity::new, MobCategory.MISC)
                    .sized(PersonEntity.WIDTH, PersonEntity.HEIGHT)
                    .clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
    );

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TCivilization.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + TCivilization.MOD_ID))
                    .icon(() -> new ItemStack(WOOD_ASH.get()))
                    .displayItems((params, output) -> ITEMS.getEntries().forEach(e -> output.accept(e.get())))
                    .build());

    private TcRegistries() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        DATA_COMPONENTS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);
        modBus.addListener(TcRegistries::onEntityAttributes);
    }

    private static void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(PERSON_ENTITY.get(), PersonEntity.createAttributes().build());
    }
}
