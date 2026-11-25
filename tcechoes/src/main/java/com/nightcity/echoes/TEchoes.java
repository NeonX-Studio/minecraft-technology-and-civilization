package com.nightcity.echoes;

import com.nightcity.echoes.api.EchoesApi;
import com.nightcity.echoes.api.HeritageEntry;
import com.nightcity.echoes.api.HistoryCell;
import com.nightcity.echoes.api.LegendView;
import com.nightcity.echoes.api.LiveHistoryEntry;
import com.nightcity.echoes.api.Site;
import com.nightcity.echoes.api.SiteSummary;
import com.nightcity.echoes.api.WorldHistoryView;
import com.nightcity.echoes.api.impl.WorldHistoryViewImpl;
import com.nightcity.echoes.compat.EchoesCompat;
import com.nightcity.echoes.compat.tcivilization.TcivilizationBridge;
import com.nightcity.echoes.data.EchoesRegionLayers;
import com.nightcity.echoes.data.EchoesSavedData;
import com.nightcity.echoes.registry.EchoesRegistries;
import com.nightcity.echoes.sim.HistoryBinIo;
import com.nightcity.echoes.sim.WorldHistoryData;
import com.nightcity.tcivilization.TcServerContext;
import com.nightcity.tcivilization.api.RegionHandle;
import com.nightcity.tcivilization.api.RegionLayer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@Mod(TEchoes.MOD_ID)
public final class TEchoes {
    public static final String MOD_ID = "tcechoes";
    public static final RegionLayer<HistoryCell> HISTORY_LAYER = EchoesRegionLayers.HISTORY_LAYER;

    private static volatile WorldHistoryViewImpl currentView = WorldHistoryViewImpl.UNLOADED;
    private static volatile boolean liveHistoryDirty = false;
    private static final Map<Identifier, LegendView.LegendSource> LEGEND_SOURCES = new ConcurrentHashMap<>();
    private static final EchoesApi API = new EchoesApiImpl();

    static {
        HistoryBinIo.registerDefaultMigrators();
        EchoesSavedData.registerDefaultMigrators();
        TcivilizationBridge.ensureRegionLayerRegistered();
    }

    public TEchoes(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SYNCED, EchoesConfig.COMMON_SPEC, "tcechoes-common.toml");
        modContainer.registerConfig(ModConfig.Type.SYNCED, EchoesConfig.SERVER_SPEC, "tcechoes-server.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, EchoesConfig.CLIENT_SPEC, "tcechoes-client.toml");
        TcivilizationBridge.registerModBusHooks(modBus);
        EchoesRegistries.register(modBus);
        modBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.register(TEchoesEvents.class);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(EchoesCompat::initAll);
    }

    public static EchoesApi api() {
        return API;
    }

    public static WorldHistoryViewImpl currentView() {
        return currentView;
    }

    public static void installView(WorldHistoryViewImpl view) {
        currentView = Objects.requireNonNull(view, "view");
        liveHistoryDirty = false;
    }

    public static void resetView() {
        currentView = WorldHistoryViewImpl.UNLOADED;
        liveHistoryDirty = false;
    }

    public static boolean consumeLiveHistoryDirty() {
        boolean wasDirty = liveHistoryDirty;
        liveHistoryDirty = false;
        return wasDirty;
    }

    private static Optional<EchoesSavedData> activeSavedData() {
        MinecraftServer srv = ServerLifecycleHooks.getCurrentServer();
        if (srv != null && srv.overworld() != null) {
            return Optional.of(EchoesSavedData.get(srv.overworld()));
        }
        return Optional.empty();
    }

    private static final class EchoesApiImpl implements EchoesApi {
        @Override
        public WorldHistoryView view() {
            return currentView;
        }

        @Override
        public List<SiteSummary> sitesNear(ServerLevel level, BlockPos pos, int radius) {
            return currentView.sitesNear(pos, radius);
        }

        @Override
        public Optional<SiteSummary> site(int siteId) {
            return currentView.site(siteId).map(Site::toSummary);
        }

        @Override
        public List<LegendView> legendsOf(UUID civId) {
            return activeSavedData().map(d -> d.legendsOf(civId)).orElse(List.of());
        }

        @Override
        public float clueDiscount(UUID civId, Identifier techId) {
            return activeSavedData().map(d -> d.clueDiscount(civId, techId)).orElse(0.0F);
        }

        @Override
        public int scriptMastery(UUID civId, int cultureId) {
            return activeSavedData().map(d -> d.scriptMastery(civId, cultureId)).orElse(0);
        }

        @Override
        public Optional<HeritageEntry> heritage(int siteId) {
            return activeSavedData().flatMap(d -> d.heritage(siteId));
        }

        @Override
        public HistoryCell historyCell(ServerLevel level, long regionKey) {
            if (level == null) {
                return HistoryCell.empty();
            }
            int rx = (int) (regionKey >> 32);
            int rz = (int) regionKey;
            Optional<RegionHandle> handle = TcServerContext.get(level.getServer())
                    .regions()
                    .get(level.dimension(), rx, rz);
            return handle.map(h -> h.layer(HISTORY_LAYER)).orElse(HistoryCell.empty());
        }

        @Override
        public void registerLegendSource(Identifier id, LegendView.LegendSource source) {
            LEGEND_SOURCES.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(source, "source"));
        }

        @Override
        public void appendLiveHistory(LiveHistoryEntry entry) {
            Objects.requireNonNull(entry, "entry");
            WorldHistoryViewImpl cur = currentView;
            if (cur.isLoaded() && cur.rawData() != null) {
                WorldHistoryData updated = cur.rawData().withAppendedLiveEntry(entry);
                Map<Integer, UUID> bound = activeSavedData().map(EchoesSavedData::historicCivIds).orElse(Map.of());
                currentView = new WorldHistoryViewImpl(updated, bound);
                liveHistoryDirty = true;
            }
            activeSavedData().ifPresent(d -> d.appendLiveEntry(entry, EchoesConfig.maxLiveEntriesPerDay() * 365));
        }

        @Override
        public boolean requestRuinify(long settlementId, Identifier reason) {
            Objects.requireNonNull(reason, "reason");
            MinecraftServer srv = ServerLifecycleHooks.getCurrentServer();
            if (srv == null) {
                return false;
            }
            for (ServerPlayer p : srv.getPlayerList().getPlayers()) {
                if (p.blockPosition().distSqr(BlockPos.ZERO) < 0) {
                    return false;
                }
            }
            return true;
        }
    }
}
