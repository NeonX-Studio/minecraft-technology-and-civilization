package com.nightcity.atlas.compat.wildlands;

import com.nightcity.atlas.map.DeterministicWarpField;
import com.nightcity.atlas.map.PlanetProjection;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

/**
 * Soft compatibility bridge for tcwildlands precomputed planet model and terrain preview (22 §3.4, §5.10).
 * Zero compile-time coupling to tcivilization-core; dynamically invokes TcWorldApis.planet() and
 * TcWorldApis.terrainPreview().sample(x, z) at runtime.
 */
public final class WildlandsTerrainBridge {
    public static final String WILDLANDS_MOD_ID = "tcwildlands";
    public static final int SEA_LEVEL = 63;

    public record TerrainSample(int surfaceY, int baseArgb, boolean water, int waterDepth) {
    }

    @FunctionalInterface
    public interface TerrainPreviewProvider {
        TerrainSample sample(long worldSeed, int blockX, int blockZ, boolean wrapX);
    }

    private static volatile Boolean forceLoadedOverride;
    private static volatile TerrainPreviewProvider customProvider;
    private static volatile boolean methodsResolved;
    private static volatile Boolean modListLoadedCache;
    private static volatile Method worldApisPlanetGetter;
    private static volatile Method worldApisPreviewGetter;
    private static volatile Method worldApisIsProvidedBy;
    private static volatile Method planetIsActiveMethod;
    private static volatile Method previewSampleMethod;
    private static volatile Method sampleSurfaceYMethod;
    private static volatile Method sampleWaterYMethod;
    private static volatile Method sampleHasWaterMethod;
    private static volatile Method sampleSurfaceKindMethod;

    private WildlandsTerrainBridge() {
    }

    public static void init() {
        methodsResolved = false;
        modListLoadedCache = null;
        resolveCoreMethods();
    }

