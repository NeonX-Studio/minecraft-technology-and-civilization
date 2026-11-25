package com.nightcity.wildlands.api.impl;

import com.nightcity.tcivilization.RegionCoordinates;
import com.nightcity.tcivilization.api.CivCalendar;
import com.nightcity.tcivilization.api.TcThreads;
import com.nightcity.tcivilization.api.world.ActiveHazardView;
import com.nightcity.tcivilization.api.world.Anomaly;
import com.nightcity.tcivilization.api.world.AnomalyKind;
import com.nightcity.tcivilization.api.world.ClimateApi;
import com.nightcity.tcivilization.api.world.ClimateNormals;
import com.nightcity.tcivilization.api.world.ClimateSample;
import com.nightcity.tcivilization.api.world.Deposit;
import com.nightcity.tcivilization.api.world.DepositSummary;
import com.nightcity.tcivilization.api.world.FlatWorldFallback;
import com.nightcity.tcivilization.api.world.ForecastSkill;
import com.nightcity.tcivilization.api.world.GeoApi;
import com.nightcity.tcivilization.api.world.GeoProvince;
import com.nightcity.tcivilization.api.world.GroundwaterInfo;
import com.nightcity.tcivilization.api.world.HazardApi;
import com.nightcity.tcivilization.api.world.HazardForecast;
import com.nightcity.tcivilization.api.world.HazardProfile;
import com.nightcity.tcivilization.api.world.HistoricalTrace;
import com.nightcity.tcivilization.api.world.HydroApi;
import com.nightcity.tcivilization.api.world.LatLon;
import com.nightcity.tcivilization.api.world.LocalSeasonProvider;
import com.nightcity.tcivilization.api.world.MitigationRef;
import com.nightcity.tcivilization.api.world.PlanetApi;
import com.nightcity.tcivilization.api.world.PlanetSummary;
import com.nightcity.tcivilization.api.world.PrecipType;
import com.nightcity.tcivilization.api.world.ProspectMethod;
import com.nightcity.tcivilization.api.world.ProspectReading;
import com.nightcity.tcivilization.api.world.ProspectingApi;
import com.nightcity.tcivilization.api.world.Reservoir;
import com.nightcity.tcivilization.api.world.RiverInfo;
import com.nightcity.tcivilization.api.world.Season;
import com.nightcity.tcivilization.api.world.SeasonalColorApi;
import com.nightcity.tcivilization.api.world.SiteScore;
import com.nightcity.tcivilization.api.world.SoilParent;
import com.nightcity.tcivilization.api.world.StrataColumn;
import com.nightcity.tcivilization.api.world.SuitabilityApi;
import com.nightcity.tcivilization.api.world.TerrainCost;
import com.nightcity.tcivilization.api.world.TerrainPreviewApi;
import com.nightcity.tcivilization.api.world.TintTarget;
import com.nightcity.tcivilization.api.world.TransportMode;
import com.nightcity.tcivilization.api.world.WaterWorkRef;
import com.nightcity.tcivilization.api.world.WeatherApi;
import com.nightcity.tcivilization.api.world.WeatherForecast;
import com.nightcity.tcivilization.api.world.WeatherState;
import com.nightcity.tcivilization.api.world.WorldApiSet;
import com.nightcity.tcivilization.api.world.event.DepositDiscoveredEvent;
import com.nightcity.tcivilization.api.world.event.ReservoirDepletedEvent;
import com.nightcity.wildlands.planet.PlanetCoordinates;
import com.nightcity.wildlands.planet.PlanetModel;
import com.nightcity.wildlands.planet.PlanetRuntime;
import com.nightcity.wildlands.planet.WildlandsCatalog;
import com.nightcity.wildlands.weather.RegionalWeatherEngine;
import com.nightcity.wildlands.worldgen.PlanetChunkSynthesizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

/**
 * Concrete Wildlands implementations of all core-hosted com.nightcity.tcivilization.api.world contracts (10 §12.1).
 */
public final class WildlandsApiImpl {
    private static final Map<Long, Long> RESERVOIR_REMAINING_MB = new ConcurrentHashMap<>();
    private static final Set<String> RESERVOIR_THRESHOLDS_FIRED = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Set<Long>> DISCOVERED_DEPOSITS_BY_CIV = new ConcurrentHashMap<>();
    private static final List<WaterWorkRef> WATER_WORKS = new CopyOnWriteArrayList<>();
    private static final List<MitigationRef> MITIGATIONS = new CopyOnWriteArrayList<>();
    private static final Map<Long, Float> GROUNDWATER_DRAWDOWN = new ConcurrentHashMap<>();

