package com.nightcity.skills;

import com.nightcity.skills.api.Skill;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

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

    public static double getProficiencyMultiplier(ServerLevel level, UUID playerId, String skillRaw) {
        PlayerSkillsProfile profile = getProfile(level, playerId);
        Optional<Skill> skillOpt = Skill.resolve(skillRaw);
        if (skillOpt.isEmpty()) {
            return 1.0D;
        }
        Skill skill = skillOpt.get();
        Predicate<String> modCheck = m -> ModList.get() != null && ModList.get().isLoaded(m);
        return profile.multiplier(skill, skill.primaryEffect(), TSkills.talents().snapshot(), modCheck);
    }
}
