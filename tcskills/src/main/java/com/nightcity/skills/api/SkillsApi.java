package com.nightcity.skills.api;

import com.nightcity.skills.TSkills;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface SkillsApi {
    static SkillsApi get() {
        return TSkills.api();
    }

    int level(Player player, Skill skill);

    int level(UUID playerId, Skill skill);

    long xp(UUID playerId, Skill skill);

    long reserveXp(UUID playerId, Skill skill);

    int growthLevel(Player player);

    int freePoints(Player player);

    List<Skill> specialties(Player player);

    boolean isSpecialty(Player player, Skill skill);

    boolean hasTalent(Player player, Identifier talentId);

    double multiplier(Player player, Skill skill, Identifier effect);

    double multiplier(UUID playerId, Skill skill, Identifier effect);

    boolean hasOp(Player player, Identifier op);

    float opValue(Player player, Identifier op, float fallback);

    GainResult grant(ServerPlayer player, Skill skill, int amount, Identifier source, Set<GainFlag> flags);

    GainResult grantOffline(UUID playerId, Skill skill, int amount, Identifier source);

    default @Nullable Object rawProfile(UUID playerId) {
        return null;
    }
}
