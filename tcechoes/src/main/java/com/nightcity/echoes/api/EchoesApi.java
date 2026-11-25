package com.nightcity.echoes.api;

import com.nightcity.echoes.TEchoes;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

/**
 * Public API entry point for Echoes world history, sites, legends, clues, and heritage (18 §12.1).
 */
public interface EchoesApi {
    static EchoesApi get() {
        return TEchoes.api();
    }

    WorldHistoryView view();

    List<SiteSummary> sitesNear(ServerLevel level, BlockPos pos, int radius);

    Optional<SiteSummary> site(int siteId);

    List<LegendView> legendsOf(UUID civId);

    float clueDiscount(UUID civId, Identifier techId);

    int scriptMastery(UUID civId, int cultureId);

    Optional<HeritageEntry> heritage(int siteId);

    HistoryCell historyCell(ServerLevel level, long regionKey);

    void registerLegendSource(Identifier id, LegendView.LegendSource source);

    void appendLiveHistory(LiveHistoryEntry entry);

    boolean requestRuinify(long settlementId, Identifier reason);
}
