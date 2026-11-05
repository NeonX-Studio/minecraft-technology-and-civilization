package com.nightcity.industry;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.Mod;

@Mod(TIndustry.MOD_ID)
public final class TIndustry {
    public static final String MOD_ID = "tcindustry";

    public TIndustry() {
        NeoForge.EVENT_BUS.register(TIndustryEvents.class);
    }
}
