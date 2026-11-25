package com.nightcity.industry.compat.tcexchange;

public final class TcexchangeBridge {
    private static volatile boolean initialized;

    private TcexchangeBridge() {
    }

    public static void init() {
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
