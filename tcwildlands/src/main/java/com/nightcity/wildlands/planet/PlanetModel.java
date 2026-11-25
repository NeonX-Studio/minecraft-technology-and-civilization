package com.nightcity.wildlands.planet;

import com.nightcity.tcivilization.RegionCoordinates;
import com.nightcity.tcivilization.api.world.ClimateNormals;
import com.nightcity.tcivilization.api.world.Deposit;
import com.nightcity.tcivilization.api.world.FloodStage;
import com.nightcity.tcivilization.api.world.GeoProvince;
import com.nightcity.tcivilization.api.world.PlanetSummary;
import com.nightcity.tcivilization.api.world.Reservoir;
import com.nightcity.tcivilization.api.world.RiverInfo;
import com.nightcity.tcivilization.api.world.StrataColumn;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec2;

/**
 * Immutable, thread-safe L0 + Climate + Object-Table planet model backed by planet.bin (10 §5.2, §11.2).
 * Provides O(1) wrapped lookups, C1-continuous bicubic interpolation across the x = ±81,920 seam,
 * and deterministic L1 (64x64 + 4-skirt = 72x72 at 8-block resolution) tile caching.
 */
public final class PlanetModel {
    public static final int GRID_W = PlanetCoordinates.L0_WIDTH;   // 2560
    public static final int GRID_H = PlanetCoordinates.L0_HEIGHT;  // 1280
    public static final int CLIM_W = PlanetCoordinates.CLIM_WIDTH; // 1280
    public static final int CLIM_H = PlanetCoordinates.CLIM_HEIGHT;// 640

    public static final String[] KOPPEN_PALETTE = {
            "Af", "Am", "Aw", "BWh", "BWk", "BSh", "BSk",
            "Cfa", "Cfb", "Cfc", "Csa", "Csb", "Cwa", "Cwb",
            "Dfa", "Dfb", "Dfc", "Dfd", "Dwa", "Dwb", "Dwc",
            "ET", "EF"
    };

    public static final int[] D8_DX = {0, 1, 1, 1, 0, -1, -1, -1, 0};
    public static final int[] D8_DZ = {0, -1, 0, 1, 1, 1, 0, -1, -1};

    public record PlateInfo(
            int id,
            boolean continental,
            float centerX,
            float centerY,
            float centerZ,
            float poleX,
            float poleY,
            float poleZ,
            float omega,
            float weight
    ) {
    }

    public record HotspotInfo(int id, int blockX, int blockZ, float strength) {
    }

    public record VolcanoInfo(int id, int blockX, int blockZ, byte kind, byte maxVei, int recurrenceDays) {
    }

    public record FaultInfo(int id, int x1, int z1, int x2, int z2, byte boundaryKind, float maxMagnitude, float loadingRate) {
    }

    public record LakeInfo(int id, int centerX, int centerZ, short surfaceMeters, int areaCells, boolean endorheic, float salinity) {
    }

    public record DeepConduitInfo(int id, int blockX, int blockZ, int thresholdY, float heatFlowMw) {
    }

    public record StructureSiteInfo(int id, byte structureIndex, int blockX, int blockZ) {
    }

    public record StartSiteCandidate(
            int index,
            int blockX,
            int blockZ,
            int surfaceY,
            int score,
            String koppen,
            Identifier biome,
            Identifier primaryRock,
            int waterDistanceBlocks
    ) {
    }

    public record ContinentStat(int id, int cellCount, int spanXBlocks, int spanZBlocks, int maxSpanBlocks, boolean touchesMidLatitude) {
    }

    public record L1Tile(
            int rx,
            int rz,
            short[] heightQ16,
            short[] waterSurfaceQ16,
            byte[] channelWidth,
            byte[] flowVx,
            byte[] flowVz,
            byte[] biome,
            byte[] cliffMask,
            byte[] rockTop,
            List<Deposit> nearbyDeposits
    ) {
        public static final int SKIRT = 4;
        public static final int CORE = 64;
        public static final int SIZE = CORE + SKIRT * 2; // 72
        public static final int STEP = 8;
    }

    private final int formatVersion;
    private final int generatorVersion;
    private final long worldSeed;
    private final long planetDigest;
    private final byte[] paramsDigest;
    private final int plateCount;
    private final int continentCount;
    private final int hotspotCount;
    private final float oceanFraction;
    private final PlanetSummary summary;

    // L0 arrays (length = GRID_W * GRID_H = 3,276,800)
    private final byte[] plate;
    private final byte[] crust;
    private final short[] elevation;
    private final byte[] relief;
    private final byte[] sediment;
    private final byte[] flowDir;
    private final short[] flowAcc;
    private final short[] basin;
    private final short[] lake;
    private final byte[] riverOrder;
    private final byte[] province;
    private final byte[] rockTop;
    private final byte[] groundwater;
    private final byte[] koppen;
    private final byte[] biome;

