package com.nightcity.tcivilization.api;

import com.mojang.logging.LogUtils;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

/**
 * Stateless forwarding facade to tcskills (03 §5.11, DR-33).
 * Validates the 22 canonical skill IDs and degrades to no-op / 1.0 when tcskills is absent.
 */
public final class ProficiencyBridge {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Set<String> VALID_SKILLS = Set.of(
            "foraging", "hunting", "woodcutting", "mining",
            "farming", "husbandry", "cooking", "medicine",
            "crafting", "smithing", "engineering", "construction",
            "logistics", "trade", "diplomacy", "leadership", "combat",
            "scholarship", "surveying", "exploration", "aeronautics", "arcana"
    );

    private static final Set<String> WARNED_INVALID = ConcurrentHashMap.newKeySet();
    private static volatile ProficiencySink activeSink;

    private ProficiencyBridge() {
    }

    public interface ProficiencySink {
        void add(ServerPlayer player, String skillId, int amount, Identifier source);

        double multiplier(Player player, String skillId, Identifier effectType);
    }

    public interface Sink extends ProficiencySink {
    }

    public static void registerSink(ProficiencySink sink) {
        Objects.requireNonNull(sink, "sink");
        if (activeSink != null && activeSink != sink) {
            if (TcThreads.isStrictMode()) {
                throw new IllegalStateException("ProficiencySink already registered");
            }
            LOGGER.error("Duplicate ProficiencySink registration ignored: {}", sink.getClass().getName());
            return;
        }
        activeSink = sink;
    }

    public static void clearSinkForTests() {
        activeSink = null;
        WARNED_INVALID.clear();
    }

    public static boolean isAvailable() {
        return activeSink != null;
    }

    public static boolean isValidSkillId(String skillId) {
        if (skillId == null) {
            return false;
        }
        String normalized = skillId.startsWith("tcskills:") ? skillId.substring("tcskills:".length()) : skillId;
        return VALID_SKILLS.contains(normalized);
    }

    public static void add(ServerPlayer player, String skillId, int amount, Identifier source) {
        String normalized = normalizeAndValidate(skillId);
        if (normalized == null || amount <= 0) {
            return;
        }
        ProficiencySink sink = activeSink;
        if (sink != null && player != null) {
            sink.add(player, normalized, amount, source);
        }
    }

    public static double multiplier(Player player, String skillId, Identifier effectType) {
        String normalized = normalizeAndValidate(skillId);
        if (normalized == null) {
            return 1.0D;
        }
        ProficiencySink sink = activeSink;
        if (sink == null || player == null) {
            return 1.0D;
        }
        return sink.multiplier(player, normalized, effectType);
    }

    private static String normalizeAndValidate(String skillId) {
        if (skillId == null || skillId.isBlank()) {
            handleInvalid(String.valueOf(skillId));
            return null;
        }
        String normalized = skillId.startsWith("tcskills:") ? skillId.substring("tcskills:".length()) : skillId;
        if (!VALID_SKILLS.contains(normalized)) {
            handleInvalid(skillId);
            return null;
        }
        return normalized;
    }

    private static void handleInvalid(String skillId) {
        if (TcThreads.isStrictMode()) {
            throw new IllegalArgumentException("Unknown TC skill ID: " + skillId);
        }
        if (WARNED_INVALID.add(skillId)) {
            LOGGER.warn("Unknown TC skill ID '{}' ignored by ProficiencyBridge", skillId);
        }
    }
}
