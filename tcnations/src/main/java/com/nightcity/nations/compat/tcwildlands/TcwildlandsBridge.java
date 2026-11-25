package com.nightcity.nations.compat.tcwildlands;

import com.nightcity.nations.NationsConfig;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.core.BlockPos;
import net.neoforged.fml.ModList;

public final class TcwildlandsBridge {
    @FunctionalInterface
    public interface RegionHabitabilitySampler {
        OptionalDouble sampleRegionHabitability(int regionX, int regionZ);
    }

    private static volatile boolean initialized;
    private static volatile RegionHabitabilitySampler testSamplerOverride;

    private TcwildlandsBridge() {
    }

    public static void init() {
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static void setTestSamplerOverride(RegionHabitabilitySampler sampler) {
        testSamplerOverride = sampler;
    }

    public static boolean isWildlandsAvailable() {
        if (testSamplerOverride != null) {
            return true;
        }
        return NationsConfig.isCompatHooksEnabled()
                && ModList.get() != null
                && ModList.get().isLoaded("tcwildlands");
    }

    /**
     * Computes Wildlands region habitability H(r) in [0, 1] per 23 §5.1.2:
     * H = 0.35 * tempSuit + 0.25 * precipSuit + 0.20 * freshwater + 0.10 * coastal + 0.10 * soilFertility.
     */
    public static OptionalDouble queryRegionHabitability(int regionX, int regionZ) {
        RegionHabitabilitySampler override = testSamplerOverride;
        if (override != null) {
            return override.sampleRegionHabitability(regionX, regionZ);
        }
        if (!isWildlandsAvailable()) {
            return OptionalDouble.empty();
        }
        int centerX = regionX * 512 + 256;
        int centerZ = regionZ * 512 + 256;
        BlockPos center = new BlockPos(centerX, 64, centerZ);
        try {
            Class<?> worldApis = Class.forName("com.nightcity.tcivilization.api.world.TcWorldApis");
            Object planet = worldApis.getMethod("planet").invoke(null);
            if (planet == null) {
                return OptionalDouble.empty();
            }
            Boolean active = (Boolean) planet.getClass().getMethod("isPlanetActive").invoke(planet);
            if (active == null || !active) {
                return OptionalDouble.empty();
            }
            int surfaceY = ((Number) planet.getClass().getMethod("surfaceY", int.class, int.class)
                    .invoke(planet, centerX, centerZ)).intValue();
            if (surfaceY < 63) {
                return OptionalDouble.of(0.0D);
            }

            Object climate = worldApis.getMethod("climate").invoke(null);
            Object normals = climate.getClass().getMethod("normals", BlockPos.class).invoke(climate, center);
            double annualMeanC = ((Number) normals.getClass().getMethod("annualMeanC").invoke(normals)).doubleValue();
            double annualPrecipMm = ((Number) normals.getClass().getMethod("annualPrecipMm").invoke(normals)).doubleValue();

            double tempSuit = clamp01(1.0D - StrictMath.abs(annualMeanC - 15.0D) / 25.0D);
            double precipSuit = clamp01(1.0D - StrictMath.abs(annualPrecipMm - 900.0D) / 1100.0D);

            Object hydro = worldApis.getMethod("hydro").invoke(null);
            Optional<?> riverOpt = (Optional<?>) hydro.getClass().getMethod("river", BlockPos.class).invoke(hydro, center);
            double waterScore = (riverOpt != null && riverOpt.isPresent()) ? 1.0D : 0.25D;

            double coastal = isNearCoastReflective(planet, centerX, centerZ) ? 1.0D : 0.0D;

            Object suitability = worldApis.getMethod("suitability").invoke(null);
            double soilFertility = extractSoilFertility(suitability, center);

            double h = 0.35D * tempSuit + 0.25D * precipSuit + 0.20D * waterScore + 0.10D * coastal + 0.10D * soilFertility;
            return OptionalDouble.of(clamp01(h));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return OptionalDouble.empty();
        }
    }

    private static boolean isNearCoastReflective(Object planet, int cx, int cz) {
        try {
            Method surfaceMethod = planet.getClass().getMethod("surfaceY", int.class, int.class);
            int[][] offsets = {{256, 0}, {-256, 0}, {0, 256}, {0, -256}};
            for (int[] off : offsets) {
                int y = ((Number) surfaceMethod.invoke(planet, cx + off[0], cz + off[1])).intValue();
                if (y < 63) {
                    return true;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return false;
    }

    private static double extractSoilFertility(Object suitability, BlockPos pos) {
        if (suitability == null) {
            return 0.5D;
        }
        try {
            Object soil = suitability.getClass().getMethod("soilParent", BlockPos.class).invoke(suitability, pos);
            if (soil != null) {
                double om = ((Number) soil.getClass().getMethod("organicMatter").invoke(soil)).doubleValue();
                return clamp01(0.3D + om * 2.0D);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return 0.5D;
    }

    private static double clamp01(double v) {
        return Math.max(0.0D, Math.min(1.0D, v));
    }
}
