package com.nightcity.frontiers;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.Mod;

@Mod(TFrontiers.MOD_ID)
public final class TFrontiers {
    public static final String MOD_ID = "tcfrontiers";

    public TFrontiers() {
        NeoForge.EVENT_BUS.register(TFrontiersEvents.class);
    }
}
