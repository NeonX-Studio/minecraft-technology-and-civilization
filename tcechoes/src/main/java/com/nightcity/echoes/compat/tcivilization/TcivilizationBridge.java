package com.nightcity.echoes.compat.tcivilization;

import com.nightcity.echoes.TEchoes;
import com.nightcity.echoes.data.EchoesRegionLayers;
import com.nightcity.tcivilization.api.event.RegisterMemoryTypesEvent;
import com.nightcity.tcivilization.api.event.RegisterRegionLayersEvent;
import com.nightcity.tcivilization.region.RegionLayerRegistry;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;

/**
 * Core tcivilization integration registering tcechoes:history RegionLayer and memory types (18 §3.1, §12.1).
 */
public final class TcivilizationBridge {
    private static volatile boolean initialized;

    private TcivilizationBridge() {
    }

    public static void registerModBusHooks(IEventBus modBus) {
        ensureRegionLayerRegistered();
        modBus.addListener(TcivilizationBridge::onRegisterRegionLayers);
        modBus.addListener(TcivilizationBridge::onRegisterMemoryTypes);
    }

    public static void ensureRegionLayerRegistered() {
        RegionLayerRegistry reg = RegionLayerRegistry.global();
        if (reg.get(EchoesRegionLayers.HISTORY_LAYER_ID).isEmpty()) {
            try {
                reg.register(EchoesRegionLayers.HISTORY_LAYER);
            } catch (IllegalStateException ignored) {
                // Registry already frozen in an isolated test environment.
            }
        }
    }

    private static void onRegisterRegionLayers(RegisterRegionLayersEvent event) {
        if (!event.registeredLayers().containsKey(EchoesRegionLayers.HISTORY_LAYER_ID)) {
            event.register(EchoesRegionLayers.HISTORY_LAYER);
        }
    }

    private static void onRegisterMemoryTypes(RegisterMemoryTypesEvent event) {
        Identifier defaultIcon = Identifier.fromNamespaceAndPath(TEchoes.MOD_ID, "chronicle_book");
        registerMem(event, "ruin_found", 3, defaultIcon);
        registerMem(event, "legend_revealed", 5, defaultIcon);
        registerMem(event, "artefact_deciphered", 4, defaultIcon);
        registerMem(event, "heritage_designated", 4, defaultIcon);
        registerMem(event, "stele_raised", 3, defaultIcon);
    }

    private static void registerMem(RegisterMemoryTypesEvent event, String path, int importance, Identifier icon) {
        Identifier id = Identifier.fromNamespaceAndPath(TEchoes.MOD_ID, path);
        if (!event.registeredTypes().containsKey(id)) {
            event.register(new RegisterMemoryTypesEvent.MemoryTypeSpec(id, importance, icon));
        }
    }

    public static void init() {
        ensureRegionLayerRegistered();
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
