package com.nightcity.commons.compat.tcfrontiers;

public final class TcfrontiersBridge {
    private static volatile boolean initialized;

    private TcfrontiersBridge() {
    }

    public static void init() {
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
