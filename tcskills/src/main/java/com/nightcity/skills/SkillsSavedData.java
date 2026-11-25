package com.nightcity.skills;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.skills.api.GainFlag;
import com.nightcity.skills.api.GainResult;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.fml.ModList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public final class SkillsSavedData extends SavedData {
    public static final int FORMAT_VERSION = 1;

    public static final Codec<SkillsSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("formatVersion").xmap(opt -> opt.orElse(0), Optional::of).forGetter(SkillsSavedData::formatVersion),
            PlayerSkillsProfile.CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(SkillsSavedData::profiles)
    ).apply(instance, SkillsSavedData::new));

    public static final SavedDataType<SkillsSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(TSkills.MOD_ID, "player_skills"),
            SkillsSavedData::new,
            CODEC
    );

    private final int formatVersion;
    private final Map<UUID, PlayerSkillsProfile> players;

    public SkillsSavedData() {
        this(FORMAT_VERSION, List.of());
    }

    private SkillsSavedData(int version, List<PlayerSkillsProfile> storedProfiles) {
        this.formatVersion = FORMAT_VERSION;
        this.players = new HashMap<>();
        for (PlayerSkillsProfile profile : storedProfiles) {
            this.players.put(profile.playerId(), profile);
        }
    }

    public static SkillsSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public int formatVersion() {
        return formatVersion;
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

    public void putProfile(PlayerSkillsProfile updated) {
        players.put(updated.playerId(), updated);
        setDirty();
    }

    public GrowthResult addProficiency(UUID playerId, String skill, int amount) {
        return addProficiency(playerId, skill, amount, Set.of());
    }

    public GrowthResult addProficiency(UUID playerId, String skill, int amount, Set<GainFlag> flags) {
        PlayerSkillsProfile before = profile(playerId);
        Predicate<String> modCheck = m -> ModList.get() != null && ModList.get().isLoaded(m);
        var outcome = before.gainProficiency(skill, amount, flags, TSkills.talents().snapshot(), modCheck);
        players.put(playerId, outcome.profile());
        setDirty();
        return new GrowthResult(outcome.profile(), outcome.profile().growthLevel() > before.growthLevel(), outcome.result());
    }

    public TalentResult learnTalent(UUID playerId, String talent) {
        PlayerSkillsProfile before = profile(playerId);
        if (before.hasTalent(talent)) {
            return TalentResult.ALREADY_KNOWN;
        }
        Identifier tid = talent.contains(":") ? Identifier.parse(talent) : Identifier.fromNamespaceAndPath(TSkills.MOD_ID, talent);
        if (TSkills.talents().snapshot().byId().containsKey(tid)) {
            Optional<PlayerSkillsProfile> committed = before.commitTalents(List.of(tid), TSkills.talents().snapshot());
            if (committed.isEmpty()) {
                return TalentResult.NO_SKILL_POINTS;
            }
            players.put(playerId, committed.get());
            setDirty();
            return TalentResult.LEARNED;
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

    public record GrowthResult(PlayerSkillsProfile profile, boolean leveledUp, GainResult gainResult) {
        public GrowthResult(PlayerSkillsProfile profile, boolean leveledUp) {
            this(profile, leveledUp, GainResult.UNAVAILABLE_RESULT);
        }
    }

    public enum TalentResult {
        LEARNED,
        ALREADY_KNOWN,
        NO_SKILL_POINTS
    }
}
