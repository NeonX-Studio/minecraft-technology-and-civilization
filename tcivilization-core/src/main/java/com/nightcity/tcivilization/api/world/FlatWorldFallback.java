package com.nightcity.tcivilization.api.world;

import com.nightcity.tcivilization.api.CivCalendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import org.jspecify.annotations.Nullable;

/**
 * Default flat world fallback used when tcwildlands is absent (03 §12.5, 10 §3.1, AC-WLD-30).
 * Fixed latitude 45°N, identity X coordinates, calendar-driven seasons, temperate baseline climate.
 */
public final class FlatWorldFallback {
    public static final double FALLBACK_LAT_DEG = 45.0;
    private static final Identifier PLAINS_BIOME = Identifier.withDefaultNamespace("plains");
    private static final Identifier STONE_ROCK = Identifier.withDefaultNamespace("stone");
    private static final Identifier GRASS_BLOCK = Identifier.withDefaultNamespace("grass_block");
    private static final Identifier DIRT_BLOCK = Identifier.withDefaultNamespace("dirt");

    public static final PlanetApi PLANET = new PlanetApi() {
        @Override
        public boolean isPlanetActive() {
            return false;
        }

        @Override
        public int planetWidth() {
            return PLANET_WIDTH;
        }

        @Override
        public int planetHeight() {
            return PLANET_HEIGHT;
        }

        @Override
        public LatLon latLon(int x, int z) {
            double lon = Math.clamp((x / (double) X_MAX) * 180.0, -180.0, 180.0);
            return new LatLon(FALLBACK_LAT_DEG, lon);
        }

        @Override
        public BlockPos toBlock(double latDeg, double lonDeg, int y) {
            int x = (int) Math.round((lonDeg / 180.0) * X_MAX);
            int z = 0;
            return new BlockPos(x, y, z);
        }

        @Override
        public int wrapX(int x) {
            return x;
        }

        @Override
        public int wrappedDx(int fromX, int toX) {
            return toX - fromX;
        }

        @Override
        public double wrappedDistance(BlockPos a, BlockPos b) {
            double dx = b.getX() - a.getX();
            double dz = b.getZ() - a.getZ();
            return Math.hypot(dx, dz);
        }

        @Override
        public double altitudeMeters(double y) {
            if (y >= SEA_LEVEL) {
                return 8_850.0 * Math.pow(Math.max(0.0, (y - SEA_LEVEL) / 797.0), 1.25);
            }
            return -(SEA_LEVEL - y) * 10.0;
        }

        @Override
        public double yForAltitude(double meters) {
            if (meters >= 0.0) {
                return SEA_LEVEL + 797.0 * Math.pow(Math.min(8_850.0, meters) / 8_850.0, 0.8);
            }
            double depth = -meters;
            if (depth <= 5_000.0) {
                return SEA_LEVEL - 340.0 * Math.pow(depth / 5_000.0, 0.8);
            }
            return -277.0 - ((depth - 5_000.0) / 6_000.0) * 183.0;
        }

        @Override
        public int surfaceY(int x, int z) {
            return 64;
        }

        @Override
        public Identifier biomeAt(int x, int z) {
            return PLAINS_BIOME;
        }

        @Override
        public float dayLengthHours(double latDeg, int dayOfYear) {
            int doy = Math.floorMod(dayOfYear, CivCalendar.YEAR_DAYS);
            double declRad = Math.toRadians(23.44 * Math.sin(2.0 * Math.PI * doy / CivCalendar.YEAR_DAYS));
            double latRad = Math.toRadians(Math.clamp(latDeg, -89.9, 89.9));
            double cosH0 = Math.clamp(-Math.tan(latRad) * Math.tan(declRad), -1.0, 1.0);
            double h0 = Math.acos(cosH0);
            return (float) (24.0 * (h0 / Math.PI));
        }

        @Override
        public float solarElevationDeg(BlockPos pos, long gameTime) {
            int doy = (int) Math.floorMod(Math.floorDiv(gameTime, CivCalendar.DAY_TICKS), CivCalendar.YEAR_DAYS);
            double declRad = Math.toRadians(23.44 * Math.sin(2.0 * Math.PI * doy / CivCalendar.YEAR_DAYS));
            double latRad = Math.toRadians(FALLBACK_LAT_DEG);
            long tickOfDay = Math.floorMod(gameTime, CivCalendar.DAY_TICKS);
            double hourAngle = ((tickOfDay - 6_000.0) / 24_000.0) * 2.0 * Math.PI;
            double sinEl = Math.sin(latRad) * Math.sin(declRad) + Math.cos(latRad) * Math.cos(declRad) * Math.cos(hourAngle);
            return (float) Math.toDegrees(Math.asin(Math.clamp(sinEl, -1.0, 1.0)));
        }

        @Override
        public PlanetSummary summary() {
            return PlanetSummary.flatFallback();
        }
    };

