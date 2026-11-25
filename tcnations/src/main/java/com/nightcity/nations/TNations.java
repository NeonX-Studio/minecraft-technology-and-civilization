package com.nightcity.nations;

import com.nightcity.nations.compat.NationsCompat;
import com.nightcity.nations.data.NationsInfluenceLayer;
import com.nightcity.nations.data.NationsRegionLayers;
import com.nightcity.nations.data.NationsSavedData;
import com.nightcity.nations.registry.NationsRegistries;
import com.nightcity.nations.vanilla.NationsVanillaGuard;
import com.nightcity.tcivilization.api.event.RegisterRegionLayersEvent;
import com.nightcity.tcivilization.region.RegionLayerRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TNations.MOD_ID)
public final class TNations {
    public static final String MOD_ID = "tcnations";
    public static final NationsRegionLayers.RegionLayerDef<NationsRegionLayers.EmptyLayerData> INFLUENCE_LAYER = NationsRegionLayers.INFLUENCE_LAYER;

    public TNations(IEventBus modBus, ModContainer modContainer) {
        NationsSavedData.registerDefaultMigrators();
        registerInfluenceLayerSafely();
        modContainer.registerConfig(ModConfig.Type.SYNCED, NationsConfig.SERVER_SPEC, "tcnations-server.toml");
        NationsRegistries.register(modBus);
        modBus.addListener(this::onRegisterRegionLayers);
        modBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.register(TNationsEvents.class);
    }

    private void registerInfluenceLayerSafely() {
        try {
            if (RegionLayerRegistry.global().get(NationsInfluenceLayer.ID).isEmpty()) {
                RegionLayerRegistry.global().register(NationsInfluenceLayer.INSTANCE);
            }
        } catch (IllegalStateException ignored) {
        }
    }

    private void onRegisterRegionLayers(RegisterRegionLayersEvent event) {
        if (!event.registeredLayers().containsKey(NationsInfluenceLayer.ID)) {
            event.register(NationsInfluenceLayer.INSTANCE);
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NationsCompat.initAll();
            NationsVanillaGuard.registerPolicyFallbacks();
        });
    }
}
