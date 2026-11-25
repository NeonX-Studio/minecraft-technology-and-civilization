package com.nightcity.atlas.map;

/**
 * Personal SL2 memory half-life decay, cairn anchoring, charcoal blur, and forgetting threshold (22 §5.3).
 */
public final class MemoryDecayModel {
    public static final long GAME_SECONDS_PER_MC_DAY = 1200L; // 24,000 ticks / 20 TPS
    public static final double NORMAL_STRENGTH_THRESHOLD = 0.25;
    public static final double FORGET_STRENGTH_THRESHOLD = 0.05;
    public static final double CAIRN_ANCHOR_MULTIPLIER = 4.0;
    public static final int CAIRN_ANCHOR_RADIUS_BLOCKS = 48;

    private MemoryDecayModel() {
    }

    public static double baseHalfLifeDays(int tier) {
        if (tier <= 0) {
            return 3.0;
        }
        if (tier == 1) {
            return 6.0;
        }
        return 12.0;
    }

    public static double halfLifeDays(int tier, int explorationSkillLevel, boolean nearCairn) {
        double base = baseHalfLifeDays(tier);
        double skillFactor = 1.0 + 0.02 * Math.clamp(explorationSkillLevel, 0, 25);
        double anchor = nearCairn ? CAIRN_ANCHOR_MULTIPLIER : 1.0;
        return base * skillFactor * anchor;
    }

    /**
     * Computes memory retention m(t) in [0, 1] (22 §5.3).
     */
    public static double strength(
            long seenGameSeconds,
            long nowGameSeconds,
            int tier,
            int explorationSkillLevel,
            boolean nearCairn,
            boolean memoryDecayEnabled,
            double decaySpeedMultiplier
    ) {
        if (!memoryDecayEnabled || decaySpeedMultiplier <= 0.0 || nowGameSeconds <= seenGameSeconds) {
            return 1.0;
        }
        double elapsedSeconds = (double) (nowGameSeconds - seenGameSeconds) * decaySpeedMultiplier;
        double halfLifeSeconds = halfLifeDays(tier, explorationSkillLevel, nearCairn) * GAME_SECONDS_PER_MC_DAY;
        if (halfLifeSeconds <= 0.0) {
            return 0.0;
        }
        return Math.pow(0.5, elapsedSeconds / halfLifeSeconds);
    }

    public static boolean isForgotten(double strength) {
        return strength < FORGET_STRENGTH_THRESHOLD;
    }

    /**
     * Charcoal blur radius in pixels: 4 * (1 - m) for m >= 0.25, capped at 3.8px when fading.
     */
    public static double blurRadiusPixels(double strength) {
        if (strength < FORGET_STRENGTH_THRESHOLD) {
            return 4.0;
        }
        double clamped = Math.clamp(strength, 0.05, 1.0);
        return 4.0 * (1.0 - clamped);
    }

    /**
     * Opacity factor: 1.0 for m >= 0.25; m / 0.25 for 0.05 <= m < 0.25; 0.0 when forgotten.
     */
    public static float opacity(double strength) {
        if (strength < FORGET_STRENGTH_THRESHOLD) {
            return 0.0f;
        }
        if (strength >= NORMAL_STRENGTH_THRESHOLD) {
            return 1.0f;
        }
        return (float) (strength / NORMAL_STRENGTH_THRESHOLD);
    }
}
