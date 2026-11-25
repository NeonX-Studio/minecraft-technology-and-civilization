package com.nightcity.ecology;

import com.nightcity.ecology.compat.EcologyCompat;
import com.nightcity.ecology.data.EcologyEcosystemLayer;
import com.nightcity.ecology.data.EcologyRegionLayers;
import com.nightcity.ecology.registry.EcologyRegistries;
import com.nightcity.ecology.spawn.EcologySpawnManager;
import com.nightcity.tcivilization.api.event.RegisterRegionLayersEvent;
import com.nightcity.tcivilization.region.RegionLayerRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TEcology.MOD_ID)
public final class TEcology {
    public static final String MOD_ID = "tcecology";
    public static final EcologyRegionLayers.RegionLayerDef<EcologyRegionLayers.EmptyLayerData> ECOSYSTEM_LAYER = EcologyRegionLayers.ECOSYSTEM_LAYER;

    public TEcology(IEventBus modBus, ModContainer modContainer) {
        registerEcosystemLayerSafely();
        modContainer.registerConfig(ModConfig.Type.SYNCED, EcologyConfig.SERVER_SPEC, "tcecology-server.toml");
        EcologyRegistries.register(modBus);
        modBus.addListener(this::onRegisterRegionLayers);
        modBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.register(TEcologyEvents.class);
    }

    private void registerEcosystemLayerSafely() {
        try {
            if (RegionLayerRegistry.global().get(EcologyEcosystemLayer.ID).isEmpty()) {
                RegionLayerRegistry.global().register(EcologyEcosystemLayer.INSTANCE);
            }
        } catch (IllegalStateException ignored) {
        }
    }

    private void onRegisterRegionLayers(RegisterRegionLayersEvent event) {
        if (!event.registeredLayers().containsKey(EcologyEcosystemLayer.ID)) {
            event.register(EcologyEcosystemLayer.INSTANCE);
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            EcologyCompat.initAll();
            EcologySpawnManager.registerVanillaPolicies();
        });
    }
}
