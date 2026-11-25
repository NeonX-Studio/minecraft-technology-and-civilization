package com.nightcity.wildlands.weather;

import com.nightcity.tcivilization.api.CivCalendar;
import com.nightcity.tcivilization.api.world.ClimateNormals;
import com.nightcity.tcivilization.api.world.ForecastSkill;
import com.nightcity.tcivilization.api.world.LatLon;
import com.nightcity.tcivilization.api.world.LocalSeason;
import com.nightcity.tcivilization.api.world.PrecipType;
import com.nightcity.tcivilization.api.world.Season;
import com.nightcity.tcivilization.api.world.SeasonRegime;
import com.nightcity.tcivilization.api.world.WeatherForecast;
import com.nightcity.tcivilization.api.world.WeatherState;
import com.nightcity.tcivilization.api.world.WeatherSystemKind;
import com.nightcity.tcivilization.api.world.event.SeasonChangedEvent;
import com.nightcity.tcivilization.api.world.event.WeatherChangedEvent;
import com.nightcity.tcivilization.sim.TcEventBus;
import com.nightcity.wildlands.planet.PeriodicNoise;
import com.nightcity.wildlands.planet.PlanetCoordinates;
import com.nightcity.wildlands.planet.PlanetModel;
import com.nightcity.wildlands.planet.PlanetRuntime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

/**
 * Regional 160x80 weather grid (1024x1024 blocks/cell), synoptic weather systems, DR-13 hemisphere/monsoon
 * seasons, and vanilla crop climate factor (10 §5.12-5.13, §16.4).
 */
public final class RegionalWeatherEngine {
    public static final int CELL_BLOCKS = 1_024;
    public static final int GRID_W = PlanetCoordinates.PLANET_WIDTH / CELL_BLOCKS;  // 160
    public static final int GRID_H = PlanetCoordinates.PLANET_HEIGHT / CELL_BLOCKS; // 80
    public static final int CELL_COUNT = GRID_W * GRID_H;                           // 12,800

    public static final class SynopticSystem {
        public final int id;
        public final WeatherSystemKind kind;
        public double cellX;
        public double cellZ;
        public float radiusCells;
        public float intensity;
        public int remainingTicks;

        public SynopticSystem(int id, WeatherSystemKind kind, double cellX, double cellZ, float radiusCells, float intensity, int remainingTicks) {
            this.id = id;
            this.kind = kind;
            this.cellX = cellX;
            this.cellZ = cellZ;
            this.radiusCells = radiusCells;
            this.intensity = intensity;
            this.remainingTicks = remainingTicks;
        }
    }

    private static final WeatherState[] CELLS = new WeatherState[CELL_COUNT];
    private static final long[] OVERRIDE_UNTIL_TICK = new long[CELL_COUNT];
    private static final List<SynopticSystem> SYSTEMS = new CopyOnWriteArrayList<>();
    private static volatile int lastGlobalSeasonIndex = -1;
    private static volatile int lastGlobalYear = -1;
    private static volatile long currentTick = 0L;
    private static volatile int nextSystemId = 1;
    private static volatile float globalDeltaT = 0.0f;
    private static volatile double co2Ppm = 280.0;
    private static volatile long dayTimeOffsetTicks = 0L;

    public static long effectiveDayTime(long rawDayTime) {
        return rawDayTime + dayTimeOffsetTicks;
    }

    public static void setDayOfYearOverride(long rawDayTime, int targetDoy) {
        long curDoy = Math.floorMod(Math.floorDiv(rawDayTime, CivCalendar.DAY_TICKS), CivCalendar.YEAR_DAYS);
        dayTimeOffsetTicks = (targetDoy - curDoy) * CivCalendar.DAY_TICKS;
    }

    static {
        reset();
    }

    private RegionalWeatherEngine() {
    }

    public static void reset() {
        for (int i = 0; i < CELL_COUNT; i++) {
            CELLS[i] = WeatherState.clear();
            OVERRIDE_UNTIL_TICK[i] = 0L;
        }
        SYSTEMS.clear();
        lastGlobalSeasonIndex = -1;
        lastGlobalYear = -1;
        currentTick = 0L;
        globalDeltaT = 0.0f;
        co2Ppm = 280.0;
    }

