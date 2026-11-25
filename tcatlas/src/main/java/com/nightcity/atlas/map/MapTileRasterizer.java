package com.nightcity.atlas.map;

import com.nightcity.atlas.api.ChunkKnowledge;
import com.nightcity.atlas.api.KnowledgeHolder;
import com.nightcity.atlas.api.SurveyLevel;
import com.nightcity.atlas.compat.wildlands.WildlandsTerrainBridge;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.UUID;

/**
 * High-throughput CPU tile stylizer & rasterizer producing 256x256 ARGB pixels (22 §5.1, §5.3, §5.4, §5.9, §5.11).
 * Uses 17x17 control-grid warp interpolation so each 256x256 tile builds in ~0.15-0.35 ms.
 */
public final class MapTileRasterizer {
    public static final int TILE_SIZE = PlanetProjection.TILE_PIXELS; // 256
    public static final int PIXEL_COUNT = TILE_SIZE * TILE_SIZE;      // 65,536

    public static final int UNEXPLORED_MEMORY_BG = 0xFF1E1B18;
    public static final int UNEXPLORED_PARCHMENT_BG = 0xFF2B241D;
    public static final int POLAR_UNEXPLORED_BG = 0xFF252D36;

    public record RenderParams(
            Identifier dimension,
            @Nullable UUID viewerId,
            long holderSalt,
            int zoom,
            int tileX,
            int tileZ,
            boolean wrapEastWest,
            boolean mapWarpEnabled,
            boolean revealAllCreative,
            long nowGameSeconds,
            int currentDay,
            int tier,
            int explorationSkillLevel
    ) {
    }

    private MapTileRasterizer() {
    }