    private WildlandsApiImpl() {
    }

    public static void clearMutableStateForTests() {
        RESERVOIR_REMAINING_MB.clear();
        RESERVOIR_THRESHOLDS_FIRED.clear();
        DISCOVERED_DEPOSITS_BY_CIV.clear();
        WATER_WORKS.clear();
        MITIGATIONS.clear();
        GROUNDWATER_DRAWDOWN.clear();
        PlanetChunkSynthesizer.clearTracesForTests();
    }

    public static WorldApiSet createApiSet() {
        return new WorldApiSet(
                PLANET_API,
                CLIMATE_API,
                WEATHER_API,
                GEO_API,
                HYDRO_API,
                SUITABILITY_API,
                HAZARD_API,
                PROSPECTING_API,
                SEASON_PROVIDER,
                TERRAIN_PREVIEW_API
        );
    }

    public static final PlanetApi PLANET_API = new PlanetApiImpl();

    public static final class PlanetApiImpl implements PlanetApi {
        @Override
        public boolean isPlanetActive() {
            return PlanetRuntime.isPlanetActive();
        }

        @Override
        public int planetWidth() {
            return PlanetCoordinates.PLANET_WIDTH;
        }

        @Override
        public int planetHeight() {
            return PlanetCoordinates.PLANET_HEIGHT;
        }

        @Override
        public LatLon latLon(int x, int z) {
            if (!PlanetRuntime.isPlanetActive()) {
                return FlatWorldFallback.PLANET.latLon(x, z);
            }
            return PlanetCoordinates.latLon(x, z);
        }

        @Override
        public BlockPos toBlock(double latDeg, double lonDeg, int y) {
            return PlanetCoordinates.toBlock(latDeg, lonDeg, y);
        }

        @Override
        public int wrapX(int x) {
            return PlanetRuntime.isPlanetActive() ? PlanetCoordinates.wrapX(x) : x;
        }

        @Override
        public int wrappedDx(int fromX, int toX) {
            return PlanetRuntime.isPlanetActive() ? PlanetCoordinates.wrappedDx(fromX, toX) : (toX - fromX);
        }

        @Override
        public double wrappedDistance(BlockPos a, BlockPos b) {
            return PlanetRuntime.isPlanetActive() ? PlanetCoordinates.wrappedDistance(a, b) : FlatWorldFallback.PLANET.wrappedDistance(a, b);
        }

        @Override
        public double altitudeMeters(double y) {
            return PlanetCoordinates.altitudeMeters(y);
        }

        @Override
        public double yForAltitude(double meters) {
            return PlanetCoordinates.yForAltitude(meters);
        }

        @Override
        public int surfaceY(int x, int z) {
            PlanetModel m = PlanetRuntime.model();
            if (m == null) {
                return 64;
            }
            return m.sampleL1SurfaceY(x, z);
        }

        @Override
        public Identifier biomeAt(int x, int z) {
            PlanetModel m = PlanetRuntime.model();
            if (m == null) {
                return FlatWorldFallback.PLANET.biomeAt(x, z);
            }
            return m.biomeSpecAt(x, z).id();
        }

        @Override
        public float dayLengthHours(double latDeg, int dayOfYear) {
            return PlanetCoordinates.dayLengthHours(latDeg, dayOfYear);
        }

        @Override
        public float solarElevationDeg(BlockPos pos, long gameTime) {
            return PlanetCoordinates.solarElevationDeg(pos, gameTime);
        }

        @Override
        public PlanetSummary summary() {
            PlanetModel m = PlanetRuntime.model();
            return m != null ? m.summary() : PlanetSummary.flatFallback();
        }
    }

    public static final LocalSeasonProvider SEASON_PROVIDER = new SeasonProviderImpl();

    public static final class SeasonProviderImpl implements LocalSeasonProvider {
        @Override
        public com.nightcity.tcivilization.api.world.LocalSeason season(@Nullable Level level, BlockPos pos, long dayIndex) {
            return RegionalWeatherEngine.resolveLocalSeason(pos, (int) Math.floorMod(dayIndex, CivCalendar.YEAR_DAYS));
        }
    }

    public static final ClimateApi CLIMATE_API = new ClimateApiImpl();

