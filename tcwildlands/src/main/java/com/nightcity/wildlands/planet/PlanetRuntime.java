package com.nightcity.wildlands.planet;

import com.mojang.logging.LogUtils;
import com.nightcity.tcivilization.api.world.PlanetProgressStages;
import com.nightcity.tcivilization.api.world.event.PlanetGeneratedEvent;
import com.nightcity.wildlands.WildlandsConfig;
import com.nightcity.wildlands.worldgen.PlanetChunkSynthesizer;
import com.nightcity.wildlands.worldgen.TcDeepEarthChunkGenerator;
import com.nightcity.wildlands.worldgen.TcPlanetChunkGenerator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Manages the active server/test PlanetModel, <world>/tc/planet.bin loading/generation, mode flags,
 * and start-site selection state (10 §5.2, §11.1, §16.6).
 */
public final class PlanetRuntime {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Identifier DEEP_EARTH_DIM_TYPE = WildlandsCatalog.wl("deep_earth");
    public static final Identifier EARTHLIKE_DIM_TYPE = WildlandsCatalog.wl("earthlike");

    @Nullable
    private static volatile PlanetModel activeModel;
    private static volatile boolean planetModeActive = false;
    private static volatile boolean deepEarthModeActive = false;
    private static volatile boolean borderInitialized = false;
    private static volatile boolean serverInitialized = false;
    private static volatile StartSiteStore.@Nullable Selection startSiteSelection;

    private PlanetRuntime() {
    }

    @Nullable
    public static PlanetModel model() {
        return activeModel;
    }

    public static boolean isPlanetActive() {
        return planetModeActive && activeModel != null;
    }

    public static boolean isDeepEarthActive() {
        return deepEarthModeActive;
    }

    public static boolean isBorderInitialized() {
        return borderInitialized;
    }

    public static void markBorderInitialized() {
        borderInitialized = true;
    }

    public static Optional<StartSiteStore.Selection> startSite() {
        return Optional.ofNullable(startSiteSelection);
    }

    public static void activateForTests(@Nullable PlanetModel model, boolean planetMode, boolean deepEarthMode) {
        activeModel = model;
        planetModeActive = planetMode && model != null;
        deepEarthModeActive = deepEarthMode;
        borderInitialized = false;
        serverInitialized = false;
        startSiteSelection = null;
    }

    public static void clear() {
        activeModel = null;
        planetModeActive = false;
        deepEarthModeActive = false;
        borderInitialized = false;
        serverInitialized = false;
        startSiteSelection = null;
    }

    public static void refreshDeepEarthFlag(MinecraftServer server) {
        ServerLevel nether = server.getLevel(Level.NETHER);
        if (nether != null) {
            boolean generatorIsDeep = nether.getChunkSource().getGenerator() instanceof TcDeepEarthChunkGenerator;
            boolean dimTypeIsDeep = nether.dimensionTypeRegistration().unwrapKey()
                    .map(k -> DEEP_EARTH_DIM_TYPE.equals(k.identifier()))
                    .orElse(false);
            deepEarthModeActive = generatorIsDeep || dimTypeIsDeep;
        } else {
            deepEarthModeActive = WildlandsConfig.isReplaceNether();
        }
    }

