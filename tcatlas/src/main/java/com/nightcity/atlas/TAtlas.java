package com.nightcity.atlas;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TAtlas.MOD_ID)
public final class TAtlas {
    public static final String MOD_ID = "tcatlas";

    public TAtlas() {
        NeoForge.EVENT_BUS.register(TAtlasEvents.class);
    }
}