    public static final class ClimateApiImpl implements ClimateApi {
        @Override
        public ClimateSample sample(BlockPos pos, int dayOfYear) {
            PlanetModel m = PlanetRuntime.model();
            if (m == null) {
                return FlatWorldFallback.CLIMATE.sample(pos, dayOfYear);
            }
            int doy = Math.floorMod(dayOfYear, CivCalendar.YEAR_DAYS);
            ClimateNormals n = m.climateNormalsAt(pos.getX(), pos.getZ());
            int month = Math.clamp(doy / 4, 0, 11);
            float meanC = n.tempMonthlyC()[month] + RegionalWeatherEngine.globalDeltaT();
            float precipDay = n.precipMonthlyMm()[month] / 30.0f;
            float humidity = Math.clamp(n.annualPrecipMm() / 1_600.0f, 0.08f, 0.98f);
            float frost = meanC < 0.0f ? 0.9f : (meanC < 4.0f ? 0.35f : 0.0f);
            Season s = RegionalWeatherEngine.resolveLocalSeason(pos, doy).effectiveSeason();
            return new ClimateSample(
                    meanC, meanC - 4.5f, meanC + 4.5f, precipDay, humidity, frost,
                    Math.max(0.0f, meanC - 5.0f), meanC < -1.0f ? 0.35f : 0.0f, s
            );
        }

        @Override
        public ClimateNormals normals(BlockPos pos) {
            PlanetModel m = PlanetRuntime.model();
            return m != null ? m.climateNormalsAt(pos.getX(), pos.getZ()) : FlatWorldFallback.CLIMATE.normals(pos);
        }

        @Override
        public String koppen(BlockPos pos) {
            return normals(pos).koppen();
        }

        @Override
        public float temperatureNow(@Nullable ServerLevel level, BlockPos pos) {
            long dayTime = level != null ? level.getOverworldClockTime() : 6_000L;
            WeatherState ws = RegionalWeatherEngine.stateAt(pos);
            return RegionalWeatherEngine.sampleTemperatureInstant(pos.getX(), pos.getY(), pos.getZ(), dayTime, ws.tempAnomalyC());
        }

        @Override
        public float globalDeltaT() {
            return RegionalWeatherEngine.globalDeltaT();
        }
    }

    public static final WeatherApi WEATHER_API = new WeatherApiImpl();

    public static final class WeatherApiImpl implements WeatherApi {
        @Override
        public WeatherState current(@Nullable Level level, BlockPos pos) {
            if (!PlanetRuntime.isPlanetActive()) {
                return FlatWorldFallback.WEATHER.current(level, pos);
            }
            return RegionalWeatherEngine.stateAt(pos);
        }

        @Override
        public Vec2 wind(BlockPos pos) {
            return PlanetRuntime.isPlanetActive() ? RegionalWeatherEngine.windAt(pos) : FlatWorldFallback.WEATHER.wind(pos);
        }

        @Override
        public boolean isPrecipitating(@Nullable Level level, BlockPos pos) {
            return current(level, pos).type() != PrecipType.NONE;
        }

        @Override
        public PrecipType precipType(@Nullable Level level, BlockPos pos) {
            return current(level, pos).type();
        }

        @Override
        public WeatherForecast forecast(BlockPos pos, int days, ForecastSkill skill) {
            return RegionalWeatherEngine.buildForecast(pos, days, skill);
        }
    }

    public static final GeoApi GEO_API = new GeoApiImpl();

    public static final class GeoApiImpl implements GeoApi {
        @Override
        public Identifier rockAt(int x, int y, int z) {
            PlanetModel m = PlanetRuntime.model();
            if (m == null) {
                return FlatWorldFallback.GEO.rockAt(x, y, z);
            }
            return m.strataAt(x, z).rockAtY(y);
        }

        @Override
        public StrataColumn strata(int x, int z) {
            PlanetModel m = PlanetRuntime.model();
            return m != null ? m.strataAt(x, z) : FlatWorldFallback.GEO.strata(x, z);
        }

        @Override
        public GeoProvince province(int x, int z) {
            PlanetModel m = PlanetRuntime.model();
            return m != null ? m.provinceAt(x, z) : GeoProvince.CRATON_PLATFORM;
        }

        @Override
        public float heatFlow(BlockPos pos) {
            PlanetModel m = PlanetRuntime.model();
            if (m == null) {
                return 65.0f;
            }
            GeoProvince prov = m.provinceAt(pos.getX(), pos.getZ());
            float base = switch (prov) {
                case RIFT, ARC, LIP -> 125.0f;
                case YOUNG_OROGEN -> 85.0f;
                case CRATON_SHIELD -> 42.0f;
                default -> 65.0f;
            };
            if (pos.getY() < -350) {
                base += (-350 - pos.getY()) * 1.4f;
            }
            return base;
        }

