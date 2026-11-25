package com.nightcity.orbit;

import com.nightcity.orbit.compat.OrbitCompat;
import com.nightcity.orbit.data.OrbitRegionLayers;
import com.nightcity.orbit.registry.OrbitRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TOrbit.MOD_ID)
public final class TOrbit {
    public static final String MOD_ID = "tcorbit";
    public static final OrbitRegionLayers.RegionLayerDef<OrbitRegionLayers.EmptyLayerData> REMOTE_SENSING_LAYER = OrbitRegionLayers.REMOTE_SENSING_LAYER;

    public TOrbit(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SYNCED, OrbitConfig.SERVER_SPEC, "tcorbit-server.toml");
        OrbitRegistries.register(modBus);
        modBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.register(TOrbitEvents.class);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(OrbitCompat::initAll);
    }
}
