package com.nightcity.atlas;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AtlasRegistries {
    public static final DeferredRegister.Blocks BLOCKS = com.nightcity.atlas.registry.AtlasRegistries.BLOCKS;
    public static final DeferredRegister.Items ITEMS = com.nightcity.atlas.registry.AtlasRegistries.ITEMS;
    public static final DeferredRegister<SoundEvent> SOUNDS = com.nightcity.atlas.registry.AtlasRegistries.SOUNDS;
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = com.nightcity.atlas.registry.AtlasRegistries.CREATIVE_MODE_TABS;

    private AtlasRegistries() {
    }

    public static void register(IEventBus modBus) {
        com.nightcity.atlas.registry.AtlasRegistries.register(modBus);
    }
}
