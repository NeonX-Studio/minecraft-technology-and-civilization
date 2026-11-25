package com.nightcity.discovery;

import com.nightcity.discovery.compat.DiscoveryCompat;
import com.nightcity.discovery.data.DiscoveryRegionLayers;
import com.nightcity.discovery.registry.DiscoveryRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TDiscovery.MOD_ID)
public final class TDiscovery {
    public static final String MOD_ID = "tcdiscovery";
    public static final DiscoveryRegionLayers.RegionLayerDef<DiscoveryRegionLayers.EmptyLayerData> FIELD_RECORD_LAYER = DiscoveryRegionLayers.FIELD_RECORD_LAYER;

    public TDiscovery(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SYNCED, DiscoveryConfig.SERVER_SPEC, "tcdiscovery-server.toml");
        DiscoveryRegistries.register(modBus);
        modBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.register(TDiscoveryEvents.class);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(DiscoveryCompat::initAll);
    }
}
