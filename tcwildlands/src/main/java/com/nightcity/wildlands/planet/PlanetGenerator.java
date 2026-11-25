package com.nightcity.wildlands.planet;

import com.nightcity.tcivilization.api.world.Deposit;
import com.nightcity.tcivilization.api.world.DepositType;
import com.nightcity.tcivilization.api.world.GeoProvince;
import com.nightcity.tcivilization.api.world.PlanetProgressStages;
import com.nightcity.tcivilization.api.world.Reservoir;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

/**
 * Complete deterministic P0-P8 planet precomputation pipeline (02 §6.1, §8.3; 10 §5.2-5.10, §5.18).
 * Parallel loops write strictly to disjoint row slices using pure functions of (seed, gx, gz);
 * sequential passes use deterministic canonical ordering so planet.bin is byte-for-byte identical.
 */
public final class PlanetGenerator {
    public static final Identifier STAGE_PLATES = WildlandsCatalog.wl("plates");
    public static final Identifier STAGE_OROGENY = WildlandsCatalog.wl("orogeny");
    public static final Identifier STAGE_EROSION = WildlandsCatalog.wl("erosion");
    public static final Identifier STAGE_HYDROLOGY = WildlandsCatalog.wl("hydrology");
    public static final Identifier STAGE_CLIMATE = WildlandsCatalog.wl("climate");
    public static final Identifier STAGE_BIOMES = WildlandsCatalog.wl("biomes");
    public static final Identifier STAGE_GEOLOGY = WildlandsCatalog.wl("geology");

    private static final int W = PlanetModel.GRID_W;      // 2560
    private static final int H = PlanetModel.GRID_H;      // 1280
    private static final int N = W * H;                   // 3,276,800
    private static final int CW = PlanetModel.CLIM_W;     // 1280
    private static final int CH = PlanetModel.CLIM_H;     // 640
    private static final int CN = CW * CH;                // 819,200
    private static final int MIN_CONTINENT_CELLS = 3_662; // ~15,000,000 sq blocks / 4096

    public record Config(double landTarget, int configuredPlateCount, int erosionSteps, int threads) {
        public static Config defaults() {
            return new Config(0.29, 0, 80, Math.max(1, Math.min(Runtime.getRuntime().availableProcessors() - 1, 8)));
        }
    }

    private record CratonBlob(double cx, double cy, double cz, double radiusRadX, double radiusRadZ, double angle, int plateId) {
    }

    private record TectonicAttempt(
            long subSeed,
            List<PlanetModel.PlateInfo> plates,
            List<PlanetModel.HotspotInfo> hotspots,
            List<PlanetModel.VolcanoInfo> volcanoes,
            List<PlanetModel.FaultInfo> faults,
            byte[] plate,
            byte[] crust,
            float[] heightM,
            List<PlanetModel.ContinentStat> continents,
            float oceanFraction,
            boolean valid
    ) {
    }

    private PlanetGenerator() {
    }

    public static List<PlanetProgressStages.StageInfo> builtinStages() {
        return List.of(
                new PlanetProgressStages.StageInfo(STAGE_PLATES, Component.translatable("gui.tcwildlands.progress.plates"), 100),
                new PlanetProgressStages.StageInfo(STAGE_OROGENY, Component.translatable("gui.tcwildlands.progress.orogeny"), 200),
                new PlanetProgressStages.StageInfo(STAGE_EROSION, Component.translatable("gui.tcwildlands.progress.erosion"), 300),
                new PlanetProgressStages.StageInfo(STAGE_HYDROLOGY, Component.translatable("gui.tcwildlands.progress.hydrology"), 400),
                new PlanetProgressStages.StageInfo(STAGE_CLIMATE, Component.translatable("gui.tcwildlands.progress.climate"), 500),
                new PlanetProgressStages.StageInfo(STAGE_BIOMES, Component.translatable("gui.tcwildlands.progress.biomes"), 600),
                new PlanetProgressStages.StageInfo(STAGE_GEOLOGY, Component.translatable("gui.tcwildlands.progress.geology"), 700)
        );
    }

    public static PlanetModel generate(long worldSeed, Config cfg, @Nullable MinecraftServer server) {
        int poolSize = Math.max(1, Math.min(cfg.threads(), 8));
        ForkJoinPool pool = new ForkJoinPool(poolSize);
        try {
            PlanetProgressStages.report(server, STAGE_PLATES, 0.1f, Optional.empty());

            // P0-P2: Plates, continental cratons, boundary orogeny, trenches, and continent validation (up to 8 attempts)
            TectonicAttempt chosen = null;
            for (int attempt = 0; attempt < 8; attempt++) {
                long subSeed = PeriodicNoise.mix64(worldSeed ^ (attempt * 0x9E3779B97F4A7C15L));
                TectonicAttempt candidate = buildTectonics(subSeed, cfg, pool);
                chosen = candidate;
                if (candidate.valid()) {
                    break;
                }
            }
            PlanetProgressStages.complete(server, STAGE_PLATES);
            PlanetProgressStages.report(server, STAGE_OROGENY, 1.0f, Optional.empty());
            PlanetProgressStages.complete(server, STAGE_OROGENY);

            // P3: Stream-power + thermal + sediment erosion
            PlanetProgressStages.report(server, STAGE_EROSION, 0.1f, Optional.empty());
            byte[] sediment = new byte[N];
            byte[] relief = new byte[N];
            runErosion(chosen.subSeed(), chosen.heightM(), sediment, relief, cfg.erosionSteps(), pool, server);

            // Calibrate post-erosion mountain peak (~800 Y), abyssal ocean (~-300 Y), and deepest trench (~-460 Y)
            calibrateVerticalExtremes(chosen.heightM());
            // Re-verify continents after erosion
            List<PlanetModel.ContinentStat> finalContinents = analyzeContinents(chosen.heightM(), false);
            int continentCount = Math.max(3, finalContinents.size());
            int oceanCells = 0;
            short[] elevation = new short[N];
            for (int i = 0; i < N; i++) {
                short em = (short) Math.clamp(Math.round(chosen.heightM()[i]), -11_000, 8_840);
                elevation[i] = em;
                if (em < 0) {
                    oceanCells++;
                }
            }
            float oceanFraction = oceanCells / (float) N;
            PlanetProgressStages.complete(server, STAGE_EROSION);

            // P4 & P5: Preliminary climate -> Priority-Flood hydrology -> full 12-month climate & Koppen
            PlanetProgressStages.report(server, STAGE_HYDROLOGY, 0.2f, Optional.empty());
            byte[] tempMonthly = new byte[CN * 12];
            byte[] precipMonthly = new byte[CN * 12];
            byte[] windSeasonal = new byte[CN * 8];
            byte[] currents = new byte[CN * 2];
            float[] annualRunoffMm = new float[N];

            computeClimate(chosen.subSeed(), elevation, tempMonthly, precipMonthly, windSeasonal, currents, annualRunoffMm, pool);

            byte[] flowDir = new byte[N];
            short[] flowAcc = new short[N];
            short[] basin = new short[N];
            short[] lake = new short[N];
            byte[] riverOrder = new byte[N];
            List<PlanetModel.LakeInfo> lakes = computeHydrology(elevation, annualRunoffMm, flowDir, flowAcc, basin, lake, riverOrder);
            PlanetProgressStages.complete(server, STAGE_HYDROLOGY);

            PlanetProgressStages.report(server, STAGE_CLIMATE, 0.6f, Optional.empty());
            byte[] koppen = new byte[N];
            computeKoppenL0(elevation, tempMonthly, precipMonthly, koppen, pool);
            PlanetProgressStages.complete(server, STAGE_CLIMATE);

            // P6: Biome assignment (64 surface biomes) + majority-vote smoothing
            PlanetProgressStages.report(server, STAGE_BIOMES, 0.3f, Optional.empty());
            byte[] province = new byte[N];
            byte[] rockTop = new byte[N];
            byte[] groundwater = new byte[N];
            computeGeologyL0(chosen.subSeed(), chosen.plate(), chosen.crust(), elevation, sediment, riverOrder, koppen, province, rockTop, groundwater, pool);

            byte[] biome = new byte[N];
            computeBiomes(chosen.subSeed(), elevation, relief, riverOrder, lake, lakes, koppen, province, rockTop, groundwater, tempMonthly, precipMonthly, chosen.volcanoes(), biome, pool);
            PlanetProgressStages.complete(server, STAGE_BIOMES);

            // P7 & P8: Global mineral deposits (~18,000), reservoirs, structures, deep conduits, and start sites
            PlanetProgressStages.report(server, STAGE_GEOLOGY, 0.4f, Optional.empty());
            List<Deposit> deposits = generateDeposits(worldSeed, elevation, province, rockTop, riverOrder, biome);
            List<Reservoir> reservoirs = generateReservoirs(worldSeed, elevation, province);
            List<PlanetModel.DeepConduitInfo> deepConduits = generateDeepConduits(worldSeed, chosen.hotspots(), chosen.faults());
            List<PlanetModel.StructureSiteInfo> structures = generateStructures(worldSeed, elevation, province, biome, chosen.volcanoes());
            List<PlanetModel.StartSiteCandidate> startSites = selectStartSites(worldSeed, elevation, relief, riverOrder, lake, koppen, biome, rockTop, chosen.volcanoes());

            byte[] paramsDigest = PlanetBinFile.computeParamsDigest(worldSeed, cfg.landTarget(), cfg.configuredPlateCount(), cfg.erosionSteps());
            long planetDigest = computePlanetDigest(worldSeed, elevation, biome, deposits.size());

            PlanetModel model = new PlanetModel(
                    PlanetBinFile.CURRENT_FORMAT_VERSION,
                    PlanetBinFile.GENERATOR_VERSION,
                    worldSeed,
                    planetDigest,
                    paramsDigest,
                    chosen.plates().size(),
                    continentCount,
                    chosen.hotspots().size(),
                    oceanFraction,
                    chosen.plate(),
                    chosen.crust(),
                    elevation,
                    relief,
                    sediment,
                    flowDir,
                    flowAcc,
                    basin,
                    lake,
                    riverOrder,
                    province,
                    rockTop,
                    groundwater,
                    koppen,
                    biome,
                    tempMonthly,
                    precipMonthly,
                    windSeasonal,
                    currents,
                    chosen.plates(),
                    chosen.hotspots(),
                    chosen.volcanoes(),
                    chosen.faults(),
                    lakes,
                    deposits,
                    reservoirs,
                    deepConduits,
                    structures,
                    startSites,
                    finalContinents
            );
            PlanetProgressStages.complete(server, STAGE_GEOLOGY);
            return model;
        } finally {
            pool.shutdown();
        }
    }