    public static int toCellX(int blockX) {
        int wx = PlanetCoordinates.wrapX(blockX);
        return Math.floorMod(Math.floorDiv(wx - PlanetCoordinates.X_MIN, CELL_BLOCKS), GRID_W);
    }

    public static int toCellZ(int blockZ) {
        int cz = PlanetCoordinates.clampZ(blockZ);
        return Math.clamp(Math.floorDiv(cz - PlanetCoordinates.Z_MIN, CELL_BLOCKS), 0, GRID_H - 1);
    }

    public static int cellIndex(int cellX, int cellZ) {
        return Math.clamp(cellZ, 0, GRID_H - 1) * GRID_W + Math.floorMod(cellX, GRID_W);
    }

    public static int activeSystemCount() {
        return SYSTEMS.size();
    }

    public static float globalDeltaT() {
        return globalDeltaT;
    }

    public static double co2Ppm() {
        return co2Ppm;
    }

    public static void addEmissionsKg(double co2eKg) {
        co2Ppm = Math.max(260.0, co2Ppm + co2eKg * 1.0e-9);
        globalDeltaT = (float) (3.0 * (Math.log(co2Ppm / 280.0) / Math.log(2.0)));
    }

    public static void seedInitialSystems(long worldSeed) {
        SYSTEMS.clear();
        Random rng = new Random(worldSeed ^ 0x57EA_74E2L);
        for (int i = 0; i < 36; i++) {
            WeatherSystemKind kind = switch (i % 5) {
                case 0, 1 -> WeatherSystemKind.EXTRATROPICAL_CYCLONE;
                case 2 -> WeatherSystemKind.SUBTROPICAL_HIGH;
                case 3 -> WeatherSystemKind.TROPICAL_CONVECTION;
                default -> WeatherSystemKind.MONSOON;
            };
            double cx = rng.nextDouble() * GRID_W;
            double cz = switch (kind) {
                case EXTRATROPICAL_CYCLONE -> (rng.nextBoolean() ? 14.0 : 58.0) + rng.nextDouble() * 10.0;
                case SUBTROPICAL_HIGH -> (rng.nextBoolean() ? 24.0 : 50.0) + rng.nextDouble() * 5.0;
                default -> 34.0 + rng.nextDouble() * 12.0;
            };
            float rad = kind == WeatherSystemKind.TROPICAL_CONVECTION ? 1.5f : 3.2f;
            SYSTEMS.add(new SynopticSystem(nextSystemId++, kind, cx, cz, rad, 0.55f + rng.nextFloat() * 0.45f, 48_000 + rng.nextInt(72_000)));
        }
        for (int cz = 0; cz < GRID_H; cz++) {
            for (int cx = 0; cx < GRID_W; cx++) {
                CELLS[cellIndex(cx, cz)] = evaluateCellState(cx, cz, 6_000L);
            }
        }
    }

