package com.nightcity.orbit.compat.tcechoes;

public final class TcechoesBridge {
    private static volatile boolean initialized;

    private TcechoesBridge() {
    }

    public static void init() {
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
