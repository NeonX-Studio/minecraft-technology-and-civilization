package com.nightcity.tcivilization.client.api;

import com.nightcity.tcivilization.api.CeremonySpec;
import org.jspecify.annotations.Nullable;

/**
 * L4 client ceremony overlay state (03 §5.10.5, §12.2).
 */
public final class CeremonyOverlay {
    @Nullable
    private static volatile CeremonySpec activeSpec;
    private static volatile int remainingTicks;

    private CeremonyOverlay() {
    }

    public static void show(CeremonySpec spec) {
        if (spec != null) {
            activeSpec = spec;
            remainingTicks = 120;
        }
    }

    @Nullable
    public static CeremonySpec active() {
        return activeSpec;
    }

    public static void tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
            if (remainingTicks == 0) {
                activeSpec = null;
            }
        }
    }
}
