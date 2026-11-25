package com.nightcity.nations.compat.tcsettlements;

public final class TcsettlementsBridge {
    private static volatile boolean initialized;

    private TcsettlementsBridge() {
    }

    public static void init() {
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