    private static TectonicAttempt buildTectonics(long seed, Config cfg, ForkJoinPool pool) {
        Random rng = new Random(seed);
        int plateCount = cfg.configuredPlateCount() >= 8 ? cfg.configuredPlateCount() : (12 + rng.nextInt(7));

        double[] px = new double[plateCount];
        double[] py = new double[plateCount];
        double[] pz = new double[plateCount];
        float[] pWeight = new float[plateCount];
        for (int i = 0; i < plateCount; i++) {
            double u = rng.nextDouble() * 2.0 - 1.0;
            double lon = rng.nextDouble() * 2.0 * Math.PI - Math.PI;
            double r = Math.sqrt(Math.max(0.0, 1.0 - u * u));
            px[i] = r * Math.cos(lon);
            py[i] = u;
            pz[i] = r * Math.sin(lon);
            pWeight[i] = 0.75f + rng.nextFloat() * 0.65f;
        }

        // 3 Lloyd-style spherical relaxation passes on coarse Fibonacci sphere (2048 samples)
        int fibN = 2048;
        double golden = Math.PI * (3.0 - Math.sqrt(5.0));
        for (int iter = 0; iter < 3; iter++) {
            double[] sx = new double[plateCount];
            double[] sy = new double[plateCount];
            double[] sz = new double[plateCount];
            int[] cnt = new int[plateCount];
            for (int s = 0; s < fibN; s++) {
                double y = 1.0 - (s / (double) (fibN - 1)) * 2.0;
                double rad = Math.sqrt(Math.max(0.0, 1.0 - y * y));
                double th = golden * s;
                double vx = Math.cos(th) * rad;
                double vz = Math.sin(th) * rad;
                int best = 0;
                double bestScore = -1e9;
                for (int k = 0; k < plateCount; k++) {
                    double dot = vx * px[k] + y * py[k] + vz * pz[k];
                    double score = dot - (1.0 - pWeight[k]) * 0.12;
                    if (score > bestScore) {
                        bestScore = score;
                        best = k;
                    }
                }
                sx[best] += vx;
                sy[best] += y;
                sz[best] += vz;
                cnt[best]++;
            }
            for (int k = 0; k < plateCount; k++) {
                if (cnt[k] > 0) {
                    double len = Math.sqrt(sx[k] * sx[k] + sy[k] * sy[k] + sz[k] * sz[k]);
                    if (len > 1e-6) {
                        px[k] = sx[k] / len;
                        py[k] = sy[k] / len;
                        pz[k] = sz[k] / len;
                    }
                }
            }
        }

        // Generate 4..6 well-separated continental craton cores so each continent spans 22,000-46,000 blocks
        int numContinents = 4 + rng.nextInt(3); // 4, 5, or 6
        List<CratonBlob> blobs = new ArrayList<>(numContinents);
        double baseLon = rng.nextDouble() * 2.0 * Math.PI - Math.PI;
        for (int c = 0; c < numContinents; c++) {
            double lon = baseLon + (2.0 * Math.PI * c / numContinents) + (rng.nextDouble() - 0.5) * (0.45 * 2.0 * Math.PI / numContinents);
            // Alternate northern/southern mid-latitudes so >= 2 continents touch [20°, 55°]
            double latDeg;
            if (c == 0) {
                latDeg = 28.0 + rng.nextDouble() * 18.0;
            } else if (c == 1) {
                latDeg = -(25.0 + rng.nextDouble() * 20.0);
            } else {
                latDeg = (rng.nextDouble() * 2.0 - 1.0) * 48.0;
            }
            double latRad = Math.toRadians(latDeg);
            double cx = Math.cos(latRad) * Math.cos(lon);
            double cy = Math.sin(latRad);
            double cz = Math.cos(latRad) * Math.sin(lon);
            // Desired half-span: 12,000..19,500 blocks -> total span ~24,000..39,000 blocks (well inside 20k..50k!)
            double halfBlocksX = 12_500.0 + rng.nextDouble() * 6_800.0;
            double halfBlocksZ = 11_000.0 + rng.nextDouble() * 6_200.0;
            double radX = (halfBlocksX / PlanetCoordinates.PLANET_WIDTH) * 2.0 * Math.PI;
            double radZ = (halfBlocksZ / PlanetCoordinates.PLANET_HEIGHT) * Math.PI;
            int nearestPlate = 0;
            double bestDot = -2.0;
            for (int k = 0; k < plateCount; k++) {
                double dot = cx * px[k] + cy * py[k] + cz * pz[k];
                if (dot > bestDot) {
                    bestDot = dot;
                    nearestPlate = k;
                }
            }
            blobs.add(new CratonBlob(cx, cy, cz, radX, radZ, rng.nextDouble() * Math.PI, nearestPlate));
        }

        boolean[] isContPlate = new boolean[plateCount];
        for (CratonBlob b : blobs) {
            isContPlate[b.plateId()] = true;
        }

        List<PlanetModel.PlateInfo> plates = new ArrayList<>(plateCount);
        for (int k = 0; k < plateCount; k++) {
            double pu = rng.nextDouble() * 2.0 - 1.0;
            double plon = rng.nextDouble() * 2.0 * Math.PI;
            double pr = Math.sqrt(Math.max(0.0, 1.0 - pu * pu));
            float poleX = (float) (pr * Math.cos(plon));
            float poleY = (float) pu;
            float poleZ = (float) (pr * Math.sin(plon));
            float omega = 0.25f + rng.nextFloat() * 0.90f;
            plates.add(new PlanetModel.PlateInfo(
                    k, isContPlate[k],
                    (float) px[k], (float) py[k], (float) pz[k],
                    poleX, poleY, poleZ, omega, pWeight[k]
            ));
        }

        int hotspotCount = 6 + rng.nextInt(7);
        List<PlanetModel.HotspotInfo> hotspots = new ArrayList<>(hotspotCount);
        double[] hx = new double[hotspotCount];
        double[] hy = new double[hotspotCount];
        double[] hz = new double[hotspotCount];
        for (int h = 0; h < hotspotCount; h++) {
            int bx = PlanetCoordinates.X_MIN + rng.nextInt(PlanetCoordinates.PLANET_WIDTH);
            int bz = -34_000 + rng.nextInt(68_000);
            float str = 0.6f + rng.nextFloat() * 0.6f;
            hotspots.add(new PlanetModel.HotspotInfo(h, bx, bz, str));
            int gx = PlanetModel.blockToL0X(bx);
            int gz = PlanetModel.blockToL0Z(bz);
            double[] uv = new double[3];
            PlanetCoordinates.l0ToUnitSphere(gx, gz, uv);
            hx[h] = uv[0];
            hy[h] = uv[1];
            hz[h] = uv[2];
        }

        // Designate one major collision orogen & one major oceanic trench so every planet has realistic extremes
        int peakBlobIdx = rng.nextInt(blobs.size());
        CratonBlob peakBlob = blobs.get(peakBlobIdx);
        double trenchAngle = baseLon + Math.PI / numContinents;
        double trenchLat = Math.toRadians((rng.nextBoolean() ? 1.0 : -1.0) * (15.0 + rng.nextDouble() * 25.0));
        double txSphere = Math.cos(trenchLat) * Math.cos(trenchAngle);
        double tySphere = Math.sin(trenchLat);
        double tzSphere = Math.cos(trenchLat) * Math.sin(trenchAngle);

        byte[] plateArr = new byte[N];
        byte[] crustArr = new byte[N];
        float[] heightM = new float[N];

        pool.submit(() -> IntStream.range(0, H).parallel().forEach(gz -> {
            double[] uv = new double[3];
            int bz = PlanetCoordinates.Z_MIN + gz * PlanetCoordinates.L0_STEP + 32;
            double latDeg = (-bz / (double) PlanetCoordinates.Z_MAX) * 90.0;
            int row = gz * W;

            for (int gx = 0; gx < W; gx++) {
                int bx = PlanetCoordinates.X_MIN + gx * PlanetCoordinates.L0_STEP + 32;
                PlanetCoordinates.l0ToUnitSphere(gx, gz, uv);

                double warp1 = PeriodicNoise.sample2D(seed ^ 0x101L, bx, bz, 8192) * 0.08;
                double warp2 = PeriodicNoise.sample2D(seed ^ 0x202L, bx, bz, 2048) * 0.035;
                double coastNoise = PeriodicNoise.fbm2D(seed ^ 0x303L, bx, bz, 4, 4096, 0.52);
                double ridgeNoise = PeriodicNoise.sphericalStretchedRidge2D(seed ^ 0x404L, bx, bz, 3, 2048);

                // Nearest and second-nearest plate
                int p1 = 0;
                int p2 = 1;
                double d1 = -1e9;
                double d2 = -1e9;
                for (int k = 0; k < plateCount; k++) {
                    PlanetModel.PlateInfo pi = plates.get(k);
                    double dot = uv[0] * pi.centerX() + uv[1] * pi.centerY() + uv[2] * pi.centerZ();
                    double score = dot + warp1 + warp2 - (1.0 - pi.weight()) * 0.08;
                    if (score > d1) {
                        d2 = d1;
                        p2 = p1;
                        d1 = score;
                        p1 = k;
                    } else if (score > d2) {
                        d2 = score;
                        p2 = k;
                    }
                }
                plateArr[row + gx] = (byte) p1;
                double boundaryDist = Math.max(0.0, d1 - d2); // ~0 at boundary, ~0.15 in interior

                // Evaluate continental craton membership with natural seaway separation between distinct blobs
                double bestContVal = -10.0;
                double secondContVal = -10.0;
                int ownerBlob = -1;
                for (int bi = 0; bi < blobs.size(); bi++) {
                    CratonBlob b = blobs.get(bi);
                    double dot = Math.clamp(uv[0] * b.cx() + uv[1] * b.cy() + uv[2] * b.cz(), -1.0, 1.0);
                    double angDist = Math.acos(dot);
                    double avgRad = 0.5 * (b.radiusRadX() + b.radiusRadZ());
                    double norm = 1.0 - (angDist / Math.max(0.08, avgRad)) + coastNoise * 0.28 + warp2 * 1.8;
                    if (norm > bestContVal) {
                        secondContVal = bestContVal;
                        bestContVal = norm;
                        ownerBlob = bi;
                    } else if (norm > secondContVal) {
                        secondContVal = norm;
                    }
                }
                // Prevent two cratons from merging into a >50k supercontinent via a marginal seaway
                if (bestContVal > 0.0 && secondContVal > -0.08) {
                    double saddle = 0.16 - Math.abs(bestContVal - secondContVal);
                    if (saddle > 0.0) {
                        bestContVal -= saddle * 2.2;
                    }
                }
                // Keep extreme poles mostly polar ocean / bounded ice cap
                if (Math.abs(latDeg) > 78.0) {
                    bestContVal -= (Math.abs(latDeg) - 78.0) * 0.06;
                }

                boolean continentalCrust = bestContVal > -0.10;
                int ageCode = (int) Math.clamp(Math.round(boundaryDist * 500.0), 0, 127);
                crustArr[row + gx] = (byte) ((continentalCrust ? 0x80 : 0x00) | ageCode);

                // Relative plate motion at boundary
                PlanetModel.PlateInfo pa = plates.get(p1);
                PlanetModel.PlateInfo pb = plates.get(p2);
                double vax = pa.omega() * (pa.poleY() * uv[2] - pa.poleZ() * uv[1]);
                double vay = pa.omega() * (pa.poleZ() * uv[0] - pa.poleX() * uv[2]);
                double vaz = pa.omega() * (pa.poleX() * uv[1] - pa.poleY() * uv[0]);
                double vbx = pb.omega() * (pb.poleY() * uv[2] - pb.poleZ() * uv[1]);
                double vby = pb.omega() * (pb.poleZ() * uv[0] - pb.poleX() * uv[2]);
                double vbz = pb.omega() * (pb.poleX() * uv[1] - pb.poleY() * uv[0]);
                double nx = pb.centerX() - pa.centerX();
                double ny = pb.centerY() - pa.centerY();
                double nz = pb.centerZ() - pa.centerZ();
                double nLen = Math.max(1e-5, Math.sqrt(nx * nx + ny * ny + nz * nz));
                // Positive vn = divergent, negative vn = convergent
                double vn = ((vbx - vax) * nx + (vby - vay) * ny + (vbz - vaz) * nz) / nLen;

                double h;
                if (bestContVal >= 0.0) {
                    // Land: plains + craton shields + old orogens + boundary mountain belts
                    double interior = Math.min(1.0, bestContVal * 2.2);
                    h = 55.0 + 320.0 * interior + coastNoise * 160.0 + ridgeNoise * 240.0;
                    if (vn < -0.15) {
                        double convFalloff = Math.exp(-Math.pow(boundaryDist / 0.045, 2.0));
                        h += (2_800.0 + 2_600.0 * ridgeNoise) * convFalloff;
                    } else if (vn > 0.20) {
                        double riftFalloff = Math.exp(-Math.pow(boundaryDist / 0.020, 2.0));
                        h -= 450.0 * riftFalloff;
                    }
                    // Major Himalayan-scale plateau & peak on peakBlob
                    if (ownerBlob == peakBlobIdx) {
                        double dotPeak = Math.clamp(uv[0] * peakBlob.cx() + uv[1] * peakBlob.cy() + uv[2] * peakBlob.cz(), -1.0, 1.0);
                        double dPeak = Math.acos(dotPeak);
                        double orogen = Math.exp(-Math.pow(dPeak / 0.055, 2.0));
                        h += orogen * (4_600.0 + 3_200.0 * ridgeNoise);
                    }
                } else if (continentalCrust) {
                    // Continental shelf (-20 m .. -180 m)
                    double shelfFrac = (-bestContVal) / 0.10;
                    h = -15.0 - 165.0 * shelfFrac + coastNoise * 20.0;
                } else {
                    // Oceanic lithosphere cooling model: -2,500 m at mid-ocean ridge down to -5,200 m abyssal plain
                    double ageMyr = Math.clamp(boundaryDist * 950.0, 0.0, 165.0);
                    h = -(2_500.0 + 210.0 * Math.sqrt(ageMyr)) + coastNoise * 220.0;
                    if (vn > 0.15) {
                        // Mid-ocean ridge uplift
                        h += 1_400.0 * Math.exp(-Math.pow(boundaryDist / 0.030, 2.0));
                    } else if (vn < -0.15) {
                        // Subduction trench + island arc
                        double trenchProfile = Math.exp(-Math.pow(boundaryDist / 0.012, 2.0));
                        h -= 4_200.0 * trenchProfile;
                        double arcProfile = Math.exp(-Math.pow((boundaryDist - 0.028) / 0.010, 2.0));
                        h += (3_200.0 + 1_600.0 * ridgeNoise) * arcProfile;
                    }
                    // Major Mariana-scale subduction trench
                    double dotTrench = Math.clamp(uv[0] * txSphere + uv[1] * tySphere + uv[2] * tzSphere, -1.0, 1.0);
                    double dTrench = Math.acos(dotTrench);
                    if (dTrench < 0.07) {
                        double arcSlot = Math.exp(-Math.pow((dTrench - 0.025) / 0.010, 2.0));
                        h = Math.min(h, -5_200.0 - 5_650.0 * arcSlot);
                    }
                }

                // Hotspots (volcanic swells / island chains / LIPs)
                for (int hi = 0; hi < hotspotCount; hi++) {
                    double dotH = Math.clamp(uv[0] * hx[hi] + uv[1] * hy[hi] + uv[2] * hz[hi], -1.0, 1.0);
                    double dh = Math.acos(dotH);
                    if (dh < 0.04) {
                        double swell = Math.exp(-Math.pow(dh / 0.018, 2.0)) * hotspots.get(hi).strength();
                        h += (continentalCrust ? 1_400.0 : 3_400.0) * swell;
                    }
                }

                heightM[row + gx] = (float) Math.clamp(h, -10_950.0, 8_820.0);
            }
        })).join();

        // Split any oversized continent > 50,000 blocks and evaluate validation rules
        List<PlanetModel.ContinentStat> continents = analyzeContinents(heightM, true);
        int validSpanCount = 0;
        int midLatCount = 0;
        for (PlanetModel.ContinentStat cs : continents) {
            if (cs.maxSpanBlocks() >= 20_000 && cs.maxSpanBlocks() <= 50_000) {
                validSpanCount++;
            }
            if (cs.touchesMidLatitude()) {
                midLatCount++;
            }
        }

        int oceanCount = 0;
        for (int i = 0; i < N; i++) {
            if (heightM[i] < 0.0f) {
                oceanCount++;
            }
        }
        float oceanFrac = oceanCount / (float) N;
        boolean valid = continents.size() >= 3 && continents.size() <= 8 && validSpanCount >= 3 && midLatCount >= 2;

        // Synthesize fault segments and volcanoes along convergent/rift boundaries & hotspots
        List<PlanetModel.FaultInfo> faults = new ArrayList<>();
        List<PlanetModel.VolcanoInfo> volcanoes = new ArrayList<>();
        Random objRng = new Random(seed ^ 0x55AA_7711L);
        int faultId = 1;
        int volcId = 1;
        for (int gz = 40; gz < H - 40; gz += 24) {
            for (int gx = 0; gx < W; gx += 24) {
                int idx = gz * W + gx;
                byte p0 = plateArr[idx];
                byte pRight = plateArr[gz * W + ((gx + 2) % W)];
                byte pDown = plateArr[(gz + 2) * W + gx];
                if (p0 != pRight || p0 != pDown) {
                    int bx = PlanetCoordinates.X_MIN + gx * PlanetCoordinates.L0_STEP;
                    int bz = PlanetCoordinates.Z_MIN + gz * PlanetCoordinates.L0_STEP;
                    if (faults.size() < 2_400) {
                        faults.add(new PlanetModel.FaultInfo(
                                faultId++, bx, bz, bx + 1024, bz + 1024,
                                (byte) 1, 6.5f + objRng.nextFloat() * 2.3f, 0.005f + objRng.nextFloat() * 0.015f
                        ));
                    }
                    if (heightM[idx] > 800.0f && objRng.nextFloat() < 0.22f && volcanoes.size() < 360) {
                        volcanoes.add(new PlanetModel.VolcanoInfo(
                                volcId++, bx, bz, (byte) 1, (byte) (2 + objRng.nextInt(4)), 120 + objRng.nextInt(400)
                        ));
                    }
                }
            }
        }
        for (PlanetModel.HotspotInfo hs : hotspots) {
            volcanoes.add(new PlanetModel.VolcanoInfo(volcId++, hs.blockX(), hs.blockZ(), (byte) 0, (byte) 3, 90));
        }

        return new TectonicAttempt(seed, plates, hotspots, volcanoes, faults, plateArr, crustArr, heightM, continents, oceanFrac, valid);
    }

