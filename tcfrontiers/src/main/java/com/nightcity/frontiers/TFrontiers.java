package com.nightcity.frontiers;

import com.nightcity.frontiers.compat.FrontiersCompat;
import com.nightcity.frontiers.data.FrontiersRegionLayers;
import com.nightcity.frontiers.registry.FrontiersRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TFrontiers.MOD_ID)
public final class TFrontiers {
    public static final String MOD_ID = "tcfrontiers";
    public static final FrontiersRegionLayers.RegionLayerDef<FrontiersRegionLayers.EmptyLayerData> DANGER_LAYER = FrontiersRegionLayers.DANGER_LAYER;

    public TFrontiers(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SYNCED, FrontiersConfig.SERVER_SPEC, "tcfrontiers-server.toml");
        FrontiersRegistries.register(modBus);
        modBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.register(TFrontiersEvents.class);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(FrontiersCompat::initAll);
    }
}