    public static synchronized void onServerStarting(MinecraftServer server) {
        refreshDeepEarthFlag(server);
        ServerLevel overworld = server.overworld();
        if (serverInitialized) {
            if (overworld != null && activeModel != null && overworld.getChunkSource().getGenerator() instanceof TcPlanetChunkGenerator planetGen) {
                planetGen.bindModel(activeModel);
            }
            return;
        }
        boolean generatorIsPlanet = overworld != null && overworld.getChunkSource().getGenerator() instanceof TcPlanetChunkGenerator;
        boolean configPlanet = WildlandsConfig.isReplaceOverworld();
        planetModeActive = generatorIsPlanet;
        serverInitialized = true;

        if (!planetModeActive && !configPlanet) {
            activeModel = null;
            return;
        }
        if (!planetModeActive) {
            // Degraded vanilla preset selected at world creation (10 §2.5)
            activeModel = null;
            return;
        }

        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        Path planetBin = worldRoot.resolve("tc").resolve("planet.bin");
        long seed = overworld != null ? overworld.getSeed() : 0L;

        boolean freshlyGenerated = false;
        PlanetModel loaded = null;
        if (Files.exists(planetBin)) {
            try {
                loaded = PlanetBinFile.read(planetBin);
                for (PlanetProgressStages.StageInfo st : PlanetGenerator.builtinStages()) {
                    PlanetProgressStages.complete(server, st.id());
                }
                LOGGER.info("Loaded <world>/tc/planet.bin (seed={}, continents={}, deposits={})",
                        loaded.worldSeed(), loaded.continentCount(), loaded.deposits().size());
            } catch (Exception ex) {
                LOGGER.warn("Corrupt or outdated planet.bin, regenerating deterministically from seed {}: {}", seed, ex.getMessage());
            }
        }

        if (loaded == null) {
            freshlyGenerated = true;
            PlanetProgressStages.resetProgressRun();
            PlanetGenerator.Config cfg = new PlanetGenerator.Config(
                    WildlandsConfig.SERVER_SPEC.isLoaded() ? WildlandsConfig.PLANET_LAND_TARGET.getAsDouble() : 0.29,
                    WildlandsConfig.SERVER_SPEC.isLoaded() ? WildlandsConfig.PLANET_PLATE_COUNT.getAsInt() : 0,
                    WildlandsConfig.SERVER_SPEC.isLoaded() ? WildlandsConfig.PLANET_EROSION_STEPS.getAsInt() : 80,
                    WildlandsConfig.effectivePrecomputeThreads()
            );
            long t0 = System.nanoTime();
            loaded = PlanetGenerator.generate(seed, cfg, server);
            long elapsedMs = (System.nanoTime() - t0) / 1_000_000L;
            try {
                PlanetBinFile.write(planetBin, loaded);
                LOGGER.info("Generated and saved <world>/tc/planet.bin in {} ms (continents={}, peakY={})",
                        elapsedMs, loaded.continentCount(), loaded.summary().maxElevationY());
            } catch (IOException ex) {
                LOGGER.error("Failed to write planet.bin: {}", ex.getMessage());
            }
        }

        activeModel = loaded;
        if (overworld != null && overworld.getChunkSource().getGenerator() instanceof TcPlanetChunkGenerator planetGen) {
            planetGen.bindModel(loaded);
        }

        // Load or auto-pick start_site.json
        Path startSiteFile = StartSiteStore.resolvePath(worldRoot);
        try {
            Optional<StartSiteStore.Selection> existing = StartSiteStore.readIfExists(startSiteFile);
            if (existing.isPresent()) {
                StartSiteStore.Selection sel = existing.get();
                startSiteSelection = sel;
                syncServerWorldSpawn(server, resolveHabitableSpawnPos(loaded, sel.chosenX(), sel.chosenZ()));
            } else if (WildlandsConfig.SERVER_SPEC.isLoaded() && (!WildlandsConfig.START_SITE_ENABLED.getAsBoolean() || WildlandsConfig.START_SITE_AUTO_PICK_SECONDS.getAsInt() == 0)) {
                chooseCandidateStartSite(server, 0, null, "auto");
            } else if (overworld != null && overworld.getLevelData() instanceof ServerLevelData sld && sld.isInitialized()) {
                BlockPos currentSpawn = sld.getRespawnData().pos();
                if (isSubmergedOrOceanSpawn(loaded, currentSpawn)) {
                    syncServerWorldSpawn(server, resolveInitialWorldSpawn(server));
                }
            }
        } catch (IOException ex) {
            LOGGER.warn("Failed reading start_site.json: {}", ex.getMessage());
        }

        NeoForge.EVENT_BUS.post(new PlanetGeneratedEvent(server, loaded.summary(), freshlyGenerated));
    }

    public static int effectiveMaxStep() {
        return WildlandsConfig.SERVER_SPEC.isLoaded() ? WildlandsConfig.PLANET_MAX_STEP.getAsInt() : 3;
    }

    public static boolean isDryHabitableColumn(PlanetChunkSynthesizer.ColumnProfile prof) {
        if (prof.surfaceY() <= PlanetCoordinates.SEA_LEVEL) {
            return false;
        }
        if (prof.waterY() != Integer.MIN_VALUE && prof.waterY() >= prof.surfaceY()) {
            return false;
        }
        String biomePath = prof.biomeId().getPath();
        return !biomePath.contains("ocean")
                && !biomePath.contains("shelf_sea")
                && !biomePath.equals("polar_sea_ice")
                && !biomePath.equals("tropical_coral_reef")
                && !biomePath.equals("abyssal_trench")
                && !biomePath.endsWith("_river")
                && !biomePath.endsWith("_lake")
                && !biomePath.startsWith("deep_");
    }

