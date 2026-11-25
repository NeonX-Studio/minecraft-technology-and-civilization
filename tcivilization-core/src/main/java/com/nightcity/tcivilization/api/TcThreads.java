package com.nightcity.tcivilization.api;

import com.mojang.logging.LogUtils;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

/**
 * Thread-safety assertions for main-thread-only core APIs.
 */
public final class TcThreads {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> LOGGED_VIOLATIONS = ConcurrentHashMap.newKeySet();
    private static volatile boolean strictMode = false;
    private static volatile Thread boundMainThread;

    private TcThreads() {
    }

    public static void setStrictMode(boolean strict) {
        strictMode = strict;
    }

    public static boolean isStrictMode() {
        return strictMode;
    }

    public static void bindMainThread(Thread thread) {
        boundMainThread = thread;
    }

    public static void assertMain() {
        Thread expected = boundMainThread;
        if (expected == null || Thread.currentThread() == expected) {
            return;
        }
        String site = Thread.currentThread().getName();
        if (strictMode) {
            throw new IllegalStateException("Expected main thread, but called from: " + site);
        }
        if (LOGGED_VIOLATIONS.add(site)) {
            LOGGER.error("Main-thread API called off-thread from '{}'", site);
        }
    }

    public static void assertMain(MinecraftServer server) {
        if (server != null && !server.isSameThread()) {
            if (strictMode) {
                throw new IllegalStateException("Expected server main thread, called from: " + Thread.currentThread().getName());
            }
            if (LOGGED_VIOLATIONS.add(Thread.currentThread().getName())) {
                LOGGER.error("Server main-thread API called off-thread from '{}'", Thread.currentThread().getName());
            }
        }
    }
}
