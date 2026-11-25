package com.nightcity.industry.compat.tcatlas;

public final class TcatlasBridge {
    private static volatile boolean initialized;

    private TcatlasBridge() {
    }

    public static void init() {
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
