package com.nightcity.echoes.compat.tcorbit;

public final class TcorbitBridge {
    private static volatile boolean initialized;

    private TcorbitBridge() {
    }

    public static void init() {
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