    public static void tickServer(@Nullable MinecraftServer server, @Nullable ServerLevel overworld, long dayTime, long gameTick) {
        currentTick = gameTick;
        CivCalendar.CalendarDate cal = CivCalendar.fromDayTime(dayTime);
        if (lastGlobalSeasonIndex != cal.seasonIndex() || lastGlobalYear != cal.year()) {
            if (lastGlobalSeasonIndex != -1) {
                SeasonChangedEvent evt = new SeasonChangedEvent(server, cal.year(), cal.seasonIndex(), false);
                TcEventBus.global().enqueue(evt);
                NeoForge.EVENT_BUS.post(evt);
            }
            lastGlobalSeasonIndex = cal.seasonIndex();
            lastGlobalYear = cal.year();
        }

        if (SYSTEMS.isEmpty()) {
            PlanetModel m = PlanetRuntime.model();
            seedInitialSystems(m != null ? m.worldSeed() : 12345L);
        }

        // Move synoptic systems every 200 ticks
        if (gameTick % 200L == 0L) {
            for (SynopticSystem sys : SYSTEMS) {
                double latDeg = 90.0 - ((sys.cellZ + 0.5) / GRID_H) * 180.0;
                double stepX = Math.abs(latDeg) >= 30.0 && Math.abs(latDeg) <= 65.0 ? 1.1 : -0.75;
                sys.cellX = (sys.cellX + stepX + GRID_W) % GRID_W;
                sys.remainingTicks -= 200;
                if (sys.remainingTicks <= 0) {
                    sys.remainingTicks = 72_000;
                }
            }
        }

        // Update 64 cells per tick (12,800 / 200 = 64 -> <= 0.08 ms/tick budget)
        int slice = (int) Math.floorMod(gameTick, 200L);
        int startIdx = slice * 64;
        int endIdx = Math.min(CELL_COUNT, startIdx + 64);
        for (int idx = startIdx; idx < endIdx; idx++) {
            if (OVERRIDE_UNTIL_TICK[idx] > gameTick) {
                continue;
            }
            int cz = idx / GRID_W;
            int cx = idx - cz * GRID_W;
            WeatherState prev = CELLS[idx];
            WeatherState next = evaluateCellState(cx, cz, dayTime);
            CELLS[idx] = next;
            if (prev.type() != next.type() && overworld != null) {
                EnumSet<WeatherChangedEvent.WeatherFlag> flags = EnumSet.noneOf(WeatherChangedEvent.WeatherFlag.class);
                if (next.type() == PrecipType.SNOW) flags.add(WeatherChangedEvent.WeatherFlag.SNOW_START);
                if (next.type() == PrecipType.HAIL) flags.add(WeatherChangedEvent.WeatherFlag.HAIL);
                if (next.convective() >= 2) flags.add(WeatherChangedEvent.WeatherFlag.THUNDER);
                if (next.precipMmPerHour() >= 10.0f) flags.add(WeatherChangedEvent.WeatherFlag.HEAVY_RAIN);
                NeoForge.EVENT_BUS.post(new WeatherChangedEvent(overworld, cx, cz, prev, next, flags));
            }
        }
    }

    private static WeatherState evaluateCellState(int cx, int cz, long dayTime) {
        int bx = PlanetCoordinates.X_MIN + cx * CELL_BLOCKS + 512;
        int bz = PlanetCoordinates.Z_MIN + cz * CELL_BLOCKS + 512;
        double latDeg = PlanetCoordinates.latLon(bx, bz).latDeg();
        boolean westerly = Math.abs(latDeg) >= 30.0 && Math.abs(latDeg) <= 62.0;
        float windU = westerly ? 6.0f : -4.5f;
        float windV = (float) (latDeg >= 0 ? -1.2 : 1.2);

        float cloud = 0.22f;
        float precipRate = 0.0f;
        int convective = 0;
        float tempAnom = 0.0f;
        WeatherSystemKind bestKind = null;

        for (SynopticSystem sys : SYSTEMS) {
            double dx = Math.abs(cx - sys.cellX);
            dx = Math.min(dx, GRID_W - dx);
            double dz = cz - sys.cellZ;
            double dist = Math.hypot(dx, dz);
            if (dist <= sys.radiusCells) {
                float w = (float) (1.0 - dist / sys.radiusCells) * sys.intensity;
                if (sys.kind == WeatherSystemKind.SUBTROPICAL_HIGH) {
                    cloud = Math.max(0.05f, cloud - 0.25f * w);
                    tempAnom += 2.5f * w;
                } else {
                    cloud = Math.min(1.0f, cloud + 0.75f * w);
                    precipRate += 6.5f * w;
                    if (sys.kind == WeatherSystemKind.TROPICAL_CONVECTION || sys.kind == WeatherSystemKind.TROPICAL_CYCLONE) {
                        convective = Math.max(convective, w > 0.65f ? 2 : 1);
                    } else if (sys.kind == WeatherSystemKind.EXTRATROPICAL_CYCLONE) {
                        tempAnom -= 3.5f * w;
                        if (w > 0.75f) convective = Math.max(convective, 2);
                    }
                    windU += 4.5f * w;
                    bestKind = sys.kind;
                }
            }
        }

        float localTemp = sampleTemperatureInstant(bx, 72, bz, dayTime, tempAnom);
        PrecipType pType = PrecipType.NONE;
        if (precipRate >= 0.4f) {
            if (localTemp < -0.5f) pType = PrecipType.SNOW;
            else if (localTemp < 1.2f) pType = PrecipType.SLEET;
            else if (convective >= 3) pType = PrecipType.HAIL;
            else pType = PrecipType.RAIN;
        } else {
            precipRate = 0.0f;
        }

        float windSpeed = (float) Math.hypot(windU, windV);
        float windDir = (float) ((Math.toDegrees(Math.atan2(-windU, windV)) + 360.0) % 360.0);
        return new WeatherState(pType, precipRate, cloud, windSpeed, windDir, convective, tempAnom, Optional.ofNullable(bestKind));
    }