    public static int[] rasterize(SurveyAtlasStore store, RenderParams params) {
        int[] pixels = new int[PIXEL_COUNT];
        int zoom = Math.clamp(params.zoom(), PlanetProjection.MIN_ZOOM, PlanetProjection.MAX_ZOOM);
        boolean wrapX = params.wrapEastWest();
        boolean clampZ = wrapX;
        int bpp = PlanetProjection.blocksPerPixel(zoom);
        int originX = PlanetProjection.tileOriginBlockX(params.tileX(), zoom, wrapX);
        int originZ = PlanetProjection.tileOriginBlockZ(params.tileZ(), zoom, clampZ);
        int bgBase = params.tier() <= 0 ? UNEXPLORED_MEMORY_BG : UNEXPLORED_PARCHMENT_BG;

        DeterministicWarpField warpField = params.mapWarpEnabled()
                ? new DeterministicWarpField(store.worldSeed(), params.holderSalt() != 0 ? params.holderSalt() : KnowledgeHolder.PLAYER_CIVILIZATION.seedSalt(), 0L, wrapX)
                : null;

        DeterministicWarpField.ChunkEpsilonLookup epsLookup = (cx, cz) -> {
            SurveyAtlasStore.ResolvedChunk rc = store.resolveChunk(
                    params.viewerId(), params.dimension(), cx, cz,
                    params.nowGameSeconds(), params.explorationSkillLevel(), wrapX
            );
            return rc.effectiveEpsilon();
        };

        // Precompute 17x17 inverse-warp displacement grid (16px cells) for sub-millisecond tile generation
        final int gridN = 17;
        double[] dispX = new double[gridN * gridN];
        double[] dispZ = new double[gridN * gridN];
        if (warpField != null && !params.revealAllCreative() && zoom <= 5) {
            for (int gz = 0; gz < gridN; gz++) {
                int qz = originZ + (gz << 4) * bpp;
                for (int gx = 0; gx < gridN; gx++) {
                    int rawQx = originX + (gx << 4) * bpp;
                    int qx = wrapX ? PlanetProjection.wrapBlockX(rawQx) : rawQx;
                    DeterministicWarpField.Vec2d inv = warpField.warpInverse(qx, qz, epsLookup);
                    double dx = wrapX ? PlanetProjection.wrappedDx(qx, inv.x()) : (inv.x() - qx);
                    double dz = inv.z() - qz;
                    int gi = gz * gridN + gx;
                    dispX[gi] = dx;
                    dispZ[gi] = dz;
                }
            }
        }

        byte[] memoryBlurMask = new byte[PIXEL_COUNT];
        boolean anyMemoryBlur = false;
        int[] heights = (params.tier() >= 6 && zoom <= 4) ? new int[PIXEL_COUNT] : null;
        boolean[] heightKnownMask = heights != null ? new boolean[PIXEL_COUNT] : null;

        int cachedChunkX = Integer.MIN_VALUE;
        int cachedChunkZ = Integer.MIN_VALUE;
        SurveyAtlasStore.ResolvedChunk cachedResolved = null;
        ChunkTerrainSummary cachedSummary = null;

        for (int pz = 0; pz < TILE_SIZE; pz++) {
            int rawWorldZ = originZ + pz * bpp;
            if (clampZ && (rawWorldZ < -PlanetProjection.Z_LIMIT || rawWorldZ >= PlanetProjection.Z_LIMIT)) {
                Arrays.fill(pixels, pz * TILE_SIZE, (pz + 1) * TILE_SIZE, 0xFF14181E);
                continue;
            }
            boolean polar = PlanetProjection.isPolar(rawWorldZ);
            int gz0 = pz >>> 4;
            double tz = (pz & 15) / 16.0;

            for (int px = 0; px < TILE_SIZE; px++) {
                int rawWorldX = originX + px * bpp;
                if (wrapX && zoom >= 8 && rawWorldX >= PlanetProjection.HALF_WIDTH) {
                    // Beyond single planet strip inside L8/L9 tile: wrap seamlessly
                    rawWorldX = PlanetProjection.wrapBlockX(rawWorldX);
                }

                double sampleX = rawWorldX;
                double sampleZ = rawWorldZ;
                if (warpField != null && !params.revealAllCreative() && zoom <= 5) {
                    int gx0 = px >>> 4;
                    double tx = (px & 15) / 16.0;
                    int i00 = gz0 * gridN + gx0;
                    double wx0 = dispX[i00] + (dispX[i00 + 1] - dispX[i00]) * tx;
                    double wx1 = dispX[i00 + gridN] + (dispX[i00 + gridN + 1] - dispX[i00 + gridN]) * tx;
                    double wz0 = dispZ[i00] + (dispZ[i00 + 1] - dispZ[i00]) * tx;
                    double wz1 = dispZ[i00 + gridN] + (dispZ[i00 + gridN + 1] - dispZ[i00 + gridN]) * tx;
                    sampleX = rawWorldX + (wx0 + (wx1 - wx0) * tz);
                    sampleZ = rawWorldZ + (wz0 + (wz1 - wz0) * tz);
                }

                int bx = wrapX ? PlanetProjection.wrapBlockX((int) Math.floor(sampleX)) : (int) Math.floor(sampleX);
                int bz = clampZ ? PlanetProjection.clampBlockZ((int) Math.floor(sampleZ)) : (int) Math.floor(sampleZ);
                int cx = bx >> 4;
                int cz = bz >> 4;

                if (cx != cachedChunkX || cz != cachedChunkZ || cachedResolved == null) {
                    cachedChunkX = cx;
                    cachedChunkZ = cz;
                    cachedResolved = params.revealAllCreative()
                            ? new SurveyAtlasStore.ResolvedChunk(SurveyLevel.PRECISE, new ChunkKnowledge(SurveyLevel.PRECISE, true, 0, params.currentDay()), 1.0, false)
                            : store.resolveChunk(params.viewerId(), params.dimension(), cx, cz, params.nowGameSeconds(), params.explorationSkillLevel(), wrapX);
                    cachedSummary = null;
                }

                int outIdx = (pz << 8) | px;
                SurveyLevel sl = cachedResolved.effectiveSl();
                if (sl == SurveyLevel.UNKNOWN) {
                    int bg = polar ? POLAR_UNEXPLORED_BG : bgBase;
                    if (((px ^ pz) & 15) == 0) {
                        bg = blendArgb(bg, 0xFF3A3128, 0.25f);
                    }
                    pixels[outIdx] = bg;
                    continue;
                }

                int baseColor;
                int surfaceY;
                if (zoom >= 3 || sl == SurveyLevel.RUMOR) {
                    int coarseStep = sl == SurveyLevel.RUMOR ? 64 : Math.max(16, bpp);
                    int qbx = Math.floorDiv(bx, coarseStep) * coarseStep;
                    int qbz = Math.floorDiv(bz, coarseStep) * coarseStep;
                    WildlandsTerrainBridge.TerrainSample ts = WildlandsTerrainBridge.sampleTerrain(null, store.worldSeed(), qbx, qbz);
                    baseColor = ts.baseArgb();
                    surfaceY = ts.surfaceY();
                } else {
                    if (cachedSummary == null) {
                        cachedSummary = store.getOrCreateSummary(params.dimension(), cx, cz, wrapX);
                    }
                    int lx = bx & 15;
                    int lz = bz & 15;
                    baseColor = cachedSummary.argb(lx, lz);
                    surfaceY = cachedSummary.surfaceY(lx, lz);
                }

                if (polar) {
                    baseColor = blendArgb(baseColor, 0xFFEAF3FA, 0.55f);
                }

                int styled = stylePixel(
                        baseColor, sl, cachedResolved.archiveKnowledge(), cachedResolved.memoryStrength(),
                        px, pz, bgBase, params.currentDay()
                );
                pixels[outIdx] = styled;

                if (sl == SurveyLevel.MEMORY) {
                    int radius = (int) Math.round(MemoryDecayModel.blurRadiusPixels(cachedResolved.memoryStrength()));
                    if (radius > 0) {
                        memoryBlurMask[outIdx] = (byte) Math.min(4, radius);
                        anyMemoryBlur = true;
                    }
                }
                if (heights != null) {
                    heights[outIdx] = surfaceY;
                    heightKnownMask[outIdx] = cachedResolved.archiveKnowledge().heightKnown() || params.revealAllCreative();
                }
            }
        }

        if (anyMemoryBlur) {
            applyCharcoalMemoryBlur(pixels, memoryBlurMask);
        }
        if (heights != null) {
            overlayContours(pixels, heights, heightKnownMask, bpp);
        }
        return pixels;
    }

