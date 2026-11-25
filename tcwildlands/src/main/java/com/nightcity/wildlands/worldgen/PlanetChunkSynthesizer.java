package com.nightcity.wildlands.worldgen;

import com.nightcity.tcivilization.RegionCoordinates;
import com.nightcity.tcivilization.api.world.Deposit;
import com.nightcity.tcivilization.api.world.GeoProvince;
import com.nightcity.tcivilization.api.world.HistoricalTrace;
import com.nightcity.tcivilization.api.world.StrataColumn;
import com.nightcity.wildlands.block.OreGrade;
import com.nightcity.wildlands.planet.PeriodicNoise;
import com.nightcity.wildlands.planet.PlanetCoordinates;
import com.nightcity.wildlands.planet.PlanetModel;
import com.nightcity.wildlands.planet.WildlandsCatalog;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;

/**
 * Pure, thread-safe, chunk-boundary-independent L2 chunk and column synthesizer (10 §5.9-5.11, VO-WG-01..08).
 * Produces deterministic block IDs and ore grades across Y = -512..1023 (height 1536) with:
 * - Zero vanilla stone, deepslate, or vanilla ores
 * - Seamless east-west periodicity and ±256-block mirror buffer at x = ±81,920
 * - Strictly legal 87 (host, mineral) ore combinations + 3 coal seams + supergene oxidation + caves.
 */
public final class PlanetChunkSynthesizer {
    public static final int MIN_Y = PlanetCoordinates.Y_MIN; // -512
    public static final int MAX_Y_INCLUSIVE = PlanetCoordinates.Y_MAX - 1; // 1023
    public static final int HEIGHT = PlanetCoordinates.Y_MAX - PlanetCoordinates.Y_MIN; // 1536

    private static final Identifier AIR = WildlandsCatalog.mc("air");
    private static final Identifier BEDROCK = WildlandsCatalog.mc("bedrock");
    private static final Identifier WATER = WildlandsCatalog.mc("water");
    private static final Identifier ICE = WildlandsCatalog.mc("ice");
    private static final Identifier PACKED_ICE = WildlandsCatalog.mc("packed_ice");
    private static final Identifier DRIPSTONE_BLOCK = WildlandsCatalog.mc("dripstone_block");
    private static final Identifier BRINE = WildlandsCatalog.wl("brine");
    private static final Identifier DEEP_THRESHOLD = WildlandsCatalog.wl("deep_threshold");
    private static final Identifier GOSSAN = WildlandsCatalog.wl("gossan");
    private static final Identifier LOOSE_STONES = WildlandsCatalog.wl("loose_stones");
    private static final Identifier ORE_FLOAT = WildlandsCatalog.wl("ore_float");
    private static final Identifier VOLCANIC_ASH = WildlandsCatalog.wl("volcanic_ash");
    private static final Identifier FLOOD_SILT = WildlandsCatalog.wl("flood_silt");

    private static final Map<Long, Float> ANCIENT_MINED_BY_DEPOSIT = new ConcurrentHashMap<>();
    private static final Map<Long, List<HistoricalTrace>> TRACES_BY_REGION = new ConcurrentHashMap<>();

    public record ColumnProfile(
            int x,
            int z,
            int surfaceY,
            int waterY,
            int slopeBlocks,
            Identifier biomeId,
            Identifier topBlockId,
            Identifier underBlockId,
            Identifier parentSoilId,
            StrataColumn strata
    ) {
    }

    public record SynthesizedChunk(
            int chunkX,
            int chunkZ,
            Identifier[] blocks, // 16 * 16 * HEIGHT
            byte[] grades,       // 0 = n/a, 1 = poor, 2 = normal, 3 = rich
            int[] surfaceY,      // 16 * 16
            int[] waterY,        // 16 * 16
            Identifier[] biomes  // 16 * 16
    ) {
        public static int index(int localX, int localZ, int y) {
            return ((localZ * 16 + localX) * HEIGHT) + (y - MIN_Y);
        }

        public Identifier blockAt(int localX, int y, int localZ) {
            return blocks[index(localX, localZ, y)];
        }

        public byte gradeAt(int localX, int y, int localZ) {
            return grades[index(localX, localZ, y)];
        }
    }

