package com.nightcity.tcivilization.api;

import java.util.Objects;

/**
 * Capability token restricting Civilization.eraIndex writes to tctechnology (03 §12.1, §12.5).
 */
public final class EraWriteToken {
    private static volatile String claimedOwner;
    private static final EraWriteToken INSTANCE = new EraWriteToken();

    private EraWriteToken() {
    }

    public static synchronized EraWriteToken acquire(String modId) {
        Objects.requireNonNull(modId, "modId");
        if (!"tctechnology".equals(modId)) {
            throw new SecurityException("Only 'tctechnology' may acquire EraWriteToken, got: " + modId);
        }
        if (claimedOwner != null && !claimedOwner.equals(modId) && TcThreads.isStrictMode()) {
            throw new IllegalStateException("EraWriteToken already claimed by " + claimedOwner);
        }
        claimedOwner = modId;
        return INSTANCE;
    }

    public static void resetForTests() {
        claimedOwner = null;
    }
}