    /**
     * Connected-component continent analysis on the wrapped 2560x1280 L0 grid.
     * If splitOversized is true, carves a natural marine strait across any landmass > 50,000 blocks.
     */
    static List<PlanetModel.ContinentStat> analyzeContinents(float[] heightM, boolean splitOversized) {
        for (int pass = 0; pass < (splitOversized ? 3 : 1); pass++) {
            int[] comp = new int[N];
            Arrays.fill(comp, -1);
            int[] queue = new int[N];
            List<PlanetModel.ContinentStat> stats = new ArrayList<>();
            boolean carvedAny = false;

            for (int start = 0; start < N; start++) {
                if (heightM[start] < 0.0f || comp[start] != -1) {
                    continue;
                }
                int cid = stats.size();
                int head = 0;
                int tail = 0;
                queue[tail++] = start;
                comp[start] = cid;

                boolean[] usedCols = new boolean[W];
                int minGz = H;
                int maxGz = -1;
                boolean midLat = false;

                while (head < tail) {
                    int u = queue[head++];
                    int gz = u / W;
                    int gx = u - gz * W;
                    usedCols[gx] = true;
                    if (gz < minGz) minGz = gz;
                    if (gz > maxGz) maxGz = gz;
                    double lat = Math.abs(90.0 - (gz / (double) H) * 180.0);
                    if (lat >= 20.0 && lat <= 55.0) {
                        midLat = true;
                    }

                    int left = gz * W + (gx == 0 ? W - 1 : gx - 1);
                    if (heightM[left] >= 0.0f && comp[left] == -1) {
                        comp[left] = cid;
                        queue[tail++] = left;
                    }
                    int right = gz * W + (gx == W - 1 ? 0 : gx + 1);
                    if (heightM[right] >= 0.0f && comp[right] == -1) {
                        comp[right] = cid;
                        queue[tail++] = right;
                    }
                    if (gz > 0) {
                        int up = (gz - 1) * W + gx;
                        if (heightM[up] >= 0.0f && comp[up] == -1) {
                            comp[up] = cid;
                            queue[tail++] = up;
                        }
                    }
                    if (gz < H - 1) {
                        int dn = (gz + 1) * W + gx;
                        if (heightM[dn] >= 0.0f && comp[dn] == -1) {
                            comp[dn] = cid;
                            queue[tail++] = dn;
                        }
                    }
                }

                if (tail < MIN_CONTINENT_CELLS) {
                    continue;
                }

                int spanCols = circularSpan(usedCols);
                int spanXBlocks = spanCols * PlanetCoordinates.L0_STEP;
                int spanZBlocks = (maxGz - minGz + 1) * PlanetCoordinates.L0_STEP;
                int maxSpan = Math.max(spanXBlocks, spanZBlocks);

                if (splitOversized && maxSpan > 49_500 && pass < 2) {
                    // Carve a tectonic seaway through the middle of the oversized landmass
                    carveSeaway(heightM, comp, cid, usedCols, minGz, maxGz, spanXBlocks >= spanZBlocks);
                    carvedAny = true;
                }
                stats.add(new PlanetModel.ContinentStat(cid, tail, spanXBlocks, spanZBlocks, maxSpan, midLat));
            }
            if (!carvedAny) {
                stats.sort(Comparator.comparingInt(PlanetModel.ContinentStat::cellCount).reversed());
                return stats;
            }
        }
        return List.of();
    }

    private static int circularSpan(boolean[] usedCols) {
        int firstFalse = -1;
        for (int i = 0; i < W; i++) {
            if (!usedCols[i]) {
                firstFalse = i;
                break;
            }
        }
        if (firstFalse == -1) {
            return W;
        }
        int maxGap = 0;
        int curGap = 0;
        for (int step = 0; step < W; step++) {
            int idx = (firstFalse + step) % W;
            if (!usedCols[idx]) {
                curGap++;
                if (curGap > maxGap) {
                    maxGap = curGap;
                }
            } else {
                curGap = 0;
            }
        }
        return W - maxGap;
    }

    private static void carveSeaway(float[] heightM, int[] comp, int cid, boolean[] usedCols, int minGz, int maxGz, boolean splitVertical) {
        if (splitVertical) {
            // Find median occupied column
            int total = 0;
            for (boolean b : usedCols) if (b) total++;
            int target = total / 2;
            int seen = 0;
            int splitCol = 0;
            for (int gx = 0; gx < W; gx++) {
                if (usedCols[gx]) {
                    seen++;
                    if (seen >= target) {
                        splitCol = gx;
                        break;
                    }
                }
            }
            for (int gz = minGz; gz <= maxGz; gz++) {
                int wobble = (int) Math.round(Math.sin(gz * 0.08) * 6.0);
                for (int dx = -8; dx <= 8; dx++) {
                    int gx = Math.floorMod(splitCol + wobble + dx, W);
                    int idx = gz * W + gx;
                    if (comp[idx] == cid) {
                        double profile = 1.0 - (Math.abs(dx) / 9.0);
                        heightM[idx] = (float) (-80.0 - 220.0 * profile);
                    }
                }
            }
        } else {
            int splitGz = (minGz + maxGz) / 2;
            for (int gx = 0; gx < W; gx++) {
                int wobble = (int) Math.round(Math.sin(gx * 0.06) * 6.0);
                for (int dz = -8; dz <= 8; dz++) {
                    int gz = Math.clamp(splitGz + wobble + dz, 0, H - 1);
                    int idx = gz * W + gx;
                    if (comp[idx] == cid) {
                        double profile = 1.0 - (Math.abs(dz) / 9.0);
                        heightM[idx] = (float) (-80.0 - 220.0 * profile);
                    }
                }
            }
        }
    }

