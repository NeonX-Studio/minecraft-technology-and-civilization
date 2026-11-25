package com.nightcity.beyond;

import com.nightcity.beyond.compat.BeyondCompat;
import com.nightcity.beyond.data.BeyondRegionLayers;
import com.nightcity.beyond.registry.BeyondRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TBeyond.MOD_ID)
public final class TBeyond {
    public static final String MOD_ID = "tcbeyond";
    public static final BeyondRegionLayers.RegionLayerDef<BeyondRegionLayers.EmptyLayerData> ANOMALY_LAYER = BeyondRegionLayers.ANOMALY_LAYER;

    public TBeyond(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SYNCED, BeyondConfig.SERVER_SPEC, "tcbeyond-server.toml");
        BeyondRegistries.register(modBus);
        modBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.register(TBeyondEvents.class);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(BeyondCompat::initAll);
    }
}
