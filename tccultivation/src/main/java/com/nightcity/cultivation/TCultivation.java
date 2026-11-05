package com.nightcity.cultivation;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.Mod;

@Mod(TCultivation.MOD_ID)
public final class TCultivation {
    public static final String MOD_ID = "tccultivation";

    public TCultivation() {
        NeoForge.EVENT_BUS.register(TCultivationEvents.class);
    }
}
