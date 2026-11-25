package com.nightcity.wildlands.compat.tcivilization;

import com.nightcity.tcivilization.api.VanillaPolicyService;
import com.nightcity.tcivilization.api.VanillaSystem;
import com.nightcity.tcivilization.api.world.PlanetProgressStages;
import com.nightcity.tcivilization.api.world.TcWorldApis;
import com.nightcity.tcivilization.api.world.event.EmissionRecordedEvent;
import com.nightcity.tcivilization.sim.TcEventBus;
import com.nightcity.wildlands.api.impl.WildlandsApiImpl;
import com.nightcity.wildlands.data.WildlandsRegionLayers;
import com.nightcity.wildlands.network.WildlandsPayloads;
import com.nightcity.wildlands.planet.PlanetBinFile;
import com.nightcity.wildlands.planet.PlanetGenerator;
import com.nightcity.wildlands.weather.RegionalWeatherEngine;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TcivilizationBridge {
    private static volatile boolean initialized;
    private static final AtomicLong LAST_PROGRESS_BROADCAST_MS = new AtomicLong(0L);

    private TcivilizationBridge() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        PlanetBinFile.registerMigrators();
        WildlandsRegionLayers.registerMigrators();

        TcWorldApis.install(WildlandsApiImpl.createApiSet());

        PlanetProgressStages.installProvider("tcwildlands", new PlanetProgressStages.Listener() {
            @Override
            public void onReport(MinecraftServer server, PlanetProgressStages.StageInfo stage, float fraction, Optional<Component> detail) {
                long now = System.currentTimeMillis();
                long prev = LAST_PROGRESS_BROADCAST_MS.get();
                if (now - prev >= 250L && LAST_PROGRESS_BROADCAST_MS.compareAndSet(prev, now)) {
                    broadcastProgress(server, stage, detail.map(Component::getString).orElse(""));
                }
            }

            @Override
            public void onComplete(MinecraftServer server, PlanetProgressStages.StageInfo stage, boolean allCompleted) {
                broadcastProgress(server, stage, "");
            }
        }, PlanetGenerator.builtinStages());

        VanillaPolicyService policy = VanillaPolicyService.get();
        policy.registerOwner(VanillaSystem.WORLDGEN, "tcwildlands", 100);
        policy.registerOwner(VanillaSystem.STRUCTURES, "tcwildlands", 100);
        policy.registerOwner(VanillaSystem.WEATHER, "tcwildlands", 100);
        policy.registerOwner(VanillaSystem.FIRE, "tcwildlands", 100);
        policy.registerOwner(VanillaSystem.NETHER, "tcwildlands", 100);

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(EmissionRecordedEvent.class, evt -> {
            if ("co2e".equals(evt.pollutant().getPath())) {
                RegionalWeatherEngine.addEmissionsKg(evt.amountKg());
            }
        });
    }

    private static void broadcastProgress(MinecraftServer server, PlanetProgressStages.StageInfo activeStage, String detail) {
        if (server == null || server.getPlayerList() == null) {
            return;
        }
        List<PlanetProgressStages.StageInfo> stages = PlanetProgressStages.stages();
        int idx = 0;
        for (int i = 0; i < stages.size(); i++) {
            if (stages.get(i).id().equals(activeStage.id())) {
                idx = i;
                break;
            }
        }
        var payload = new WildlandsPayloads.PlanetProgressPayload(idx, stages.size(), PlanetProgressStages.overallFraction(), detail);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