    // Climate arrays (base = CLIM_W * CLIM_H = 819,200)
    private final byte[] tempMonthly;   // * 12 (0.5 C step)
    private final byte[] precipMonthly; // * 12 (log encoded)
    private final byte[] windSeasonal;  // * 8  (0.25 m/s step)
    private final byte[] currents;      // * 2  (0.1 m/s step)

    // Object tables
    private final List<PlateInfo> plates;
    private final List<HotspotInfo> hotspots;
    private final List<VolcanoInfo> volcanoes;
    private final List<FaultInfo> faults;
    private final List<LakeInfo> lakes;
    private final List<Deposit> deposits;
    private final List<Reservoir> reservoirs;
    private final List<DeepConduitInfo> deepConduits;
    private final List<StructureSiteInfo> structures;
    private final List<StartSiteCandidate> startSites;
    private final List<ContinentStat> continentStats;

    // Spatial indices
    private final Map<Long, Deposit> depositById;
    private final Map<Long, List<Deposit>> depositsByRegion;
    private final Map<Long, Reservoir> reservoirById;
    private final Map<Long, List<Reservoir>> reservoirsByRegion;
    private final Map<Integer, LakeInfo> lakeById;

    // Thread-safe L1 LRU cache
    private final Map<Long, L1Tile> l1Cache;

