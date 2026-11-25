package com.nightcity.commons.compat.tcnetworks;

public final class TcnetworksBridge {
    private static volatile boolean initialized;

    private TcnetworksBridge() {
    }

    public static void init() {
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
