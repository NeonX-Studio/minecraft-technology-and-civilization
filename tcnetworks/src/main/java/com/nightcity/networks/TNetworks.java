package com.nightcity.networks;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.Mod;

@Mod(TNetworks.MOD_ID)
public final class TNetworks {
    public static final String MOD_ID = "tcnetworks";

    public TNetworks() {
        NeoForge.EVENT_BUS.register(TNetworksEvents.class);
    }
}
