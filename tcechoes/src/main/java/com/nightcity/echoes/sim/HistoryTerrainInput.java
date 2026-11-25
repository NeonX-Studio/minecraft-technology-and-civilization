package com.nightcity.echoes.sim;

import com.nightcity.echoes.api.ArchStyle;
import com.nightcity.echoes.api.SimulationMode;
import com.nightcity.tcivilization.RegionCoordinates;
import com.nightcity.tcivilization.api.RegionApi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

/**
 * Read-only, thread-safe terrain/climate/deposit input grid for 5,000-year pre-simulation (18 §5.1.1, §5.1.2).
 * Never triggers chunk generation.
 */
public interface HistoryTerrainInput {
    enum BiomeBand {
        FERTILE_VALLEY(1.0D, ArchStyle.TIMBER, ArchStyle.RAMMED_EARTH),
        TEMPERATE_WOODLAND(0.7D, ArchStyle.TIMBER, ArchStyle.DRYSTONE),
        WETLAND_JUNGLE(0.5D, ArchStyle.TERRACED, ArchStyle.TIMBER),
        ARID_BADLANDS(0.2D, ArchStyle.MUDBRICK, ArchStyle.RAMMED_EARTH),
        ALPINE_TUNDRA(0.2D, ArchStyle.DRYSTONE, ArchStyle.CAVE_CUT),
        OCEAN(0.0D, ArchStyle.ASHLAR, ArchStyle.TIMBER);

        private final double weight;
        private final ArchStyle primary;
        private final ArchStyle secondary;

        BiomeBand(double weight, ArchStyle primary, ArchStyle secondary) {
            this.weight = weight;
            this.primary = primary;
            this.secondary = secondary;
        }

        public double weight() {
            return weight;
        }

        public ArchStyle primaryStyle() {
            return primary;
        }

        public ArchStyle secondaryStyle() {
            return secondary;
        }
    }

    enum DepositScale {
        SMALL(1.5D),
        MEDIUM(1.0D),
        LARGE(0.6D);

        private final double scaleFactor;

        DepositScale(double scaleFactor) {
            this.scaleFactor = scaleFactor;
        }

        public double scaleFactor() {
            return scaleFactor;
        }
    }

    record MineralDeposit(
            String depositId,
            String mineral,
            int rx,
            int rz,
            boolean surfaceSigns,
            DepositScale scale
    ) {
    }

    record CellSample(
            int rx,
            int rz,
            boolean ocean,
            boolean coastal,
            boolean riverValley,
            boolean mountain,
            boolean desert,
            boolean tundra,
            int continentId,
            int elevation,
            double slope,
            double terrainCost,
            double habitability,
            double humidity,
            double freezeThaw,
            double sedimentRate,
            double annualHazardRate,
            BiomeBand biomeBand
    ) {
        public long regionKey() {
            return RegionApi.regionKey(rx, rz);
        }

        public double climateWeatheringFactor() {
            return 1.0D + 0.6D * humidity + 0.4D * freezeThaw;
        }
    }

    SimulationMode mode();

    int minRx();

    int maxRxInclusive();

    int minRz();

    int maxRzInclusive();

    int centerRx();

    int centerRz();

    int standaloneRadiusRegions();

    boolean wrapsX();

    default int width() {
        return maxRxInclusive() - minRx() + 1;
    }

    default int height() {
        return maxRzInclusive() - minRz() + 1;
    }

    CellSample sample(int rx, int rz);

    List<MineralDeposit> deposits();