    private PlanetChunkSynthesizer() {
    }

    public static void recordAncientMining(long depositId, float fraction) {
        float clamped = Math.clamp(fraction, 0.0f, 0.8f);
        ANCIENT_MINED_BY_DEPOSIT.merge(depositId, clamped, Math::max);
    }

    public static float ancientMinedFraction(long depositId) {
        return ANCIENT_MINED_BY_DEPOSIT.getOrDefault(depositId, 0.0f);
    }

    public static void addHistoricalTrace(long regionKey, HistoricalTrace trace) {
        TRACES_BY_REGION.compute(regionKey, (k, existing) -> {
            java.util.ArrayList<HistoricalTrace> next = existing == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(existing);
            if (next.size() < 64) {
                next.add(trace);
            }
            return List.copyOf(next);
        });
    }

    public static void clearTracesForTests() {
        ANCIENT_MINED_BY_DEPOSIT.clear();
        TRACES_BY_REGION.clear();
    }

    /**
     * Computes raw pre-slope-limit surface Y at world block (x, z), seamlessly periodic in X.
     */
    public static int rawDetailSurfaceY(PlanetModel model, int x, int z) {
        int wx = PlanetCoordinates.wrapX(x);
        int cz = PlanetCoordinates.clampZ(z);
        int baseL1Y = model.sampleL1SurfaceY(wx, cz);
        float reliefM = model.sampleBilinearReliefMeters(wx, cz);
        double fbm = PeriodicNoise.sphericalStretchedFbm2D(model.worldSeed() ^ 0x1234_5678L, wx, cz, 4, 128, 0.45);
        double ridge = PeriodicNoise.sphericalStretchedRidge2D(model.worldSeed() ^ 0x8765_4321L, wx, cz, 2, 96);
        double detailBlocks = fbm * Math.min(18.0, reliefM * 0.08) + (ridge - 0.5) * Math.min(10.0, reliefM * 0.04);

        int waterY = model.sampleWaterSurfaceY(wx, cz);
        if (waterY != Integer.MIN_VALUE && baseL1Y <= waterY) {
            // Preserve carved riverbeds and smooth ocean floors
            detailBlocks *= 0.25;
        }
        return Math.clamp((int) Math.round(baseL1Y + detailBlocks), MIN_Y + 8, MAX_Y_INCLUSIVE - 8);
    }

    /**
     * Computes slope-limited surface Y (10 §5.9.5) as a pure function of (x, z) so chunk boundaries and the
     * x = ±81,920 wrap seam never exhibit discontinuities.
     */
    public static int computeSurfaceY(PlanetModel model, int x, int z, int maxStep) {
        int wx = PlanetCoordinates.wrapX(x);
        int cz = PlanetCoordinates.clampZ(z);
        int center = rawDetailSurfaceY(model, wx, cz);

        WildlandsCatalog.BiomeSpec bSpec = model.biomeSpecAt(wx, cz);
        boolean allowCliff = bSpec.index() == 15 || bSpec.index() == 42 || bSpec.index() == 59 || bSpec.index() == 60 || Math.abs(cz) >= 40_700;
        if (allowCliff) {
            return center;
        }

        int n = rawDetailSurfaceY(model, wx, cz - 1);
        int s = rawDetailSurfaceY(model, wx, cz + 1);
        int w = rawDetailSurfaceY(model, wx - 1, cz);
        int e = rawDetailSurfaceY(model, wx + 1, cz);
        int minNeighbor = Math.min(Math.min(n, s), Math.min(w, e));
        int stepCap = Math.max(2, maxStep);
        int y = Math.min(center, minNeighbor + stepCap);

        // Apply FAULT_SCARP historical trace if present
        long rk = RegionCoordinates.fromBlock(wx, cz).key();
        List<HistoricalTrace> traces = TRACES_BY_REGION.get(rk);
        if (traces != null) {
            for (HistoricalTrace tr : traces) {
                if (tr.kind() == HistoricalTrace.Kind.FAULT_SCARP) {
                    int dx = PlanetCoordinates.wrappedDx(tr.pos().getX(), wx);
                    int dz = cz - tr.pos().getZ();
                    if (Math.abs(dx) <= tr.radius() && Math.abs(dz) <= tr.radius() && dx > 0) {
                        y += 3;
                    }
                }
            }
        }
        return Math.clamp(y, MIN_Y + 8, MAX_Y_INCLUSIVE - 4);
    }

