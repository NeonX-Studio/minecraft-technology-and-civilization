package com.nightcity.nations.compat.tcindustry;

public final class TcindustryBridge {
    private static volatile boolean initialized;

    private TcindustryBridge() {
    }

    public static void init() {
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
