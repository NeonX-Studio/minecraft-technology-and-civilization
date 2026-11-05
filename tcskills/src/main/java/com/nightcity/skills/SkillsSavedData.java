package com.nightcity.skills;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SkillsSavedData extends SavedData {
    public static final SavedDataType<SkillsSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(TSkills.MOD_ID, "player_skills"),
            SkillsSavedData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    PlayerSkillsProfile.CODEC.listOf().fieldOf("players").orElse(List.of()).forGetter(SkillsSavedData::profiles)
            ).apply(instance, SkillsSavedData::new))
    );

    private final Map<UUID, PlayerSkillsProfile> players;

    public SkillsSavedData() {
        this(List.of());
    }

    private SkillsSavedData(List<PlayerSkillsProfile> storedProfiles) {
        players = new HashMap<>();
        for (PlayerSkillsProfile profile : storedProfiles) {
            players.put(profile.playerId(), profile);
        }
    }

    public static SkillsSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public PlayerSkillsProfile profile(UUID playerId) {
        PlayerSkillsProfile profile = players.get(playerId);
        if (profile == null) {
            profile = PlayerSkillsProfile.empty(playerId);
            players.put(playerId, profile);
            setDirty();
        }
        return profile;
    }

    public GrowthResult addProficiency(UUID playerId, String skill, int amount) {
        PlayerSkillsProfile before = profile(playerId);
        PlayerSkillsProfile after = before.addProficiency(skill, amount);
        players.put(playerId, after);
        setDirty();
        return new GrowthResult(after, after.growthLevel() > before.growthLevel());
    }

    public TalentResult learnTalent(UUID playerId, String talent) {
        PlayerSkillsProfile before = profile(playerId);
        if (before.hasTalent(talent)) {
            return TalentResult.ALREADY_KNOWN;
        }
        if (before.skillPoints() <= 0) {
            return TalentResult.NO_SKILL_POINTS;
        }
        players.put(playerId, before.learnTalent(talent));
        setDirty();
        return TalentResult.LEARNED;
    }

    private List<PlayerSkillsProfile> profiles() {
        return List.copyOf(players.values());
    }

    public record GrowthResult(PlayerSkillsProfile profile, boolean leveledUp) {
    }

    public enum TalentResult {
        LEARNED,
        ALREADY_KNOWN,
        NO_SKILL_POINTS
    }
}