    public static ColumnProfile profileColumn(PlanetModel model, int x, int z, int maxStep) {
        int wx = PlanetCoordinates.wrapX(x);
        int cz = PlanetCoordinates.clampZ(z);
        int surfY = computeSurfaceY(model, wx, cz, maxStep);
        int eY = computeSurfaceY(model, wx + 1, cz, maxStep);
        int sY = computeSurfaceY(model, wx, cz + 1, maxStep);
        int slope = Math.max(Math.abs(surfY - eY), Math.abs(surfY - sY));

        int wY = model.sampleWaterSurfaceY(wx, cz);
        WildlandsCatalog.BiomeSpec biome = model.biomeSpecAt(wx, cz);
        StrataColumn strata = model.strataAt(wx, cz);

        Identifier top = biome.topBlock();
        Identifier under = biome.underBlock();
        Identifier parent = biome.parentSoil();
        if (slope >= 2 && surfY > PlanetCoordinates.SEA_LEVEL + 2 && biome.index() != 55 && biome.index() != 60) {
            Identifier bedrock = strata.rockAtY(surfY);
            top = bedrock;
            under = bedrock;
        }
        return new ColumnProfile(wx, cz, surfY, wY, slope, biome.id(), top, under, parent, strata);
    }

    public static SynthesizedChunk synthesizeChunk(PlanetModel model, int chunkX, int chunkZ) {
        return synthesizeChunk(model, chunkX, chunkZ, 3);
    }