    private static void runErosion(
            long seed,
            float[] heightM,
            byte[] sediment,
            byte[] relief,
            int configuredSteps,
            ForkJoinPool pool,
            @Nullable MinecraftServer server
    ) {
        // Multi-pass thermal talus + fluvial incision/deposition approximation of 10 §5.4
        int passes = Math.clamp(configuredSteps / 16, 3, 6);
        float[] nextH = new float[N];
        float[] sedFloat = new float[N];

        for (int pass = 0; pass < passes; pass++) {
            final int p = pass;
            pool.submit(() -> IntStream.range(0, H).parallel().forEach(gz -> {
                int row = gz * W;
                int upRow = Math.max(0, gz - 1) * W;
                int dnRow = Math.min(H - 1, gz + 1) * W;
                for (int gx = 0; gx < W; gx++) {
                    int idx = row + gx;
                    float h = heightM[idx];
                    if (h < -200.0f) {
                        nextH[idx] = h;
                        continue;
                    }
                    int lx = gx == 0 ? W - 1 : gx - 1;
                    int rx = gx == W - 1 ? 0 : gx + 1;
                    float hl = heightM[row + lx];
                    float hr = heightM[row + rx];
                    float hu = heightM[upRow + gx];
                    float hd = heightM[dnRow + gx];
                    float minN = Math.min(Math.min(hl, hr), Math.min(hu, hd));
                    float avgN = 0.25f * (hl + hr + hu + hd);

                    float drop = h - minN;
                    float eroded = 0.0f;
                    if (h > 0.0f && drop > 45.0f) {
                        // Stream-power + talus relaxation
                        eroded = (drop - 45.0f) * 0.22f;
                    }
                    float laplacian = avgN - h;
                    float deposited = (h >= 0.0f && laplacian > 12.0f) ? laplacian * 0.18f : 0.0f;
                    nextH[idx] = h - eroded + deposited + laplacian * 0.06f;
                    if (deposited > 0.0f || eroded > 0.0f) {
                        sedFloat[idx] = Math.min(63.0f, sedFloat[idx] + Math.max(0.0f, deposited * 0.15f));
                    }
                }
            })).join();
            System.arraycopy(nextH, 0, heightM, 0, N);
            PlanetProgressStages.report(server, STAGE_EROSION, (p + 1) / (float) passes, Optional.empty());
        }

        // Compute local relief (4-300 m) and encode sediment (0.25 m step)
        pool.submit(() -> IntStream.range(0, H).parallel().forEach(gz -> {
            int row = gz * W;
            int upRow = Math.max(0, gz - 1) * W;
            int dnRow = Math.min(H - 1, gz + 1) * W;
            for (int gx = 0; gx < W; gx++) {
                int idx = row + gx;
                int lx = gx == 0 ? W - 1 : gx - 1;
                int rx = gx == W - 1 ? 0 : gx + 1;
                float h = heightM[idx];
                float maxDiff = Math.max(
                        Math.max(Math.abs(h - heightM[row + lx]), Math.abs(h - heightM[row + rx])),
                        Math.max(Math.abs(h - heightM[upRow + gx]), Math.abs(h - heightM[dnRow + gx]))
                );
                float relM = Math.clamp(4.0f + maxDiff * 0.45f, 4.0f, 300.0f);
                relief[idx] = PlanetModel.encodeReliefMeters(relM);
                sediment[idx] = (byte) Math.clamp(Math.round(sedFloat[idx] * 4.0f), 0, 255);
            }
        })).join();
    }

    private static void calibrateVerticalExtremes(float[] heightM) {
        float maxLand = 0.0f;
        float minTrench = 0.0f;
        for (int i = 0; i < N; i++) {
            float h = heightM[i];
            if (h > maxLand) maxLand = h;
            if (h < minTrench) minTrench = h;
        }
        // Target peak ~8,150 m (Y ~ 808, strictly around ~800) and trench ~-10,900 m (Y ~ -457, around -460)
        if (maxLand > 100.0f && (maxLand < 7_600.0f || maxLand > 8_750.0f)) {
            float targetPeak = 8_150.0f;
            float scaleHigh = targetPeak / maxLand;
            for (int i = 0; i < N; i++) {
                if (heightM[i] > 1_500.0f) {
                    float t = (heightM[i] - 1_500.0f) / (maxLand - 1_500.0f);
                    float factor = 1.0f + (scaleHigh - 1.0f) * t;
                    heightM[i] *= factor;
                }
            }
        }
        if (minTrench < -1_000.0f && (minTrench > -10_400.0f || minTrench < -11_000.0f)) {
            float targetTrench = -10_850.0f;
            float scaleDeep = targetTrench / minTrench;
            for (int i = 0; i < N; i++) {
                if (heightM[i] < -5_500.0f) {
                    float t = (heightM[i] - (-5_500.0f)) / (minTrench - (-5_500.0f));
                    float factor = 1.0f + (scaleDeep - 1.0f) * t;
                    heightM[i] *= factor;
                }
            }
        }
    }

    /**
     * Priority-Flood depression filling (Barnes 2014) using a zero-allocation primitive binary min-heap.
     * Guarantees 100% (>= 99%) of rivers drain monotonically to ocean or a closed endorheic lake sink.
     */
    private static List<PlanetModel.LakeInfo> computeHydrology(
            short[] elevation,
            float[] annualRunoffMm,
            byte[] flowDir,
            short[] flowAcc,
            short[] basin,
            short[] lake,
            byte[] riverOrder
    ) {
        boolean[] visited = new boolean[N];
        float[] filled = new float[N];
        int[] drainOrder = new int[N];
        int drainCount = 0;

        // Primitive min-heap (only coastal ocean boundary + land cells enter heap -> <= 1.4M entries)
        int[] heapCell = new int[N];
        float[] heapKey = new float[N];
        int heapSize = 0;

        short nextBasinId = 1;
        for (int gz = 0; gz < H; gz++) {
            int row = gz * W;
            for (int gx = 0; gx < W; gx++) {
                int idx = row + gx;
                if (elevation[idx] < 0) {
                    visited[idx] = true;
                    filled[idx] = elevation[idx];
                    flowDir[idx] = 0;
                    // Seed only coastal ocean cells (those adjacent to at least one land cell)
                    boolean coastal = false;
                    for (int dir = 1; dir <= 8; dir++) {
                        int ngz = gz + PlanetModel.D8_DZ[dir];
                        if (ngz < 0 || ngz >= H) continue;
                        int ngx = Math.floorMod(gx + PlanetModel.D8_DX[dir], W);
                        if (elevation[ngz * W + ngx] >= 0) {
                            coastal = true;
                            break;
                        }
                    }
                    if (coastal) {
                        short bid = nextBasinId++;
                        if (nextBasinId == Short.MAX_VALUE) nextBasinId = 1;
                        basin[idx] = bid;
                        heapSize = heapPush(heapCell, heapKey, heapSize, idx, filled[idx]);
                    }
                }
            }
        }

        // Ensure polar edge land cells also have a boundary outlet if ocean didn't touch a pole
        if (heapSize == 0) {
            visited[0] = true;
            basin[0] = 1;
            heapSize = heapPush(heapCell, heapKey, heapSize, 0, elevation[0]);
        }

        int[] oppositeDir = {0, 5, 6, 7, 8, 1, 2, 3, 4};
        while (heapSize > 0) {
            int u = heapCell[0];
            float uElev = heapKey[0];
            heapSize = heapPop(heapCell, heapKey, heapSize);

            int gz = u / W;
            int gx = u - gz * W;
            if (elevation[u] >= 0) {
                drainOrder[drainCount++] = u;
            }

            for (int dir = 1; dir <= 8; dir++) {
                int ngz = gz + PlanetModel.D8_DZ[dir];
                if (ngz < 0 || ngz >= H) {
                    continue;
                }
                int ngx = Math.floorMod(gx + PlanetModel.D8_DX[dir], W);
                int v = ngz * W + ngx;
                if (visited[v]) {
                    continue;
                }
                visited[v] = true;
                float rawV = elevation[v];
                float nextFill = Math.max(rawV, uElev + 0.02f);
                filled[v] = nextFill;
                flowDir[v] = (byte) oppositeDir[dir];
                basin[v] = basin[u];
                heapSize = heapPush(heapCell, heapKey, heapSize, v, nextFill);
            }
        }

        // Detect lakes (filled - raw > 30 m, connected area > 40 cells) and classify arid endorheic basins
        List<PlanetModel.LakeInfo> lakes = new ArrayList<>();
        int[] lakeQueue = new int[65_536];
        boolean[] lakeVisited = new boolean[N];
        int nextLakeId = 1;
        for (int i = 0; i < N && nextLakeId < 60_000; i++) {
            if (elevation[i] < 0 || lakeVisited[i] || (filled[i] - elevation[i]) <= 30.0f) {
                continue;
            }
            int head = 0;
            int tail = 0;
            List<Integer> members = new ArrayList<>();
            lakeQueue[tail++] = i;
            lakeVisited[i] = true;
            int deepestCell = i;
            short minRaw = elevation[i];
            float sumRunoff = 0.0f;

            while (head < tail) {
                int u = lakeQueue[head % lakeQueue.length];
                head++;
                members.add(u);
                sumRunoff += annualRunoffMm[u];
                if (elevation[u] < minRaw) {
                    minRaw = elevation[u];
                    deepestCell = u;
                }
                int gz = u / W;
                int gx = u - gz * W;
                for (int dir = 1; dir <= 8; dir++) {
                    int ngz = gz + PlanetModel.D8_DZ[dir];
                    if (ngz < 0 || ngz >= H) continue;
                    int ngx = Math.floorMod(gx + PlanetModel.D8_DX[dir], W);
                    int v = ngz * W + ngx;
                    if (!lakeVisited[v] && elevation[v] >= 0 && (filled[v] - elevation[v]) > 30.0f && members.size() < 8_000) {
                        lakeVisited[v] = true;
                        if (tail - head < lakeQueue.length - 1) {
                            lakeQueue[tail % lakeQueue.length] = v;
                            tail++;
                        }
                    }
                }
            }

            if (members.size() >= 40) {
                int lid = nextLakeId++;
                float meanRunoff = sumRunoff / members.size();
                boolean endorheic = meanRunoff < 180.0f;
                short surfM = (short) Math.round(filled[deepestCell]);
                for (int m : members) {
                    lake[m] = (short) lid;
                }
                if (endorheic) {
                    flowDir[deepestCell] = 0; // terminal endorheic sink
                }
                int cgz = deepestCell / W;
                int cgx = deepestCell - cgz * W;
                lakes.add(new PlanetModel.LakeInfo(
                        lid,
                        PlanetCoordinates.X_MIN + cgx * PlanetCoordinates.L0_STEP,
                        PlanetCoordinates.Z_MIN + cgz * PlanetCoordinates.L0_STEP,
                        surfM,
                        members.size(),
                        endorheic,
                        endorheic ? 28.0f : 0.2f
                ));
            }
        }

        // Write Priority-Flood filled elevation back to non-lake land cells so rivers never climb micro-pits
        for (int i = 0; i < N; i++) {
            if (elevation[i] >= 0 && lake[i] == 0 && filled[i] > elevation[i]) {
                elevation[i] = (short) Math.min(Short.MAX_VALUE, (int) Math.floor(filled[i]));
            }
        }

        // Upstream-to-downstream discharge Q (m^3/s) and Strahler stream order (1..9)
        float[] discharge = new float[N];
        byte[] maxChildOrder = new byte[N];
        byte[] secondChildOrder = new byte[N];

        double eqCellAreaM2 = Math.pow(PlanetCoordinates.L0_STEP * PlanetCoordinates.METERS_PER_BLOCK, 2.0);
        double secondsPerYear = 365.25 * 86_400.0;

        for (int i = drainCount - 1; i >= 0; i--) {
            int u = drainOrder[i];
            int gz = u / W;
            int gx = u - gz * W;
            double latRad = (0.5 - ((gz + 0.5) / H)) * Math.PI;
            double cellAreaM2 = eqCellAreaM2 * Math.max(0.08, Math.cos(latRad));
            double localRunoffM = Math.max(0.01, annualRunoffMm[u] * 0.001);
            float localQ = (float) ((localRunoffM * cellAreaM2) / secondsPerYear * 0.45);
            float totalQ = discharge[u] + localQ;
            discharge[u] = totalQ;
            flowAcc[u] = PlanetModel.encodeDischargeM3s(totalQ);

            int ord;
            if (totalQ < 5.0f) {
                ord = 0;
            } else if (maxChildOrder[u] == 0) {
                ord = 1;
            } else if (maxChildOrder[u] == secondChildOrder[u]) {
                ord = Math.min(9, maxChildOrder[u] + 1);
            } else {
                ord = maxChildOrder[u];
            }
            riverOrder[u] = (byte) ord;

            int dir = flowDir[u] & 0xFF;
            if (dir >= 1 && dir <= 8) {
                int ngz = gz + PlanetModel.D8_DZ[dir];
                int ngx = Math.floorMod(gx + PlanetModel.D8_DX[dir], W);
                if (ngz >= 0 && ngz < H) {
                    int down = ngz * W + ngx;
                    discharge[down] += totalQ;
                    if (ord > maxChildOrder[down]) {
                        secondChildOrder[down] = maxChildOrder[down];
                        maxChildOrder[down] = (byte) ord;
                    } else if (ord > secondChildOrder[down]) {
                        secondChildOrder[down] = (byte) ord;
                    }
                }
            }
        }
        return lakes;
    }

