package com.nightcity.ecology.compat.tcwildlands;

import com.nightcity.ecology.EcologyConfig;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;

public final class TcwildlandsBridge {
    public record ClimateEnvelopeSample(double annualTempC, double annualPrecipMm, Optional<Identifier> planetBiome) {}

    @FunctionalInterface
    public interface ClimateOracle {
        Optional<ClimateEnvelopeSample> query(BlockPos pos);
    }

    private static volatile boolean initialized;
    private static volatile ClimateOracle testOracleOverride;

    private TcwildlandsBridge() {
    }

    public static void init() {
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static void setTestOracleOverride(ClimateOracle oracle) {
        testOracleOverride = oracle;
    }

    public static boolean isWildlandsAvailable() {
        if (testOracleOverride != null) {
            return true;
        }
        return EcologyConfig.isCompatHooksEnabled()
                && ModList.get() != null
                && ModList.get().isLoaded("tcwildlands");
    }

    public static Optional<ClimateEnvelopeSample> queryClimateAt(BlockPos pos) {
        ClimateOracle override = testOracleOverride;
        if (override != null) {
            return override.query(pos);
        }
        if (!isWildlandsAvailable()) {
            return Optional.empty();
        }
        try {
            Class<?> worldApis = Class.forName("com.nightcity.tcivilization.api.world.TcWorldApis");
            Object planet = worldApis.getMethod("planet").invoke(null);
            if (planet == null) {
                return Optional.empty();
            }
            Boolean active = (Boolean) planet.getClass().getMethod("isPlanetActive").invoke(planet);
            if (active == null || !active) {
                return Optional.empty();
            }
            Object climate = worldApis.getMethod("climate").invoke(null);
            Object normals = climate.getClass().getMethod("normals", BlockPos.class).invoke(climate, pos);
            double meanC = ((Number) normals.getClass().getMethod("annualMeanC").invoke(normals)).doubleValue();
            double precipMm = ((Number) normals.getClass().getMethod("annualPrecipMm").invoke(normals)).doubleValue();
            Object biomeObj = planet.getClass().getMethod("biomeAt", int.class, int.class)
                    .invoke(planet, pos.getX(), pos.getZ());
            Optional<Identifier> biomeId = biomeObj instanceof Identifier id ? Optional.of(id) : Optional.empty();
            return Optional.of(new ClimateEnvelopeSample(meanC, precipMm, biomeId));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Optional.empty();
        }
    }
}