    private static int stylePixel(
            int terrainArgb,
            SurveyLevel sl,
            ChunkKnowledge archive,
            double memoryStrength,
            int px,
            int pz,
            int unexploredBg,
            int currentDay
    ) {
        return switch (sl) {
            case UNKNOWN -> unexploredBg;
            case RUMOR -> {
                int muted = desaturate(terrainArgb, 0.55f);
                int parchment = blendArgb(muted, 0xFF7B6950, 0.35f);
                yield ((px + pz) & 7) == 0 ? blendArgb(parchment, 0xFF3E3326, 0.5f) : parchment;
            }
            case MEMORY -> {
                int charcoal = toCharcoalTone(terrainArgb);
                float alpha = MemoryDecayModel.opacity(memoryStrength);
                yield alpha >= 0.999f ? charcoal : blendArgb(unexploredBg, charcoal, alpha);
            }
            case SKETCH -> {
                int hideTint = blendArgb(desaturate(terrainArgb, 0.25f), 0xFFC8B28B, 0.18f);
                double desat = archive.freshnessDesaturation(currentDay);
                yield desat > 0.0 ? desaturate(hideTint, (float) desat) : hideTint;
            }
            case SURVEYED, PRECISE -> {
                double desat = archive.freshnessDesaturation(currentDay);
                yield desat > 0.0 ? desaturate(terrainArgb, (float) desat) : (terrainArgb | 0xFF000000);
            }
        };
    }

    private static int toCharcoalTone(int argb) {
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        int lum = (r * 77 + g * 150 + b * 29) >>> 8;
        int cr = Math.clamp((lum * 215) >>> 8, 28, 220);
        int cg = Math.clamp((lum * 198) >>> 8, 24, 204);
        int cb = Math.clamp((lum * 170) >>> 8, 20, 180);
        return 0xFF000000 | (cr << 16) | (cg << 8) | cb;
    }

