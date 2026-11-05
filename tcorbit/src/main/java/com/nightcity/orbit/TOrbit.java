package com.nightcity.orbit;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TOrbit.MOD_ID)
public final class TOrbit {
    public static final String MOD_ID = "tcorbit";

    public TOrbit() {
        NeoForge.EVENT_BUS.register(TOrbitEvents.class);
    }
}
