package com.nightcity.commons;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.Mod;

@Mod(TCommons.MOD_ID)
public final class TCommons {
    public static final String MOD_ID = "tccommons";

    public TCommons() {
        NeoForge.EVENT_BUS.register(TCommonsEvents.class);
    }
}