    public static final LocalSeasonProvider SEASONS = (level, pos, dayIndex) -> {
        int dayInYear = (int) Math.floorMod(dayIndex, CivCalendar.YEAR_DAYS);
        int seasonIdx = dayInYear / CivCalendar.SEASON_DAYS;
        float progress = (dayInYear % CivCalendar.SEASON_DAYS) / (float) CivCalendar.SEASON_DAYS;
        Season astro = Season.fromAstronomicalIndex(seasonIdx);
        return new LocalSeason(astro, SeasonRegime.TEMPERATE, astro.translationKey(), progress, false);
    };

    public static final ClimateApi CLIMATE = new ClimateApi() {
        @Override
        public ClimateSample sample(BlockPos pos, int dayOfYear) {
            int doy = Math.floorMod(dayOfYear, CivCalendar.YEAR_DAYS);
            float meanC = (float) (11.0 + 10.0 * Math.cos(2.0 * Math.PI * (doy - 18.0) / 48.0)) - Math.max(0, pos.getY() - 64) * 0.05f;
            Season s = Season.fromAstronomicalIndex(doy / CivCalendar.SEASON_DAYS);
            float frost = meanC < 0.0f ? 0.8f : (meanC < 4.0f ? 0.3f : 0.0f);
            return new ClimateSample(meanC, meanC - 5.0f, meanC + 5.0f, 2.2f, 0.65f, frost, Math.max(0.0f, meanC - 5.0f), meanC < 0.0f ? 0.25f : 0.0f, s);
        }

        @Override
        public ClimateNormals normals(BlockPos pos) {
            float[] temps = new float[12];
            float[] precips = new float[12];
            float sumT = 0.0f;
            float sumP = 0.0f;
            for (int m = 0; m < 12; m++) {
                temps[m] = (float) (11.0 + 10.0 * Math.cos(2.0 * Math.PI * (m - 6.5) / 12.0));
                precips[m] = 65.0f;
                sumT += temps[m];
                sumP += precips[m];
            }
            return new ClimateNormals(temps, precips, "Cfb", sumT / 12.0f, sumP, 0.2f);
        }

        @Override
        public String koppen(BlockPos pos) {
            return "Cfb";
        }

        @Override
        public float temperatureNow(@Nullable ServerLevel level, BlockPos pos) {
            long dayTime = level != null ? level.getOverworldClockTime() : 6_000L;
            int doy = (int) Math.floorMod(Math.floorDiv(dayTime, CivCalendar.DAY_TICKS), CivCalendar.YEAR_DAYS);
            return sample(pos, doy).meanC();
        }

        @Override
        public float globalDeltaT() {
            return 0.0f;
        }
    };

    public static final WeatherApi WEATHER = new WeatherApi() {
        @Override
        public WeatherState current(@Nullable Level level, BlockPos pos) {
            if (level != null && level.isRaining()) {
                return new WeatherState(PrecipType.RAIN, 3.0f, 0.9f, 4.0f, 270.0f, level.isThundering() ? 2 : 0, -1.5f, Optional.empty());
            }
            return WeatherState.clear();
        }

        @Override
        public Vec2 wind(BlockPos pos) {
            return new Vec2(2.5f, 0.0f);
        }

        @Override
        public boolean isPrecipitating(@Nullable Level level, BlockPos pos) {
            return level != null && level.isRaining();
        }

        @Override
        public PrecipType precipType(@Nullable Level level, BlockPos pos) {
            return (level != null && level.isRaining()) ? PrecipType.RAIN : PrecipType.NONE;
        }

        @Override
        public WeatherForecast forecast(BlockPos pos, int days, ForecastSkill skill) {
            int clampedDays = Math.clamp(days, 1, 5);
            return new WeatherForecast(pos, clampedDays, skill, List.of(), skill.relativeError());
        }
    };

    public static final GeoApi GEO = new GeoApi() {
        @Override
        public Identifier rockAt(int x, int y, int z) {
            return STONE_ROCK;
        }

        @Override
        public StrataColumn strata(int x, int z) {
            return new StrataColumn(GeoProvince.CRATON_PLATFORM, List.of(new StrataColumn.Layer(STONE_ROCK, 64, 576, 0.0f, 0.0f)));
        }

        @Override
        public GeoProvince province(int x, int z) {
            return GeoProvince.CRATON_PLATFORM;
        }

        @Override
        public float heatFlow(BlockPos pos) {
            return 65.0f;
        }

        @Override
        public float seismicHazardPga(BlockPos pos) {
            return 0.02f;
        }

        @Override
        public Optional<Reservoir> reservoirAt(BlockPos pos) {
            return Optional.empty();
        }

        @Override
        public long extract(long reservoirId, long amountMb, boolean simulate) {
            return 0L;
        }

        @Override
        public DepositSummary depositSummary(long regionKey, Optional<UUID> civFilter) {
            return DepositSummary.empty(regionKey);
        }

        @Override
        public List<Deposit> knownDeposits(UUID civ, BlockPos center, int radius) {
            return List.of();
        }

        @Override
        public void markDiscovered(long depositId, UUID civ, Optional<ServerPlayer> by, ProspectMethod method) {
        }

        @Override
        public void recordHistoricalTrace(HistoricalTrace trace) {
        }
    };

