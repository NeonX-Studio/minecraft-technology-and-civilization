package com.nightcity.nations.compat.tccommons;

public final class TccommonsBridge {
    private static volatile boolean initialized;

    private TccommonsBridge() {
    }

    public static void init() {
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