    /**
     * Exact 18 §5.1.2 biome path/tag classification weight.
     */
    static BiomeBand classifyBiomeKey(String biomeId) {
        if (biomeId == null) {
            return BiomeBand.FERTILE_VALLEY;
        }
        String lower = biomeId.toLowerCase(Locale.ROOT);
        if (lower.contains("ocean") || lower.contains("deep_")) {
            return BiomeBand.OCEAN;
        }
        if (lower.contains("river") || lower.contains("plains") || lower.contains("forest")
                || lower.contains("meadow") || lower.contains("birch") || lower.contains("cherry")
                || lower.contains("flower") || lower.contains("grove")) {
            return BiomeBand.FERTILE_VALLEY;
        }
        if (lower.contains("taiga") || lower.contains("savanna")) {
            return BiomeBand.TEMPERATE_WOODLAND;
        }
        if (lower.contains("jungle") || lower.contains("swamp") || lower.contains("mangrove")) {
            return BiomeBand.WETLAND_JUNGLE;
        }
        if (lower.contains("desert") || lower.contains("badlands") || lower.contains("eroded")) {
            return BiomeBand.ARID_BADLANDS;
        }
        if (lower.contains("snow") || lower.contains("frozen") || lower.contains("ice")
                || lower.contains("peak") || lower.contains("mountain") || lower.contains("slopes")
                || lower.contains("stony") || lower.contains("windswept")) {
            return BiomeBand.ALPINE_TUNDRA;
        }
        return BiomeBand.TEMPERATE_WOODLAND;
    }

