package com.nightcity.atlas;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nightcity.atlas.api.ChunkKnowledge;
import com.nightcity.atlas.api.HudLevel;
import com.nightcity.atlas.api.HudSkin;
import com.nightcity.atlas.api.HudWidget;
import com.nightcity.atlas.api.MemoryMark;
import com.nightcity.atlas.api.OkabeItoPalette;
import com.nightcity.atlas.api.SurveyLevel;
import com.nightcity.atlas.api.Waypoint;
import com.nightcity.atlas.client.hud.BuiltinHudWidgets;
import com.nightcity.atlas.client.map.MinimapRenderer;
import com.nightcity.atlas.compat.tcivilization.TcivilizationBridge;
import com.nightcity.atlas.compat.wildlands.WildlandsTerrainBridge;
import com.nightcity.atlas.data.AtlasPersistence;
import com.nightcity.atlas.map.DeterministicWarpField;
import com.nightcity.atlas.map.MapTileCache;
import com.nightcity.atlas.map.MapTileRasterizer;
import com.nightcity.atlas.map.MemoryDecayModel;
import com.nightcity.atlas.map.PlanetProjection;
import com.nightcity.atlas.map.SurveyAtlasStore;
import com.nightcity.atlas.waypoint.WaypointManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AtlasP1PlanetTest {

    @AfterEach
    void resetTestOverrides() {
        WildlandsTerrainBridge.setForceWildlandsLoadedForTests(null);
        WildlandsTerrainBridge.setCustomPreviewProvider(null);
        AtlasConfig.setRuntimeDebugMinimapOverride(null);
        TAtlas.surveyStore().clearAll();
        TAtlas.waypoints().clearAll();
    }

    @Test
    void eastWestWrappingShortestDistanceBearingAndSeamTileIndices() {
        assertEquals(-81_920, PlanetProjection.wrapBlockX(81_920));
        assertEquals(81_919, PlanetProjection.wrapBlockX(-81_921));
        assertEquals(500, PlanetProjection.wrapBlockX(163_840 + 500));

        double distEast = PlanetProjection.distance(81_900, 0, -81_900, 0, true);
        assertEquals(40.0, distEast, 1e-6);
        assertEquals(90.0, PlanetProjection.bearingDegrees(81_900, 0, -81_900, 0, true), 1e-6);

        double distWest = PlanetProjection.distance(-81_900, 0, 81_900, 0, true);
        assertEquals(40.0, distWest, 1e-6);
        assertEquals(270.0, PlanetProjection.bearingDegrees(-81_900, 0, 81_900, 0, true), 1e-6);

        assertEquals(163_800.0, PlanetProjection.distance(81_900, 0, -81_900, 0, false), 1e-6);

        assertEquals(640, PlanetProjection.tileColumns(0));
        assertEquals(639, PlanetProjection.tileIndexX(81_919, 0, true));
        assertEquals(0, PlanetProjection.tileIndexX(81_920, 0, true));
        assertEquals(0, PlanetProjection.tileIndexX(-81_920, 0, true));
        assertEquals(0, PlanetProjection.wrapTileIndexX(640, 0, true));
        assertEquals(639, PlanetProjection.wrapTileIndexX(-1, 0, true));

        assertEquals(5, PlanetProjection.tileColumns(7));
        assertEquals(4, PlanetProjection.tileIndexX(81_919, 7, true));
        assertEquals(0, PlanetProjection.tileIndexX(81_920, 7, true));
        assertEquals(0, PlanetProjection.wrapTileIndexX(5, 7, true));

        int eastSeamBlockX = PlanetProjection.pixelToBlockX(639, 255, 0, true);
        int westSeamBlockX = PlanetProjection.pixelToBlockX(0, 0, 0, true);
        assertEquals(81_919, eastSeamBlockX);
        assertEquals(-81_920, westSeamBlockX);
        assertEquals(1.0, PlanetProjection.distance(eastSeamBlockX, 120, westSeamBlockX, 120, true), 1e-6);
    }

    @Test
    void surveyLevelMergeMemoryHalfLifeDecayAndDeterministicWarpField() {
        ChunkKnowledge sl3Coarse = new ChunkKnowledge(SurveyLevel.SKETCH, false, 5, 10);
        ChunkKnowledge sl3Better = new ChunkKnowledge(SurveyLevel.SKETCH, true, 3, 25);
        ChunkKnowledge sl4 = new ChunkKnowledge(SurveyLevel.SURVEYED, false, 2, 18);

        assertEquals(sl3Coarse, ChunkKnowledge.unpack(sl3Coarse.pack()));
        ChunkKnowledge m1 = ChunkKnowledge.merge(sl3Coarse, sl3Better);
        assertEquals(m1, ChunkKnowledge.merge(sl3Better, sl3Coarse));
        assertEquals(3, m1.errClass());
        assertTrue(m1.heightKnown());
        assertEquals(25, m1.observedDay());
        assertEquals(SurveyLevel.SURVEYED, ChunkKnowledge.merge(m1, sl4).sl());

        SurveyAtlasStore store = new SurveyAtlasStore();
        UUID player = UUID.randomUUID();
        Identifier dim = SurveyAtlasStore.OVERWORLD_ID;
        store.recordPersonalSighting(player, dim, 10, 20, 0L, true);

        double at3Days = store.memoryStrength(player, dim, 10, 20, 3600L, 0, true);
        assertEquals(0.5, at3Days, 1e-6);
        assertEquals(2.0, MemoryDecayModel.blurRadiusPixels(at3Days), 1e-6);
        assertEquals(1.0f, MemoryDecayModel.opacity(at3Days), 1e-5f);

        double at10Days = store.memoryStrength(player, dim, 10, 20, 12_000L, 0, true);
        assertTrue(at10Days > 0.05 && at10Days < 0.25);
        assertEquals((float) (at10Days / 0.25), MemoryDecayModel.opacity(at10Days), 1e-5f);

        assertEquals(SurveyLevel.UNKNOWN, store.resolveChunk(player, dim, 10, 20, 18_000L, 0, true).effectiveSl());
        assertEquals(1, store.pruneForgottenMemories(player, dim, 18_000L, 0, true));

        store.registerCairn(dim, new BlockPos(50 * 16 + 8, 70, 50 * 16 + 8));
        store.recordPersonalSighting(player, dim, 50, 50, 0L, true);
        assertEquals(0.5, store.memoryStrength(player, dim, 50, 50, 14_400L, 0, true), 1e-6);

        DeterministicWarpField warpA1 = new DeterministicWarpField(12345L, 777L, 0L, true);
        DeterministicWarpField warpA2 = new DeterministicWarpField(12345L, 777L, 0L, true);
        DeterministicWarpField warpB = new DeterministicWarpField(12345L, 777L, 999L, true);
        assertEquals(warpA1.sampleUnitVector(1520.5, -840.25), warpA2.sampleUnitVector(1520.5, -840.25));
        assertNotEquals(warpA1.sampleUnitVector(1520.5, -840.25), warpB.sampleUnitVector(1520.5, -840.25));
        assertTrue(warpA1.sampleUnitVector(1520.5, -840.25).length() <= 1.0 + 1e-9);

        DeterministicWarpField.Vec2d precisePos = warpA1.warpForward(400.0, -300.0, (cx, cz) -> 1.0);
        assertEquals(400.0, precisePos.x(), 1e-9);
        assertEquals(-300.0, precisePos.z(), 1e-9);

        DeterministicWarpField.Vec2d sketchPos = warpA1.warpForward(400.0, -300.0, (cx, cz) -> 32.0);
        double shift = PlanetProjection.distance(400.0, -300.0, sketchPos.x(), sketchPos.z(), true);
        assertTrue(shift > 1.0 && shift <= 32.0);

        DeterministicWarpField.Vec2d seamEast = warpA1.sampleUnitVector(81_919.99, 256.0);
        DeterministicWarpField.Vec2d seamWest = warpA1.sampleUnitVector(-81_920.0, 256.0);
        assertEquals(seamEast.x(), seamWest.x(), 1e-3);
        assertEquals(seamEast.z(), seamWest.z(), 1e-3);
    }

    @Test
    void tileCacheMemoryLruDiskQuotaPruningDirtyInvalidationAnd4msFrameBudget(@TempDir Path tempDir) {
        WildlandsTerrainBridge.setForceWildlandsLoadedForTests(true);
        SurveyAtlasStore store = new SurveyAtlasStore();
        store.setWorldSeed(987654321L);
        Identifier dim = SurveyAtlasStore.OVERWORLD_ID;
        UUID viewer = UUID.randomUUID();

        for (int dz = -6; dz <= 6; dz++) {
            for (int dx = -6; dx <= 6; dx++) {
                int cx = PlanetProjection.wrapBlockX(81_900 + dx * 16) >> 4;
                store.mergeArchiveKnowledge(dim, cx, dz, new ChunkKnowledge(SurveyLevel.SKETCH, true, 4, 5), true);
            }
        }

        Set<MapTileCache.TileKey> evicted = new HashSet<>();
        long quotaBytes = 120L * 1024L;
        try (MapTileCache cache = new MapTileCache(store, tempDir.resolve("tiles"), 24, quotaBytes, 4, 2)) {
            cache.setEvictionCallback(evicted::add);
            for (int i = 0; i < 36; i++) {
                MapTileCache.TileKey key = new MapTileCache.TileKey(dim, 2, i * 8, 10);
                MapTileRasterizer.RenderParams rp = new MapTileRasterizer.RenderParams(
                        dim, viewer, 42L, 2, key.tileX(), key.tileZ(), true, true, false, 100L, 5, 2, 0
                );
                assertEquals(MapTileRasterizer.PIXEL_COUNT, cache.getOrBuildBlocking(key, rp, true).argbPixels().length);
            }
            assertTrue(cache.memoryTileCount() <= 24);
            assertFalse(evicted.isEmpty());
            long diskUsed = cache.computeDiskUsageBytes();
            assertTrue(diskUsed > 0L && diskUsed <= quotaBytes);

            MapTileCache.TileKey cleanKey = new MapTileCache.TileKey(dim, 0, 100, 160);
            cache.getOrBuildBlocking(cleanKey, new MapTileRasterizer.RenderParams(
                    dim, viewer, 42L, 0, 100, 160, true, true, false, 100L, 5, 2, 0
            ), false);
            store.mergeArchiveKnowledge(dim, 0, 0, new ChunkKnowledge(SurveyLevel.PRECISE, true, 0, 12), true);
            assertTrue(cache.dirtyTileKeysSnapshot().contains(new MapTileCache.TileKey(
                    dim, 0, PlanetProjection.tileIndexX(0, 0, true), PlanetProjection.tileIndexZ(0, 0, true))));
            assertFalse(cache.dirtyTileKeysSnapshot().contains(cleanKey));

            // Warm up viewport across x = +-81,920 seam then measure EC-P1-5 <= 4 ms frame budget
            for (int w = 0; w < 12; w++) {
                cache.collectViewportFrame(dim, viewer, 42L, 0, 81_910.0, 0.0, 860, 520, true, true, false, 200L, 12, 2, 0);
            }
            double totalMs = 0.0;
            double maxFrameMs = 0.0;
            for (int f = 0; f < 25; f++) {
                if (f == 10) {
                    store.mergeArchiveKnowledge(dim, 5118, 0, new ChunkKnowledge(SurveyLevel.SURVEYED, true, 1, 13), true);
                }
                double panX = PlanetProjection.wrapX(81_880.0 + f * 8.0);
                MapTileCache.ViewportFrameResult res = cache.collectViewportFrame(
                        dim, viewer, 42L, 0, panX, 0.0, 860, 520, true, true, false, 200L, 13, 2, 0
                );
                assertFalse(res.visibleTiles().isEmpty());
                totalMs += res.elapsedMillis();
                maxFrameMs = Math.max(maxFrameMs, res.elapsedMillis());
            }
            assertTrue(totalMs / 25.0 <= 4.0, "Avg frame build exceeded 4 ms: " + (totalMs / 25.0));
            assertTrue(maxFrameMs <= 4.0, "Peak frame build exceeded 4 ms: " + maxFrameMs);
        }
    }

    @Test
    void waypointsMemoryMarksOkabeItoAndFormatVersion1MigrationRoundTrip(@TempDir Path tempDir) throws Exception {
        SurveyAtlasStore store = new SurveyAtlasStore();
        WaypointManager wm = new WaypointManager(store);
        UUID player = UUID.randomUUID();
        Identifier dim = SurveyAtlasStore.OVERWORLD_ID;

        Set<Integer> colors = new HashSet<>();
        Set<OkabeItoPalette.WaypointShape> shapes = new HashSet<>();
        for (OkabeItoPalette.WaypointCategory cat : OkabeItoPalette.WaypointCategory.userSelectable()) {
            assertTrue(colors.add(cat.defaultArgb()));
            assertTrue(shapes.add(cat.defaultShape()));
        }

        Waypoint rejectedAtT0 = new Waypoint(
                UUID.randomUUID(), "Camp", OkabeItoPalette.WaypointCategory.HOME,
                OkabeItoPalette.WaypointCategory.HOME.identifier(),
                OkabeItoPalette.WaypointCategory.HOME.defaultArgb(),
                OkabeItoPalette.WaypointCategory.HOME.defaultShape(),
                Waypoint.Share.PRIVATE, dim, new BlockPos(100, 64, 100), 16, player, 1L, Optional.empty(), Optional.empty()
        );
        assertFalse(wm.addWaypoint(rejectedAtT0, 0, true).accepted());

        MemoryMark m1 = wm.placeMemoryMark(player, dim, new BlockPos(16, 64, 16), 100L, "Berry", true);
        wm.placeMemoryMark(player, dim, new BlockPos(64, 64, 64), 110L, "Flint", true);
        wm.placeMemoryMark(player, dim, new BlockPos(128, 64, 128), 120L, "Ford", true);
        wm.placeMemoryMark(player, dim, new BlockPos(256, 64, 256), 130L, "Cave", true);
        List<WaypointManager.ActiveMemoryMark> active = wm.activeMemoryMarks(player, dim, 200L, 0, true);
        assertEquals(3, active.size());
        assertTrue(active.stream().noneMatch(a -> a.mark().id().equals(m1.id())));

        assertEquals(3, wm.upgradeMemoryMarksIfEligible(player, 1, 2L, 200L, true).size());
        for (int i = 0; i < 8; i++) {
            Waypoint wp = new Waypoint(
                    UUID.randomUUID(), "WP-" + i, OkabeItoPalette.WaypointCategory.RESOURCE,
                    OkabeItoPalette.WaypointCategory.RESOURCE.identifier(),
                    OkabeItoPalette.WaypointCategory.RESOURCE.defaultArgb(),
                    OkabeItoPalette.WaypointCategory.RESOURCE.defaultShape(),
                    Waypoint.Share.PRIVATE, dim, new BlockPos(i * 32, 70, -i * 16), 8, player, 2L,
                    Optional.of("Iron"), Optional.empty()
            );
            assertTrue(wm.addWaypoint(wp, 1, true).accepted());
        }
        assertFalse(wm.addWaypoint(rejectedAtT0, 1, true).accepted());

        store.setStandaloneTier(2);
        store.mergeArchiveKnowledge(dim, -12, 34, new ChunkKnowledge(SurveyLevel.SURVEYED, true, 2, 9), true);
        Path wpFile = tempDir.resolve("waypoints.dat");
        Path surveyFile = tempDir.resolve("survey.dat");
        AtlasPersistence.writeCompressedAtomically(wpFile, AtlasPersistence.encodeWaypointsState(wm));
        AtlasPersistence.writeCompressedAtomically(surveyFile, AtlasPersistence.encodeSurveyState(store, List.of(player), List.of(dim)));

        CompoundTag wpDisk = AtlasPersistence.readCompressed(wpFile);
        CompoundTag surveyDisk = AtlasPersistence.readCompressed(surveyFile);
        assertEquals(1, AtlasPersistence.readFormatVersion(wpDisk));
        assertEquals(1, AtlasPersistence.readFormatVersion(surveyDisk));

        SurveyAtlasStore reloadedStore = new SurveyAtlasStore();
        WaypointManager reloadedWm = new WaypointManager(reloadedStore);
        AtlasPersistence.decodeSurveyState(surveyDisk, reloadedStore, true);
        AtlasPersistence.decodeWaypointsState(wpDisk, reloadedWm, true);
        assertEquals(2, reloadedStore.standaloneTier());
        assertEquals(SurveyLevel.SURVEYED, reloadedStore.archiveKnowledge(dim, -12, 34, true).sl());
        assertEquals(11, reloadedWm.visibleWaypoints(player, dim).size());

        // v0 -> v1 migration & future version guard
        CompoundTag legacyV0 = new CompoundTag();
        ListTag legacyWps = new ListTag();
        CompoundTag oldWp = new CompoundTag();
        oldWp.putString("title", "Ancient Camp");
        oldWp.putInt("x", 81_930);
        oldWp.putInt("y", 68);
        oldWp.putInt("z", -400);
        oldWp.putString("category", "home");
        oldWp.putString("owner", player.toString());
        legacyWps.add(oldWp);
        legacyV0.put("legacy_waypoints", legacyWps);

        WaypointManager migratedWm = new WaypointManager(new SurveyAtlasStore());
        AtlasPersistence.decodeWaypointsState(legacyV0, migratedWm, true);
        assertEquals(-81_910, migratedWm.visibleWaypoints(player, dim).getFirst().pos().getX());

        CompoundTag futureTag = new CompoundTag();
        futureTag.putInt("formatVersion", 99);
        assertThrows(IllegalStateException.class,
                () -> AtlasPersistence.migrate(AtlasPersistence.WAYPOINTS_FILE_ID, AtlasPersistence.FORMAT_VERSION, futureTag));
    }

    @Test
    void minimapDebugGateWildlandsFallbackAndTcivilizationHooks() throws Exception {
        assertFalse(MinimapRenderer.isAllowedToRender(0, false));
        assertFalse(MinimapRenderer.isAllowedToRender(2, false));
        assertTrue(MinimapRenderer.isAllowedToRender(0, true));

        HudWidget.HudContext liveCtx = new HudWidget.HudContext(HudLevel.STANDARD, HudSkin.defaultStoneSkin(), 0f, false);
        HudWidget.HudContext editorCtx = new HudWidget.HudContext(HudLevel.TACTICAL, HudSkin.defaultStoneSkin(), 0f, true);
        AtlasConfig.setRuntimeDebugMinimapOverride(false);
        assertFalse(BuiltinHudWidgets.MINIMAP.shouldRender(liveCtx));
        assertTrue(BuiltinHudWidgets.MINIMAP.shouldRender(editorCtx));
        AtlasConfig.setRuntimeDebugMinimapOverride(true);
        assertTrue(BuiltinHudWidgets.MINIMAP.shouldRender(liveCtx));

        SurveyAtlasStore store = TAtlas.surveyStore();
        Identifier dim = SurveyAtlasStore.OVERWORLD_ID;
        UUID player = UUID.randomUUID();
        store.mergeArchiveKnowledge(dim, 0, 0, new ChunkKnowledge(SurveyLevel.SURVEYED, true, 1, 1), false);
        int[] buf = MinimapRenderer.buildMinimapBuffer(
                store, player, dim, 8.0, 8.0, 0.0f,
                MinimapRenderer.Mode.NORTH_UP, MinimapRenderer.Shape.CIRCLE, 64, 0, true, false, 100L, 1
        );
        assertEquals(0, buf[0]);
        assertNotEquals(0, buf[64 * 128 + 64]);

        Waypoint farWp = new Waypoint(
                UUID.randomUUID(), "Far Peak", OkabeItoPalette.WaypointCategory.DANGER,
                OkabeItoPalette.WaypointCategory.DANGER.identifier(),
                OkabeItoPalette.WaypointCategory.DANGER.defaultArgb(),
                OkabeItoPalette.WaypointCategory.DANGER.defaultShape(),
                Waypoint.Share.CIVILIZATION, dim, new BlockPos(1000, 120, -1000), 8, player, 1L, Optional.empty(), Optional.empty()
        );
        MinimapRenderer.ProjectedMarker projected = MinimapRenderer.projectWaypoint(
                farWp, 0.0, 0.0, 0.0f, MinimapRenderer.Mode.NORTH_UP, MinimapRenderer.Shape.CIRCLE, 64, 48, false
        );
        assertTrue(projected.clampedToEdge());
        assertTrue(MinimapRenderer.computeNorthIndicator(0.0f, MinimapRenderer.Mode.NORTH_UP, MinimapRenderer.Shape.CIRCLE, 48).dy() < 0);

        WildlandsTerrainBridge.setForceWildlandsLoadedForTests(false);
        assertFalse(WildlandsTerrainBridge.isEastWestWrapActive(dim));
        WildlandsTerrainBridge.setForceWildlandsLoadedForTests(true);
        assertTrue(WildlandsTerrainBridge.isEastWestWrapActive(dim));

        TcivilizationBridge.init();
        assertTrue(TcivilizationBridge.registeredVanillaSystems().containsKey("LOCATOR_BAR"));
        assertTrue(TcivilizationBridge.registeredVanillaSystems().containsKey("COORDINATES"));
        assertEquals(3, TcivilizationBridge.buildWheelSpecs(false).size());
        assertEquals(4, TcivilizationBridge.buildWheelSpecs(true).size());

        TAtlas.surveyStore().setStandaloneTier(1);
        assertTrue(TAtlas.waypoints().addWaypoint(farWp, 1, false).accepted());
        assertTrue(TAtlas.waypoints().startNavigation(player, farWp.id()));
        assertEquals(farWp.id(), TcivilizationBridge.pinnedTrackerWaypoints().get(player));
        TAtlas.waypoints().stopNavigation(player);
        assertFalse(TcivilizationBridge.pinnedTrackerWaypoints().containsKey(player));

        Path coreClasses = Path.of("..", "tcivilization-core", "build", "classes", "java", "main");
        if (Files.isDirectory(coreClasses)) {
            try (URLClassLoader cl = new URLClassLoader(new URL[]{coreClasses.toUri().toURL()}, getClass().getClassLoader())) {
                TcivilizationBridge.initWithClassLoader(cl);
                Class<?> policyCls = Class.forName("com.nightcity.tcivilization.api.VanillaPolicyService", true, cl);
                Class<?> sysEnum = Class.forName("com.nightcity.tcivilization.api.VanillaSystem", true, cl);
                Object service = policyCls.getMethod("get").invoke(null);
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object locOwner = policyCls.getMethod("owner", sysEnum).invoke(service, Enum.valueOf((Class<Enum>) sysEnum, "LOCATOR_BAR"));
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object coordOwner = policyCls.getMethod("owner", sysEnum).invoke(service, Enum.valueOf((Class<Enum>) sysEnum, "COORDINATES"));
                assertEquals(Optional.of("tcatlas"), locOwner);
                assertEquals(Optional.of("tcatlas"), coordOwner);
            }
        }

        try (var enIn = getClass().getResourceAsStream("/assets/tcatlas/lang/en_us.json");
             var zhIn = getClass().getResourceAsStream("/assets/tcatlas/lang/zh_cn.json")) {
            assertNotNull(enIn);
            assertNotNull(zhIn);
            JsonObject en = JsonParser.parseReader(new InputStreamReader(enIn, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject zh = JsonParser.parseReader(new InputStreamReader(zhIn, StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals(en.keySet(), zh.keySet());
        }
    }
}
