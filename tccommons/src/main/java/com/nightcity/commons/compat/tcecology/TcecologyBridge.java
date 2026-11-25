package com.nightcity.commons.compat.tcecology;

public final class TcecologyBridge {
    private static volatile boolean initialized;

    private TcecologyBridge() {
    }

    public static void init() {
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
