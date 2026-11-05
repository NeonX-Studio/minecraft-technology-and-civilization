package com.nightcity.discovery;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.Mod;

@Mod(TDiscovery.MOD_ID)
public final class TDiscovery {
    public static final String MOD_ID = "tcdiscovery";

    public TDiscovery() {
        NeoForge.EVENT_BUS.register(TDiscoveryEvents.class);
    }
}