    public static SynthesizedChunk synthesizeChunk(PlanetModel model, int chunkX, int chunkZ, int maxStep) {
        Identifier[] blocks = new Identifier[16 * 16 * HEIGHT];
        byte[] grades = new byte[16 * 16 * HEIGHT];
        int[] surfaceY = new int[256];
        int[] waterY = new int[256];
        Identifier[] biomes = new Identifier[256];

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int centerWx = PlanetCoordinates.wrapX(baseX + 8);
        int centerCz = PlanetCoordinates.clampZ(baseZ + 8);
        int rx = RegionCoordinates.regionX(centerWx);
        int rz = RegionCoordinates.regionZ(centerCz);
        PlanetModel.L1Tile l1Tile = model.getOrComputeL1Tile(rx, rz);
        List<Deposit> candidateDeposits = l1Tile.nearbyDeposits();
        long rk = RegionCoordinates.key(rx, rz);
        List<HistoricalTrace> traces = TRACES_BY_REGION.getOrDefault(rk, List.of());

        boolean chunkMayHaveConduit = false;
        for (PlanetModel.DeepConduitInfo dc : model.deepConduits()) {
            if (Math.abs(PlanetCoordinates.wrappedDx(dc.blockX(), centerWx)) <= 16 && Math.abs(dc.blockZ() - centerCz) <= 16) {
                chunkMayHaveConduit = true;
                break;
            }
        }

        ColumnProfile[] profiles = new ColumnProfile[256];
        int maxSurfInChunk = MIN_Y + 16;
        for (int lz = 0; lz < 16; lz++) {
            int cz = PlanetCoordinates.clampZ(baseZ + lz);
            for (int lx = 0; lx < 16; lx++) {
                int wx = PlanetCoordinates.wrapX(baseX + lx);
                int col2d = lz * 16 + lx;
                ColumnProfile prof = profileColumn(model, wx, cz, maxStep);
                profiles[col2d] = prof;
                surfaceY[col2d] = prof.surfaceY();
                waterY[col2d] = prof.waterY();
                biomes[col2d] = prof.biomeId();
                if (prof.surfaceY() > maxSurfInChunk) {
                    maxSurfInChunk = prof.surfaceY();
                }
            }
        }

        // World-aligned 4x8x4 trilinear subgrid for lithology-coupled 3D cave fields (seamless across chunks and ±81,920)
        int caveTopY = Math.max(MIN_Y + 24, maxSurfInChunk);
        int gyCount = ((caveTopY - MIN_Y) >> 3) + 2;
        float[] caveGrid1 = new float[5 * 5 * gyCount];
        float[] caveGrid2 = new float[5 * 5 * gyCount];
        long caveSeed1 = model.worldSeed() ^ 0xCA7E_1111L;
        long caveSeed2 = model.worldSeed() ^ 0xCA7E_2222L;
        for (int gz = 0; gz < 5; gz++) {
            int scz = PlanetCoordinates.clampZ(baseZ + (gz << 2));
            for (int gx = 0; gx < 5; gx++) {
                int swx = PlanetCoordinates.wrapX(baseX + (gx << 2));
                int cellBase = (gz * 5 + gx) * gyCount;
                for (int gy = 0; gy < gyCount; gy++) {
                    int sy = MIN_Y + (gy << 3);
                    caveGrid1[cellBase + gy] = (float) PeriodicNoise.sample3D(caveSeed1, swx, sy, scz, 96, 32);
                    caveGrid2[cellBase + gy] = (float) PeriodicNoise.sample3D(caveSeed2, swx, sy, scz, 64, 24);
                }
            }
        }

        for (int lz = 0; lz < 16; lz++) {
            int worldZ = baseZ + lz;
            int cz = PlanetCoordinates.clampZ(worldZ);
            int gz0 = lz >> 2;
            float fz = (lz & 3) * 0.25f;
            for (int lx = 0; lx < 16; lx++) {
                int worldX = baseX + lx;
                int wx = PlanetCoordinates.wrapX(worldX);
                int col2d = lz * 16 + lx;
                int gx0 = lx >> 2;
                float fx = (lx & 3) * 0.25f;
                int c00 = (gz0 * 5 + gx0) * gyCount;
                int c10 = (gz0 * 5 + (gx0 + 1)) * gyCount;
                int c01 = ((gz0 + 1) * 5 + gx0) * gyCount;
                int c11 = ((gz0 + 1) * 5 + (gx0 + 1)) * gyCount;

                ColumnProfile prof = profiles[col2d];
                boolean hasDeepConduit = chunkMayHaveConduit && isDeepConduitColumn(model, wx, cz);
                boolean isSaltLake = prof.biomeId().getPath().equals("arid_salt_lake");
                boolean submerged = prof.waterY() != Integer.MIN_VALUE && prof.waterY() > prof.surfaceY();

                // Fold offset for stratigraphy
                int foldBlocks = (int) Math.round(PeriodicNoise.sample3D(model.worldSeed() ^ 0xF01DL, wx, 0, cz, 256, 64) *
                        ((prof.strata().province() == GeoProvince.YOUNG_OROGEN || prof.strata().province() == GeoProvince.OLD_OROGEN) ? 18.0 : 4.0));

                int colBase = (lz * 16 + lx) * HEIGHT;
                int topSolidY = Math.max(prof.surfaceY(), submerged ? prof.waterY() : prof.surfaceY());

                // Fill air above topSolidY
                for (int y = topSolidY + 1; y <= MAX_Y_INCLUSIVE; y++) {
                    blocks[colBase + (y - MIN_Y)] = AIR;
                }

                // Fill water/brine/ice column if submerged
                if (submerged) {
                    Identifier fluid = isSaltLake ? BRINE : WATER;
                    boolean seaIce = prof.biomeId().getPath().equals("polar_sea_ice");
                    for (int y = prof.surfaceY() + 1; y <= prof.waterY(); y++) {
                        blocks[colBase + (y - MIN_Y)] = (seaIce && y == prof.waterY()) ? ICE : fluid;
                    }
                }

                // Bedrock base (-512..-506)
                for (int y = MIN_Y; y <= MIN_Y + 6; y++) {
                    if (hasDeepConduit && y == -496) {
                        blocks[colBase + (y - MIN_Y)] = DEEP_THRESHOLD;
                    } else {
                        blocks[colBase + (y - MIN_Y)] = BEDROCK;
                    }
                }

                // Fill solid rock + soil + caves + ores from MIN_Y + 7 .. prof.surfaceY()
                for (int y = MIN_Y + 7; y <= prof.surfaceY(); y++) {
                    int bIdx = colBase + (y - MIN_Y);
                    if (hasDeepConduit && y <= -496 && y >= -505) {
                        blocks[bIdx] = y == -496 ? DEEP_THRESHOLD : WildlandsCatalog.wl("basalt_rock");
                        continue;
                    }

                    int depthBelowSurface = prof.surfaceY() - y;
                    Identifier block;
                    if (!submerged && depthBelowSurface == 0) {
                        block = prof.topBlockId();
                    } else if (depthBelowSurface <= 2) {
                        block = submerged ? prof.underBlockId() : prof.underBlockId();
                    } else if (depthBelowSurface <= 5 && prof.slopeBlocks() < 2) {
                        block = prof.parentSoilId();
                    } else {
                        block = prof.strata().rockAtY(y + foldBlocks);
                    }

                    // Lithology-coupled caves (10 §5.11.1, VO-WG-05)
                    if (depthBelowSurface > 6 && y > MIN_Y + 16 && y < prof.surfaceY() - 6) {
                        int relY = y - MIN_Y;
                        int gy0 = relY >> 3;
                        float fy = (relY & 7) * 0.125f;
                        double c1 = trilinear(caveGrid1, c00, c10, c01, c11, gy0, fx, fy, fz);
                        double c2 = trilinear(caveGrid2, c00, c10, c01, c11, gy0, fx, fy, fz);
                        String path = block.getPath();
                        boolean carbonate = path.equals("limestone") || path.equals("dolomite");
                        double threshold = carbonate ? 0.018 : 0.011;
                        if (c1 * c1 + c2 * c2 < threshold) {
                            blocks[bIdx] = (y < -120) ? WATER : (carbonate && depthBelowSurface == 7 ? DRIPSTONE_BLOCK : AIR);
                            continue;
                        }
                    }

                    blocks[bIdx] = block;
                }

                // Overlay mineral deposits intersecting this column (strictly legal 87 ore blocks + 3 coal seams)
                if (!candidateDeposits.isEmpty()) {
                    for (Deposit d : candidateDeposits) {
                        int dx = PlanetCoordinates.wrappedDx(d.center().getX(), wx);
                        if (Math.abs(dx) > d.radiusXZ()) continue;
                        int dz = cz - d.center().getZ();
                        if (Math.abs(dz) > d.radiusXZ()) continue;
                        double horizSq = (dx * dx + dz * dz) / (double) Math.max(1, d.radiusXZ() * d.radiusXZ());
                        if (horizSq >= 1.0) continue;

                        int minY = Math.max(MIN_Y + 8, d.center().getY() - d.radiusY());
                        int maxY = Math.min(prof.surfaceY(), d.center().getY() + d.radiusY());
                        float ancientMined = ancientMinedFraction(d.id());

                        for (int y = minY; y <= maxY; y++) {
                            int bIdx = colBase + (y - MIN_Y);
                            if (AIR.equals(blocks[bIdx]) || WATER.equals(blocks[bIdx]) || BRINE.equals(blocks[bIdx])) {
                                continue;
                            }
                            double dyNorm = (y - d.center().getY()) / (double) Math.max(1, d.radiusY());
                            double rSq = horizSq + dyNorm * dyNorm;
                            if (rSq >= 1.0) continue;

                            // Ancient workings backfill upper minedFraction with host cobble (10 §12.1)
                            if (ancientMined > 0.0f) {
                                double fromTopFrac = (maxY - y) / (double) Math.max(1, maxY - minY + 1);
                                if (fromTopFrac < ancientMined) {
                                    WildlandsCatalog.RockSpec hSpec = WildlandsCatalog.ROCK_BY_ID.get(d.hostRock());
                                    blocks[bIdx] = hSpec != null ? hSpec.cobbleId() : WildlandsCatalog.wl("granite_cobble");
                                    grades[bIdx] = 0;
                                    continue;
                                }
                            }

                            double oreNoise = 0.5 * (PeriodicNoise.sample3D(model.worldSeed() ^ d.id(), wx, y, cz, 24, 12) + 1.0);
                            float g = (float) (d.gradeMean() * Math.pow(1.0 - rSq, 0.7) * (0.75 + 0.5 * oreNoise));
                            if (g < 0.10f) {
                                continue;
                            }

                            // Supergene oxidation cap near surface for sulfide ores (10 §5.10.4)
                            int depth = prof.surfaceY() - y;
                            Identifier effectiveMineral = d.mineral();
                            if (depth == 0 && d.surfaceSigns().contains(Deposit.Sign.GOSSAN)) {
                                blocks[bIdx] = GOSSAN;
                                grades[bIdx] = 0;
                                continue;
                            } else if (depth <= 10) {
                                if (d.mineral().getPath().equals("chalcopyrite")) {
                                    effectiveMineral = WildlandsCatalog.wl("malachite");
                                } else if (d.mineral().getPath().equals("sphalerite")) {
                                    effectiveMineral = WildlandsCatalog.wl("calamine");
                                }
                            }

                            Identifier currentHost = blocks[bIdx];
                            Identifier oreBlock = WildlandsCatalog.resolveLegalOreBlock(effectiveMineral, currentHost);
                            OreGrade grade = OreGrade.fromGradeValue(g);
                            blocks[bIdx] = oreBlock;
                            grades[bIdx] = (byte) grade.rawDropCount();
                        }
                    }
                }

                // Apply ASH_LAYER / FLOOD_TERRACE traces if present
                for (HistoricalTrace tr : traces) {
                    int dx = PlanetCoordinates.wrappedDx(tr.pos().getX(), wx);
                    int dz = cz - tr.pos().getZ();
                    if (dx * dx + dz * dz <= tr.radius() * tr.radius() && !submerged) {
                        if (tr.kind() == HistoricalTrace.Kind.ASH_LAYER && prof.surfaceY() - 2 > MIN_Y + 10) {
                            blocks[colBase + (prof.surfaceY() - 2 - MIN_Y)] = VOLCANIC_ASH;
                        } else if (tr.kind() == HistoricalTrace.Kind.FLOOD_TERRACE && prof.surfaceY() <= PlanetCoordinates.SEA_LEVEL + 8) {
                            blocks[colBase + (prof.surfaceY() - MIN_Y)] = FLOOD_SILT;
                        }
                    }
                }

                // Surface loose_stones / ore_floatprospecting signs (deterministic hash)
                if (!submerged && prof.slopeBlocks() < 2 && prof.surfaceY() + 1 < MAX_Y_INCLUSIVE) {
                    long surfHash = PeriodicNoise.hash2D(model.worldSeed() ^ 0x5704_E500L, wx, cz);
                    if ((surfHash & 0x3FL) == 0L) {
                        blocks[colBase + (prof.surfaceY() + 1 - MIN_Y)] = !candidateDeposits.isEmpty() && ((surfHash >>> 8) & 3L) == 0L
                                ? ORE_FLOAT
                                : LOOSE_STONES;
                    }
                }
            }
        }
        return new SynthesizedChunk(chunkX, chunkZ, blocks, grades, surfaceY, waterY, biomes);
    }