        @Override
        public float seismicHazardPga(BlockPos pos) {
            PlanetModel m = PlanetRuntime.model();
            if (m == null) {
                return 0.02f;
            }
            float minSq = Float.MAX_VALUE;
            for (PlanetModel.FaultInfo f : m.faults()) {
                float dx = PlanetCoordinates.wrappedDx(pos.getX(), f.x1());
                float dz = pos.getZ() - f.z1();
                float dSq = dx * dx + dz * dz;
                if (dSq < minSq) {
                    minSq = dSq;
                }
            }
            double distKm = Math.sqrt(minSq) * 0.2446;
            return (float) Math.clamp(0.85 * Math.exp(-distKm / 120.0), 0.02, 0.95);
        }

        @Override
        public Optional<Reservoir> reservoirAt(BlockPos pos) {
            PlanetModel m = PlanetRuntime.model();
            if (m == null) {
                return Optional.empty();
            }
            long rk = RegionCoordinates.fromBlock(pos.getX(), pos.getZ()).key();
            for (Reservoir r : m.reservoirsInRegion(rk)) {
                int dx = PlanetCoordinates.wrappedDx(r.center().getX(), pos.getX());
                int dz = pos.getZ() - r.center().getZ();
                if (dx * dx + dz * dz <= r.radiusXZ() * r.radiusXZ() && pos.getY() <= r.topY() && pos.getY() >= r.bottomY()) {
                    long rem = RESERVOIR_REMAINING_MB.getOrDefault(r.id(), r.initialCapacityMb());
                    return Optional.of(new Reservoir(r.id(), r.kind(), r.center(), r.radiusXZ(), r.topY(), r.bottomY(), r.initialCapacityMb(), rem, r.pressureBar()));
                }
            }
            return Optional.empty();
        }

        @Override
        public synchronized long extract(long reservoirId, long amountMb, boolean simulate) {
            TcThreads.assertMain();
            if (amountMb <= 0L) {
                return 0L;
            }
            PlanetModel m = PlanetRuntime.model();
            if (m == null) {
                return 0L;
            }
            Optional<Reservoir> opt = m.reservoirById(reservoirId);
            if (opt.isEmpty()) {
                return 0L;
            }
            Reservoir r = opt.get();
            long cur = RESERVOIR_REMAINING_MB.getOrDefault(reservoirId, r.initialCapacityMb());
            long taken = Math.min(cur, amountMb);
            if (!simulate && taken > 0L) {
                long next = cur - taken;
                RESERVOIR_REMAINING_MB.put(reservoirId, next);
                float frac = r.initialCapacityMb() > 0 ? (next / (float) r.initialCapacityMb()) : 0.0f;
                for (float threshold : new float[]{0.5f, 0.1f, 0.0f}) {
                    if (frac <= threshold && RESERVOIR_THRESHOLDS_FIRED.add(reservoirId + ":" + threshold)) {
                        NeoForge.EVENT_BUS.post(new ReservoirDepletedEvent(reservoirId, threshold));
                    }
                }
            }
            return taken;
        }

        @Override
        public DepositSummary depositSummary(long regionKey, Optional<UUID> civFilter) {
            PlanetModel m = PlanetRuntime.model();
            if (m == null) {
                return DepositSummary.empty(regionKey);
            }
            Set<Long> known = civFilter.map(c -> DISCOVERED_DEPOSITS_BY_CIV.getOrDefault(c, Set.of())).orElse(null);
            List<Deposit> list = new ArrayList<>();
            Map<Identifier, Long> tonnageMap = new LinkedHashMap<>();
            for (Deposit d : m.depositsInRegion(regionKey)) {
                boolean disc = known == null || known.contains(d.id());
                if (civFilter.isPresent() && !disc) {
                    continue;
                }
                float ancientFrac = PlanetChunkSynthesizer.ancientMinedFraction(d.id());
                long remTonnage = Math.max(0L, Math.round(d.tonnage() * (1.0f - ancientFrac)));
                Deposit view = new Deposit(
                        d.id(), d.type(), d.mineral(), d.byproducts(), d.center(),
                        d.radiusXZ(), d.radiusY(), d.strikeDeg(), d.dipDeg(),
                        d.hostRock(), d.gradeMean(), remTonnage, disc, d.surfaceSigns()
                );
                list.add(view);
                tonnageMap.merge(d.mineral(), remTonnage, Long::sum);
            }
            return new DepositSummary(regionKey, list, tonnageMap);
        }

