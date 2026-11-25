package com.nightcity.networks;

import com.nightcity.networks.compat.NetworksCompat;
import com.nightcity.networks.data.NetworksRegionLayers;
import com.nightcity.networks.registry.NetworksRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TNetworks.MOD_ID)
public final class TNetworks {
    public static final String MOD_ID = "tcnetworks";
    public static final NetworksRegionLayers.RegionLayerDef<NetworksRegionLayers.EmptyLayerData> TRANSPORT_LAYER = NetworksRegionLayers.TRANSPORT_LAYER;

    public TNetworks(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SYNCED, NetworksConfig.SERVER_SPEC, "tcnetworks-server.toml");
        NetworksRegistries.register(modBus);
        modBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.register(TNetworksEvents.class);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(NetworksCompat::initAll);
    }
}
