package com.nightcity.exchange.registry;

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

public final class ExchangeRegistries {
    public static final String MOD_ID = "tcexchange";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

    private static final List<String> BLOCK_PATHS = List.of(
        "barter_mat", "market_stall", "merchant_scale", "mint", "order_board", "counting_house",
        "guild_hall", "fair_banner", "trading_post", "bank_counter", "bank_vault", "exchange_board",
        "stock_ticker", "wire_counter", "forex_board", "payment_terminal", "clearing_house"
    );
    private static final List<String> BLOCK_ITEM_PATHS = List.of(
        "barter_mat", "market_stall", "merchant_scale", "mint", "order_board", "counting_house",
        "guild_hall", "fair_banner", "trading_post", "bank_counter", "bank_vault", "exchange_board",
        "stock_ticker", "wire_counter", "forex_board", "payment_terminal", "clearing_house"
    );
    private static final List<String> STANDALONE_ITEM_PATHS = List.of(
        "cowrie_shell", "salt_cake", "hacksilver", "coin_die", "bronze_coin", "silver_coin",
        "gold_coin", "coin_purse", "contract_scroll", "credit_chit", "guild_charter", "bill_of_exchange",
        "banknote", "ledger", "share_certificate", "insurance_policy", "bond", "cheque_book",
        "payment_card"
    );
    private static final List<String> SOUND_PATHS = List.of(
        "event.coin_clink", "block.mint.strike", "block.market_stall.bell"
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
                "tcexchange:barter_mat", "tcexchange:market_stall", "tcexchange:merchant_scale", "tcexchange:mint", "tcexchange:order_board", "tcexchange:counting_house",
                "tcexchange:guild_hall", "tcexchange:fair_banner", "tcexchange:trading_post", "tcexchange:bank_counter", "tcexchange:bank_vault", "tcexchange:exchange_board",
                "tcexchange:stock_ticker", "tcexchange:wire_counter", "tcexchange:forex_board", "tcexchange:payment_terminal", "tcexchange:clearing_house"
        ));
        map.put("item", List.of(
                "tcexchange:cowrie_shell", "tcexchange:salt_cake", "tcexchange:hacksilver", "tcexchange:coin_die", "tcexchange:bronze_coin", "tcexchange:silver_coin",
                "tcexchange:gold_coin", "tcexchange:coin_purse", "tcexchange:contract_scroll", "tcexchange:credit_chit", "tcexchange:guild_charter", "tcexchange:bill_of_exchange",
                "tcexchange:banknote", "tcexchange:ledger", "tcexchange:share_certificate", "tcexchange:insurance_policy", "tcexchange:bond", "tcexchange:cheque_book",
                "tcexchange:payment_card"
        ));
        map.put("block_entity", List.of(
                "tcexchange:market_stall", "tcexchange:mint", "tcexchange:order_board", "tcexchange:counting_house", "tcexchange:guild_hall", "tcexchange:trading_post",
                "tcexchange:bank_counter", "tcexchange:bank_vault", "tcexchange:exchange_board", "tcexchange:wire_counter", "tcexchange:forex_board", "tcexchange:payment_terminal",
                "tcexchange:clearing_house"
        ));
        map.put("menu", List.of(
                "tcexchange:market", "tcexchange:barter", "tcexchange:mint", "tcexchange:order_board", "tcexchange:contract_editor", "tcexchange:caravan_planner",
                "tcexchange:counting_house", "tcexchange:bank", "tcexchange:exchange_board"
        ));
        map.put("keybind", List.of(
                "tcexchange:open_market"
        ));
        map.put("payload", List.of(
                "tcexchange:ticker_sync", "tcexchange:trade_offer", "tcexchange:trade_result", "tcexchange:order_place", "tcexchange:order_cancel", "tcexchange:contract_edit",
                "tcexchange:contract_sign", "tcexchange:caravan_plan", "tcexchange:mint_request", "tcexchange:bank_action"
        ));
        map.put("hud_widget", List.of(
                "tcexchange:market_ticker"
        ));
        map.put("civ_tab", List.of(
                "tcexchange:market"
        ));
        map.put("region_layer", List.of(
                "tcexchange:market"
        ));
        map.put("sound", List.of(
                "tcexchange:event.coin_clink", "tcexchange:block.mint.strike", "tcexchange:block.market_stall.bell"
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
                    .icon(() -> new ItemStack(REGISTERED_ITEMS.getOrDefault("market_stall", REGISTERED_ITEMS.values().iterator().next()).get()))
                    .displayItems((params, output) -> ITEMS.getEntries().forEach(e -> output.accept(e.get())))
                    .build());

    private ExchangeRegistries() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);
    }
}