    public static WeatherState stateAt(BlockPos pos) {
        return CELLS[cellIndex(toCellX(pos.getX()), toCellZ(pos.getZ()))];
    }

    public static Vec2 windAt(BlockPos pos) {
        WeatherState st = stateAt(pos);
        double rad = Math.toRadians(st.windDirDeg());
        return new Vec2((float) (-Math.sin(rad) * st.windSpeed()), (float) (Math.cos(rad) * st.windSpeed()));
    }

    public static void setWeatherOverride(BlockPos center, PrecipType type, int convective, long durationTicks, boolean global) {
        WeatherState forced = switch (type) {
            case NONE -> new WeatherState(PrecipType.NONE, 0.0f, 0.1f, 2.0f, 270.0f, 0, 0.0f, Optional.empty());
            case SNOW -> new WeatherState(PrecipType.SNOW, 4.5f, 0.95f, 6.0f, 270.0f, 0, -4.0f, Optional.of(WeatherSystemKind.COLD_SURGE));
            default -> new WeatherState(type, convective >= 2 ? 12.0f : 5.0f, 0.98f, convective >= 2 ? 11.0f : 5.5f, 270.0f, convective, -2.0f, Optional.of(WeatherSystemKind.EXTRATROPICAL_CYCLONE));
        };
        long until = currentTick + Math.max(20L, durationTicks);
        if (global) {
            for (int i = 0; i < CELL_COUNT; i++) {
                CELLS[i] = forced;
                OVERRIDE_UNTIL_TICK[i] = until;
            }
        } else {
            int cx = toCellX(center.getX());
            int cz = toCellZ(center.getZ());
            for (int dz = -1; dz <= 1; dz++) {
                int nz = Math.clamp(cz + dz, 0, GRID_H - 1);
                for (int dx = -1; dx <= 1; dx++) {
                    int idx = cellIndex(cx + dx, nz);
                    CELLS[idx] = forced;
                    OVERRIDE_UNTIL_TICK[idx] = until;
                }
            }
        }
    }

    public static LocalSeason resolveLocalSeason(BlockPos pos, long dayIndex) {
        LatLon ll = PlanetCoordinates.latLon(pos.getX(), pos.getZ());
        boolean south = ll.latDeg() < 0.0;
        int doy = (int) Math.floorMod(dayIndex, CivCalendar.YEAR_DAYS);
        int shiftedDoy = south ? Math.floorMod(doy + 24, CivCalendar.YEAR_DAYS) : doy;
        int seasonIdx = shiftedDoy / CivCalendar.SEASON_DAYS;
        float progress = (shiftedDoy % CivCalendar.SEASON_DAYS) / (float) CivCalendar.SEASON_DAYS;
        Season astro = Season.fromAstronomicalIndex(seasonIdx);

        double absLat = Math.abs(ll.latDeg());
        PlanetModel m = PlanetRuntime.model();
        String koppen = m != null ? m.climateNormalsAt(pos.getX(), pos.getZ()).koppen() : "Cfb";

        if (absLat >= 66.5) {
            return new LocalSeason(astro, SeasonRegime.POLAR, astro.translationKey(), progress, south);
        }
        if (koppen.startsWith("BW") || koppen.startsWith("BS")) {
            return new LocalSeason(astro, SeasonRegime.ARID, astro.translationKey(), progress, south);
        }
        if (absLat < 23.44) {
            boolean wet = (astro == Season.SPRING || astro == Season.SUMMER);
            Season trop = wet ? Season.WET : Season.DRY;
            return new LocalSeason(astro, SeasonRegime.MONSOON, trop.translationKey(), progress, south);
        }
        return new LocalSeason(astro, SeasonRegime.TEMPERATE, astro.translationKey(), progress, south);
    }

