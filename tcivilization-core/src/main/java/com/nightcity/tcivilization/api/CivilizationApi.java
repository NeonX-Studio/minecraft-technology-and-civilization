package com.nightcity.tcivilization.api;

import com.nightcity.tcivilization.CivilizationSavedData;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

/**
 * Server-side civilization state, statistics, and milestone API (03 §5.1, §12.1).
 */
public interface CivilizationApi {
    UUID PLAYER_CIV_ID = Civilization.PLAYER_CIV_ID;

    static CivilizationApi get(MinecraftServer server) {
        return CivilizationSavedData.get(server.overworld());
    }

    Civilization playerCiv();

    Optional<Civilization> civ(UUID id);

    Optional<Civilization> civOf(ServerPlayer player);

    OptionalInt era(UUID civ);

    void setEra(UUID civ, int era, EraWriteToken token);

    Civilization createAiCivilization(String name, Identifier culture, BannerPatternLayers banner);

    Civilization createHistoricCivilization(String name, Identifier culture, BannerPatternLayers banner, int endYear);

    CivStatsView stats(UUID civ);

    void setFlag(UUID civ, Identifier flag, boolean value);

    List<MilestoneRecord> milestones(UUID civ);

    void recordMilestone(MilestoneRecord record);

    SettlementRegistry settlements();

    MemoryApi memories();
}