    /**
     * Builds a standalone 49x49 (or radius*2+1) region window around spawn using read-only noise biome sampling
     * on a live ServerLevel (18 §2.4, §5.1.1, §5.1.2). Never loads or generates chunks.
     */
    static HistoryTerrainInput fromServerLevelStandalone(
            ServerLevel level,
            long worldSeed,
            int spawnRx,
            int spawnRz,
            int radiusRegions
    ) {
        int r = Math.clamp(radiusRegions, 8, 64);
        int minRx = Math.max(-RegionCoordinates.REGION_WIDTH / 2, spawnRx - r);
        int maxRx = Math.min(RegionCoordinates.REGION_WIDTH / 2 - 1, spawnRx + r);
        int minRz = Math.max(-RegionCoordinates.REGION_HEIGHT / 2, spawnRz - r);
        int maxRz = Math.min(RegionCoordinates.REGION_HEIGHT / 2 - 1, spawnRz + r);
        int w = maxRx - minRx + 1;
        int h = maxRz - minRz + 1;

        BiomeBand[] dominantBands = new BiomeBand[w * h];
        double[] hab = new double[w * h];
        boolean[] hasRiver = new boolean[w * h];
        boolean[] isOcean = new boolean[w * h];
        List<MineralDeposit> deposits = new ArrayList<>();

        for (int dz = 0; dz < h; dz++) {
            int rz = minRz + dz;
            for (int dx = 0; dx < w; dx++) {
                int rx = minRx + dx;
                int idx = dz * w + dx;
                double weightSum = 0.0D;
                int[] bandCounts = new int[BiomeBand.values().length];
                int riverHits = 0;
                int oceanHits = 0;
                int badlandsHits = 0;
                int mountainHits = 0;
                for (int sx = 0; sx < 8; sx++) {
                    int bx = rx * 512 + 32 + sx * 64;
                    for (int sz = 0; sz < 8; sz++) {
                        int bz = rz * 512 + 32 + sz * 64;
                        Holder<Biome> holder = level.getNoiseBiome(
                                QuartPos.fromBlock(bx),
                                QuartPos.fromBlock(64),
                                QuartPos.fromBlock(bz)
                        );
                        String keyStr = holder.unwrapKey()
                                .map((ResourceKey<Biome> k) -> k.identifier().toString())
                                .orElse("minecraft:plains");
                        BiomeBand band = classifyBiomeKey(keyStr);
                        weightSum += band.weight();
                        bandCounts[band.ordinal()]++;
                        if (keyStr.contains("river")) {
                            riverHits++;
                        }
                        if (band == BiomeBand.OCEAN) {
                            oceanHits++;
                        }
                        if (keyStr.contains("badlands")) {
                            badlandsHits++;
                        }
                        if (band == BiomeBand.ALPINE_TUNDRA) {
                            mountainHits++;
                        }
                    }
                }
                boolean ocean = oceanHits >= 40;
                isOcean[idx] = ocean;
                hasRiver[idx] = !ocean && riverHits > 0;
                hab[idx] = ocean ? 0.0D : StrictMath.min(1.0D, weightSum / 64.0D);

                BiomeBand best = BiomeBand.FERTILE_VALLEY;
                int bestCount = -1;
                for (BiomeBand b : BiomeBand.values()) {
                    if (bandCounts[b.ordinal()] > bestCount) {
                        bestCount = bandCounts[b.ordinal()];
                        best = b;
                    }
                }
                dominantBands[idx] = ocean ? BiomeBand.OCEAN : best;

                long depHash = SplitMix64.rand(worldSeed ^ SplitMix64.ECHOES_SALT, 0, RegionApi.regionKey(rx, rz), 0x444550L);
                if (!ocean && badlandsHits >= 8 && SplitMix64.double01(depHash) < 0.45D) {
                    deposits.add(new MineralDeposit("standalone_gold_" + rx + "_" + rz, "gold", rx, rz, true, DepositScale.SMALL));
                } else if (!ocean && mountainHits >= 8 && SplitMix64.double01(depHash) < 0.50D) {
                    String[] ores = {"iron", "copper", "coal"};
                    String ore = ores[SplitMix64.intRange(depHash >>> 8, ores.length)];
                    DepositScale scale = DepositScale.values()[SplitMix64.intRange(depHash >>> 16, DepositScale.values().length)];
                    deposits.add(new MineralDeposit("standalone_" + ore + "_" + rx + "_" + rz, ore, rx, rz, true, scale));
                }
            }
        }

        CellSample[] cells = new CellSample[w * h];
        for (int dz = 0; dz < h; dz++) {
            int rz = minRz + dz;
            for (int dx = 0; dx < w; dx++) {
                int rx = minRx + dx;
                int idx = dz * w + dx;
                boolean ocean = isOcean[idx];
                boolean coastal = false;
                if (!ocean) {
                    coastal = (dx > 0 && isOcean[idx - 1])
                            || (dx + 1 < w && isOcean[idx + 1])
                            || (dz > 0 && isOcean[idx - w])
                            || (dz + 1 < h && isOcean[idx + w]);
                }
                BiomeBand band = dominantBands[idx];
                boolean mountain = band == BiomeBand.ALPINE_TUNDRA;
                boolean desert = band == BiomeBand.ARID_BADLANDS;
                boolean tundra = band == BiomeBand.ALPINE_TUNDRA && StrictMath.abs(rz) > 20;
                int elev = ocean ? 42 : (mountain ? 140 : 68);
                double slope = mountain ? 0.45D : (coastal ? 0.12D : 0.08D);
                double cost = ocean ? 2.5D : (mountain ? 3.2D : (band == BiomeBand.WETLAND_JUNGLE ? 2.0D : 1.0D));
                double humidity = switch (band) {
                    case WETLAND_JUNGLE, OCEAN -> 0.85D;
                    case FERTILE_VALLEY -> 0.60D;
                    case TEMPERATE_WOODLAND -> 0.50D;
                    case ALPINE_TUNDRA -> 0.35D;
                    case ARID_BADLANDS -> 0.12D;
                };
                double freeze = band == BiomeBand.ALPINE_TUNDRA ? 0.75D : 0.15D;
                double sed = hasRiver[idx] ? 2.0D : (desert ? 1.5D : 1.0D);
                double haz = (hasRiver[idx] || mountain || desert) ? 0.0025D : 0.0008D;
                cells[idx] = new CellSample(
                        rx, rz, ocean, coastal, hasRiver[idx], mountain, desert, tundra,
                        ocean ? -1 : 0, elev, slope, cost, hab[idx], humidity, freeze, sed, haz, band
                );
            }
        }
        List<MineralDeposit> immutableDeposits = List.copyOf(deposits);
        return new ArrayBackedTerrainInput(
                SimulationMode.STANDALONE, minRx, maxRx, minRz, maxRz, spawnRx, spawnRz, r, false, cells, immutableDeposits
        );
    }

