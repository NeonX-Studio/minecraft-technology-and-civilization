package com.nightcity.skills.data;

public final class SkillCurves {
    public static final int MAX_NON_SPECIALTY_LEVEL = 40;
    public static final int MAX_SPECIALTY_LEVEL = 50;
    public static final int MAX_GROWTH_LEVEL = 100;

    private static final long[] CUMULATIVE_SKILL_XP = new long[MAX_SPECIALTY_LEVEL + 1];
    private static final int[] XP_TO_NEXT = new int[MAX_SPECIALTY_LEVEL];

    static {
        long sum = 0L;
        CUMULATIVE_SKILL_XP[0] = 0L;
        for (int level = 0; level < MAX_SPECIALTY_LEVEL; level++) {
            double raw = 15.0 * Math.pow(level + 1, 1.5);
            int rounded5 = (int) (Math.round(raw / 5.0) * 5L);
            XP_TO_NEXT[level] = rounded5;
            sum += rounded5;
            CUMULATIVE_SKILL_XP[level + 1] = sum;
        }
    }

    private SkillCurves() {
    }

    public static int xpToNext(int level) {
        if (level < 0 || level >= MAX_SPECIALTY_LEVEL) {
            return 0;
        }
        return XP_TO_NEXT[level];
    }

    public static long cumulativeSkillXp(int level) {
        int clamped = Math.clamp(level, 0, MAX_SPECIALTY_LEVEL);
        return CUMULATIVE_SKILL_XP[clamped];
    }

    public static long maxTotalSkillXp() {
        return CUMULATIVE_SKILL_XP[MAX_SPECIALTY_LEVEL];
    }

    public static int levelForXp(long xp, boolean isSpecialty) {
        int cap = isSpecialty ? MAX_SPECIALTY_LEVEL : MAX_NON_SPECIALTY_LEVEL;
        int lvl = 0;
        for (int i = 1; i <= cap; i++) {
            if (xp >= CUMULATIVE_SKILL_XP[i]) {
                lvl = i;
            } else {
                break;
            }
        }
        return lvl;
    }

    public static long reserveXp(long xp, boolean isSpecialty) {
        if (isSpecialty) {
            return 0L;
        }
        long capXp = CUMULATIVE_SKILL_XP[MAX_NON_SPECIALTY_LEVEL];
        return Math.max(0L, Math.min(xp, maxTotalSkillXp()) - capXp);
    }

    public static long cumulativeGrowthXp(int growthLevel) {
        int g = Math.clamp(growthLevel, 1, MAX_GROWTH_LEVEL);
        return 50L * (g - 1L) * (g + 10L);
    }

    public static int growthCost(int targetGrowthLevel) {
        if (targetGrowthLevel <= 1 || targetGrowthLevel > MAX_GROWTH_LEVEL) {
            return 0;
        }
        return 500 + 100 * (targetGrowthLevel - 1);
    }

    public static int growthLevelForTotalGain(long totalGain) {
        int g = 1;
        for (int candidate = 2; candidate <= MAX_GROWTH_LEVEL; candidate++) {
            if (totalGain >= cumulativeGrowthXp(candidate)) {
                g = candidate;
            } else {
                break;
            }
        }
        return g;
    }

    public static int unlockedSpecialtySlots(int growthLevel) {
        int slots = 0;
        if (growthLevel >= 10) {
            slots++;
        }
        if (growthLevel >= 30) {
            slots++;
        }
        if (growthLevel >= 60) {
            slots++;
        }
        return slots;
    }

    public static int dailySoftCap(int skillLevel) {
        return 600 + 40 * Math.clamp(skillLevel, 0, MAX_SPECIALTY_LEVEL);
    }

    public static double bandMultiplier(int amount, int currentLevel) {
        if (amount >= 50) {
            return 1.0;
        }
        if (amount >= 6) {
            return Math.clamp(1.0 - (currentLevel - 25.0) / 100.0, 0.50, 1.0);
        }
        return Math.clamp(1.0 - (currentLevel - 10.0) / 60.0, 0.25, 1.0);
    }

    public static double repeatMultiplier(double recentIncludingCurrent) {
        double excess = Math.max(0.0, recentIncludingCurrent - 60.0);
        return Math.max(0.10, 1.0 / (1.0 + excess / 120.0));
    }
}
