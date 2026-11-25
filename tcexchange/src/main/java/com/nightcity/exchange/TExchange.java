package com.nightcity.exchange;

import com.nightcity.exchange.compat.ExchangeCompat;
import com.nightcity.exchange.data.ExchangeRegionLayers;
import com.nightcity.exchange.registry.ExchangeRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TExchange.MOD_ID)
public final class TExchange {
    public static final String MOD_ID = "tcexchange";
    public static final ExchangeRegionLayers.RegionLayerDef<ExchangeRegionLayers.EmptyLayerData> MARKET_LAYER = ExchangeRegionLayers.MARKET_LAYER;

    public TExchange(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SYNCED, ExchangeConfig.SERVER_SPEC, "tcexchange-server.toml");
        ExchangeRegistries.register(modBus);
        modBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.register(TExchangeEvents.class);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ExchangeCompat::initAll);
    }
}
