package com.nightcity.settlements;

import com.nightcity.settlements.compat.SettlementsCompat;
import com.nightcity.settlements.data.SettlementsRegionLayers;
import com.nightcity.settlements.registry.SettlementsRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TSettlements.MOD_ID)
public final class TSettlements {
    public static final String MOD_ID = "tcsettlements";
    public static final SettlementsRegionLayers.RegionLayerDef<SettlementsRegionLayers.EmptyLayerData> DEMOGRAPHY_LAYER = SettlementsRegionLayers.DEMOGRAPHY_LAYER;

    public TSettlements(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SYNCED, SettlementsConfig.SERVER_SPEC, "tcsettlements-server.toml");
        SettlementsRegistries.register(modBus);
        modBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.register(TSettlementsEvents.class);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(SettlementsCompat::initAll);
    }
}
