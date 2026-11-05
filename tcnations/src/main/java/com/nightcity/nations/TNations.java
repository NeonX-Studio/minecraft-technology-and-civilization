package com.nightcity.nations;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TNations.MOD_ID)
public final class TNations {
    public static final String MOD_ID = "tcnations";

    public TNations() {
        NeoForge.EVENT_BUS.register(TNationsEvents.class);
    }
}
