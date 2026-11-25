package com.nightcity.atlas.compat.tcwildlands;

public final class TcwildlandsBridge {
    private static volatile boolean initialized;

    private TcwildlandsBridge() {
    }

    public static void init() {
        com.nightcity.atlas.compat.wildlands.WildlandsTerrainBridge.init();
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
