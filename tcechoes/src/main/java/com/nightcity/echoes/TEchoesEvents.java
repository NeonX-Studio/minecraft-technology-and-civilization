package com.nightcity.echoes;

import com.mojang.logging.LogUtils;
import com.nightcity.echoes.api.EchoesApi;
import com.nightcity.echoes.api.HistoryCell;
import com.nightcity.echoes.api.Polity;
import com.nightcity.echoes.api.PolityFate;
import com.nightcity.echoes.api.WorldHistoryView;
import com.nightcity.echoes.api.impl.WorldHistoryViewImpl;
import com.nightcity.echoes.data.EchoesSavedData;
import com.nightcity.echoes.sim.HistoryBinIo;
import com.nightcity.echoes.world.EchoesWorldBootstrap;
import com.nightcity.tcivilization.RegionCoordinates;
import com.nightcity.tcivilization.api.RegionApi;
import com.nightcity.tcivilization.api.TcThreads;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

public final class TEchoesEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    private TEchoesEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (EchoesConfig.SERVER_SPEC.isLoaded() && !EchoesConfig.ENABLED.get()) {
            return;
        }
        MinecraftServer server = event.getServer();
        TcThreads.bindMainThread(server.getRunningThread());
        EchoesWorldBootstrap.initializeServer(server);
    }

    @SubscribeEvent
    public static void onLevelSave(LevelEvent.Save event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        WorldHistoryViewImpl view = TEchoes.currentView();
        if (view.isLoaded() && view.rawData() != null && TEchoes.consumeLiveHistoryDirty()) {
            Path historyFile = HistoryBinIo.resolvePath(level.getServer().getWorldPath(LevelResource.ROOT));
            try {
                HistoryBinIo.writeAtomic(historyFile, view.rawData());
            } catch (IOException ex) {
                LOGGER.error("Failed saving live history updates to {}: {}", historyFile, ex.getMessage());
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TEchoes.resetView();
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tcechoes")
                .then(Commands.literal("status").executes(context -> sendStatus(context.getSource()))));
    }

    private static int sendStatus(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        int rx = RegionCoordinates.regionX(pos.getX());
        int rz = RegionCoordinates.regionZ(pos.getZ());
        long regionKey = RegionApi.regionKey(rx, rz);
        HistoryCell cell = EchoesApi.get().historyCell(level, regionKey);
        int historicCivCount = level != null && level.getServer() != null
                ? EchoesSavedData.get(level.getServer().overworld()).historicCivIds().size()
                : 0;

        List<Component> lines = buildStatusMessages(EchoesApi.get().view(), historicCivCount, rx, rz, cell);
        for (Component line : lines) {
            source.sendSuccess(() -> line, false);
        }
        return 1;
    }

    public static List<Component> buildStatusMessages(
            WorldHistoryView view,
            int historicCivCount,
            int rx,
            int rz,
            HistoryCell cell
    ) {
        List<Component> lines = new ArrayList<>(5);
        lines.add(Component.translatable("command.tcechoes.status"));
        if (view == null || !view.isLoaded()) {
            lines.add(Component.translatable("command.tcechoes.status.unloaded"));
            lines.add(Component.translatable("command.tcechoes.status.details"));
            return lines;
        }

        int survivors = 0;
        int remnants = 0;
        int extinct = 0;
        for (Polity p : view.polities()) {
            if (p.fate() == PolityFate.SURVIVOR) {
                survivors++;
            } else if (p.fate() == PolityFate.REMNANT) {
                remnants++;
            } else {
                extinct++;
            }
        }

        String modeKey = view.mode().name().toLowerCase(Locale.ROOT);
        String seedHex = "0x" + Long.toHexString(view.historySeed()).toUpperCase(Locale.ROOT);
        lines.add(Component.translatable(
                "command.tcechoes.status.summary",
                Component.translatable("mode.tcechoes." + modeKey),
                view.formatVersion(),
                seedHex,
                view.startYear(),
                view.endYear(),
                view.simulationMillis()
        ));
        lines.add(Component.translatable(
                "command.tcechoes.status.polities",
                view.cultures().size(),
                view.polities().size(),
                survivors,
                remnants,
                extinct,
                historicCivCount
        ));
        lines.add(Component.translatable(
                "command.tcechoes.status.sites",
                view.events().size(),
                view.edges().size(),
                view.sites().size(),
                view.routes().size(),
                view.mines().size(),
                view.legendSeeds().size()
        ));

        HistoryCell safeCell = cell != null ? cell : HistoryCell.empty();
        lines.add(Component.translatable(
                "command.tcechoes.status.region",
                rx,
                rz,
                safeCell.sites().size(),
                safeCell.routes().size(),
                safeCell.mines().size(),
                String.format(Locale.ROOT, "%.2f", safeCell.meanSiteWeathering()),
                Integer.toHexString(safeCell.hazardFlags() & 0xFF)
        ));
        return lines;
    }
}