    public static boolean isSubmergedOrOceanSpawn(PlanetModel model, BlockPos pos) {
        PlanetChunkSynthesizer.ColumnProfile prof = PlanetChunkSynthesizer.profileColumn(
                model, pos.getX(), pos.getZ(), effectiveMaxStep()
        );
        return !isDryHabitableColumn(prof);
    }

    public static BlockPos resolveHabitableSpawnPos(PlanetModel model, int preferredX, int preferredZ) {
        int maxStep = effectiveMaxStep();
        BlockPos nearPreferred = findDryLandNear(model, preferredX, preferredZ, 192, 4, maxStep);
        if (nearPreferred != null) {
            return nearPreferred;
        }
        for (PlanetModel.StartSiteCandidate cand : model.startSites()) {
            BlockPos nearCand = findDryLandNear(model, cand.blockX(), cand.blockZ(), 96, 4, maxStep);
            if (nearCand != null) {
                return nearCand;
            }
        }
        short[] elev = model.rawElevation();
        int midZ = PlanetModel.GRID_H / 2;
        for (int dz = 0; dz < midZ; dz++) {
            for (int sign : new int[]{-1, 1}) {
                int gz = Math.clamp(midZ + sign * dz, 1, PlanetModel.GRID_H - 2);
                for (int gx = 0; gx < PlanetModel.GRID_W; gx += 2) {
                    if (elev[PlanetModel.l0Index(gx, gz)] > 20) {
                        int bx = PlanetCoordinates.X_MIN + gx * PlanetCoordinates.L0_STEP + 32;
                        int bz = PlanetCoordinates.Z_MIN + gz * PlanetCoordinates.L0_STEP + 32;
                        BlockPos found = findDryLandNear(model, bx, bz, 64, 8, maxStep);
                        if (found != null) {
                            return found;
                        }
                    }
                }
            }
        }
        int wx = PlanetCoordinates.wrapX(preferredX);
        int cz = PlanetCoordinates.clampZ(preferredZ);
        int sy = Math.max(PlanetCoordinates.SEA_LEVEL + 1, PlanetChunkSynthesizer.computeSurfaceY(model, wx, cz, maxStep));
        return new BlockPos(wx, sy + 1, cz);
    }

    @Nullable
    private static BlockPos findDryLandNear(PlanetModel model, int centerX, int centerZ, int maxRadius, int step, int maxStep) {
        BlockPos steepFallback = null;
        for (int r = 0; r <= maxRadius; r += step) {
            if (r == 0) {
                PlanetChunkSynthesizer.ColumnProfile p0 = PlanetChunkSynthesizer.profileColumn(model, centerX, centerZ, maxStep);
                if (isDryHabitableColumn(p0)) {
                    if (p0.slopeBlocks() <= 2) {
                        return new BlockPos(p0.x(), p0.surfaceY() + 1, p0.z());
                    }
                    steepFallback = new BlockPos(p0.x(), p0.surfaceY() + 1, p0.z());
                }
                continue;
            }
            for (int dx = -r; dx <= r; dx += step) {
                for (int dz : new int[]{-r, r}) {
                    PlanetChunkSynthesizer.ColumnProfile prof = PlanetChunkSynthesizer.profileColumn(model, centerX + dx, centerZ + dz, maxStep);
                    if (isDryHabitableColumn(prof)) {
                        if (prof.slopeBlocks() <= 2) {
                            return new BlockPos(prof.x(), prof.surfaceY() + 1, prof.z());
                        }
                        if (steepFallback == null) {
                            steepFallback = new BlockPos(prof.x(), prof.surfaceY() + 1, prof.z());
                        }
                    }
                }
            }
            for (int dz = -r + step; dz <= r - step; dz += step) {
                for (int dx : new int[]{-r, r}) {
                    PlanetChunkSynthesizer.ColumnProfile prof = PlanetChunkSynthesizer.profileColumn(model, centerX + dx, centerZ + dz, maxStep);
                    if (isDryHabitableColumn(prof)) {
                        if (prof.slopeBlocks() <= 2) {
                            return new BlockPos(prof.x(), prof.surfaceY() + 1, prof.z());
                        }
                        if (steepFallback == null) {
                            steepFallback = new BlockPos(prof.x(), prof.surfaceY() + 1, prof.z());
                        }
                    }
                }
            }
        }
        return steepFallback;
    }

