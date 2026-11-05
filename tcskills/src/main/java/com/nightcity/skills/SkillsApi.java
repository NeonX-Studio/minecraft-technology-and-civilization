package com.nightcity.skills;

import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public final class SkillsApi {
    private SkillsApi() {
    }

    public static PlayerSkillsProfile getProfile(ServerLevel level, UUID playerId) {
        return SkillsSavedData.get(level).profile(playerId);
    }

    public static SkillsSavedData.GrowthResult addProficiency(ServerLevel level, UUID playerId, String skill, int amount) {
        return SkillsSavedData.get(level).addProficiency(playerId, skill, amount);
    }

    public static int getSkillLevel(ServerLevel level, UUID playerId, String skill) {
        return getProfile(level, playerId).skillLevel(skill);
    }

    public static double getProficiencyMultiplier(ServerLevel level, UUID playerId, String skill) {
        PlayerSkillsProfile profile = getProfile(level, playerId);
        double multiplier = 1.0 + Math.min(profile.skillLevel(skill) - 1, 20) * 0.05;
        if (profile.hasTalent("efficient_learner")) {
            multiplier += 0.10;
        }
        if (profile.hasTalent(skill + "_specialist")) {
            multiplier += 0.15;
        }
        return multiplier;
    }
}