    private static float trilinear(float[] grid, int c00, int c10, int c01, int c11, int gy0, float fx, float fy, float fz) {
        float v000 = grid[c00 + gy0];
        float v001 = grid[c00 + gy0 + 1];
        float v100 = grid[c10 + gy0];
        float v101 = grid[c10 + gy0 + 1];
        float v010 = grid[c01 + gy0];
        float v011 = grid[c01 + gy0 + 1];
        float v110 = grid[c11 + gy0];
        float v111 = grid[c11 + gy0 + 1];

        float c00y = v000 + (v001 - v000) * fy;
        float c10y = v100 + (v101 - v100) * fy;
        float c01y = v010 + (v011 - v010) * fy;
        float c11y = v110 + (v111 - v110) * fy;

        float c0z = c00y + (c10y - c00y) * fx;
        float c1z = c01y + (c11y - c01y) * fx;
        return c0z + (c1z - c0z) * fz;
    }

    private static boolean isDeepConduitColumn(PlanetModel model, int wx, int cz) {
        for (PlanetModel.DeepConduitInfo dc : model.deepConduits()) {
            if (PlanetCoordinates.wrappedDx(dc.blockX(), wx) == 0 && dc.blockZ() == cz) {
                return true;
            }
        }
        return false;
    }
}