    private static int heapPush(int[] cells, float[] keys, int size, int cell, float key) {
        int idx = size;
        cells[idx] = cell;
        keys[idx] = key;
        while (idx > 0) {
            int parent = (idx - 1) >>> 1;
            if (keys[parent] <= key) {
                break;
            }
            cells[idx] = cells[parent];
            keys[idx] = keys[parent];
            idx = parent;
        }
        cells[idx] = cell;
        keys[idx] = key;
        return size + 1;
    }

    private static int heapPop(int[] cells, float[] keys, int size) {
        int newSize = size - 1;
        if (newSize <= 0) {
            return 0;
        }
        int lastCell = cells[newSize];
        float lastKey = keys[newSize];
        int idx = 0;
        int half = newSize >>> 1;
        while (idx < half) {
            int left = (idx << 1) + 1;
            int right = left + 1;
            int best = (right < newSize && keys[right] < keys[left]) ? right : left;
            if (lastKey <= keys[best]) {
                break;
            }
            cells[idx] = cells[best];
            keys[idx] = keys[best];
            idx = best;
        }
        cells[idx] = lastCell;
        keys[idx] = lastKey;
        return newSize;
    }

    private static void computeClimate(
            long seed,
            short[] elevation,
            byte[] tempMonthly,
            byte[] precipMonthly,
            byte[] windSeasonal,
            byte[] currents,
            float[] annualRunoffMm,
            ForkJoinPool pool
    ) {
        // Precompute zonal continentality / distance-to-ocean on climate grid (1280x640)
        float[] distCoastWest = new float[CN];
        float[] distCoastEast = new float[CN];
        for (int cz = 0; cz < CH; cz++) {
            int row = cz * CW;
            float d = 0.0f;
            for (int pass = 0; pass < 2; pass++) {
                for (int cx = 0; cx < CW; cx++) {
                    short elev = elevation[(cz * 2) * W + (cx * 2)];
                    d = elev < 0 ? 0.0f : Math.min(1.0f, d + 128.0f / 5_000.0f);
                    distCoastWest[row + cx] = d;
                }
            }
            d = 0.0f;
            for (int pass = 0; pass < 2; pass++) {
                for (int cx = CW - 1; cx >= 0; cx--) {
                    short elev = elevation[(cz * 2) * W + (cx * 2)];
                    d = elev < 0 ? 0.0f : Math.min(1.0f, d + 128.0f / 5_000.0f);
                    distCoastEast[row + cx] = d;
                }
            }
        }

        pool.submit(() -> IntStream.range(0, CH).parallel().forEach(cz -> {
            double latDeg = 90.0 - ((cz + 0.5) / CH) * 180.0;
            double absLat = Math.abs(latDeg);
            double latRad = Math.toRadians(latDeg);
            boolean south = latDeg < 0.0;
            double mPeak = south ? 0.5 : 6.5;

            for (int cx = 0; cx < CW; cx++) {
                int cIdx = cz * CW + cx;
                int gx = cx * 2;
                int gz = cz * 2;
                short elevM = elevation[gz * W + gx];
                short elevW = elevation[gz * W + Math.floorMod(gx - 2, W)];
                short elevE = elevation[gz * W + Math.floorMod(gx + 2, W)];
                int bx = PlanetCoordinates.X_MIN + cx * PlanetCoordinates.CLIM_STEP;
                int bz = PlanetCoordinates.Z_MIN + cz * PlanetCoordinates.CLIM_STEP;

                // Prevailing wind direction: trades (0..30) blow westward; westerlies (30..60) blow eastward; polar easterlies (60..90)
                boolean westerlyBelt = absLat >= 30.0 && absLat <= 62.0;
                float cont = westerlyBelt ? distCoastWest[cIdx] : distCoastEast[cIdx];
                double orogSlope = westerlyBelt ? (elevM - elevW) / 256.0 : (elevM - elevE) / 256.0;

                // Ocean current SST anomaly: warm western boundary in subtropics, warm eastern boundary in high mid-lats
                double coastalDecay = Math.exp(-cont * 4.0);
                double sstAnom = westerlyBelt ? (2.8 * (1.0 - distCoastWest[cIdx]) * coastalDecay) : (2.2 * (1.0 - distCoastEast[cIdx]) * coastalDecay);

                // Seasonal wind & surface currents
                float baseWindU = westerlyBelt ? 6.5f : -5.0f;
                float baseWindV = (float) (absLat < 25.0 ? (south ? 1.8 : -1.8) : (south ? -1.2 : 1.2));
                for (int s = 0; s < 4; s++) {
                    windSeasonal[cIdx * 8 + s * 2] = (byte) Math.clamp(Math.round(baseWindU * 4.0f), -127, 127);
                    windSeasonal[cIdx * 8 + s * 2 + 1] = (byte) Math.clamp(Math.round(baseWindV * 4.0f), -127, 127);
                }
                if (elevM < 0) {
                    currents[cIdx * 2] = (byte) Math.clamp(Math.round(baseWindU * 1.5f), -127, 127);
                    currents[cIdx * 2 + 1] = (byte) Math.clamp(Math.round(baseWindV * 2.0f), -127, 127);
                }

                double hKm = Math.max(0.0, elevM) / 1_000.0;
                double amp = (2.0 + 0.35 * absLat) * (0.4 + 0.6 * cont);
                double regNoise = PeriodicNoise.sample2D(seed ^ 0xCC11L, bx, bz, 8192) * 1.8;
                float sumP = 0.0f;
                float sumT = 0.0f;

                for (int m = 0; m < 12; m++) {
                    double seasonal = amp * Math.cos(2.0 * Math.PI * (m - mPeak) / 12.0);
                    double tBase = 45.0 * Math.pow(Math.max(0.0, Math.cos(latRad)), 1.3) - 18.0 + seasonal + sstAnom + regNoise;
                    float tempC = (float) (tBase - 6.5 * hKm);
                    tempMonthly[cIdx * 12 + m] = (byte) Math.clamp(Math.round(tempC * 2.0f), -127, 127);
                    sumT += tempC;

                    double itczLat = 8.0 * Math.sin(2.0 * Math.PI * (m - 3.0) / 12.0);
                    double itczMask = Math.exp(-Math.pow((latDeg - itczLat) / 11.0, 2.0));
                    double subtropDry = Math.exp(-Math.pow((absLat - 27.0) / 7.5, 2.0));
                    double frontal = Math.exp(-Math.pow((absLat - 50.0) / 12.0, 2.0));
                    double monsoon = (absLat >= 10.0 && absLat <= 34.0 && distCoastEast[cIdx] < 0.45)
                            ? Math.max(0.0, Math.cos(2.0 * Math.PI * (m - mPeak) / 12.0)) * 140.0
                            : 0.0;
                    double orog = orogSlope > 0.0 ? orogSlope * 95.0 : orogSlope * 35.0; // windward boost + leeward rain shadow
                    double moistureDecay = elevM < 0 ? 1.15 : Math.exp(-cont * 1.45);
                    double rawP = (220.0 * itczMask + 115.0 * frontal + 28.0) * (1.0 - 0.78 * subtropDry) * moistureDecay + monsoon + orog;
                    float pMm = (float) Math.clamp(rawP, 2.0, 950.0);
                    precipMonthly[cIdx * 12 + m] = PlanetModel.encodePrecipMm(pMm);
                    sumP += pMm;
                }

                float meanT = sumT / 12.0f;
                float evap = Math.max(0.0f, (meanT + 10.0f) * 28.0f);
                float runoff = Math.max(15.0f, sumP - evap * 0.55f);
                // Populate corresponding 2x2 L0 block
                for (int dz = 0; dz < 2; dz++) {
                    for (int dx = 0; dx < 2; dx++) {
                        annualRunoffMm[(gz + dz) * W + (gx + dx)] = runoff;
                    }
                }
            }
        })).join();
    }