    public static BlockPos resolveInitialWorldSpawn(@Nullable MinecraftServer server) {
        PlanetModel m = activeModel;
        if (m == null && server != null) {
            onServerStarting(server);
            m = activeModel;
        }
        if (m == null) {
            return new BlockPos(8, PlanetCoordinates.SEA_LEVEL + 2, 8);
        }
        StartSiteStore.Selection sel = startSiteSelection;
        if (sel != null) {
            return resolveHabitableSpawnPos(m, sel.chosenX(), sel.chosenZ());
        }
        if (!m.startSites().isEmpty()) {
            PlanetModel.StartSiteCandidate first = m.startSites().getFirst();
            return resolveHabitableSpawnPos(m, first.blockX(), first.blockZ());
        }
        return resolveHabitableSpawnPos(m, 0, 0);
    }

    public static void syncServerWorldSpawn(MinecraftServer server, BlockPos spawnPos) {
        LevelData.RespawnData respawnData = LevelData.RespawnData.of(Level.OVERWORLD, spawnPos, 0.0F, 0.0F);
        if (server.getPlayerList() != null) {
            server.setRespawnData(respawnData);
        } else if (server.overworld() != null && server.overworld().getLevelData() instanceof ServerLevelData sld) {
            sld.setSpawn(respawnData);
        }
    }

    public static Optional<StartSiteStore.Selection> chooseCandidateStartSite(
            MinecraftServer server,
            int candidateIndex,
            @Nullable UUID chosenBy,
            String mode
    ) {
        PlanetModel m = activeModel;
        if (m == null) {
            return Optional.empty();
        }
        List<PlanetModel.StartSiteCandidate> top5 = StartSiteStore.topFiveCandidates(m);
        if (top5.isEmpty()) {
            return Optional.empty();
        }
        int idx = Math.clamp(candidateIndex, 0, top5.size() - 1);
        PlanetModel.StartSiteCandidate chosen = top5.get(idx);
        BlockPos safeSpawn = resolveHabitableSpawnPos(m, chosen.blockX(), chosen.blockZ());
        return commitStartSite(server, safeSpawn.getX(), safeSpawn.getY() - 1, safeSpawn.getZ(), chosen.score(), idx, chosenBy, mode, top5);
    }

    public static Optional<StartSiteStore.Selection> chooseCustomStartSite(
            MinecraftServer server,
            int x,
            int z,
            @Nullable UUID chosenBy
    ) {
        PlanetModel m = activeModel;
        if (m == null) {
            return Optional.empty();
        }
        int wx = PlanetCoordinates.wrapX(x);
        int cz = PlanetCoordinates.clampZ(z);
        PlanetChunkSynthesizer.ColumnProfile prof = PlanetChunkSynthesizer.profileColumn(m, wx, cz, effectiveMaxStep());
        int topY = prof.waterY() != Integer.MIN_VALUE ? Math.max(prof.surfaceY(), prof.waterY()) : prof.surfaceY();
        int sy = Math.max(PlanetCoordinates.SEA_LEVEL + 1, topY);
        List<PlanetModel.StartSiteCandidate> top5 = StartSiteStore.topFiveCandidates(m);
        return commitStartSite(server, wx, sy, cz, 50, -1, chosenBy, "custom", top5);
    }

    private static Optional<StartSiteStore.Selection> commitStartSite(
            MinecraftServer server,
            int x,
            int y,
            int z,
            int score,
            int idx,
            @Nullable UUID chosenBy,
            String mode,
            List<PlanetModel.StartSiteCandidate> top5
    ) {
        ServerLevel overworld = server.overworld();
        long gameTime = overworld != null ? overworld.getOverworldClockTime() : 0L;
        StartSiteStore.Selection sel = new StartSiteStore.Selection(StartSiteStore.FORMAT_VERSION, x, y, z, score, idx, chosenBy, mode, gameTime, top5);
        startSiteSelection = sel;
        syncServerWorldSpawn(server, new BlockPos(x, y + 1, z));
        Path file = StartSiteStore.resolvePath(server.getWorldPath(LevelResource.ROOT));
        try {
            StartSiteStore.write(file, sel);
        } catch (IOException ex) {
            LOGGER.error("Failed writing start_site.json: {}", ex.getMessage());
        }
        return Optional.of(sel);
    }
}