        @Override
        public List<Deposit> knownDeposits(UUID civ, BlockPos center, int radius) {
            PlanetModel m = PlanetRuntime.model();
            if (m == null) {
                return List.of();
            }
            Set<Long> known = DISCOVERED_DEPOSITS_BY_CIV.getOrDefault(civ, Set.of());
            if (known.isEmpty()) {
                return List.of();
            }
            List<Deposit> out = new ArrayList<>();
            long rSq = (long) radius * radius;
            for (Long id : known) {
                m.depositById(id).ifPresent(d -> {
                    long dx = PlanetCoordinates.wrappedDx(center.getX(), d.center().getX());
                    long dz = d.center().getZ() - center.getZ();
                    if (dx * dx + dz * dz <= rSq) {
                        out.add(new Deposit(
                                d.id(), d.type(), d.mineral(), d.byproducts(), d.center(),
                                d.radiusXZ(), d.radiusY(), d.strikeDeg(), d.dipDeg(),
                                d.hostRock(), d.gradeMean(), d.tonnage(), true, d.surfaceSigns()
                        ));
                    }
                });
            }
            return out;
        }

        @Override
        public void markDiscovered(long depositId, UUID civ, Optional<ServerPlayer> by, ProspectMethod method) {
            TcThreads.assertMain();
            PlanetModel m = PlanetRuntime.model();
            if (m == null) {
                return;
            }
            m.depositById(depositId).ifPresent(d -> {
                Set<Long> set = DISCOVERED_DEPOSITS_BY_CIV.computeIfAbsent(civ, k -> ConcurrentHashMap.newKeySet());
                if (set.add(depositId)) {
                    NeoForge.EVENT_BUS.post(new DepositDiscoveredEvent(d, civ, by, method));
                }
            });
        }

        @Override
        public void recordHistoricalTrace(HistoricalTrace trace) {
            TcThreads.assertMain();
            if (trace.kind() == HistoricalTrace.Kind.ANCIENT_WORKINGS && trace.depositId().isPresent()) {
                PlanetChunkSynthesizer.recordAncientMining(trace.depositId().getAsLong(), trace.minedFraction());
            } else {
                int clampedRadius = Math.clamp(trace.radius(), 8, 128);
                HistoricalTrace norm = new HistoricalTrace(trace.kind(), trace.pos(), clampedRadius, trace.year(), trace.depositId(), 0.0f);
                long rk = RegionCoordinates.fromBlock(trace.pos().getX(), trace.pos().getZ()).key();
                PlanetChunkSynthesizer.addHistoricalTrace(rk, norm);
            }
        }
    }

    public static final HydroApi HYDRO_API = new HydroApiImpl();

    public static final class HydroApiImpl implements HydroApi {
        @Override
        public Optional<RiverInfo> river(BlockPos pos) {
            PlanetModel m = PlanetRuntime.model();
            return m != null ? m.riverAt(pos) : Optional.empty();
        }

        @Override
        public GroundwaterInfo groundwater(BlockPos pos) {
            PlanetModel m = PlanetRuntime.model();
            if (m == null) {
                return FlatWorldFallback.HYDRO.groundwater(pos);
            }
            int idx = PlanetModel.l0Index(PlanetModel.blockToL0X(pos.getX()), PlanetModel.blockToL0Z(pos.getZ()));
            long rk = RegionCoordinates.fromBlock(pos.getX(), pos.getZ()).key();
            float draw = GROUNDWATER_DRAWDOWN.getOrDefault(rk, 0.0f);
            float depth = m.decodeGroundwaterMeters(idx) + draw;
            Identifier rock = WildlandsCatalog.rockByPalette(m.rawRockTop()[idx] & 0xFF).id();
            return new GroundwaterInfo(depth, rock, 0.25f, depth < 0.8f);
        }

        @Override
        public int basinId(int x, int z) {
            PlanetModel m = PlanetRuntime.model();
            if (m == null) {
                return 0;
            }
            int idx = PlanetModel.l0Index(PlanetModel.blockToL0X(x), PlanetModel.blockToL0Z(z));
            return m.rawBasin()[idx] & 0xFFFF;
        }

        @Override
        public float floodRisk(BlockPos pos) {
            Optional<RiverInfo> riv = river(pos);
            if (riv.isEmpty()) {
                return 0.02f;
            }
            int aboveWater = pos.getY() - riv.get().waterSurfaceY();
            return Math.clamp(1.0f - Math.max(0, aboveWater) / 10.0f, 0.0f, 1.0f);
        }

        @Override
        public Vec2 flowVelocity(BlockPos pos) {
            PlanetModel m = PlanetRuntime.model();
            return m != null ? m.sampleFlowVelocity(pos.getX(), pos.getZ()) : Vec2.ZERO;
        }

        @Override
        public void registerWaterWork(WaterWorkRef ref) {
            TcThreads.assertMain();
            WATER_WORKS.add(ref);
        }