    private static void computeKoppenL0(
            short[] elevation,
            byte[] tempMonthly,
            byte[] precipMonthly,
            byte[] koppen,
            ForkJoinPool pool
    ) {
        pool.submit(() -> IntStream.range(0, H).parallel().forEach(gz -> {
            int cz = gz / 2;
            boolean north = gz < H / 2;
            int row = gz * W;
            for (int gx = 0; gx < W; gx++) {
                int cx = gx / 2;
                int cIdx = cz * CW + cx;
                float hDiffKm = (Math.max(0, elevation[row + gx]) - Math.max(0, elevation[(cz * 2) * W + (cx * 2)])) / 1000.0f;

                float tAnn = 0.0f;
                float tCold = 999.0f;
                float tHot = -999.0f;
                int warmMonthsAbove10 = 0;
                float pAnn = 0.0f;
                float pDry = 9999.0f;
                float pSummer = 0.0f;
                float pWinter = 0.0f;
                float pMaxSummer = 0.0f;
                float pMaxWinter = 0.0f;
                float pMinSummer = 9999.0f;
                float pMinWinter = 9999.0f;

                for (int m = 0; m < 12; m++) {
                    float t = tempMonthly[cIdx * 12 + m] * 0.5f - 6.5f * hDiffKm;
                    float p = PlanetModel.decodePrecipMm(precipMonthly[cIdx * 12 + m]);
                    tAnn += t;
                    if (t < tCold) tCold = t;
                    if (t > tHot) tHot = t;
                    if (t >= 10.0f) warmMonthsAbove10++;
                    pAnn += p;
                    if (p < pDry) pDry = p;
                    boolean summerHalf = north ? (m >= 3 && m <= 8) : (m < 3 || m > 8);
                    if (summerHalf) {
                        pSummer += p;
                        if (p > pMaxSummer) pMaxSummer = p;
                        if (p < pMinSummer) pMinSummer = p;
                    } else {
                        pWinter += p;
                        if (p > pMaxWinter) pMaxWinter = p;
                        if (p < pMinWinter) pMinWinter = p;
                    }
                }
                tAnn /= 12.0f;

                float summerShare = pAnn > 0.1f ? (pSummer / pAnn) : 0.5f;
                float pThCm = 2.0f * tAnn + (summerShare >= 0.70f ? 28.0f : (summerShare >= 0.30f ? 14.0f : 0.0f));
                float pThMm = Math.max(20.0f, pThCm * 10.0f);

                int code;
                if (tHot < 10.0f) {
                    code = tHot >= 0.0f ? 21 : 22; // ET, EF
                } else if (pAnn < pThMm) {
                    boolean desert = pAnn < 0.5f * pThMm;
                    boolean hot = tAnn >= 18.0f;
                    code = desert ? (hot ? 3 : 4) : (hot ? 5 : 6); // BWh, BWk, BSh, BSk
                } else if (tCold >= 18.0f) {
                    if (pDry >= 60.0f) code = 0; // Af
                    else if (pDry >= 100.0f - pAnn / 25.0f) code = 1; // Am
                    else code = 2; // Aw
                } else if (tCold > -3.0f) {
                    boolean drySummer = pMinSummer < 40.0f && pMaxWinter > 3.0f * pMinSummer;
                    boolean dryWinter = pMaxSummer > 10.0f * Math.max(1.0f, pMinWinter);
                    if (drySummer) code = tHot >= 22.0f ? 10 : 11; // Csa, Csb
                    else if (dryWinter) code = tHot >= 22.0f ? 12 : 13; // Cwa, Cwb
                    else code = tHot >= 22.0f ? 7 : (warmMonthsAbove10 >= 4 ? 8 : 9); // Cfa, Cfb, Cfc
                } else {
                    boolean dryWinter = pMaxSummer > 10.0f * Math.max(1.0f, pMinWinter);
                    if (tCold < -38.0f) code = 17; // Dfd
                    else if (dryWinter) code = tHot >= 22.0f ? 18 : (warmMonthsAbove10 >= 4 ? 19 : 20); // Dwa, Dwb, Dwc
                    else code = tHot >= 22.0f ? 14 : (warmMonthsAbove10 >= 4 ? 15 : 16); // Dfa, Dfb, Dfc
                }
                koppen[row + gx] = (byte) code;
            }
        })).join();
    }

    private static void computeGeologyL0(
            long seed,
            byte[] plate,
            byte[] crust,
            short[] elevation,
            byte[] sediment,
            byte[] riverOrder,
            byte[] koppen,
            byte[] province,
            byte[] rockTop,
            byte[] groundwater,
            ForkJoinPool pool
    ) {
        pool.submit(() -> IntStream.range(0, H).parallel().forEach(gz -> {
            int row = gz * W;
            int bz = PlanetCoordinates.Z_MIN + gz * PlanetCoordinates.L0_STEP;
            for (int gx = 0; gx < W; gx++) {
                int idx = row + gx;
                int bx = PlanetCoordinates.X_MIN + gx * PlanetCoordinates.L0_STEP;
                short elev = elevation[idx];
                boolean cont = (crust[idx] & 0x80) != 0;
                int age = crust[idx] & 0x7F;
                int kCode = koppen[idx] & 0xFF;
                double geoNoise = PeriodicNoise.sample2D(seed ^ 0x5EED_6E01L, bx, bz, 4096);

                GeoProvince prov;
                String rockHost;
                if (!cont && elev < -200) {
                    prov = age < 14 && geoNoise > 0.55 ? GeoProvince.OPHIOLITE : GeoProvince.OCEAN_CRUST;
                    rockHost = prov == GeoProvince.OPHIOLITE ? "peridotite" : "basalt_rock";
                } else if (elev > 2_600) {
                    prov = age < 18 ? GeoProvince.ARC : GeoProvince.YOUNG_OROGEN;
                    rockHost = prov == GeoProvince.ARC ? (geoNoise > 0.0 ? "andesite" : "diorite") : (geoNoise > 0.2 ? "slate" : "marble");
                } else if (elev > 900) {
                    prov = geoNoise > 0.4 ? GeoProvince.LIP : GeoProvince.OLD_OROGEN;
                    rockHost = prov == GeoProvince.LIP ? "basalt_rock" : (geoNoise > -0.1 ? "granite" : "schist");
                } else if (kCode <= 2 && geoNoise > 0.15) {
                    prov = GeoProvince.TROPICAL_REGOLITH;
                    rockHost = "granite";
                } else if ((sediment[idx] & 0xFF) > 28 || (elev >= 0 && elev < 160 && geoNoise < -0.15)) {
                    prov = GeoProvince.SEDIMENTARY_BASIN;
                    rockHost = geoNoise < -0.45 ? "mudstone" : (geoNoise < -0.25 ? "shale" : "sandstone_rock");
                } else if (elev >= -180 && elev < 260 && geoNoise > 0.25) {
                    prov = GeoProvince.CARBONATE_SHELF;
                    rockHost = geoNoise > 0.62 ? "chalk" : (geoNoise > 0.45 ? "dolomite" : "limestone");
                } else if (age < 12 && elev >= 0) {
                    prov = GeoProvince.RIFT;
                    rockHost = geoNoise > 0.0 ? "rhyolite" : "conglomerate";
                } else if (age > 45 && geoNoise > 0.0) {
                    prov = GeoProvince.CRATON_SHIELD;
                    rockHost = geoNoise > 0.45 ? "quartzite" : "gneiss";
                } else {
                    prov = GeoProvince.CRATON_PLATFORM;
                    rockHost = geoNoise > 0.1 ? "limestone" : "sandstone_rock";
                }

                province[idx] = (byte) prov.ordinal();
                WildlandsCatalog.RockSpec rSpec = WildlandsCatalog.ROCK_BY_HOST.getOrDefault(rockHost, WildlandsCatalog.ROCKS.getFirst());
                rockTop[idx] = (byte) rSpec.paletteIndex();

                float gwDepth = (elev < 0 || (riverOrder[idx] & 0xFF) >= 3) ? 0.3f
                        : (kCode >= 3 && kCode <= 6 ? 38.0f : Math.clamp(2.0f + Math.max(0, elev) * 0.015f, 0.4f, 65.0f));
                groundwater[idx] = PlanetModel.encodeGroundwaterMeters(gwDepth);
            }
        })).join();
    }