    /**
     * Deterministic standalone 49x49 window terrain generator from worldSeed when running without a live ServerLevel
     * (used in standalone unit tests and early bootstrap fallback).
     */
    static HistoryTerrainInput standaloneSynthetic(long worldSeed, int spawnRx, int spawnRz, int radiusRegions) {
        int r = Math.clamp(radiusRegions, 8, 64);
        int minRx = Math.max(-RegionCoordinates.REGION_WIDTH / 2, spawnRx - r);
        int maxRx = Math.min(RegionCoordinates.REGION_WIDTH / 2 - 1, spawnRx + r);
        int minRz = Math.max(-RegionCoordinates.REGION_HEIGHT / 2, spawnRz - r);
        int maxRz = Math.min(RegionCoordinates.REGION_HEIGHT / 2 - 1, spawnRz + r);
        return buildSyntheticGrid(SimulationMode.STANDALONE, worldSeed, minRx, maxRx, minRz, maxRz, spawnRx, spawnRz, r, false);
    }

    /**
     * Deterministic full-planet 320x160 X-wrapping terrain model (51,200 regions) with continents,
     * river valleys, coastlines, mountain belts, climate bands, and mineral deposits.
     */
    static HistoryTerrainInput planetSynthetic(long worldSeed) {
        return buildSyntheticGrid(
                SimulationMode.PLANET,
                worldSeed,
                -RegionCoordinates.REGION_WIDTH / 2,
                RegionCoordinates.REGION_WIDTH / 2 - 1,
                -RegionCoordinates.REGION_HEIGHT / 2,
                RegionCoordinates.REGION_HEIGHT / 2 - 1,
                0,
                0,
                0,
                true
        );
    }

