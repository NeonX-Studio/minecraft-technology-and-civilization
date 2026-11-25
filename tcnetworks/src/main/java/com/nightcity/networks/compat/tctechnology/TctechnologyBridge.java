package com.nightcity.networks.compat.tctechnology;

public final class TctechnologyBridge {
    private static volatile boolean initialized;

    private TctechnologyBridge() {
    }

    public static void init() {
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
