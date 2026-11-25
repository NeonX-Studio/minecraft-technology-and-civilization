package com.nightcity.nations;

import com.mojang.logging.LogUtils;
import com.nightcity.nations.api.NationState;
import com.nightcity.nations.api.NationsApi;
import com.nightcity.nations.compat.tcechoes.TcechoesBridge;
import com.nightcity.nations.data.NationsSavedData;
import com.nightcity.nations.seed.EchoesHistorySnapshot;
import com.nightcity.nations.seed.NationSeeder;
import com.nightcity.nations.vanilla.NationsVanillaGuard;
import com.nightcity.tcivilization.TcServerContext;
import com.nightcity.tcivilization.api.Civilization;
import com.nightcity.tcivilization.api.CivilizationApi;
import com.nightcity.tcivilization.api.SettlementRecord;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
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
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

public final class TNationsEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    private TNationsEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!NationsConfig.isEnabled()) {
            return;
        }
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        NationsSavedData savedData = NationsSavedData.get(overworld);

        if (!savedData.isSeeded() && Files.isRegularFile(NationsSavedData.civsFilePath(worldRoot))) {
            try {
                NationsSavedData diskData = NationsSavedData.loadFromDirectory(worldRoot);
                if (diskData.isSeeded()) {
                    savedData.recordSeeded(
                            diskData.worldSeed(),
                            diskData.seedSource(),
                            diskData.allNations(),
                            diskData.allRelations(),
                            diskData.regionInfluences()
                    );
                }
            } catch (IOException ex) {
                LOGGER.warn("Failed reading existing civs.dat; falling back to SavedData state: {}", ex.getMessage());
            }
        }

        NationsApi.install(savedData);
        NationsVanillaGuard.applyFirstLoadGameRules(server, savedData);

        if (savedData.isSeeded()) {
            return;
        }

        if (TcechoesBridge.isEchoesAvailable()) {
            EchoesHistorySnapshot snap = TcechoesBridge.readHistorySnapshot();
            if (snap.loaded()) {
                LOGGER.info("Seeding AI civilizations from terminal Echoes history snapshot.");
                runInitialSeeding(server, savedData, snap);
            }
        } else {
            runInitialSeeding(server, savedData, EchoesHistorySnapshot.unloaded());
        }
    }

    public static void onWorldHistoryReady(MinecraftServer server, EchoesHistorySnapshot snapshot, boolean freshlyGenerated) {
        if (!NationsConfig.isEnabled() || server == null) {
            return;
        }
        ServerLevel overworld = server.overworld();
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        NationsSavedData savedData = NationsSavedData.get(overworld);
        if (savedData.isSeeded() || Files.isRegularFile(NationsSavedData.civsFilePath(worldRoot))) {
            return;
        }
        if (!freshlyGenerated) {
            LOGGER.info("civs.dat missing on existing save; seeding AI civilizations once from current Echoes terminal state.");
        }
        runInitialSeeding(server, savedData, snapshot);
    }

    private static void runInitialSeeding(MinecraftServer server, NationsSavedData savedData, EchoesHistorySnapshot echoes) {
        ServerLevel overworld = server.overworld();
        CivilizationApi civApi = CivilizationApi.get(server);
        TcServerContext ctx = TcServerContext.get(server);
        int playerEra = civApi.era(Civilization.PLAYER_CIV_ID).orElse(0);
        BlockPos spawnPos = overworld.getRespawnData().pos();
        List<BlockPos> playerCores = civApi.settlements().all().stream()
                .filter(s -> Civilization.PLAYER_CIV_ID.equals(s.civ()))
                .filter(s -> s.corePos().dimension() == Level.OVERWORLD)
                .map(s -> s.corePos().pos())
                .toList();

        NationSeeder.SeedPlan plan = NationSeeder.computePlan(
                overworld.getSeed(),
                spawnPos,
                playerEra,
                playerCores,
                NationsConfig.seedingSettings(),
                echoes,
                NationSeeder.serverOracle(overworld)
        );
        NationSeeder.applyPlan(
                plan,
                civApi,
                ctx.regions(),
                savedData,
                spawnPos,
                playerCores,
                overworld.getOverworldClockTime(),
                true
        );
        persistCivsFilesQuietly(server, savedData);
    }

    @SubscribeEvent
    public static void onLevelSave(LevelEvent.Save event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD && NationsConfig.isEnabled()) {
            NationsSavedData data = NationsSavedData.get(level);
            if (data.isSeeded()) {
                persistCivsFilesQuietly(level.getServer(), data);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        if (overworld != null) {
            NationsSavedData data = NationsSavedData.get(overworld);
            if (data.isSeeded()) {
                persistCivsFilesQuietly(server, data);
            }
        }
        NationsApi.install(null);
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        NationsVanillaGuard.onFinalizeSpawn(event);
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tcnations")
                .then(Commands.literal("status").executes(context -> sendStatus(context.getSource()))));
    }

    public static List<String> buildStatusLines(
            NationsSavedData savedData,
            int playerEra,
            long blockedPatrols,
            long blockedVillages
    ) {
        if (savedData == null || !savedData.isSeeded()) {
            return List.of("Nations: unformed (seeding pending)");
        }
        int breath = NationsConfig.seedingSettings().breathingRadiusForPlayerEra(playerEra);
        Collection<NationState> active = savedData.activeNations();
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        lines.add(String.format(
                java.util.Locale.ROOT,
                "Nations P1 seeded: source=%s, active=%d, ownedRegions=%d, breathingRadius=%dm, blockedPatrols=%d, blockedVillages=%d",
                savedData.seedSource(),
                active.size(),
                savedData.ownedRegionCount(),
                breath,
                blockedPatrols,
                blockedVillages
        ));
        for (NationState s : active) {
            lines.add(String.format(
                    java.util.Locale.ROOT,
                    " - %s [%s, E%d, %s] pop=%.0f cap=(%d,%d) aggr=%d agenda=%s",
                    s.name(),
                    s.government().name(),
                    s.eraIndex(),
                    s.culture().getPath(),
                    s.population(),
                    s.capitalPos().getX(),
                    s.capitalPos().getZ(),
                    s.personality().aggression(),
                    s.agenda().getPath()
            ));
        }
        return lines;
    }

    private static int sendStatus(CommandSourceStack source) {
        ServerLevel overworld = source.getServer().overworld();
        NationsSavedData savedData = NationsSavedData.get(overworld);
        int playerEra = CivilizationApi.get(source.getServer()).era(Civilization.PLAYER_CIV_ID).orElse(0);
        source.sendSuccess(() -> Component.translatable("command.tcnations.status"), false);
        if (!savedData.isSeeded()) {
            source.sendSuccess(() -> Component.translatable("command.tcnations.status.unformed"), false);
            return 1;
        }
        List<String> lines = buildStatusLines(
                savedData,
                playerEra,
                NationsVanillaGuard.blockedPatrols(),
                NationsVanillaGuard.blockedVillageEntities() + NationsVanillaGuard.blockedVillageStructures()
        );
        for (String line : lines) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static void persistCivsFilesQuietly(MinecraftServer server, NationsSavedData data) {
        try {
            data.saveToDirectory(server.getWorldPath(LevelResource.ROOT));
        } catch (IOException ex) {
            LOGGER.warn("Failed saving tc/nations/civs.dat: {}", ex.getMessage());
        }
    }
}