    private static void computeBiomes(
            long seed,
            short[] elevation,
            byte[] relief,
            byte[] riverOrder,
            short[] lake,
            List<PlanetModel.LakeInfo> lakes,
            byte[] koppen,
            byte[] province,
            byte[] rockTop,
            byte[] groundwater,
            byte[] tempMonthly,
            byte[] precipMonthly,
            List<PlanetModel.VolcanoInfo> volcanoes,
            byte[] biome,
            ForkJoinPool pool
    ) {
        boolean[] volcanoMask = new boolean[N];
        for (PlanetModel.VolcanoInfo v : volcanoes) {
            int vgx = PlanetModel.blockToL0X(v.blockX());
            int vgz = PlanetModel.blockToL0Z(v.blockZ());
            for (int dz = -4; dz <= 4; dz++) {
                int gz = Math.clamp(vgz + dz, 0, H - 1);
                for (int dx = -4; dx <= 4; dx++) {
                    if (dx * dx + dz * dz <= 16) {
                        volcanoMask[gz * W + Math.floorMod(vgx + dx, W)] = true;
                    }
                }
            }
        }

        boolean[] saltLakeIds = new boolean[65_536];
        for (PlanetModel.LakeInfo l : lakes) {
            if (l.id() >= 0 && l.id() < saltLakeIds.length && l.endorheic()) {
                saltLakeIds[l.id()] = true;
            }
        }

        byte[] rawBiome = new byte[N];
        pool.submit(() -> IntStream.range(0, H).parallel().forEach(gz -> {
            int row = gz * W;
            int cz = gz / 2;
            double latDeg = 90.0 - ((gz + 0.5) / H) * 180.0;
            double absLat = Math.abs(latDeg);

            for (int gx = 0; gx < W; gx++) {
                int idx = row + gx;
                int cIdx = cz * CW + (gx / 2);
                short elev = elevation[idx];
                int kCode = koppen[idx] & 0xFF;
                float tAnn = 0.0f;
                float pAnn = 0.0f;
                for (int m = 0; m < 12; m++) {
                    tAnn += tempMonthly[cIdx * 12 + m] * 0.5f;
                    pAnn += PlanetModel.decodePrecipMm(precipMonthly[cIdx * 12 + m]);
                }
                tAnn = tAnn / 12.0f - Math.max(0, elev) * 0.0065f;

                // 1. Ocean
                if (elev < 0) {
                    int b;
                    if (elev < -6_000) b = 9; // abyssal_trench
                    else if (tAnn < -1.5f) b = 0; // polar_sea_ice
                    else if (tAnn < 4.0f) b = elev > -200 ? 2 : 1; // cold_shelf_sea / polar_deep_ocean
                    else if (tAnn < 12.0f) b = elev > -200 ? 2 : 3;
                    else if (tAnn < 20.0f) b = elev > -200 ? 4 : 5;
                    else if (elev > -60 && tAnn > 22.0f) b = 8; // tropical_coral_reef
                    else b = elev > -200 ? 6 : 7;
                    rawBiome[idx] = (byte) b;
                    continue;
                }

                // Polar ice cap
                if (absLat >= 85.0) {
                    rawBiome[idx] = 55; // polar_ice_sheet
                    continue;
                }

                // 2. Lakes & major rivers
                int lk = lake[idx] & 0xFFFF;
                if (lk > 0) {
                    rawBiome[idx] = (byte) (saltLakeIds[lk] ? 21 : (tAnn < 4.0f ? 20 : 19));
                    continue;
                }
                int rOrd = riverOrder[idx] & 0xFF;
                if (rOrd >= 4) {
                    rawBiome[idx] = (byte) (kCode <= 2 ? 16 : (tAnn < 4.0f ? 18 : 17));
                    continue;
                }

                // 3. Coast check (within 1 L0 cell of ocean)
                boolean nearOcean = false;
                for (int d = 1; d <= 8; d++) {
                    int ngz = Math.clamp(gz + PlanetModel.D8_DZ[d], 0, H - 1);
                    int ngx = Math.floorMod(gx + PlanetModel.D8_DX[d], W);
                    if (elevation[ngz * W + ngx] < 0) {
                        nearOcean = true;
                        break;
                    }
                }
                if (nearOcean && elev < 90) {
                    if (rOrd >= 3) rawBiome[idx] = (byte) (kCode <= 2 ? 22 : 23); // delta
                    else if (kCode <= 2) rawBiome[idx] = (byte) (pAnn > 1800 ? 10 : 11);
                    else if (absLat > 48.0 && elev > 35) rawBiome[idx] = 15; // fjord
                    else if (tAnn < 6.0f) rawBiome[idx] = 14; // cold_rocky_shore
                    else rawBiome[idx] = (byte) (pAnn > 900 && elev < 20 ? 13 : 12);
                    continue;
                }

                // 4. Special geology (volcanic / karst / badlands / canyon)
                if (volcanoMask[idx]) {
                    rawBiome[idx] = (byte) ((gx + gz) % 3 == 0 ? 63 : 62);
                    continue;
                }
                int rTop = rockTop[idx] & 0xFF;
                if (kCode >= 7 && kCode <= 13 && pAnn > 1100.0f && (rTop == 12 || rTop == 13) && elev > 180) {
                    rawBiome[idx] = 45; // subtropical_karst_hills
                    continue;
                }
                if (kCode >= 3 && kCode <= 6 && elev > 250 && (relief[idx] & 0xFF) > 150) {
                    rawBiome[idx] = (byte) (rOrd >= 2 ? 42 : 40); // arid_canyon / arid_badlands
                    continue;
                }

                // 5. Floodplain & wetland
                if (rOrd == 3 && elev < 140) {
                    rawBiome[idx] = (byte) (kCode <= 2 ? 25 : 24);
                    continue;
                }
                if ((groundwater[idx] & 0xFF) < 28 && elev < 110 && pAnn > 450.0f) {
                    rawBiome[idx] = (byte) (kCode <= 2 ? 26 : (tAnn < 4.0f ? 28 : 27));
                    continue;
                }

                // 6. Highland (snowline & treeline per 10 §5.8.1)
                double aridity = Math.clamp(1.0 - pAnn / 1500.0, 0.0, 1.0);
                double snowline = 5_200.0 * Math.pow(Math.max(0.0, Math.cos(Math.toRadians(latDeg))), 1.5) - 400.0 + 500.0 * aridity;
                double treeline = snowline - 900.0;
                if (elev > snowline) {
                    rawBiome[idx] = 60; // alpine_glacier
                    continue;
                }
                if (elev > 3_500 && (relief[idx] & 0xFF) < 120) {
                    rawBiome[idx] = 61; // cold_high_plateau
                    continue;
                }
                if (elev > treeline) {
                    if ((relief[idx] & 0xFF) > 185) rawBiome[idx] = 59; // alpine_rocky_peaks
                    else rawBiome[idx] = (byte) (pAnn > 700.0f ? 57 : 58); // alpine_meadow / alpine_tundra
                    continue;
                }
                if (elev > treeline - 750.0 && kCode > 2) {
                    rawBiome[idx] = (byte) (kCode <= 6 ? 58 : 56); // temperate_montane_forest
                    continue;
                }
                if (kCode <= 2 && elev > 1_200 && pAnn > 1_800.0f) {
                    rawBiome[idx] = 33; // tropical_cloud_forest
                    continue;
                }

                // 7. Koppen master table
                int b = switch (kCode) {
                    case 0 -> 29; // Af -> tropical_rainforest
                    case 1 -> 30; // Am -> tropical_monsoon_forest
                    case 2 -> pAnn >= 1_000.0f ? 31 : 32; // Aw -> dry_forest / savanna
                    case 3 -> pAnn < 90.0f && elev < 140 ? 41 : ((rTop == 10 || (gx + gz) % 2 == 0) ? 34 : 35); // BWh
                    case 4 -> 38; // BWk -> cold_desert
                    case 5 -> 37; // BSh -> hot_steppe
                    case 6 -> pAnn > 360.0f ? 50 : 39; // BSk -> grassland / cold_steppe
                    case 7 -> 43; // Cfa -> humid_subtropical_forest
                    case 8, 9 -> pAnn > 2_000.0f ? 49 : 48; // Cfb/Cfc -> temperate_rainforest / broadleaf
                    case 10, 11 -> pAnn >= 600.0f ? 47 : 46; // Csa/Csb -> mediterranean
                    case 12, 13 -> 44; // Cwa/Cwb -> subtropical_monsoon_forest
                    case 14, 15, 18, 19 -> pAnn < 560.0f ? 50 : 51; // Dfa/Dfb -> grassland / mixed_forest
                    case 16, 20 -> 52; // Dfc/Dwc -> boreal_forest
                    case 17 -> 53; // Dfd -> boreal_larch_forest
                    case 21 -> 54; // ET -> polar_tundra
                    default -> 55; // EF -> polar_ice_sheet
                };
                rawBiome[idx] = (byte) b;
            }
        })).join();

        // Majority-vote smoothing pass (preserving rivers, lakes, ocean/land divide, and volcanic features)
        pool.submit(() -> IntStream.range(0, H).parallel().forEach(gz -> {
            int row = gz * W;
            int[] counts = new int[69];
            int[] touched = new int[9];
            for (int gx = 0; gx < W; gx++) {
                int idx = row + gx;
                int self = rawBiome[idx] & 0xFF;
                if (self <= 25 || self >= 62) {
                    biome[idx] = (byte) self;
                    continue;
                }
                int tCount = 0;
                int bestB = self;
                int bestC = 0;
                for (int dz = -1; dz <= 1; dz++) {
                    int ngz = Math.clamp(gz + dz, 0, H - 1);
                    for (int dx = -1; dx <= 1; dx++) {
                        int ngx = Math.floorMod(gx + dx, W);
                        int nb = rawBiome[ngz * W + ngx] & 0xFF;
                        if (nb < 10) continue;
                        if (counts[nb] == 0) touched[tCount++] = nb;
                        int c = ++counts[nb];
                        if (c > bestC) {
                            bestC = c;
                            bestB = nb;
                        }
                    }
                }
                for (int i = 0; i < tCount; i++) {
                    counts[touched[i]] = 0;
                }
                biome[idx] = (byte) bestB;
            }
        })).join();
    }

    private static List<Deposit> generateDeposits(
            long seed,
            short[] elevation,
            byte[] province,
            byte[] rockTop,
            byte[] riverOrder,
            byte[] biome
    ) {
        Random rng = new Random(seed ^ 0xDE90_5175L);
        int targetTotal = 18_000;
        List<Deposit> out = new ArrayList<>(targetTotal);
        long nextId = 1L;

        for (WildlandsCatalog.MineralSpec min : WildlandsCatalog.MINERALS) {
            if (min.abundanceFraction() <= 0.0f) {
                continue;
            }
            int count = Math.max(12, Math.round(targetTotal * min.abundanceFraction()));
            DepositType dType = defaultDepositTypeFor(min.key());
            EnumSet<Deposit.Sign> signs = defaultSignsFor(min.key());

            for (int i = 0; i < count; i++) {
                int gx = rng.nextInt(W);
                int gz = 24 + rng.nextInt(H - 48);
                // Prefer land or shallow shelf cells
                for (int tries = 0; tries < 6; tries++) {
                    if (elevation[gz * W + gx] >= -80) {
                        break;
                    }
                    gx = rng.nextInt(W);
                    gz = 24 + rng.nextInt(H - 48);
                }
                int idx = gz * W + gx;
                int bx = PlanetCoordinates.X_MIN + gx * PlanetCoordinates.L0_STEP + rng.nextInt(64);
                int bz = PlanetCoordinates.Z_MIN + gz * PlanetCoordinates.L0_STEP + rng.nextInt(64);
                int surfY = PlanetCoordinates.blockYForAltitude(elevation[idx]);
                int depthOffset = switch (dType) {
                    case BOG, LATERITE, PLACER, KAOLIN, CHALK_FLINT -> 2 + rng.nextInt(12);
                    case COAL_BASIN, SEDEX_MVT, MAGMATIC_SULFIDE, KIMBERLITE -> 25 + rng.nextInt(140);
                    default -> 10 + rng.nextInt(90);
                };
                int cy = Math.clamp(surfY - depthOffset, PlanetCoordinates.Y_MIN + 16, surfY);
                int rxz = 16 + rng.nextInt(48);
                int ry = 6 + rng.nextInt(24);

                Identifier hostId;
                if (!min.validHosts().isEmpty()) {
                    String chosenHost = min.validHosts().get(rng.nextInt(min.validHosts().size()));
                    WildlandsCatalog.RockSpec rSpec = WildlandsCatalog.ROCK_BY_HOST.get(chosenHost);
                    hostId = rSpec != null ? rSpec.id() : Identifier.withDefaultNamespace(chosenHost);
                } else {
                    hostId = WildlandsCatalog.rockByPalette(rockTop[idx] & 0xFF).id();
                }
                float gradeMean = min.gradeMin() + rng.nextFloat() * (min.gradeMax() - min.gradeMin());
                long tonnage = Math.max(120L, (long) (rxz * rxz * ry * 0.35 * gradeMean));
                long depId = PeriodicNoise.mix64(seed ^ (nextId++ * 0x9E3779B97F4A7C15L)) & Long.MAX_VALUE;

                out.add(new Deposit(
                        depId,
                        dType,
                        min.id(),
                        List.of(),
                        new BlockPos(PlanetCoordinates.wrapX(bx), cy, PlanetCoordinates.clampZ(bz)),
                        rxz,
                        ry,
                        rng.nextFloat() * 180.0f,
                        10.0f + rng.nextFloat() * 65.0f,
                        hostId,
                        gradeMean,
                        tonnage,
                        false,
                        signs
                ));
            }
        }
        return out;
    }

    private static DepositType defaultDepositTypeFor(String key) {
        return switch (key) {
            case "chalcopyrite", "molybdenite" -> DepositType.PORPHYRY;
            case "native_gold", "native_silver", "cinnabar" -> DepositType.EPITHERMAL;
            case "galena", "sphalerite", "calamine" -> DepositType.SEDEX_MVT;
            case "cassiterite", "wolframite", "spodumene", "emerald" -> DepositType.GREISEN_PEGMATITE;
            case "magnetite" -> DepositType.SKARN;
            case "hematite" -> DepositType.BIF;
            case "bog_iron" -> DepositType.BOG;
            case "pentlandite", "cobaltite" -> DepositType.MAGMATIC_SULFIDE;
            case "chromite" -> DepositType.PODIFORM_CHROMITE;
            case "bauxite" -> DepositType.LATERITE;
            case "ilmenite", "rutile", "monazite" -> DepositType.PLACER;
            case "sylvite" -> DepositType.EVAPORITE;
            case "phosphorite" -> DepositType.PHOSPHORITE;
            case "uraninite" -> DepositType.SANDSTONE_URANIUM;
            case "native_copper" -> DepositType.HYDROTHERMAL_VEIN;
            case "diamond" -> DepositType.KIMBERLITE;
            case "ruby", "sapphire", "jade", "lapis" -> DepositType.METAMORPHIC_GEM;
            case "sulfur", "amethyst" -> DepositType.VOLCANIC_NATIVE;
            case "niter" -> DepositType.CAVE_NITER;
            case "flint" -> DepositType.CHALK_FLINT;
            case "lignite", "bituminous_coal", "anthracite" -> DepositType.COAL_BASIN;
            case "kaolinite" -> DepositType.KAOLIN;
            default -> DepositType.GRAPHITE_SCHIST;
        };
    }

    private static EnumSet<Deposit.Sign> defaultSignsFor(String key) {
        return switch (key) {
            case "chalcopyrite", "pentlandite" -> EnumSet.of(Deposit.Sign.GOSSAN, Deposit.Sign.ORE_FLOAT);
            case "malachite", "native_copper" -> EnumSet.of(Deposit.Sign.MALACHITE_STAIN, Deposit.Sign.ORE_FLOAT);
            case "sulfur", "cinnabar" -> EnumSet.of(Deposit.Sign.FUMAROLE);
            case "magnetite", "hematite" -> EnumSet.of(Deposit.Sign.MAGNETIC_ANOMALY, Deposit.Sign.IRON_RIDGE);
            case "lignite", "bituminous_coal", "anthracite" -> EnumSet.of(Deposit.Sign.COAL_SEAM);
            case "diamond" -> EnumSet.of(Deposit.Sign.BLUE_GROUND);
            case "sylvite" -> EnumSet.of(Deposit.Sign.SALT_CRUST);
            default -> EnumSet.of(Deposit.Sign.LOOSE_STONES, Deposit.Sign.ORE_FLOAT);
        };
    }