    private static HistoryTerrainInput buildSyntheticGrid(
            SimulationMode mode,
            long worldSeed,
            int minRx,
            int maxRx,
            int minRz,
            int maxRz,
            int centerRx,
            int centerRz,
            int standaloneRadius,
            boolean wrapX
    ) {
        int w = maxRx - minRx + 1;
        int h = maxRz - minRz + 1;
        long hSeed = SplitMix64.deriveHistorySeed(worldSeed);

        int continentCount = mode == SimulationMode.PLANET ? 5 : 2;
        double[] contCx = new double[continentCount];
        double[] contCz = new double[continentCount];
        double[] contRadius = new double[continentCount];
        for (int c = 0; c < continentCount; c++) {
            long m = SplitMix64.mix64(hSeed ^ (0x434F4E54L + c * 977L));
            if (mode == SimulationMode.PLANET) {
                contCx[c] = -140.0D + (c * 64.0D) + (SplitMix64.double01(m) - 0.5D) * 26.0D;
                contCz[c] = (SplitMix64.double01(SplitMix64.mix64(m + 1)) - 0.5D) * 88.0D;
                contRadius[c] = 28.0D + SplitMix64.double01(SplitMix64.mix64(m + 2)) * 18.0D;
            } else {
                contCx[c] = centerRx + (c == 0 ? -2.0D : 9.0D);
                contCz[c] = centerRz + (c == 0 ? 1.0D : -8.0D);
                contRadius[c] = rScale(standaloneRadius) * (c == 0 ? 0.68D : 0.45D);
            }
        }

        boolean[] oceanArr = new boolean[w * h];
        int[] contArr = new int[w * h];
        double[] elevNorm = new double[w * h];

        for (int dz = 0; dz < h; dz++) {
            int rz = minRz + dz;
            for (int dx = 0; dx < w; dx++) {
                int rx = minRx + dx;
                int idx = dz * w + dx;
                double bestScore = -1.0D;
                int bestCont = 0;
                for (int c = 0; c < continentCount; c++) {
                    double ddx = rx - contCx[c];
                    if (wrapX) {
                        if (ddx > 160.0D) ddx -= 320.0D;
                        if (ddx < -160.0D) ddx += 320.0D;
                    }
                    double ddz = (rz - contCz[c]) * 1.15D;
                    double dist = StrictMath.sqrt(ddx * ddx + ddz * ddz);
                    double score = 1.0D - (dist / contRadius[c]);
                    if (score > bestScore) {
                        bestScore = score;
                        bestCont = c;
                    }
                }
                double angleX = (rx * 2.0D * StrictMath.PI) / 320.0D;
                double wave = 0.16D * StrictMath.sin(angleX * 3.0D + (hSeed & 0xff) * 0.05D)
                        + 0.11D * StrictMath.cos(rz * 0.14D - angleX * 2.0D);
                double landVal = bestScore + wave;
                if (mode == SimulationMode.STANDALONE && StrictMath.abs(rx - centerRx) <= 4 && StrictMath.abs(rz - centerRz) <= 4) {
                    landVal = StrictMath.max(landVal, 0.35D);
                }
                boolean ocean = landVal < 0.05D;
                oceanArr[idx] = ocean;
                contArr[idx] = ocean ? -1 : bestCont;
                elevNorm[idx] = ocean ? -0.3D : StrictMath.min(1.0D, landVal);
            }
        }

        CellSample[] samples = new CellSample[w * h];
        List<MineralDeposit> deposits = new ArrayList<>();

        for (int dz = 0; dz < h; dz++) {
            int rz = minRz + dz;
            double lat01 = StrictMath.min(1.0D, StrictMath.abs(rz) / 80.0D);
            for (int dx = 0; dx < w; dx++) {
                int rx = minRx + dx;
                int idx = dz * w + dx;
                boolean ocean = oceanArr[idx];
                int leftIdx = dz * w + (dx > 0 ? dx - 1 : (wrapX ? w - 1 : 0));
                int rightIdx = dz * w + (dx + 1 < w ? dx + 1 : (wrapX ? 0 : w - 1));
                int upIdx = (dz > 0 ? dz - 1 : 0) * w + dx;
                int downIdx = (dz + 1 < h ? dz + 1 : h - 1) * w + dx;

                boolean coastal = !ocean && (oceanArr[leftIdx] || oceanArr[rightIdx] || oceanArr[upIdx] || oceanArr[downIdx]);
                long cellRand = SplitMix64.rand(hSeed, 0, RegionApi.regionKey(rx, rz), 0x54455252L);
                double ridge = StrictMath.abs(StrictMath.sin(rx * 0.19D + rz * 0.11D + (hSeed & 63)));
                boolean mountain = !ocean && !coastal && (elevNorm[idx] > 0.62D || ridge > 0.92D);
                boolean riverValley = !ocean && !mountain && ((StrictMath.floorMod(rx + rz * 2 + (int) (hSeed & 7), 7) == 0) || (coastal && (cellRand & 3L) == 0L));
                boolean tundra = !ocean && lat01 > 0.76D;
                boolean desert = !ocean && !riverValley && !tundra && lat01 > 0.18D && lat01 < 0.42D && ridge < 0.45D;

                BiomeBand band;
                if (ocean) {
                    band = BiomeBand.OCEAN;
                } else if (riverValley || (coastal && !tundra && !desert)) {
                    band = BiomeBand.FERTILE_VALLEY;
                } else if (mountain || tundra) {
                    band = BiomeBand.ALPINE_TUNDRA;
                } else if (desert) {
                    band = BiomeBand.ARID_BADLANDS;
                } else if (lat01 < 0.22D) {
                    band = BiomeBand.WETLAND_JUNGLE;
                } else {
                    band = BiomeBand.TEMPERATE_WOODLAND;
                }

                double hab = band.weight();
                if (!ocean) {
                    if (riverValley) {
                        hab = StrictMath.min(1.0D, hab + 0.18D);
                    }
                    if (coastal) {
                        hab = StrictMath.min(1.0D, hab + 0.10D);
                    }
                    hab = StrictMath.max(0.05D, StrictMath.min(1.0D, hab - 0.05D * (SplitMix64.double01(cellRand) - 0.5D)));
                } else {
                    hab = 0.0D;
                }

                int elevation = ocean ? -120 : (mountain ? 380 + (int) (elevNorm[idx] * 350.0D) : (int) (65.0D + elevNorm[idx] * 120.0D));
                double slope = mountain ? 0.52D : (riverValley ? 0.06D : 0.14D);
                double terrainCost = ocean ? 2.2D : (mountain ? 3.5D : (riverValley ? 0.8D : (desert ? 1.8D : 1.1D)));
                double humidity = ocean ? 0.9D : (desert ? 0.12D : (band == BiomeBand.WETLAND_JUNGLE ? 0.85D : (riverValley || coastal ? 0.65D : 0.45D)));
                double freezeThaw = (tundra || mountain) ? 0.75D : StrictMath.max(0.05D, lat01 * 0.5D);
                double sedimentRate = riverValley ? 2.0D : (desert ? 1.5D : 1.0D);
                double hazardRate = mountain ? 0.0030D : ((riverValley || coastal) ? 0.0020D : (desert ? 0.0018D : 0.0008D));

                samples[idx] = new CellSample(
                        rx, rz, ocean, coastal, riverValley, mountain, desert, tundra,
                        contArr[idx], elevation, slope, terrainCost, hab, humidity, freezeThaw, sedimentRate, hazardRate, band
                );

                if (!ocean && (mountain || desert) && SplitMix64.double01(cellRand >>> 9) < 0.16D) {
                    String[] minerals = desert ? new String[]{"gold", "copper"} : new String[]{"copper", "tin", "iron", "coal", "gold"};
                    String mineral = minerals[SplitMix64.intRange(cellRand >>> 17, minerals.length)];
                    DepositScale scale = DepositScale.values()[SplitMix64.intRange(cellRand >>> 29, DepositScale.values().length)];
                    deposits.add(new MineralDeposit("dep_" + mineral + "_" + rx + "_" + rz, mineral, rx, rz, true, scale));
                }
            }
        }

        return new ArrayBackedTerrainInput(
                mode, minRx, maxRx, minRz, maxRz, centerRx, centerRz, standaloneRadius, wrapX, samples, List.copyOf(deposits)
        );
    }

