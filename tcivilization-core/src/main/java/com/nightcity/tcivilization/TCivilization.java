package com.nightcity.tcivilization;

import com.nightcity.tcivilization.api.TcApi;
import com.nightcity.tcivilization.api.event.RegisterFeatTriggersEvent;
import com.nightcity.tcivilization.api.event.RegisterMemoryTypesEvent;
import com.nightcity.tcivilization.person.PersonComponentRegistry;
import com.nightcity.tcivilization.region.RegionLayerRegistry;
import com.nightcity.tcivilization.registry.TcRegistries;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TCivilization.MOD_ID)
public final class TCivilization {
    public static final String MOD_ID = TcApi.MOD_ID;
    private static volatile IEventBus activeCoreModBus;

    public TCivilization(IEventBus modBus, ModContainer modContainer) {
        activeCoreModBus = modBus;
        CivilizationSavedData.registerDefaultMigrators();
        modContainer.registerConfig(ModConfig.Type.SYNCED, TCivilizationConfig.SPEC);
        TcRegistries.register(modBus);
        modBus.addListener((FMLCommonSetupEvent event) -> onCommonSetup(modBus));
        NeoForge.EVENT_BUS.register(new CivilizationEvents());
    }

    public static <T extends Event & IModBusEvent> void postModBusEvent(IEventBus fallbackBus, T event) {
        if (ModList.get() != null) {
            ModLoader.postEvent(event);
            if (fallbackBus != null && fallbackBus != activeCoreModBus) {
                fallbackBus.post(event);
            }
        } else if (fallbackBus != null) {
            fallbackBus.post(event);
        }
    }

    private static void onCommonSetup(IEventBus modBus) {
        RegionLayerRegistry.global().dispatchRegistration(modBus);
        PersonComponentRegistry.global().dispatchRegistration(modBus);
        postModBusEvent(modBus, new RegisterMemoryTypesEvent());
        postModBusEvent(modBus, new RegisterFeatTriggersEvent());
    }
}

