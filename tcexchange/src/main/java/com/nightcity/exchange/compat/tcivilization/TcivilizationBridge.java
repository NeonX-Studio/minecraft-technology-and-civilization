package com.nightcity.exchange.compat.tcivilization;

public final class TcivilizationBridge {
    private static volatile boolean initialized;

    private TcivilizationBridge() {
    }

    public static void init() {
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
