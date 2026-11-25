package com.nightcity.ecology.compat.tcnations;

public final class TcnationsBridge {
    private static volatile boolean initialized;

    private TcnationsBridge() {
    }

    public static void init() {
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
