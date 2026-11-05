package com.nightcity.settlements;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.Mod;

@Mod(TSettlements.MOD_ID)
public final class TSettlements {
    public static final String MOD_ID = "tcsettlements";

    public TSettlements() {
        NeoForge.EVENT_BUS.register(TSettlementsEvents.class);
    }
}
