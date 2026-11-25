package com.nightcity.discovery.compat.tcwildlands;

public final class TcwildlandsBridge {
    private static volatile boolean initialized;

    private TcwildlandsBridge() {
    }

    public static void init() {
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
