package com.nightcity.beyond;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.Mod;

@Mod(TBeyond.MOD_ID)
public final class TBeyond {
    public static final String MOD_ID = "tcbeyond";

    public TBeyond() {
        NeoForge.EVENT_BUS.register(TBeyondEvents.class);
    }
}
