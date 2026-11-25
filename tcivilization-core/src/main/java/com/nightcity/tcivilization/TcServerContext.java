package com.nightcity.tcivilization;

import com.nightcity.tcivilization.api.RegionGridRegistry;
import com.nightcity.tcivilization.person.HierarchicalPathfinder;
import com.nightcity.tcivilization.person.PersonComponentRegistry;
import com.nightcity.tcivilization.person.PersonManager;
import com.nightcity.tcivilization.region.RegionLayerRegistry;
import com.nightcity.tcivilization.region.RegionManager;
import com.nightcity.tcivilization.sim.SimulationScheduler;
import com.nightcity.tcivilization.sim.TcEventBus;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Per-server runtime container owning RegionManager, PersonManager, HierarchicalPathfinder, and SimulationScheduler.
 */
public final class TcServerContext implements AutoCloseable {
    private static final Map<MinecraftServer, TcServerContext> ACTIVE = new ConcurrentHashMap<>();

    private final MinecraftServer server;
    private final RegionManager regionManager;
    private final PersonManager personManager;
    private final SimulationScheduler scheduler;
    private final HierarchicalPathfinder pathfinder;

    private TcServerContext(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        int dataVersion;
        try {
            dataVersion = SharedConstants.getCurrentVersion().dataVersion().version();
        } catch (Throwable ignored) {
            dataVersion = SharedConstants.WORLD_VERSION;
        }
        this.regionManager = new RegionManager(
                worldRoot,
                dataVersion,
                RegionManager.DEFAULT_CACHE_CAPACITY,
                RegionGridRegistry.defaults(),
                RegionLayerRegistry.global(),
                TcEventBus.global(),
                () -> server.overworld().getOverworldClockTime()
        );
        this.personManager = new PersonManager(
                PersonComponentRegistry.global(),
                TcEventBus.global(),
                () -> server.overworld().getOverworldClockTime()
        );
        this.scheduler = new SimulationScheduler(this.personManager, TcEventBus.global());
        this.pathfinder = new HierarchicalPathfinder(2);
    }

    public static TcServerContext get(MinecraftServer server) {
        return ACTIVE.computeIfAbsent(server, TcServerContext::new);
    }

    public static void shutdown(MinecraftServer server) {
        TcServerContext ctx = ACTIVE.remove(server);
        if (ctx != null) {
            ctx.close();
        }
    }

    public MinecraftServer server() {
        return server;
    }

    public RegionManager regions() {
        return regionManager;
    }

    public PersonManager people() {
        return personManager;
    }

    public SimulationScheduler scheduler() {
        return scheduler;
    }

    public HierarchicalPathfinder pathfinder() {
        return pathfinder;
    }

    @Override
    public void close() {
        try {
            scheduler.close();
        } finally {
            try {
                pathfinder.close();
            } finally {
                regionManager.close();
            }
        }
    }
}
