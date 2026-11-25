package com.nightcity.wildlands;

import com.nightcity.wildlands.compat.WildlandsCompat;
import com.nightcity.wildlands.data.WildlandsRegionLayers;
import com.nightcity.wildlands.registry.WildlandsRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TWildlands.MOD_ID)
public final class TWildlands {
    public static final String MOD_ID = "tcwildlands";
    public static final WildlandsRegionLayers.RegionLayerDef<WildlandsRegionLayers.EmptyLayerData> GEO_LAYER = WildlandsRegionLayers.GEO_LAYER;
    public static final WildlandsRegionLayers.RegionLayerDef<WildlandsRegionLayers.EmptyLayerData> CLIMATE_LAYER = WildlandsRegionLayers.CLIMATE_LAYER;
    public static final WildlandsRegionLayers.RegionLayerDef<WildlandsRegionLayers.EmptyLayerData> HYDRO_LAYER = WildlandsRegionLayers.HYDRO_LAYER;

    public TWildlands(IEventBus modBus, ModContainer modContainer) {
        WildlandsRegionLayers.ensureCoreLayersRegistered();
        modContainer.registerConfig(ModConfig.Type.LOCAL, WildlandsConfig.COMMON_SPEC, "tcwildlands-common.toml");
        modContainer.registerConfig(ModConfig.Type.SYNCED, WildlandsConfig.SERVER_SPEC, "tcwildlands-server.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, WildlandsConfig.CLIENT_SPEC, "tcwildlands-client.toml");
        WildlandsRegistries.register(modBus);
        com.nightcity.wildlands.registry.WildlandsWorldgenCodecs.register(modBus);
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(WildlandsRegionLayers::registerCoreLayers);
        modBus.addListener(com.nightcity.wildlands.network.WildlandsPayloads::register);
        if (net.neoforged.fml.loading.FMLEnvironment.getDist().isClient()) {
            modBus.addListener(com.nightcity.wildlands.client.WildlandsClient::onClientSetup);
        }
        NeoForge.EVENT_BUS.register(TWildlandsEvents.class);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            com.nightcity.wildlands.compat.tcivilization.TcivilizationBridge.init();
            WildlandsCompat.initAll();
        });
    }
}
