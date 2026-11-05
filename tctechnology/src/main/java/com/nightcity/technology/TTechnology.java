package com.nightcity.technology;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.Mod;

@Mod(TTechnology.MOD_ID)
public final class TTechnology {
    public static final String MOD_ID = "tctechnology";

    public TTechnology() {
        NeoForge.EVENT_BUS.register(TTechnologyEvents.class);
    }
}
