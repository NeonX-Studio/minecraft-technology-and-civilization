package com.nightcity.wildlands;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.Mod;

@Mod(TWildlands.MOD_ID)
public final class TWildlands {
    public static final String MOD_ID = "tcwildlands";

    public TWildlands() {
        NeoForge.EVENT_BUS.register(TWildlandsEvents.class);
    }
}
