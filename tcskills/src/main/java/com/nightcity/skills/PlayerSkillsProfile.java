package com.nightcity.skills;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PlayerSkillsProfile(
        UUID playerId,
        int totalProficiency,
        int skillPoints,
        int growthLevel,
        Map<String, Integer> skills,
        List<String> talents
) {
    public static final Codec<PlayerSkillsProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("player_id").forGetter(PlayerSkillsProfile::playerId),
            Codec.INT.fieldOf("total_proficiency").orElse(0).forGetter(PlayerSkillsProfile::totalProficiency),
            Codec.INT.fieldOf("skill_points").orElse(0).forGetter(PlayerSkillsProfile::skillPoints),
            Codec.INT.fieldOf("growth_level").orElse(1).forGetter(PlayerSkillsProfile::growthLevel),
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("skills").orElse(Map.of()).forGetter(PlayerSkillsProfile::skills),
            Codec.STRING.listOf().fieldOf("talents").orElse(List.of()).forGetter(PlayerSkillsProfile::talents)
    ).apply(instance, PlayerSkillsProfile::new));

    public PlayerSkillsProfile {
        skills = Map.copyOf(skills);
        talents = List.copyOf(talents);
    }

    public static PlayerSkillsProfile empty(UUID playerId) {
        return new PlayerSkillsProfile(playerId, 0, 0, 1, Map.of(), List.of());
    }

    public PlayerSkillsProfile addProficiency(String skill, int amount) {
        if (amount <= 0) {
            return this;
        }

        Map<String, Integer> updatedSkills = new HashMap<>(skills);
        updatedSkills.merge(skill, amount, Integer::sum);
        int updatedTotal = totalProficiency + amount;
        int updatedLevel = growthLevel;
        int updatedPoints = skillPoints;
        while (updatedTotal >= updatedLevel * 100) {
            updatedLevel++;
            updatedPoints++;
        }

        return new PlayerSkillsProfile(playerId, updatedTotal, updatedPoints, updatedLevel, updatedSkills, talents);
    }

    public boolean hasTalent(String talent) {
        return talents.contains(talent);
    }

    public PlayerSkillsProfile learnTalent(String talent) {
        List<String> updatedTalents = new java.util.ArrayList<>(talents);
        updatedTalents.add(talent);
        return new PlayerSkillsProfile(playerId, totalProficiency, skillPoints - 1, growthLevel, skills, updatedTalents);
    }

    public int skillExperience(String skill) {
        return skills.getOrDefault(skill, 0);
    }

    public int skillLevel(String skill) {
        return skillExperience(skill) / 100 + 1;
    }
}
