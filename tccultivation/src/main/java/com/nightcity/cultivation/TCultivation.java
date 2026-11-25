package com.nightcity.cultivation;

import com.nightcity.cultivation.compat.CultivationCompat;
import com.nightcity.cultivation.data.CultivationRegionLayers;
import com.nightcity.cultivation.registry.CultivationRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TCultivation.MOD_ID)
public final class TCultivation {
    public static final String MOD_ID = "tccultivation";
    public static final CultivationRegionLayers.RegionLayerDef<CultivationRegionLayers.EmptyLayerData> AGRI_LAYER = CultivationRegionLayers.AGRI_LAYER;

    public TCultivation(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SYNCED, CultivationConfig.SERVER_SPEC, "tccultivation-server.toml");
        CultivationRegistries.register(modBus);
        modBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.register(TCultivationEvents.class);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CultivationCompat::initAll);
    }
}