    private static List<Reservoir> generateReservoirs(long seed, short[] elevation, byte[] province) {
        Random rng = new Random(seed ^ 0x0116_A500L);
        int count = 750;
        List<Reservoir> out = new ArrayList<>(count);
        Reservoir.Kind[] kinds = Reservoir.Kind.values();
        for (int i = 0; i < count; i++) {
            int gx = rng.nextInt(W);
            int gz = 30 + rng.nextInt(H - 60);
            int bx = PlanetCoordinates.X_MIN + gx * PlanetCoordinates.L0_STEP + 32;
            int bz = PlanetCoordinates.Z_MIN + gz * PlanetCoordinates.L0_STEP + 32;
            int surfY = PlanetCoordinates.blockYForAltitude(elevation[gz * W + gx]);
            int topY = Math.max(-420, surfY - (80 + rng.nextInt(180)));
            int botY = Math.max(-480, topY - (20 + rng.nextInt(50)));
            int prov = province[gz * W + gx] & 0xFF;
            Reservoir.Kind kind = (prov == GeoProvince.ARC.ordinal() || prov == GeoProvince.RIFT.ordinal())
                    ? Reservoir.Kind.GEOTHERMAL
                    : kinds[i % 3];
            long cap = 5_000_000L + rng.nextInt(45_000_000);
            out.add(new Reservoir(i + 1L, kind, new BlockPos(bx, (topY + botY) / 2, bz), 96 + rng.nextInt(192), topY, botY, cap, cap, 80.0f + rng.nextFloat() * 160.0f));
        }
        return out;
    }

    private static List<PlanetModel.DeepConduitInfo> generateDeepConduits(
            long seed,
            List<PlanetModel.HotspotInfo> hotspots,
            List<PlanetModel.FaultInfo> faults
    ) {
        Random rng = new Random(seed ^ 0xDEE9_C04DL);
        List<PlanetModel.DeepConduitInfo> out = new ArrayList<>(80);
        int id = 1;
        for (PlanetModel.HotspotInfo hs : hotspots) {
            int n = 2 + rng.nextInt(2);
            for (int i = 0; i < n; i++) {
                int bx = PlanetCoordinates.wrapX(hs.blockX() + rng.nextInt(1024) - 512);
                int bz = PlanetCoordinates.clampZ(hs.blockZ() + rng.nextInt(1024) - 512);
                out.add(new PlanetModel.DeepConduitInfo(id++, bx, bz, -496, 180.0f + hs.strength() * 140.0f));
            }
        }
        int step = Math.max(1, faults.size() / 48);
        for (int i = 0; i < faults.size() && out.size() < 96; i += step) {
            PlanetModel.FaultInfo f = faults.get(i);
            out.add(new PlanetModel.DeepConduitInfo(id++, PlanetCoordinates.wrapX(f.x1()), PlanetCoordinates.clampZ(f.z1()), -496, 145.0f + rng.nextFloat() * 80.0f));
        }
        return out;
    }

    private static List<PlanetModel.StructureSiteInfo> generateStructures(
            long seed,
            short[] elevation,
            byte[] province,
            byte[] biome,
            List<PlanetModel.VolcanoInfo> volcanoes
    ) {
        Random rng = new Random(seed ^ 0x57B0_C700L);
        List<PlanetModel.StructureSiteInfo> out = new ArrayList<>(2_048);
        int id = 1;
        for (PlanetModel.VolcanoInfo v : volcanoes) {
            out.add(new PlanetModel.StructureSiteInfo(id++, (byte) 13, v.blockX(), v.blockZ())); // volcanic_vent
            out.add(new PlanetModel.StructureSiteInfo(id++, (byte) 4, PlanetCoordinates.wrapX(v.blockX() + 192), PlanetCoordinates.clampZ(v.blockZ() + 192))); // geyser_field
        }
        for (int i = 0; i < 1_500; i++) {
            int gx = rng.nextInt(W);
            int gz = 24 + rng.nextInt(H - 48);
            if (elevation[gz * W + gx] < 0) continue;
            int bx = PlanetCoordinates.X_MIN + gx * PlanetCoordinates.L0_STEP + 32;
            int bz = PlanetCoordinates.Z_MIN + gz * PlanetCoordinates.L0_STEP + 32;
            byte sIdx = (byte) (i % 15);
            out.add(new PlanetModel.StructureSiteInfo(id++, sIdx, bx, bz));
        }
        return out;
    }

    private static List<PlanetModel.StartSiteCandidate> selectStartSites(
            long seed,
            short[] elevation,
            byte[] relief,
            byte[] riverOrder,
            short[] lake,
            byte[] koppen,
            byte[] biome,
            byte[] rockTop,
            List<PlanetModel.VolcanoInfo> volcanoes
    ) {
        record ScoredCell(int gx, int gz, int score, int waterDistBlocks) {
        }
        List<ScoredCell> candidates = new ArrayList<>(4_096);

        for (double maxLat : new double[]{55.0, 65.0, 80.0}) {
            candidates.clear();
            for (int gz = 40; gz < H - 40; gz += 4) {
                double lat = Math.abs(90.0 - ((gz + 0.5) / H) * 180.0);
                if (lat > maxLat) {
                    continue;
                }
                for (int gx = 0; gx < W; gx += 4) {
                    int idx = gz * W + gx;
                    short elevM = elevation[idx];
                    // Strictly on land above sea level, below 2200 m
                    if (elevM < 18 || elevM > 2_200) {
                        continue;
                    }
                    int kCode = koppen[idx] & 0xFF;
                    if (maxLat <= 55.0 && (kCode == 3 || kCode == 4 || kCode == 21 || kCode == 22)) {
                        continue; // exclude BWh, BWk, ET, EF
                    }
                    int bCode = biome[idx] & 0xFF;
                    if (bCode < 10 || (bCode >= 16 && bCode <= 21)) {
                        continue; // exclude ocean, river channel, and open lake cells
                    }

                    // Check nearest freshwater within 4 L0 cells (256 blocks)
                    int minWaterCells = 99;
                    for (int dz = -4; dz <= 4; dz++) {
                        int ngz = Math.clamp(gz + dz, 0, H - 1);
                        for (int dx = -4; dx <= 4; dx++) {
                            int ngx = Math.floorMod(gx + dx, W);
                            int nIdx = ngz * W + ngx;
                            if ((riverOrder[nIdx] & 0xFF) >= 1 || (lake[nIdx] & 0xFFFF) > 0) {
                                int d = Math.abs(dx) + Math.abs(dz);
                                if (d < minWaterCells) minWaterCells = d;
                            }
                        }
                    }
                    if (maxLat <= 55.0 && minWaterCells > 5) {
                        continue;
                    }

                    int bx = PlanetCoordinates.X_MIN + gx * PlanetCoordinates.L0_STEP + 32;
                    int bz = PlanetCoordinates.Z_MIN + gz * PlanetCoordinates.L0_STEP + 32;
                    boolean nearVolcano = false;
                    for (PlanetModel.VolcanoInfo v : volcanoes) {
                        long dx = PlanetCoordinates.wrappedDx(bx, v.blockX());
                        long dz = (long) bz - v.blockZ();
                        if (dx * dx + dz * dz < 1_000L * 1_000L) {
                            nearVolcano = true;
                            break;
                        }
                    }
                    if (nearVolcano) {
                        continue;
                    }

                    int waterScore = Math.clamp(100 - minWaterCells * 16, 20, 100);
                    int reliefPenalty = (relief[idx] & 0xFF) / 5;
                    int climBonus = (kCode >= 7 && kCode <= 15) ? 25 : 12;
                    int totalScore = Math.clamp((int) Math.round(waterScore * 0.35 + (100 - reliefPenalty) * 0.35 + climBonus), 35, 99);
                    candidates.add(new ScoredCell(gx, gz, totalScore, Math.min(256, minWaterCells * 48 + 24)));
                }
            }
            if (candidates.size() >= 10) {
                break;
            }
        }

        candidates.sort(Comparator.comparingInt(ScoredCell::score).reversed().thenComparingInt(ScoredCell::gx));

        // Greedily pick top 5 spaced >= 4,000 blocks apart, then append remaining up to 200
        List<ScoredCell> chosen = new ArrayList<>(200);
        for (ScoredCell sc : candidates) {
            if (chosen.size() >= 5) {
                break;
            }
            int bx = PlanetCoordinates.X_MIN + sc.gx() * PlanetCoordinates.L0_STEP + 32;
            int bz = PlanetCoordinates.Z_MIN + sc.gz() * PlanetCoordinates.L0_STEP + 32;
            boolean farEnough = true;
            for (ScoredCell prev : chosen) {
                int px = PlanetCoordinates.X_MIN + prev.gx() * PlanetCoordinates.L0_STEP + 32;
                int pz = PlanetCoordinates.Z_MIN + prev.gz() * PlanetCoordinates.L0_STEP + 32;
                double dist = Math.hypot(PlanetCoordinates.wrappedDx(bx, px), bz - pz);
                if (dist < 4_000.0) {
                    farEnough = false;
                    break;
                }
            }
            if (farEnough) {
                chosen.add(sc);
            }
        }
        for (ScoredCell sc : candidates) {
            if (chosen.size() >= 200) break;
            if (!chosen.contains(sc)) {
                chosen.add(sc);
            }
        }

        // Fallback guarantee: if still empty, pick highest land cell
        if (chosen.isEmpty()) {
            for (int i = 0; i < N; i++) {
                if (elevation[i] > 20) {
                    chosen.add(new ScoredCell(i % W, i / W, 60, 64));
                    if (chosen.size() >= 5) break;
                }
            }
        }

        List<PlanetModel.StartSiteCandidate> result = new ArrayList<>(chosen.size());
        for (int i = 0; i < chosen.size(); i++) {
            ScoredCell sc = chosen.get(i);
            int idx = sc.gz() * W + sc.gx();
            int bx = PlanetCoordinates.X_MIN + sc.gx() * PlanetCoordinates.L0_STEP + 32;
            int bz = PlanetCoordinates.Z_MIN + sc.gz() * PlanetCoordinates.L0_STEP + 32;
            int sy = Math.max(PlanetCoordinates.SEA_LEVEL + 2, PlanetCoordinates.blockYForAltitude(elevation[idx]));
            String kop = PlanetModel.KOPPEN_PALETTE[Math.clamp(koppen[idx] & 0xFF, 0, PlanetModel.KOPPEN_PALETTE.length - 1)];
            Identifier bId = WildlandsCatalog.biomeByIndex(biome[idx] & 0xFF).id();
            Identifier rId = WildlandsCatalog.rockByPalette(rockTop[idx] & 0xFF).id();
            result.add(new PlanetModel.StartSiteCandidate(i, bx, bz, sy, sc.score(), kop, bId, rId, sc.waterDistBlocks()));
        }
        return result;
    }

    private static long computePlanetDigest(long seed, short[] elevation, byte[] biome, int depositCount) {
        long h = PeriodicNoise.mix64(seed ^ depositCount);
        for (int i = 0; i < elevation.length; i += 64) {
            h = PeriodicNoise.mix64(h ^ ((long) elevation[i] << 16) ^ (biome[i] & 0xFFL));
        }
        return h;
    }
}
