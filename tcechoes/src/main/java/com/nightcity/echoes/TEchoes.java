package com.nightcity.echoes;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.Mod;

@Mod(TEchoes.MOD_ID)
public final class TEchoes {
    public static final String MOD_ID = "tcechoes";

    public TEchoes() {
        NeoForge.EVENT_BUS.register(TEchoesEvents.class);
    }
}