    private static int desaturate(int argb, float amount) {
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        int lum = (r * 77 + g * 150 + b * 29) >>> 8;
        int nr = Math.round(r + (lum - r) * amount);
        int ng = Math.round(g + (lum - g) * amount);
        int nb = Math.round(b + (lum - b) * amount);
        return 0xFF000000 | (nr << 16) | (ng << 8) | nb;
    }

    public static int blendArgb(int dst, int src, float alpha) {
        float a = Math.clamp(alpha, 0.0f, 1.0f);
        int dr = (dst >>> 16) & 0xFF;
        int dg = (dst >>> 8) & 0xFF;
        int db = dst & 0xFF;
        int sr = (src >>> 16) & 0xFF;
        int sg = (src >>> 8) & 0xFF;
        int sb = src & 0xFF;
        int r = Math.round(dr + (sr - dr) * a);
        int g = Math.round(dg + (sg - dg) * a);
        int b = Math.round(db + (sb - db) * a);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static void applyCharcoalMemoryBlur(int[] pixels, byte[] blurMask) {
        int[] copy = pixels.clone();
        for (int z = 1; z < TILE_SIZE - 1; z++) {
            int row = z << 8;
            for (int x = 1; x < TILE_SIZE - 1; x++) {
                int idx = row | x;
                int rMask = blurMask[idx];
                if (rMask <= 0) {
                    continue;
                }
                int step = rMask >= 3 ? 2 : 1;
                int x0 = Math.max(0, x - step);
                int x1 = Math.min(TILE_SIZE - 1, x + step);
                int z0 = Math.max(0, z - step);
                int z1 = Math.min(TILE_SIZE - 1, z + step);
                int c0 = copy[idx];
                int c1 = copy[(z0 << 8) | x];
                int c2 = copy[(z1 << 8) | x];
                int c3 = copy[row | x0];
                int c4 = copy[row | x1];
                int r = (((c0 >>> 16) & 0xFF) * 2 + ((c1 >>> 16) & 0xFF) + ((c2 >>> 16) & 0xFF) + ((c3 >>> 16) & 0xFF) + ((c4 >>> 16) & 0xFF)) / 6;
                int g = (((c0 >>> 8) & 0xFF) * 2 + ((c1 >>> 8) & 0xFF) + ((c2 >>> 8) & 0xFF) + ((c3 >>> 8) & 0xFF) + ((c4 >>> 8) & 0xFF)) / 6;
                int b = ((c0 & 0xFF) * 2 + (c1 & 0xFF) + (c2 & 0xFF) + (c3 & 0xFF) + (c4 & 0xFF)) / 6;
                pixels[idx] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }
    }

    private static void overlayContours(int[] pixels, int[] heights, boolean[] known, int bpp) {
        int interval = bpp <= 2 ? 8 : (bpp <= 8 ? 16 : 32);
        for (int z = 0; z < TILE_SIZE - 1; z++) {
            int row = z << 8;
            for (int x = 0; x < TILE_SIZE - 1; x++) {
                int idx = row | x;
                if (!known[idx] || heights[idx] <= WildlandsTerrainBridge.SEA_LEVEL) {
                    continue;
                }
                int band = Math.floorDiv(heights[idx] - WildlandsTerrainBridge.SEA_LEVEL, interval);
                int bandE = Math.floorDiv(heights[idx + 1] - WildlandsTerrainBridge.SEA_LEVEL, interval);
                int bandS = Math.floorDiv(heights[idx + TILE_SIZE] - WildlandsTerrainBridge.SEA_LEVEL, interval);
                if (band != bandE || band != bandS) {
                    boolean indexLine = (band % 5) == 0;
                    pixels[idx] = blendArgb(pixels[idx], 0xFF5C4228, indexLine ? 0.65f : 0.35f);
                }
            }
        }
    }
}
