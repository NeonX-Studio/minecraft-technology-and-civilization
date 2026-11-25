package com.nightcity.wildlands.compat.tcbeyond;

public final class TcbeyondBridge {
    private static volatile boolean initialized;

    private TcbeyondBridge() {
    }

    public static void init() {
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