        @Override
        public void withdraw(BlockPos pos, float m3PerDay, Identifier consumer) {
            TcThreads.assertMain();
            if (m3PerDay > 0.0f) {
                long rk = RegionCoordinates.fromBlock(pos.getX(), pos.getZ()).key();
                GROUNDWATER_DRAWDOWN.merge(rk, m3PerDay * 0.0001f, Float::sum);
            }
        }
    }

    public static final SuitabilityApi SUITABILITY_API = new SuitabilityApiImpl();

    public static final class SuitabilityApiImpl implements SuitabilityApi {
        @Override
        public SiteScore citySiteScore(BlockPos pos) {
            PlanetModel m = PlanetRuntime.model();
            if (m == null) {
                return FlatWorldFallback.SUITABILITY.citySiteScore(pos);
            }
            int idx = PlanetModel.l0Index(PlanetModel.blockToL0X(pos.getX()), PlanetModel.blockToL0Z(pos.getZ()));
            short elevM = m.rawElevation()[idx];
            if (elevM < 0) {
                return new SiteScore(0, Map.of("land", 0), List.of());
            }
            ClimateNormals clim = m.climateNormalsAt(pos.getX(), pos.getZ());
            int water = (m.rawRiverOrder()[idx] & 0xFF) >= 2 ? 95 : 55;
            int arable = Math.clamp(100 - (int) m.decodeReliefMeters(idx) / 2, 10, 95);
            int climateScore = Math.clamp(100 - (int) (Math.abs(clim.annualMeanC() - 15.0f) * 3.5f), 10, 98);
            int quake = Math.clamp(100 - Math.round(GEO_API.seismicHazardPga(pos) * 80.0f), 20, 100);
            int flood = Math.clamp(100 - Math.round(HYDRO_API.floodRisk(pos) * 70.0f), 20, 100);
            int total = Math.clamp(Math.round(water * 0.25f + arable * 0.25f + climateScore * 0.22f + quake * 0.14f + flood * 0.14f), 0, 100);
            return new SiteScore(total, Map.of("water", water, "arable", arable, "climate", climateScore, "quake", quake, "flood", flood), List.of());
        }

        @Override
        public SiteScore startSiteScore(BlockPos pos) {
            PlanetModel m = PlanetRuntime.model();
            if (m == null) {
                return FlatWorldFallback.SUITABILITY.startSiteScore(pos);
            }
            LatLon ll = PlanetCoordinates.latLon(pos.getX(), pos.getZ());
            ClimateNormals clim = m.climateNormalsAt(pos.getX(), pos.getZ());
            int idx = PlanetModel.l0Index(PlanetModel.blockToL0X(pos.getX()), PlanetModel.blockToL0Z(pos.getZ()));
            short elevM = m.rawElevation()[idx];
            if (elevM < 5 || elevM > 2_500 || Math.abs(ll.latDeg()) > 58.0
                    || clim.koppen().startsWith("BW") || clim.koppen().equals("EF") || clim.koppen().equals("ET")) {
                return new SiteScore(0, Map.of("gate", 0), List.of());
            }
            return citySiteScore(pos);
        }

        @Override
        public SoilParent soilParent(BlockPos pos) {
            PlanetModel m = PlanetRuntime.model();
            if (m == null) {
                return FlatWorldFallback.SUITABILITY.soilParent(pos);
            }
            WildlandsCatalog.BiomeSpec b = m.biomeSpecAt(pos.getX(), pos.getZ());
            Identifier bedrock = GEO_API.rockAt(pos.getX(), Math.max(PlanetCoordinates.Y_MIN + 10, pos.getY() - 8), pos.getZ());
            String mat = b.parentSoil().getPath();
            SoilParent.Texture tex = switch (mat) {
                case "quartz_sand", "sand" -> SoilParent.Texture.SAND;
                case "peat" -> SoilParent.Texture.PEAT;
                case "laterite", "terra_rossa" -> SoilParent.Texture.CLAY;
                case "silt", "loess" -> SoilParent.Texture.SILT;
                default -> SoilParent.Texture.LOAM;
            };
            float om = "chernozem".equals(b.underBlock().getPath()) ? 0.32f : ("peat".equals(mat) ? 0.38f : 0.12f);
            return new SoilParent(b.parentSoil(), bedrock, tex, om, 6.5f, SoilParent.Drainage.GOOD, 5.0f);
        }