    public PlanetModel(
            int formatVersion,
            int generatorVersion,
            long worldSeed,
            long planetDigest,
            byte[] paramsDigest,
            int plateCount,
            int continentCount,
            int hotspotCount,
            float oceanFraction,
            byte[] plate,
            byte[] crust,
            short[] elevation,
            byte[] relief,
            byte[] sediment,
            byte[] flowDir,
            short[] flowAcc,
            short[] basin,
            short[] lake,
            byte[] riverOrder,
            byte[] province,
            byte[] rockTop,
            byte[] groundwater,
            byte[] koppen,
            byte[] biome,
            byte[] tempMonthly,
            byte[] precipMonthly,
            byte[] windSeasonal,
            byte[] currents,
            List<PlateInfo> plates,
            List<HotspotInfo> hotspots,
            List<VolcanoInfo> volcanoes,
            List<FaultInfo> faults,
            List<LakeInfo> lakes,
            List<Deposit> deposits,
            List<Reservoir> reservoirs,
            List<DeepConduitInfo> deepConduits,
            List<StructureSiteInfo> structures,
            List<StartSiteCandidate> startSites,
            List<ContinentStat> continentStats
    ) {
        this.formatVersion = formatVersion;
        this.generatorVersion = generatorVersion;
        this.worldSeed = worldSeed;
        this.planetDigest = planetDigest;
        this.paramsDigest = paramsDigest == null ? new byte[32] : paramsDigest.clone();
        this.plateCount = plateCount;
        this.continentCount = continentCount;
        this.hotspotCount = hotspotCount;
        this.oceanFraction = oceanFraction;
        this.plate = plate;
        this.crust = crust;
        this.elevation = elevation;
        this.relief = relief;
        this.sediment = sediment;
        this.flowDir = flowDir;
        this.flowAcc = flowAcc;
        this.basin = basin;
        this.lake = lake;
        this.riverOrder = riverOrder;
        this.province = province;
        this.rockTop = rockTop;
        this.groundwater = groundwater;
        this.koppen = koppen;
        this.biome = biome;
        this.tempMonthly = tempMonthly;
        this.precipMonthly = precipMonthly;
        this.windSeasonal = windSeasonal;
        this.currents = currents;
        this.plates = List.copyOf(plates);
        this.hotspots = List.copyOf(hotspots);
        this.volcanoes = List.copyOf(volcanoes);
        this.faults = List.copyOf(faults);
        this.lakes = List.copyOf(lakes);
        this.deposits = List.copyOf(deposits);
        this.reservoirs = List.copyOf(reservoirs);
        this.deepConduits = List.copyOf(deepConduits);
        this.structures = List.copyOf(structures);
        this.startSites = List.copyOf(startSites);
        this.continentStats = continentStats == null ? List.of() : List.copyOf(continentStats);

        int maxElevM = Short.MIN_VALUE;
        int peakGx = 0;
        int peakGz = 0;
        for (int gz = 0; gz < GRID_H; gz++) {
            int row = gz * GRID_W;
            for (int gx = 0; gx < GRID_W; gx++) {
                short e = elevation[row + gx];
                if (e > maxElevM) {
                    maxElevM = e;
                    peakGx = gx;
                    peakGz = gz;
                }
            }
        }
        int peakX = PlanetCoordinates.X_MIN + peakGx * PlanetCoordinates.L0_STEP + 32;
        int peakZ = PlanetCoordinates.Z_MIN + peakGz * PlanetCoordinates.L0_STEP + 32;
        int peakY = PlanetCoordinates.blockYForAltitude(maxElevM);
        this.summary = new PlanetSummary(
                worldSeed,
                plateCount,
                continentCount,
                hotspotCount,
                oceanFraction,
                peakY,
                maxElevM,
                new BlockPos(peakX, peakY, peakZ)
        );

        Map<Long, Deposit> depId = new LinkedHashMap<>(deposits.size());
        Map<Long, List<Deposit>> depReg = new LinkedHashMap<>();
        for (Deposit d : this.deposits) {
            depId.put(d.id(), d);
            long rk = RegionCoordinates.fromBlock(d.center().getX(), d.center().getZ()).key();
            depReg.computeIfAbsent(rk, k -> new ArrayList<>()).add(d);
        }
        this.depositById = Collections.unmodifiableMap(depId);
        this.depositsByRegion = Collections.unmodifiableMap(depReg);

        Map<Long, Reservoir> resId = new LinkedHashMap<>(reservoirs.size());
        Map<Long, List<Reservoir>> resReg = new LinkedHashMap<>();
        for (Reservoir r : this.reservoirs) {
            resId.put(r.id(), r);
            long rk = RegionCoordinates.fromBlock(r.center().getX(), r.center().getZ()).key();
            resReg.computeIfAbsent(rk, k -> new ArrayList<>()).add(r);
        }
        this.reservoirById = Collections.unmodifiableMap(resId);
        this.reservoirsByRegion = Collections.unmodifiableMap(resReg);

        Map<Integer, LakeInfo> lkMap = new LinkedHashMap<>(lakes.size());
        for (LakeInfo lk : this.lakes) {
            lkMap.put(lk.id(), lk);
        }
        this.lakeById = Collections.unmodifiableMap(lkMap);

        this.l1Cache = Collections.synchronizedMap(new LinkedHashMap<>(128, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, L1Tile> eldest) {
                return size() > 96;
            }
        });
    }

    public static int l0Index(int gx, int gz) {
        int wx = Math.floorMod(gx, GRID_W);
        int cz = Math.clamp(gz, 0, GRID_H - 1);
        return cz * GRID_W + wx;
    }

    public static int climIndex(int cx, int cz) {
        int wx = Math.floorMod(cx, CLIM_W);
        int kz = Math.clamp(cz, 0, CLIM_H - 1);
        return kz * CLIM_W + wx;
    }

    public static int blockToL0X(int x) {
        int wx = PlanetCoordinates.wrapX(x);
        return Math.floorMod(Math.floorDiv(wx - PlanetCoordinates.X_MIN, PlanetCoordinates.L0_STEP), GRID_W);
    }

    public static int blockToL0Z(int z) {
        int cz = PlanetCoordinates.clampZ(z);
        return Math.clamp(Math.floorDiv(cz - PlanetCoordinates.Z_MIN, PlanetCoordinates.L0_STEP), 0, GRID_H - 1);
    }

    public int formatVersion() { return formatVersion; }
    public int generatorVersion() { return generatorVersion; }
    public long worldSeed() { return worldSeed; }
    public long planetDigest() { return planetDigest; }
    public byte[] paramsDigest() { return paramsDigest.clone(); }
    public int plateCount() { return plateCount; }
    public int continentCount() { return continentCount; }
    public int hotspotCount() { return hotspotCount; }
    public float oceanFraction() { return oceanFraction; }
    public PlanetSummary summary() { return summary; }

    public byte[] rawPlate() { return plate; }
    public byte[] rawCrust() { return crust; }
    public short[] rawElevation() { return elevation; }
    public byte[] rawRelief() { return relief; }
    public byte[] rawSediment() { return sediment; }
    public byte[] rawFlowDir() { return flowDir; }
    public short[] rawFlowAcc() { return flowAcc; }
    public short[] rawBasin() { return basin; }
    public short[] rawLake() { return lake; }
    public byte[] rawRiverOrder() { return riverOrder; }
    public byte[] rawProvince() { return province; }
    public byte[] rawRockTop() { return rockTop; }
    public byte[] rawGroundwater() { return groundwater; }
    public byte[] rawKoppen() { return koppen; }
    public byte[] rawBiome() { return biome; }
    public byte[] rawTempMonthly() { return tempMonthly; }
    public byte[] rawPrecipMonthly() { return precipMonthly; }
    public byte[] rawWindSeasonal() { return windSeasonal; }
    public byte[] rawCurrents() { return currents; }

    public List<PlateInfo> plates() { return plates; }
    public List<HotspotInfo> hotspots() { return hotspots; }
    public List<VolcanoInfo> volcanoes() { return volcanoes; }
    public List<FaultInfo> faults() { return faults; }
    public List<LakeInfo> lakes() { return lakes; }
    public List<Deposit> deposits() { return deposits; }
    public List<Reservoir> reservoirs() { return reservoirs; }
    public List<DeepConduitInfo> deepConduits() { return deepConduits; }
    public List<StructureSiteInfo> structures() { return structures; }
    public List<StartSiteCandidate> startSites() { return startSites; }
    public List<ContinentStat> continentStats() { return continentStats; }

    public Optional<Deposit> depositById(long id) {
        return Optional.ofNullable(depositById.get(id));
    }

    public List<Deposit> depositsInRegion(long regionKey) {
        return depositsByRegion.getOrDefault(regionKey, List.of());
    }

    public Optional<Reservoir> reservoirById(long id) {
        return Optional.ofNullable(reservoirById.get(id));
    }

    public List<Reservoir> reservoirsInRegion(long regionKey) {
        return reservoirsByRegion.getOrDefault(regionKey, List.of());
    }

    public Optional<LakeInfo> lakeById(int id) {
        return Optional.ofNullable(lakeById.get(id));
    }

    public short elevationMetersL0(int gx, int gz) {
        return elevation[l0Index(gx, gz)];
    }

    public float decodeReliefMeters(int idx) {
        int u = relief[idx] & 0xFF;
        return (float) (4.0 * Math.pow(75.0, u / 255.0));
    }

    public static byte encodeReliefMeters(float meters) {
        double clamped = Math.clamp(meters, 4.0, 300.0);
        int u = (int) Math.round(255.0 * (Math.log(clamped / 4.0) / Math.log(75.0)));
        return (byte) Math.clamp(u, 0, 255);
    }

    public float decodeDischargeM3s(int idx) {
        int u = flowAcc[idx] & 0xFFFF;
        if (u == 0) {
            return 0.0f;
        }
        return (float) (Math.exp((u / 65535.0) * Math.log(250_000.0)) - 1.0);
    }

    public static short encodeDischargeM3s(double q) {
        if (q <= 0.0) {
            return 0;
        }
        double clamped = Math.min(249_999.0, q);
        int u = (int) Math.round(65535.0 * (Math.log(clamped + 1.0) / Math.log(250_000.0)));
        return (short) Math.clamp(u, 0, 65535);
    }

    public static byte encodePrecipMm(float mm) {
        if (mm <= 0.0f) {
            return 0;
        }
        double clamped = Math.min(1_200.0, mm);
        int u = (int) Math.round(255.0 * (Math.log(clamped + 1.0) / Math.log(1_201.0)));
        return (byte) Math.clamp(u, 0, 255);
    }

    public static float decodePrecipMm(byte encoded) {
        int u = encoded & 0xFF;
        if (u == 0) {
            return 0.0f;
        }
        return (float) (Math.exp((u / 255.0) * Math.log(1_201.0)) - 1.0);
    }

    public static byte encodeGroundwaterMeters(float meters) {
        double clamped = Math.clamp(meters, 0.1, 120.0);
        int u = (int) Math.round(255.0 * (Math.log(clamped / 0.1) / Math.log(1_200.0)));
        return (byte) Math.clamp(u, 0, 255);
    }

    public float decodeGroundwaterMeters(int idx) {
        int u = groundwater[idx] & 0xFF;
        return (float) (0.1 * Math.pow(1_200.0, u / 255.0));
    }

    private static double catmullRom(double p0, double p1, double p2, double p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return 0.5 * ((2.0 * p1)
                + (-p0 + p2) * t
                + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2
                + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3);
    }

    /**
     * Evaluates smooth C1 bicubic L0 altitude (in meters) at any world block coordinate (x, z),
     * wrapping X periodically across x = ±81,920 with zero seam.
     */
    public double sampleBicubicAltitudeMeters(int x, int z) {
        int wx = PlanetCoordinates.wrapX(x);
        int cz = PlanetCoordinates.clampZ(z);
        double fx = (wx - PlanetCoordinates.X_MIN) / (double) PlanetCoordinates.L0_STEP - 0.5;
        double fz = (cz - PlanetCoordinates.Z_MIN) / (double) PlanetCoordinates.L0_STEP - 0.5;
        int gx0 = (int) Math.floor(fx);
        int gz0 = (int) Math.floor(fz);
        double tx = fx - gx0;
        double tz = fz - gz0;

        double[] col = new double[4];
        for (int dz = -1; dz <= 2; dz++) {
            int gz = gz0 + dz;
            double v0 = elevationMetersL0(gx0 - 1, gz);
            double v1 = elevationMetersL0(gx0, gz);
            double v2 = elevationMetersL0(gx0 + 1, gz);
            double v3 = elevationMetersL0(gx0 + 2, gz);
            col[dz + 1] = catmullRom(v0, v1, v2, v3, tx);
        }
        return catmullRom(col[0], col[1], col[2], col[3], tz);
    }

    public float sampleBilinearReliefMeters(int x, int z) {
        int wx = PlanetCoordinates.wrapX(x);
        int cz = PlanetCoordinates.clampZ(z);
        double fx = (wx - PlanetCoordinates.X_MIN) / (double) PlanetCoordinates.L0_STEP - 0.5;
        double fz = (cz - PlanetCoordinates.Z_MIN) / (double) PlanetCoordinates.L0_STEP - 0.5;
        int gx0 = (int) Math.floor(fx);
        int gz0 = (int) Math.floor(fz);
        double tx = fx - gx0;
        double tz = fz - gz0;
        float r00 = decodeReliefMeters(l0Index(gx0, gz0));
        float r10 = decodeReliefMeters(l0Index(gx0 + 1, gz0));
        float r01 = decodeReliefMeters(l0Index(gx0, gz0 + 1));
        float r11 = decodeReliefMeters(l0Index(gx0 + 1, gz0 + 1));
        double rx0 = r00 + (r10 - r00) * tx;
        double rx1 = r01 + (r11 - r01) * tx;
        return (float) (rx0 + (rx1 - rx0) * tz);
    }

    public L1Tile getOrComputeL1Tile(int rx, int rz) {
        int normRx = Math.floorMod(rx + RegionCoordinates.REGION_WIDTH / 2, RegionCoordinates.REGION_WIDTH) - RegionCoordinates.REGION_WIDTH / 2;
        int clampedRz = Math.clamp(rz, -RegionCoordinates.REGION_HEIGHT / 2, RegionCoordinates.REGION_HEIGHT / 2 - 1);
        long key = RegionCoordinates.key(normRx, clampedRz);
        L1Tile cached = l1Cache.get(key);
        if (cached != null) {
            return cached;
        }
        L1Tile computed = synthesizeL1Tile(normRx, clampedRz);
        l1Cache.put(key, computed);
        return computed;
    }

    private L1Tile synthesizeL1Tile(int rx, int rz) {
        int n = L1Tile.SIZE;
        int total = n * n;
        short[] heightQ16 = new short[total];
        short[] waterSurfaceQ16 = new short[total];
        byte[] channelWidth = new byte[total];
        byte[] flowVx = new byte[total];
        byte[] flowVz = new byte[total];
        byte[] tileBiome = new byte[total];
        byte[] cliffMask = new byte[total];
        byte[] tileRock = new byte[total];

        int baseBlockX = rx * RegionCoordinates.REGION_SIZE - L1Tile.SKIRT * L1Tile.STEP;
        int baseBlockZ = rz * RegionCoordinates.REGION_SIZE - L1Tile.SKIRT * L1Tile.STEP;

        for (int sz = 0; sz < n; sz++) {
            int bz = PlanetCoordinates.clampZ(baseBlockZ + sz * L1Tile.STEP);
            for (int sx = 0; sx < n; sx++) {
                int bx = PlanetCoordinates.wrapX(baseBlockX + sx * L1Tile.STEP);
                int idx = sz * n + sx;
                int l0Idx = l0Index(blockToL0X(bx), blockToL0Z(bz));

                double altM = sampleBicubicAltitudeMeters(bx, bz);
                float reliefM = sampleBilinearReliefMeters(bx, bz);
                double meso = PeriodicNoise.sphericalStretchedFbm2D(worldSeed ^ 0xA5A5_1111L, bx, bz, 3, 256, 0.5) * (reliefM * 0.35);
                double hM = altM + meso;

                int order = riverOrder[l0Idx] & 0xFF;
                float q = decodeDischargeM3s(l0Idx);
                int lkId = lake[l0Idx] & 0xFFFF;
                short waterQ16 = Short.MIN_VALUE;

                if (lkId > 0) {
                    LakeInfo lk = lakeById.get(lkId);
                    if (lk != null) {
                        int lkY = PlanetCoordinates.blockYForAltitude(lk.surfaceMeters());
                        waterQ16 = (short) (lkY * 16);
                    }
                } else if (order >= 2 && q >= 8.0f && altM >= 0.0) {
                    double widthM = 4.0 * Math.sqrt(q);
                    int widthB = Math.clamp((int) Math.round(widthM / 244.6 * 6.0), 2, 96);
                    int depthB = Math.clamp((int) Math.round(1.0 + 0.35 * Math.pow(q, 0.4) * 0.6), 1, 14);
                    channelWidth[idx] = (byte) Math.min(127, widthB);

                    int cellLocalX = Math.floorMod(bx - PlanetCoordinates.X_MIN, PlanetCoordinates.L0_STEP);
                    int cellLocalZ = Math.floorMod(bz - PlanetCoordinates.Z_MIN, PlanetCoordinates.L0_STEP);
                    int dir = flowDir[l0Idx] & 0xFF;
                    double meander = PeriodicNoise.sample2D(worldSeed ^ 0x7712_BEEFL, bx, bz, Math.max(64, widthB * 11)) * 14.0;
                    double distFromCenter;
                    if (dir == 2 || dir == 6) {
                        distFromCenter = Math.abs((cellLocalZ - 32.0) - meander);
                    } else if (dir == 4 || dir == 8) {
                        distFromCenter = Math.abs((cellLocalX - 32.0) - meander);
                    } else {
                        distFromCenter = Math.abs(((cellLocalX - cellLocalZ) * 0.7071) - meander * 0.7);
                    }
                    double halfW = widthB * 0.5 + 3.0;
                    if (distFromCenter <= halfW) {
                        double profile = 1.0 - (distFromCenter / halfW) * (distFromCenter / halfW);
                        int surfY = Math.max(PlanetCoordinates.SEA_LEVEL, PlanetCoordinates.blockYForAltitude(Math.max(0.0, altM)));
                        waterQ16 = (short) (surfY * 16);
                        double carvedY = surfY - Math.max(1.0, depthB * profile);
                        hM = PlanetCoordinates.altitudeMeters(carvedY);
                        if (dir >= 1 && dir <= 8) {
                            double speed = Math.clamp(0.35 + 0.15 * order, 0.2, 2.4);
                            double norm = (D8_DX[dir] != 0 && D8_DZ[dir] != 0) ? 0.70710678 : 1.0;
                            flowVx[idx] = (byte) Math.clamp((int) Math.round((D8_DX[dir] * norm * speed) / 0.02), -127, 127);
                            flowVz[idx] = (byte) Math.clamp((int) Math.round((D8_DZ[dir] * norm * speed) / 0.02), -127, 127);
                        }
                    }
                }

                double yVal = PlanetCoordinates.yForAltitude(hM);
                if (Math.abs(bz) >= 40_700) {
                    double cliffFrac = (Math.abs(bz) - 40_700.0) / 260.0;
                    yVal = Math.max(yVal, PlanetCoordinates.SEA_LEVEL + 40.0 + 120.0 * Math.clamp(cliffFrac, 0.0, 1.0));
                    cliffMask[idx] = 1;
                }
                heightQ16[idx] = (short) Math.clamp(Math.round(yVal * 16.0), PlanetCoordinates.Y_MIN * 16, (PlanetCoordinates.Y_MAX - 8) * 16);
                if (waterQ16 == Short.MIN_VALUE && heightQ16[idx] < PlanetCoordinates.SEA_LEVEL * 16) {
                    waterQ16 = (short) (PlanetCoordinates.SEA_LEVEL * 16);
                }
                waterSurfaceQ16[idx] = waterQ16;

                // Biome domain warp smoothing across L0 boundary
                int warpX = bx + (int) Math.round(PeriodicNoise.sample2D(worldSeed ^ 0x3311L, bx, bz, 96) * 24.0);
                int warpZ = bz + (int) Math.round(PeriodicNoise.sample2D(worldSeed ^ 0x4422L, bx, bz, 96) * 24.0);
                int bIdx = l0Index(blockToL0X(warpX), blockToL0Z(warpZ));
                tileBiome[idx] = biome[bIdx];
                tileRock[idx] = rockTop[l0Idx];
                int bCode = tileBiome[idx] & 0xFF;
                if (bCode == 15 || bCode == 42 || bCode == 59 || bCode == 60) {
                    cliffMask[idx] = 1; // fjord, canyon, rocky peaks, glacier
                }
            }
        }

        List<Deposit> nearDeposits = new ArrayList<>();
        for (int drx = -1; drx <= 1; drx++) {
            for (int drz = -1; drz <= 1; drz++) {
                int qrx = Math.floorMod(rx + drx + RegionCoordinates.REGION_WIDTH / 2, RegionCoordinates.REGION_WIDTH) - RegionCoordinates.REGION_WIDTH / 2;
                int qrz = rz + drz;
                if (qrz < -RegionCoordinates.REGION_HEIGHT / 2 || qrz >= RegionCoordinates.REGION_HEIGHT / 2) {
                    continue;
                }
                nearDeposits.addAll(depositsInRegion(RegionCoordinates.key(qrx, qrz)));
            }
        }

        return new L1Tile(rx, rz, heightQ16, waterSurfaceQ16, channelWidth, flowVx, flowVz, tileBiome, cliffMask, tileRock, List.copyOf(nearDeposits));
    }

    /**
     * Evaluates deterministic L1 bicubic surface Y at block (x, z), seamlessly periodic in X.
     */
    public int sampleL1SurfaceY(int x, int z) {
        int wx = PlanetCoordinates.wrapX(x);
        int cz = PlanetCoordinates.clampZ(z);
        int rx = RegionCoordinates.regionX(wx);
        int rz = RegionCoordinates.regionZ(cz);
        L1Tile tile = getOrComputeL1Tile(rx, rz);

        double localX = (wx - rx * RegionCoordinates.REGION_SIZE) / (double) L1Tile.STEP + L1Tile.SKIRT;
        double localZ = (cz - rz * RegionCoordinates.REGION_SIZE) / (double) L1Tile.STEP + L1Tile.SKIRT;
        int sx0 = Math.clamp((int) Math.floor(localX), 1, L1Tile.SIZE - 3);
        int sz0 = Math.clamp((int) Math.floor(localZ), 1, L1Tile.SIZE - 3);
        double tx = localX - sx0;
        double tz = localZ - sz0;

        double[] col = new double[4];
        for (int dz = -1; dz <= 2; dz++) {
            int row = (sz0 + dz) * L1Tile.SIZE;
            col[dz + 1] = catmullRom(
                    tile.heightQ16()[row + sx0 - 1] / 16.0,
                    tile.heightQ16()[row + sx0] / 16.0,
                    tile.heightQ16()[row + sx0 + 1] / 16.0,
                    tile.heightQ16()[row + sx0 + 2] / 16.0,
                    tx
            );
        }
        double y = catmullRom(col[0], col[1], col[2], col[3], tz);
        return Math.clamp((int) Math.round(y), PlanetCoordinates.Y_MIN + 8, PlanetCoordinates.Y_MAX - 8);
    }

    public int sampleWaterSurfaceY(int x, int z) {
        int wx = PlanetCoordinates.wrapX(x);
        int cz = PlanetCoordinates.clampZ(z);
        int rx = RegionCoordinates.regionX(wx);
        int rz = RegionCoordinates.regionZ(cz);
        L1Tile tile = getOrComputeL1Tile(rx, rz);
        int sx = Math.clamp(Math.round((wx - rx * RegionCoordinates.REGION_SIZE) / (float) L1Tile.STEP) + L1Tile.SKIRT, 0, L1Tile.SIZE - 1);
        int sz = Math.clamp(Math.round((cz - rz * RegionCoordinates.REGION_SIZE) / (float) L1Tile.STEP) + L1Tile.SKIRT, 0, L1Tile.SIZE - 1);
        short wq = tile.waterSurfaceQ16()[sz * L1Tile.SIZE + sx];
        return wq == Short.MIN_VALUE ? Integer.MIN_VALUE : (wq / 16);
    }

    public Vec2 sampleFlowVelocity(int x, int z) {
        int wx = PlanetCoordinates.wrapX(x);
        int cz = PlanetCoordinates.clampZ(z);
        int rx = RegionCoordinates.regionX(wx);
        int rz = RegionCoordinates.regionZ(cz);
        L1Tile tile = getOrComputeL1Tile(rx, rz);
        int sx = Math.clamp(Math.round((wx - rx * RegionCoordinates.REGION_SIZE) / (float) L1Tile.STEP) + L1Tile.SKIRT, 0, L1Tile.SIZE - 1);
        int sz = Math.clamp(Math.round((cz - rz * RegionCoordinates.REGION_SIZE) / (float) L1Tile.STEP) + L1Tile.SKIRT, 0, L1Tile.SIZE - 1);
        int idx = sz * L1Tile.SIZE + sx;
        return new Vec2(tile.flowVx()[idx] * 0.02f, tile.flowVz()[idx] * 0.02f);
    }

    public WildlandsCatalog.BiomeSpec biomeSpecAt(int x, int z) {
        int wx = PlanetCoordinates.wrapX(x);
        int cz = PlanetCoordinates.clampZ(z);
        int warpX = wx + (int) Math.round(PeriodicNoise.sample2D(worldSeed ^ 0x3311L, wx, cz, 96) * 20.0);
        int warpZ = cz + (int) Math.round(PeriodicNoise.sample2D(worldSeed ^ 0x4422L, wx, cz, 96) * 20.0);
        int idx = l0Index(blockToL0X(warpX), blockToL0Z(warpZ));
        return WildlandsCatalog.biomeByIndex(biome[idx] & 0xFF);
    }

    public GeoProvince provinceAt(int x, int z) {
        int idx = l0Index(blockToL0X(x), blockToL0Z(z));
        GeoProvince[] vals = GeoProvince.values();
        return vals[Math.clamp(province[idx] & 0xFF, 0, vals.length - 1)];
    }

    public StrataColumn strataAt(int x, int z) {
        int surfY = sampleL1SurfaceY(x, z);
        int idx = l0Index(blockToL0X(x), blockToL0Z(z));
        GeoProvince prov = provinceAt(x, z);
        WildlandsCatalog.RockSpec topSpec = WildlandsCatalog.rockByPalette(rockTop[idx] & 0xFF);

        Identifier midRock = switch (prov) {
            case CRATON_PLATFORM, SEDIMENTARY_BASIN -> WildlandsCatalog.wl("shale");
            case CARBONATE_SHELF -> WildlandsCatalog.wl("dolomite");
            case OLD_OROGEN, YOUNG_OROGEN -> WildlandsCatalog.wl("schist");
            case ARC -> WildlandsCatalog.mc("diorite");
            case RIFT, LIP, OCEAN_CRUST -> WildlandsCatalog.wl("gabbro");
            case OPHIOLITE -> WildlandsCatalog.wl("peridotite");
            case CRATON_SHIELD, TROPICAL_REGOLITH -> WildlandsCatalog.mc("granite");
        };
        Identifier basementRock = (prov == GeoProvince.OCEAN_CRUST || prov == GeoProvince.OPHIOLITE)
                ? WildlandsCatalog.wl("peridotite")
                : WildlandsCatalog.wl("gneiss");

        int topThick = Math.max(24, 48 + (int) Math.round(PeriodicNoise.sample2D(worldSeed ^ 0x9911L, x, z, 512) * 20.0));
        int midTopY = surfY - topThick;
        int midThick = Math.max(60, midTopY - (-140));
        int baseTopY = midTopY - midThick;
        int baseThick = Math.max(64, baseTopY - PlanetCoordinates.Y_MIN);

        float dipAngle = (prov == GeoProvince.YOUNG_OROGEN || prov == GeoProvince.OLD_OROGEN) ? 45.0f : 6.0f;
        float dipDir = (float) ((PeriodicNoise.sample2D(worldSeed ^ 0x8822L, x, z, 2048) + 1.0) * 180.0);

        return new StrataColumn(prov, List.of(
                new StrataColumn.Layer(topSpec.id(), surfY, topThick, dipDir, dipAngle),
                new StrataColumn.Layer(midRock, midTopY, midThick, dipDir, dipAngle * 0.7f),
                new StrataColumn.Layer(basementRock, baseTopY, baseThick, 0.0f, 0.0f)
        ));
    }

    public ClimateNormals climateNormalsAt(int x, int z) {
        int wx = PlanetCoordinates.wrapX(x);
        int cz = PlanetCoordinates.clampZ(z);
        int cx = Math.floorMod(Math.floorDiv(wx - PlanetCoordinates.X_MIN, PlanetCoordinates.CLIM_STEP), CLIM_W);
        int kz = Math.clamp(Math.floorDiv(cz - PlanetCoordinates.Z_MIN, PlanetCoordinates.CLIM_STEP), 0, CLIM_H - 1);
        int cIdx = climIndex(cx, kz);
        int l0Idx = l0Index(blockToL0X(wx), blockToL0Z(cz));

        float[] temps = new float[12];
        float[] precips = new float[12];
        float sumT = 0.0f;
        float sumP = 0.0f;
        float minT = Float.MAX_VALUE;
        float maxT = -Float.MAX_VALUE;
        int base12 = cIdx * 12;
        for (int m = 0; m < 12; m++) {
            float t = tempMonthly[base12 + m] * 0.5f;
            float p = decodePrecipMm(precipMonthly[base12 + m]);
            temps[m] = t;
            precips[m] = p;
            sumT += t;
            sumP += p;
            if (t < minT) minT = t;
            if (t > maxT) maxT = t;
        }
        int kCode = Math.clamp(koppen[l0Idx] & 0xFF, 0, KOPPEN_PALETTE.length - 1);
        float seasonality = (maxT - minT) / 40.0f;
        return new ClimateNormals(temps, precips, KOPPEN_PALETTE[kCode], sumT / 12.0f, sumP, seasonality);
    }

    public Optional<RiverInfo> riverAt(BlockPos pos) {
        int idx = l0Index(blockToL0X(pos.getX()), blockToL0Z(pos.getZ()));
        int order = riverOrder[idx] & 0xFF;
        float q = decodeDischargeM3s(idx);
        if (order == 0 || q < 5.0f || elevation[idx] < 0) {
            return Optional.empty();
        }
        double widthM = 4.0 * Math.sqrt(q);
        int widthB = Math.clamp((int) Math.round(widthM / 244.6 * 6.0), 1, 96);
        int depthB = Math.clamp((int) Math.round(1.0 + 0.35 * Math.pow(q, 0.4) * 0.6), 1, 14);
        int surfY = Math.max(PlanetCoordinates.SEA_LEVEL, PlanetCoordinates.blockYForAltitude(elevation[idx]));
        Vec2 vel = sampleFlowVelocity(pos.getX(), pos.getZ());
        long reachId = ((long) (basin[idx] & 0xFFFF) << 32) | (idx & 0xFFFF_FFFFL);
        return Optional.of(new RiverInfo(reachId, order, q, surfY, surfY + 2, widthB, depthB, vel, FloodStage.NORMAL));
    }
}