    public static final HydroApi HYDRO = new HydroApi() {
        @Override
        public Optional<RiverInfo> river(BlockPos pos) {
            return Optional.empty();
        }

        @Override
        public GroundwaterInfo groundwater(BlockPos pos) {
            return new GroundwaterInfo(6.0f, STONE_ROCK, 0.2f, false);
        }

        @Override
        public int basinId(int x, int z) {
            return 0;
        }

        @Override
        public float floodRisk(BlockPos pos) {
            return 0.0f;
        }

        @Override
        public Vec2 flowVelocity(BlockPos pos) {
            return Vec2.ZERO;
        }

        @Override
        public void registerWaterWork(WaterWorkRef ref) {
        }

        @Override
        public void withdraw(BlockPos pos, float m3PerDay, Identifier consumer) {
        }
    };

    public static final SuitabilityApi SUITABILITY = new SuitabilityApi() {
        @Override
        public SiteScore citySiteScore(BlockPos pos) {
            int elevPenalty = Math.min(40, Math.abs(pos.getY() - 68) / 2);
            int total = Math.clamp(75 - elevPenalty, 0, 100);
            return new SiteScore(total, Map.of("elevation", 100 - elevPenalty, "water", 70), List.of());
        }

        @Override
        public SiteScore startSiteScore(BlockPos pos) {
            return citySiteScore(pos);
        }

        @Override
        public SoilParent soilParent(BlockPos pos) {
            return new SoilParent(DIRT_BLOCK, STONE_ROCK, SoilParent.Texture.LOAM, 0.12f, 6.5f, SoilParent.Drainage.GOOD, 4.0f);
        }

        @Override
        public TerrainCost terrainCost(BlockPos from, BlockPos to, TransportMode mode) {
            double dist = Math.hypot(to.getX() - from.getX(), to.getZ() - from.getZ());
            return new TerrainCost(dist, 0.0f, 0.0f, 0.0, List.of(), List.of(), Map.of(DIRT_BLOCK, 1.0f), List.of(), (float) dist);
        }
    };

    public static final HazardApi HAZARDS = new HazardApi() {
        @Override
        public float hazardLevel(BlockPos pos) {
            return 0.0f;
        }

        @Override
        public HazardProfile profile(BlockPos pos) {
            return HazardProfile.zero();
        }

        @Override
        public List<HazardForecast> forecast(BlockPos pos, int radius, int horizonTicks, ForecastSkill skill) {
            return List.of();
        }

        @Override
        public float droughtIndex(BlockPos pos) {
            return 0.0f;
        }

        @Override
        public List<ActiveHazardView> active(@Nullable ServerLevel level) {
            return List.of();
        }

        @Override
        public void registerMitigation(MitigationRef ref) {
        }

        @Override
        public void requestManagedBurn(ServerLevel level, BlockPos center, int radius, float spreadFactor) {
        }
    };

    public static final ProspectingApi PROSPECTING = new ProspectingApi() {
        @Override
        public ProspectReading query(ServerPlayer player, ProspectMethod method, BlockPos pos, int radius) {
            return new ProspectReading(method, List.of(), Optional.empty(), 0.0f, 0, List.of());
        }

        @Override
        public ProspectReading queryAbstract(UUID civ, ProspectMethod method, BlockPos pos, int radius) {
            return new ProspectReading(method, List.of(), Optional.empty(), 0.0f, 0, List.of());
        }
    };

    public static final TerrainPreviewApi TERRAIN_PREVIEW = (x, z) -> new TerrainPreviewApi.PreviewSample(
            x, z, 64, PlanetApi.SEA_LEVEL, false, TerrainPreviewApi.SurfaceKind.GRASS, PLAINS_BIOME, GRASS_BLOCK
    );

    public static final SeasonalColorApi SEASONAL_COLORS = (pos, target, baseColor) -> baseColor;

    public static final WorldApiSet SET = new WorldApiSet(
            PLANET, CLIMATE, WEATHER, GEO, HYDRO, SUITABILITY, HAZARDS, PROSPECTING, SEASONS, TERRAIN_PREVIEW
    );

    private FlatWorldFallback() {
    }
}
