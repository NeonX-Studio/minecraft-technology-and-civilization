package com.nightcity.tcivilization;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TCivilization.MOD_ID)
public final class TCivilization {
    public static final String MOD_ID = "tcivilization";

    public TCivilization(IEventBus modBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(new CivilizationEvents());
    }
}