        @Override
        public TerrainCost terrainCost(BlockPos from, BlockPos to, TransportMode mode) {
            double dist = PLANET_API.wrappedDistance(from, to);
            int steps = Math.max(2, (int) Math.min(256, Math.ceil(dist / 32.0)));
            int dxTotal = PLANET_API.wrappedDx(from.getX(), to.getX());
            int dzTotal = to.getZ() - from.getZ();
            float maxSlope = 0.0f;
            float sumSlope = 0.0f;
            double earthwork = 0.0;
            int prevY = PLANET_API.surfaceY(from.getX(), from.getZ());
            for (int i = 1; i <= steps; i++) {
                double u = i / (double) steps;
                int sx = PLANET_API.wrapX(from.getX() + (int) Math.round(dxTotal * u));
                int sz = PlanetCoordinates.clampZ(from.getZ() + (int) Math.round(dzTotal * u));
                int curY = PLANET_API.surfaceY(sx, sz);
                float segLen = (float) Math.max(1.0, dist / steps);
                float slope = Math.abs(curY - prevY) / segLen;
                if (slope > maxSlope) maxSlope = slope;
                sumSlope += slope;
                if (slope > mode.maxGrade()) {
                    earthwork += Math.abs(curY - prevY) * mode.corridorWidthBlocks() * segLen;
                }
                prevY = curY;
            }
            float meanSlope = sumSlope / steps;
            float costIndex = (float) (dist * (1.0f + meanSlope * 4.0f));
            return new TerrainCost(dist, maxSlope, meanSlope, earthwork, List.of(), List.of(), Map.of(), List.of(), costIndex);
        }
    }

    public static final HazardApi HAZARD_API = new HazardApiImpl();

    public static final class HazardApiImpl implements HazardApi {
        @Override
        public float hazardLevel(BlockPos pos) {
            return PlanetRuntime.isPlanetActive() ? profile(pos).composite() : 0.0f;
        }

        @Override
        public HazardProfile profile(BlockPos pos) {
            if (!PlanetRuntime.isPlanetActive()) {
                return HazardProfile.zero();
            }
            float flood = HYDRO_API.floodRisk(pos);
            float quake = Math.clamp(GEO_API.seismicHazardPga(pos), 0.0f, 1.0f);
            PlanetModel m = PlanetRuntime.model();
            float volc = 0.0f;
            if (m != null) {
                for (PlanetModel.VolcanoInfo v : m.volcanoes()) {
                    double d = Math.hypot(PlanetCoordinates.wrappedDx(pos.getX(), v.blockX()), pos.getZ() - v.blockZ());
                    if (d < 2_000.0) {
                        volc = Math.max(volc, (float) (1.0 - d / 2_000.0));
                    }
                }
            }
            return new HazardProfile(flood, quake, volc, quake * 0.4f, 0.05f, 0.15f, 0.20f);
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
            TcThreads.assertMain();
            MITIGATIONS.add(ref);
        }

        @Override
        public void requestManagedBurn(ServerLevel level, BlockPos center, int radius, float spreadFactor) {
            TcThreads.assertMain();
        }
    }

    public static final ProspectingApi PROSPECTING_API = new ProspectingApiImpl();

    public static final class ProspectingApiImpl implements ProspectingApi {
        @Override
        public ProspectReading query(ServerPlayer player, ProspectMethod method, BlockPos pos, int radius) {
            return queryAbstract(com.nightcity.tcivilization.api.Civilization.PLAYER_CIV_ID, method, pos, radius);
        }

        @Override
        public ProspectReading queryAbstract(UUID civ, ProspectMethod method, BlockPos pos, int radius) {
            PlanetModel m = PlanetRuntime.model();
            if (m == null) {
                return new ProspectReading(method, List.of(), Optional.empty(), 0.0f, 0, List.of());
            }
            float sigma = switch (method) {
                case MAGNETOMETER -> 0.25f;
                case GRAVIMETER, SATELLITE -> 0.20f;
                case AIRBORNE -> 0.15f;
                case SEISMIC -> 0.10f;
                default -> 0.0f;
            };
            int costTicks = switch (method) {
                case ROCK_TAP -> 20;
                case PANNING, MAGNETOMETER -> 40;
                case GRAVIMETER -> 100;
                case SEISMIC -> 600;
                default -> 0;
            };
            int effRadius = Math.max(16, radius);
            Random rng = new Random(pos.asLong() ^ method.ordinal() ^ m.worldSeed());
            List<Anomaly> anomalies = new ArrayList<>();
            List<Long> discovered = new ArrayList<>();
            for (Deposit d : m.deposits()) {
                double dx = PlanetCoordinates.wrappedDx(pos.getX(), d.center().getX());
                double dz = d.center().getZ() - pos.getZ();
                double dist = Math.hypot(dx, dz);
                if (dist > effRadius) {
                    continue;
                }
                double depth = Math.max(0.0, pos.getY() - d.center().getY());
                float baseStrength = (float) (Math.cbrt(Math.max(1L, d.tonnage())) * 0.08 * Math.exp(-depth / 180.0));
                float noisy = baseStrength + (float) (rng.nextGaussian() * sigma * Math.max(0.2f, baseStrength));
                if (noisy >= 0.35f) {
                    int unc = Math.max(4, Math.round(effRadius * (0.5f - 0.4f * Math.min(1.0f, noisy))));
                    anomalies.add(new Anomaly(d.center(), unc, AnomalyKind.MINERALIZATION, noisy, Optional.of(d.mineral())));
                    Set<Long> set = DISCOVERED_DEPOSITS_BY_CIV.computeIfAbsent(civ, k -> ConcurrentHashMap.newKeySet());
                    if (set.add(d.id())) {
                        discovered.add(d.id());
                    }
                }
            }
            Optional<StrataColumn> col = method == ProspectMethod.CORE_SAMPLE ? Optional.of(m.strataAt(pos.getX(), pos.getZ())) : Optional.empty();
            return new ProspectReading(method, anomalies, col, sigma, costTicks, discovered);
        }
    }