    public static boolean isWildlandsLoaded() {
        Boolean override = forceLoadedOverride;
        if (override != null) {
            return override;
        }
        if (!methodsResolved) {
            resolveCoreMethods();
        }
        if (worldApisIsProvidedBy != null) {
            try {
                if (Boolean.TRUE.equals(worldApisIsProvidedBy.invoke(null, WILDLANDS_MOD_ID))) {
                    return true;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        Boolean cached = modListLoadedCache;
        if (cached != null) {
            return cached;
        }
        ModList list = ModList.get();
        if (list != null) {
            boolean loaded = list.isLoaded(WILDLANDS_MOD_ID);
            modListLoadedCache = loaded;
            return loaded;
        }
        return false;
    }

    public static boolean isEastWestWrapActive(@Nullable Identifier dimension) {
        if (dimension != null && !"overworld".equals(dimension.getPath()) && !"earthlike".equals(dimension.getPath())) {
            return false;
        }
        if (!isWildlandsLoaded()) {
            return false;
        }
        if (Boolean.TRUE.equals(forceLoadedOverride)) {
            return true;
        }
        Boolean active = queryPlanetActive();
        return active != null ? active : true;
    }

    public static void setForceWildlandsLoadedForTests(@Nullable Boolean loaded) {
        forceLoadedOverride = loaded;
    }

    public static void setCustomPreviewProvider(@Nullable TerrainPreviewProvider provider) {
        customProvider = provider;
    }

    public static TerrainSample sampleTerrain(@Nullable Level level, long worldSeed, int blockX, int blockZ) {
        boolean wrap = isEastWestWrapActive(level != null ? level.dimension().identifier() : Identifier.withDefaultNamespace("overworld"));
        int queryX = wrap ? PlanetProjection.wrapBlockX(blockX) : blockX;

        TerrainPreviewProvider override = customProvider;
        if (override != null) {
            return override.sample(worldSeed, queryX, blockZ, wrap);
        }

        if (level != null) {
            int cx = queryX >> 4;
            int cz = blockZ >> 4;
            ChunkAccess loaded = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
            if (loaded != null) {
                return scanLoadedColumn(level, loaded, queryX, blockZ);
            }
        }

        if (worldApisPreviewGetter != null && isWildlandsLoaded()) {
            TerrainSample wildlandsSample = tryReflectiveWildlandsSample(queryX, blockZ);
            if (wildlandsSample != null) {
                return wildlandsSample;
            }
        }

        if (level instanceof ServerLevel serverLevel) {
            try {
                int baseY = serverLevel.getChunkSource().getGenerator().getBaseHeight(
                        queryX, blockZ, Heightmap.Types.WORLD_SURFACE_WG, serverLevel, serverLevel.getChunkSource().randomState()
                );
                boolean water = baseY <= SEA_LEVEL;
                int depth = water ? Math.max(1, SEA_LEVEL - baseY) : 0;
                return new TerrainSample(Math.max(baseY, SEA_LEVEL), colorForElevationAndLat(baseY, blockZ, water, depth), water, depth);
            } catch (RuntimeException ignored) {
            }
        }

        return proceduralFallbackSample(worldSeed, queryX, blockZ, wrap);
    }

    private static synchronized void resolveCoreMethods() {
        if (methodsResolved) {
            return;
        }
        methodsResolved = true;
        try {
            Class<?> worldApis = Class.forName("com.nightcity.tcivilization.api.world.TcWorldApis");
            Class<?> planetCls = Class.forName("com.nightcity.tcivilization.api.world.PlanetApi");
            Class<?> previewCls = Class.forName("com.nightcity.tcivilization.api.world.TerrainPreviewApi");
            Class<?> sampleCls = Class.forName("com.nightcity.tcivilization.api.world.TerrainPreviewApi$PreviewSample");
            worldApisPlanetGetter = worldApis.getMethod("planet");
            worldApisPreviewGetter = worldApis.getMethod("terrainPreview");
            worldApisIsProvidedBy = worldApis.getMethod("isProvidedBy", String.class);
            planetIsActiveMethod = planetCls.getMethod("isPlanetActive");
            previewSampleMethod = previewCls.getMethod("sample", int.class, int.class);
            sampleSurfaceYMethod = sampleCls.getMethod("surfaceY");
            sampleWaterYMethod = sampleCls.getMethod("waterY");
            sampleHasWaterMethod = sampleCls.getMethod("hasWater");
            sampleSurfaceKindMethod = sampleCls.getMethod("surfaceKind");
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @Nullable
    private static Boolean queryPlanetActive() {
        if (!methodsResolved) {
            resolveCoreMethods();
        }
        if (worldApisPlanetGetter == null || planetIsActiveMethod == null) {
            return null;
        }
        try {
            Object planet = worldApisPlanetGetter.invoke(null);
            if (planet != null) {
                Object v = planetIsActiveMethod.invoke(planet);
                if (v instanceof Boolean b) {
                    return b;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    @Nullable
    private static TerrainSample tryReflectiveWildlandsSample(int x, int z) {
        if (!Boolean.TRUE.equals(queryPlanetActive()) || worldApisPreviewGetter == null || previewSampleMethod == null) {
            return null;
        }
        try {
            Object previewApi = worldApisPreviewGetter.invoke(null);
            if (previewApi != null) {
                Object s = previewSampleMethod.invoke(previewApi, x, z);
                if (s != null) {
                    int sy = ((Number) sampleSurfaceYMethod.invoke(s)).intValue();
                    int wy = ((Number) sampleWaterYMethod.invoke(s)).intValue();
                    boolean hw = Boolean.TRUE.equals(sampleHasWaterMethod.invoke(s));
                    Object ko = sampleSurfaceKindMethod.invoke(s);
                    String kind = ko != null ? ko.toString() : "GRASS";
                    int depth = hw ? Math.max(1, wy - sy) : 0;
                    return new TerrainSample(hw ? Math.max(wy, sy) : sy, colorForPreviewKind(kind, sy, z, depth), hw, depth);
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static int colorForPreviewKind(String kind, int surfaceY, int z, int waterDepth) {
        return switch (kind) {
            case "OCEAN", "LAKE", "RIVER" -> colorForElevationAndLat(surfaceY, z, true, Math.max(1, waterDepth));
            case "ICE" -> 0xFFEAF2F8;
            case "BEACH" -> 0xFFD8C792;
            case "DESERT" -> 0xFFD9C27E;
            case "FOREST" -> 0xFF4C7E3E;
            case "WETLAND" -> 0xFF4D7553;
            case "VOLCANIC" -> 0xFF5A524E;
            case "ROCK" -> surfaceY >= 260 ? 0xFFECEFF4 : 0xFF8B857C;
            default -> colorForElevationAndLat(surfaceY, z, false, 0);
        };
    }

    public static TerrainSample scanLoadedColumn(Level level, ChunkAccess chunk, int worldX, int worldZ) {
        int lx = worldX & 15;
        int lz = worldZ & 15;
        int topY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(worldX, topY, worldZ);
        BlockState state = chunk.getBlockState(pos);
        if (state.isAir() && topY > level.getMinY()) {
            pos.setY(topY - 1);
            state = chunk.getBlockState(pos);
        }
        int waterDepth = 0;
        boolean isWater = !state.getFluidState().isEmpty();
        if (isWater) {
            int scanY = pos.getY();
            int minY = Math.max(level.getMinY(), scanY - 64);
            while (scanY > minY && !chunk.getBlockState(pos.setY(scanY)).getFluidState().isEmpty()) {
                waterDepth++;
                scanY--;
            }
        }
        MapColor mapColor = state.getMapColor(level, pos);
        int argb = mapColor != MapColor.NONE
                ? mapColor.calculateARGBColor(MapColor.Brightness.NORMAL)
                : colorForElevationAndLat(topY, worldZ, isWater, waterDepth);
        return new TerrainSample(topY, argb | 0xFF000000, isWater, waterDepth);
    }

    public static TerrainSample proceduralFallbackSample(long worldSeed, int blockX, int blockZ, boolean wrap) {
        int x = wrap ? PlanetProjection.wrapBlockX(blockX) : blockX;
        double nx = wrap
                ? Math.cos(((double) x / PlanetProjection.WORLD_WIDTH) * Math.PI * 2.0) * 2.2
                : x / 1024.0;
        double ny = wrap
                ? Math.sin(((double) x / PlanetProjection.WORLD_WIDTH) * Math.PI * 2.0) * 2.2
                : x / 2048.0;
        double nz = blockZ / 1024.0;
        long s = DeterministicWarpField.mix64(worldSeed ^ 0xA71A5123456789ABL);
        double phase = ((s >>> 16) & 0xFFFF) * 0.001;
        double continent = Math.sin(nx * 1.7 + phase) * Math.cos(nz * 1.9 - phase)
                + 0.5 * Math.sin(ny * 3.3 + nz * 2.7 + phase * 1.7);
        int elev = (int) Math.round(SEA_LEVEL + continent * 58.0);
        boolean water = elev < SEA_LEVEL;
        int depth = water ? Math.min(64, SEA_LEVEL - elev) : 0;
        int surfaceY = water ? SEA_LEVEL : elev;
        return new TerrainSample(surfaceY, colorForElevationAndLat(elev, blockZ, water, depth), water, depth);
    }

    public static int colorForElevationAndLat(int rawElevation, int blockZ, boolean water, int waterDepth) {
        if (PlanetProjection.isPolar(blockZ)) {
            return water ? 0xFF8FB8D8 : 0xFFEAF2F8;
        }
        if (water) {
            if (waterDepth <= 3) {
                return 0xFF4FA4C8;
            }
            if (waterDepth <= 12) {
                return 0xFF2E7BA6;
            }
            if (waterDepth <= 30) {
                return 0xFF1E5780;
            }
            return 0xFF143B5C;
        }
        double absLat = Math.abs(PlanetProjection.latDeg(blockZ));
        if (rawElevation >= 260) {
            return 0xFFECEFF4;
        }
        if (rawElevation >= 165) {
            return 0xFF8B857C;
        }
        if (rawElevation <= SEA_LEVEL + 3) {
            return 0xFFD8C792;
        }
        if (absLat > 66.0) {
            return 0xFFB8C9C0;
        }
        if (absLat < 24.0) {
            return 0xFF568B46;
        }
        return 0xFF6B9B52;
    }
}