package com.nightcity.commons;

import com.nightcity.commons.compat.CommonsCompat;
import com.nightcity.commons.data.CommonsRegionLayers;
import com.nightcity.commons.registry.CommonsRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TCommons.MOD_ID)
public final class TCommons {
    public static final String MOD_ID = "tccommons";
    public static final CommonsRegionLayers.RegionLayerDef<CommonsRegionLayers.EmptyLayerData> CLAIM_LAYER = CommonsRegionLayers.CLAIM_LAYER;

    public TCommons(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SYNCED, CommonsConfig.SERVER_SPEC, "tccommons-server.toml");
        CommonsRegistries.register(modBus);
        modBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.register(TCommonsEvents.class);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CommonsCompat::initAll);
    }
}