    public static final TerrainPreviewApi TERRAIN_PREVIEW_API = new TerrainPreviewApiImpl();

    public static final class TerrainPreviewApiImpl implements TerrainPreviewApi {
        @Override
        public PreviewSample sample(int x, int z) {
            PlanetModel m = PlanetRuntime.model();
            if (m == null) {
                return FlatWorldFallback.TERRAIN_PREVIEW.sample(x, z);
            }
            PlanetChunkSynthesizer.ColumnProfile prof = PlanetChunkSynthesizer.profileColumn(m, x, z, 3);
            boolean hasWater = prof.waterY() != Integer.MIN_VALUE && prof.waterY() > prof.surfaceY();
            int waterLevel = hasWater ? prof.waterY() : PlanetCoordinates.SEA_LEVEL;
            TerrainPreviewApi.SurfaceKind kind;
            if (hasWater) {
                String bp = prof.biomeId().getPath();
                if (bp.contains("river")) kind = TerrainPreviewApi.SurfaceKind.RIVER;
                else if (bp.contains("lake")) kind = TerrainPreviewApi.SurfaceKind.LAKE;
                else kind = TerrainPreviewApi.SurfaceKind.OCEAN;
            } else {
                String bp = prof.biomeId().getPath();
                if (bp.contains("ice") || bp.contains("glacier")) kind = TerrainPreviewApi.SurfaceKind.ICE;
                else if (bp.contains("desert")) kind = TerrainPreviewApi.SurfaceKind.DESERT;
                else if (bp.contains("beach")) kind = TerrainPreviewApi.SurfaceKind.BEACH;
                else if (bp.contains("volcanic")) kind = TerrainPreviewApi.SurfaceKind.VOLCANIC;
                else if (bp.contains("forest")) kind = TerrainPreviewApi.SurfaceKind.FOREST;
                else if (prof.slopeBlocks() >= 2) kind = TerrainPreviewApi.SurfaceKind.ROCK;
                else kind = TerrainPreviewApi.SurfaceKind.GRASS;
            }
            return new TerrainPreviewApi.PreviewSample(x, z, prof.surfaceY(), waterLevel, hasWater, kind, prof.biomeId(), prof.topBlockId());
        }
    }

    public static final SeasonalColorApi SEASONAL_COLOR_API = new SeasonalColorApiImpl();

    public static final class SeasonalColorApiImpl implements SeasonalColorApi {
        @Override
        public int tint(BlockPos pos, TintTarget target, int baseColor) {
            if (target != TintTarget.GRASS && target != TintTarget.FOLIAGE) {
                return baseColor;
            }
            Season s = SEASON_PROVIDER.season(pos);
            int r = (baseColor >> 16) & 0xFF;
            int g = (baseColor >> 8) & 0xFF;
            int b = baseColor & 0xFF;
            switch (s) {
                case AUTUMN -> {
                    r = Math.min(255, (int) (r * 1.28f + 22));
                    g = (int) (g * 0.88f);
                    b = (int) (b * 0.72f);
                }
                case WINTER, DRY -> {
                    r = (int) (r * 0.92f + 14);
                    g = (int) (g * 0.86f + 10);
                    b = (int) (b * 0.88f + 12);
                }
                case SPRING -> {
                    g = Math.min(255, (int) (g * 1.08f + 8));
                }
                default -> {
                }
            }
            return (baseColor & 0xFF000000) | (r << 16) | (g << 8) | b;
        }
    }
}
