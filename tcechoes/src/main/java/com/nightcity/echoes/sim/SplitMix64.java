package com.nightcity.echoes.sim;

import java.util.UUID;

/**
 * Deterministic SplitMix64 mixing utilities matching 18 §5.2.1.
 * historySeed = worldSeed ^ 0x4543484F4553L ("ECHOES").
 */
public final class SplitMix64 {
    public static final long ECHOES_SALT = 0x4543484F4553L;
    public static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;

    private SplitMix64() {
    }

    public static long deriveHistorySeed(long worldSeed) {
        return worldSeed ^ ECHOES_SALT;
    }

    public static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    public static long hashRegion(long regionKey) {
        return mix64(regionKey ^ 0x524547494F4EL);
    }

    public static long rand(long historySeed, int step, long regionKey, long channel) {
        long seed = historySeed ^ hashRegion(regionKey) ^ (step * GOLDEN_GAMMA) ^ mix64(channel);
        return mix64(seed);
    }

    public static double double01(long mixed) {
        return (mixed >>> 11) * 0x1.0p-53D;
    }

    public static double rand01(long historySeed, int step, long regionKey, long channel) {
        return double01(rand(historySeed, step, regionKey, channel));
    }

    public static int intRange(long mixed, int boundExclusive) {
        if (boundExclusive <= 1) {
            return 0;
        }
        return (int) Long.remainderUnsigned(mixed, boundExclusive);
    }

    public static UUID deterministicUuid(long historySeed, long domain, int id) {
        long msb = mix64(historySeed ^ domain ^ ((long) id * GOLDEN_GAMMA));
        long lsb = mix64(historySeed ^ ~domain ^ ((long) id * 0xBF58476D1CE4E5B9L));
        msb = (msb & ~0x0000_f000L) | 0x0000_4000L;
        lsb = (lsb & ~(3L << 62)) | (2L << 62);
        return new UUID(msb, lsb);
    }
}
