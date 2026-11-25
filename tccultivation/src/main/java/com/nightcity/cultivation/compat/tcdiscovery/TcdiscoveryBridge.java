package com.nightcity.cultivation.compat.tcdiscovery;

public final class TcdiscoveryBridge {
    private static volatile boolean initialized;

    private TcdiscoveryBridge() {
    }

    public static void init() {
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
