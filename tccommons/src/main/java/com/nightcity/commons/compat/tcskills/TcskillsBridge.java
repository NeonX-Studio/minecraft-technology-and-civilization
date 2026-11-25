package com.nightcity.commons.compat.tcskills;

public final class TcskillsBridge {
    private static volatile boolean initialized;

    private TcskillsBridge() {
    }

    public static void init() {
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
