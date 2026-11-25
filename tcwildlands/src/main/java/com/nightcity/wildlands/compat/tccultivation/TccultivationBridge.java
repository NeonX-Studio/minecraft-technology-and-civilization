package com.nightcity.wildlands.compat.tccultivation;

public final class TccultivationBridge {
    private static volatile boolean initialized;

    private TccultivationBridge() {
    }

    public static void init() {
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