    public static float sampleTemperatureInstant(int x, int y, int z, long dayTime, float weatherAnomalyC) {
        PlanetModel model = PlanetRuntime.model();
        int doy = (int) Math.floorMod(Math.floorDiv(dayTime, CivCalendar.DAY_TICKS), CivCalendar.YEAR_DAYS);
        long tickOfDay = Math.floorMod(dayTime, CivCalendar.DAY_TICKS);
        double diurnal = 4.5 * Math.cos(2.0 * Math.PI * (tickOfDay - 8_000.0) / 24_000.0);

        if (model != null) {
            ClimateNormals normals = model.climateNormalsAt(x, z);
            int m0 = Math.floorMod(doy / 4, 12);
            int m1 = (m0 + 1) % 12;
            float frac = (doy % 4) / 4.0f;
            float baseT = normals.tempMonthlyC()[m0] * (1.0f - frac) + normals.tempMonthlyC()[m1] * frac;
            double baseSurfM = model.sampleBicubicAltitudeMeters(x, z);
            double curAltM = PlanetCoordinates.altitudeMeters(y);
            float lapseAdjust = (float) (-6.5 * Math.max(0.0, curAltM - Math.max(0.0, baseSurfM)) / 1_000.0);
            return (float) (baseT + lapseAdjust + diurnal + weatherAnomalyC + globalDeltaT);
        }
        // Fallback temperate curve
        LatLon ll = PlanetCoordinates.latLon(x, z);
        double phase = ll.latDeg() < 0 ? 42.0 : 18.0;
        double seasonal = 11.0 * Math.cos(2.0 * Math.PI * (doy - phase) / 48.0);
        return (float) (12.0 + seasonal + diurnal + weatherAnomalyC);
    }

    /**
     * Computes vanilla crop growth multiplier in the absence of Cultivation (10 §16.4, AC-WLD-19):
     * Returns 0 when local temperature < 5 C; multiplies by 1.25 on flood_silt.
     */
    public static float computeCropGrowthFactor(float localTempC, float dayLengthHours, float spi, boolean onFloodSilt, boolean greenhouse) {
        float effectiveT = greenhouse ? localTempC + 8.0f : localTempC;
        float fTemp;
        if (effectiveT < 5.0f) {
            fTemp = 0.0f;
        } else if (effectiveT < 10.0f) {
            fTemp = (effectiveT - 5.0f) / 5.0f;
        } else if (effectiveT <= 30.0f) {
            fTemp = 1.0f;
        } else if (effectiveT <= 40.0f) {
            fTemp = Math.max(0.3f, 1.0f - 0.07f * (effectiveT - 30.0f));
        } else {
            fTemp = 0.3f;
        }
        if (fTemp <= 0.0f) {
            return 0.0f;
        }
        float fLight = Math.clamp(dayLengthHours / 12.0f, 0.5f, 1.2f);
        float fDrought = Math.max(0.4f, 1.0f + 0.15f * Math.min(spi, 0.0f));
        float fSilt = onFloodSilt ? 1.25f : 1.0f;
        return Math.clamp(fTemp * fLight * fDrought * fSilt, 0.0f, 1.5f);
    }

    public static WeatherForecast buildForecast(BlockPos pos, int days, ForecastSkill skill) {
        int clampedDays = Math.clamp(days, 1, 5);
        List<WeatherForecast.DayEntry> entries = new ArrayList<>(clampedDays);
        WeatherState now = stateAt(pos);
        Random jitter = new Random(PeriodicNoise.hash2D(0xF02ECA57L, pos.getX() >> 8, pos.getZ() >> 8));
        for (int d = 1; d <= clampedDays; d++) {
            float baseProb = now.type() != PrecipType.NONE ? 0.65f : 0.25f;
            float err = (jitter.nextFloat() * 2.0f - 1.0f) * skill.relativeError();
            float prob = Math.clamp(baseProb + err, 0.0f, 1.0f);
            float meanT = sampleTemperatureInstant(pos.getX(), pos.getY(), pos.getZ(), currentTick + d * 24_000L, 0.0f);
            entries.add(new WeatherForecast.DayEntry(
                    d,
                    prob,
                    prob * 14.0f,
                    meanT - 5.0f,
                    meanT + 5.0f,
                    meanT < 0.0f ? PrecipType.SNOW : PrecipType.RAIN
            ));
        }
        return new WeatherForecast(pos, clampedDays, skill, entries, skill.relativeError());
    }
}