    private static double rScale(int r) {
        return Math.max(10.0D, r);
    }

    final class ArrayBackedTerrainInput implements HistoryTerrainInput {
        private final SimulationMode mode;
        private final int minRx;
        private final int maxRx;
        private final int minRz;
        private final int maxRz;
        private final int centerRx;
        private final int centerRz;
        private final int standaloneRadius;
        private final boolean wrapX;
        private final int w;
        private final int h;
        private final CellSample[] cells;
        private final List<MineralDeposit> deposits;

        public ArrayBackedTerrainInput(
                SimulationMode mode,
                int minRx,
                int maxRx,
                int minRz,
                int maxRz,
                int centerRx,
                int centerRz,
                int standaloneRadius,
                boolean wrapX,
                CellSample[] cells,
                List<MineralDeposit> deposits
        ) {
            this.mode = mode;
            this.minRx = minRx;
            this.maxRx = maxRx;
            this.minRz = minRz;
            this.maxRz = maxRz;
            this.centerRx = centerRx;
            this.centerRz = centerRz;
            this.standaloneRadius = standaloneRadius;
            this.wrapX = wrapX;
            this.w = maxRx - minRx + 1;
            this.h = maxRz - minRz + 1;
            this.cells = cells;
            this.deposits = deposits;
        }

        @Override
        public SimulationMode mode() {
            return mode;
        }

        @Override
        public int minRx() {
            return minRx;
        }

        @Override
        public int maxRxInclusive() {
            return maxRx;
        }

        @Override
        public int minRz() {
            return minRz;
        }

        @Override
        public int maxRzInclusive() {
            return maxRz;
        }

        @Override
        public int centerRx() {
            return centerRx;
        }

        @Override
        public int centerRz() {
            return centerRz;
        }

        @Override
        public int standaloneRadiusRegions() {
            return standaloneRadius;
        }

        @Override
        public boolean wrapsX() {
            return wrapX;
        }

        @Override
        public CellSample sample(int rx, int rz) {
            int normX = wrapX ? Math.floorMod(rx - minRx, w) : Math.clamp(rx - minRx, 0, w - 1);
            int normZ = Math.clamp(rz - minRz, 0, h - 1);
            return cells[normZ * w + normX];
        }

        @Override
        public List<MineralDeposit> deposits() {
            return deposits;
        }
    }
}
