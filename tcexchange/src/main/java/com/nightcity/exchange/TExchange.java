package com.nightcity.exchange;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.Mod;

@Mod(TExchange.MOD_ID)
public final class TExchange {
    public static final String MOD_ID = "tcexchange";

    public TExchange() {
        NeoForge.EVENT_BUS.register(TExchangeEvents.class);
    }
}
