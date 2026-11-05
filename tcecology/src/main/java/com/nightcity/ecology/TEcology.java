package com.nightcity.ecology;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.Mod;

@Mod(TEcology.MOD_ID)
public final class TEcology {
    public static final String MOD_ID = "tcecology";

    public TEcology() {
        NeoForge.EVENT_BUS.register(TEcologyEvents.class);
    }
}
