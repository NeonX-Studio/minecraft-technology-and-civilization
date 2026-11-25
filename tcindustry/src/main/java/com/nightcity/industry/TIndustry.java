package com.nightcity.industry;

import com.nightcity.industry.compat.IndustryCompat;
import com.nightcity.industry.data.IndustryRegionLayers;
import com.nightcity.industry.registry.IndustryRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TIndustry.MOD_ID)
public final class TIndustry {
    public static final String MOD_ID = "tcindustry";
    public static final IndustryRegionLayers.RegionLayerDef<IndustryRegionLayers.EmptyLayerData> EMISSION_LAYER = IndustryRegionLayers.EMISSION_LAYER;

    public TIndustry(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SYNCED, IndustryConfig.SERVER_SPEC, "tcindustry-server.toml");
        IndustryRegistries.register(modBus);
        modBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.register(TIndustryEvents.class);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(IndustryCompat::initAll);
    }
}
