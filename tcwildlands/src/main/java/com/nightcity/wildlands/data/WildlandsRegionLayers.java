package com.nightcity.wildlands.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.tcivilization.api.CivCalendar;
import com.nightcity.tcivilization.api.RegionCell;
import com.nightcity.tcivilization.api.RegionHandle;
import com.nightcity.tcivilization.api.RegionLayer;
import com.nightcity.tcivilization.api.RegionPressureContributor;
import com.nightcity.tcivilization.api.TcMigrators;
import com.nightcity.tcivilization.api.event.RegisterRegionLayersEvent;
import com.nightcity.tcivilization.api.world.HazardRecord;
import com.nightcity.tcivilization.api.world.HistoricalTrace;
import com.nightcity.tcivilization.api.world.WaterWorkRef;
import com.nightcity.wildlands.planet.PlanetRuntime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

/**
 * Core-registered tcwildlands:geo, tcwildlands:climate, and tcwildlands:hydro region layers,
 * migrators, and tcwildlands:hazard region pressure contributor (10 §3.1, §11.4).
 */
public final class WildlandsRegionLayers {
    public static final Identifier GEO_ID = Identifier.fromNamespaceAndPath("tcwildlands", "geo");
    public static final Identifier CLIMATE_ID = Identifier.fromNamespaceAndPath("tcwildlands", "climate");
    public static final Identifier HYDRO_ID = Identifier.fromNamespaceAndPath("tcwildlands", "hydro");
    public static final Identifier HAZARD_PRESSURE_ID = Identifier.fromNamespaceAndPath("tcwildlands", "hazard");

    public enum LayerSync {
        NONE, NEAR, ON_DEMAND, GLOBAL_SUMMARY
    }

    public record EmptyLayerData(int formatVersion) {
        public static final Codec<EmptyLayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("format_version", 1).forGetter(EmptyLayerData::formatVersion)
        ).apply(instance, EmptyLayerData::new));

        public EmptyLayerData() {
            this(1);
        }
    }

    public record RegionLayerDef<T>(Identifier id, Supplier<T> defaultFactory, Codec<T> codec, LayerSync sync) {
        public T createDefault() {
            return defaultFactory.get();
        }
    }

    public static final Codec<HistoricalTrace> HISTORICAL_TRACE_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.xmap(s -> HistoricalTrace.Kind.valueOf(s.toUpperCase(Locale.ROOT)), k -> k.name().toLowerCase(Locale.ROOT))
                    .fieldOf("kind").forGetter(HistoricalTrace::kind),
            BlockPos.CODEC.fieldOf("pos").forGetter(HistoricalTrace::pos),
            Codec.INT.fieldOf("radius").forGetter(HistoricalTrace::radius),
            Codec.INT.fieldOf("year").forGetter(HistoricalTrace::year),
            Codec.LONG.optionalFieldOf("deposit_id").xmap(
                    opt -> opt.map(OptionalLong::of).orElseGet(OptionalLong::empty),
                    ol -> ol.isPresent() ? Optional.of(ol.getAsLong()) : Optional.empty()
            ).forGetter(HistoricalTrace::depositId),
            Codec.FLOAT.optionalFieldOf("mined_fraction", 0.0f).forGetter(HistoricalTrace::minedFraction)
    ).apply(i, HistoricalTrace::new));

    public record DepositState(
            boolean discovered,
            Optional<UUID> discoveredByCiv,
            long minedBlocks,
            long minedOreUnits,
            float ancientMinedFraction
    ) {
        public static final Codec<DepositState> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.optionalFieldOf("discovered", false).forGetter(DepositState::discovered),
                UUIDUtil.CODEC.optionalFieldOf("civ").forGetter(DepositState::discoveredByCiv),
                Codec.LONG.optionalFieldOf("mined_blocks", 0L).forGetter(DepositState::minedBlocks),
                Codec.LONG.optionalFieldOf("mined_units", 0L).forGetter(DepositState::minedOreUnits),
                Codec.FLOAT.optionalFieldOf("ancient_mined", 0.0f).forGetter(DepositState::ancientMinedFraction)
        ).apply(i, DepositState::new));

        public static DepositState initial() {
            return new DepositState(false, Optional.empty(), 0L, 0L, 0.0f);
        }
    }

    public record GeoLayerData(
            int formatVersion,
            Map<Long, DepositState> deposits,
            Map<Long, Long> reservoirRemainingMb,
            float subsidence,
            float seismicStrain,
            List<HazardRecord> hazardLog,
            List<HistoricalTrace> traces,
            Optional<Identifier> lastSurveyMethod
    ) {
        public static final Codec<GeoLayerData> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("v", 1).forGetter(GeoLayerData::formatVersion),
                Codec.unboundedMap(Codec.STRING.xmap(Long::parseLong, String::valueOf), DepositState.CODEC)
                        .optionalFieldOf("deposits", Map.of()).forGetter(GeoLayerData::deposits),
                Codec.unboundedMap(Codec.STRING.xmap(Long::parseLong, String::valueOf), Codec.LONG)
                        .optionalFieldOf("reservoirs", Map.of()).forGetter(GeoLayerData::reservoirRemainingMb),
                Codec.FLOAT.optionalFieldOf("subsidence", 0.0f).forGetter(GeoLayerData::subsidence),
                Codec.FLOAT.optionalFieldOf("strain", 0.0f).forGetter(GeoLayerData::seismicStrain),
                HazardRecord.CODEC.listOf().optionalFieldOf("hazards", List.of()).forGetter(GeoLayerData::hazardLog),
                HISTORICAL_TRACE_CODEC.listOf().optionalFieldOf("traces", List.of()).forGetter(GeoLayerData::traces),
                Identifier.CODEC.optionalFieldOf("survey").forGetter(GeoLayerData::lastSurveyMethod)
        ).apply(i, GeoLayerData::new));

        public static GeoLayerData createDefault() {
            return new GeoLayerData(1, Map.of(), Map.of(), 0.0f, 0.0f, List.of(), List.of(), Optional.empty());
        }
    }

    public record ClimateLayerData(
            int formatVersion,
            float deltaTLocal,
            float deltaPLocal,
            float vegetationCover,
            float albedoOffset,
            List<Integer> recentPrecipMm,
            float spi
    ) {
        public static final Codec<ClimateLayerData> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("v", 1).forGetter(ClimateLayerData::formatVersion),
                Codec.FLOAT.optionalFieldOf("dt", 0.0f).forGetter(ClimateLayerData::deltaTLocal),
                Codec.FLOAT.optionalFieldOf("dp", 0.0f).forGetter(ClimateLayerData::deltaPLocal),
                Codec.FLOAT.optionalFieldOf("veg", 0.7f).forGetter(ClimateLayerData::vegetationCover),
                Codec.FLOAT.optionalFieldOf("albedo", 0.0f).forGetter(ClimateLayerData::albedoOffset),
                Codec.INT.listOf().optionalFieldOf("recent_p", List.of()).forGetter(ClimateLayerData::recentPrecipMm),
                Codec.FLOAT.optionalFieldOf("spi", 0.0f).forGetter(ClimateLayerData::spi)
        ).apply(i, ClimateLayerData::new));

        public static ClimateLayerData createDefault() {
            Integer[] zeros = new Integer[12];
            Arrays.fill(zeros, 0);
            return new ClimateLayerData(1, 0.0f, 0.0f, 0.7f, 0.0f, List.of(zeros), 0.0f);
        }
    }

    public record RiverReachState(
            float flowM3s,
            float stageM,
            int panningBudget,
            float dikeHeightM,
            boolean dammed
    ) {
        public static final Codec<RiverReachState> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.optionalFieldOf("flow", 0.0f).forGetter(RiverReachState::flowM3s),
                Codec.FLOAT.optionalFieldOf("stage", 0.0f).forGetter(RiverReachState::stageM),
                Codec.INT.optionalFieldOf("panning", 64).forGetter(RiverReachState::panningBudget),
                Codec.FLOAT.optionalFieldOf("dike", 0.0f).forGetter(RiverReachState::dikeHeightM),
                Codec.BOOL.optionalFieldOf("dammed", false).forGetter(RiverReachState::dammed)
        ).apply(i, RiverReachState::new));
    }

    public static final Codec<WaterWorkRef> WATER_WORK_CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("id").forGetter(WaterWorkRef::id),
            Codec.STRING.xmap(s -> WaterWorkRef.Kind.valueOf(s.toUpperCase(Locale.ROOT)), k -> k.name().toLowerCase(Locale.ROOT))
                    .fieldOf("kind").forGetter(WaterWorkRef::kind),
            BlockPos.CODEC.fieldOf("start").forGetter(WaterWorkRef::start),
            BlockPos.CODEC.fieldOf("end").forGetter(WaterWorkRef::end),
            Codec.FLOAT.optionalFieldOf("crest", 2.0f).forGetter(WaterWorkRef::crestHeightBlocks),
            Codec.FLOAT.optionalFieldOf("cap", 10.0f).forGetter(WaterWorkRef::capacityM3s)
    ).apply(i, WaterWorkRef::new));

    public record HydroLayerData(
            int formatVersion,
            Map<Integer, RiverReachState> reaches,
            float groundwaterDrawdownM,
            float floodRiskIndex,
            List<WaterWorkRef> waterWorks
    ) {
        public static final Codec<HydroLayerData> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("v", 1).forGetter(HydroLayerData::formatVersion),
                Codec.unboundedMap(Codec.STRING.xmap(Integer::parseInt, String::valueOf), RiverReachState.CODEC)
                        .optionalFieldOf("reaches", Map.of()).forGetter(HydroLayerData::reaches),
                Codec.FLOAT.optionalFieldOf("drawdown", 0.0f).forGetter(HydroLayerData::groundwaterDrawdownM),
                Codec.FLOAT.optionalFieldOf("flood_risk", 0.0f).forGetter(HydroLayerData::floodRiskIndex),
                WATER_WORK_CODEC.listOf().optionalFieldOf("works", List.of()).forGetter(HydroLayerData::waterWorks)
        ).apply(i, HydroLayerData::new));

        public static HydroLayerData createDefault() {
            return new HydroLayerData(1, Map.of(), 0.0f, 0.0f, List.of());
        }
    }

    public static final RegionLayer<GeoLayerData> CORE_GEO_LAYER = new RegionLayer<>() {
        @Override
        public Identifier id() {
            return GEO_ID;
        }

        @Override
        public GeoLayerData createDefault(RegionCell cell) {
            return GeoLayerData.createDefault();
        }

        @Override
        public Codec<GeoLayerData> codec() {
            return GeoLayerData.CODEC;
        }

        @Override
        public com.nightcity.tcivilization.api.LayerSync sync() {
            return com.nightcity.tcivilization.api.LayerSync.NONE;
        }
    };

    public static final RegionLayer<ClimateLayerData> CORE_CLIMATE_LAYER = new RegionLayer<>() {
        @Override
        public Identifier id() {
            return CLIMATE_ID;
        }

        @Override
        public ClimateLayerData createDefault(RegionCell cell) {
            return ClimateLayerData.createDefault();
        }

        @Override
        public Codec<ClimateLayerData> codec() {
            return ClimateLayerData.CODEC;
        }

        @Override
        public com.nightcity.tcivilization.api.LayerSync sync() {
            return com.nightcity.tcivilization.api.LayerSync.NEAR;
        }
    };

    public static final RegionLayer<HydroLayerData> CORE_HYDRO_LAYER = new RegionLayer<>() {
        @Override
        public Identifier id() {
            return HYDRO_ID;
        }

        @Override
        public HydroLayerData createDefault(RegionCell cell) {
            return HydroLayerData.createDefault();
        }

        @Override
        public Codec<HydroLayerData> codec() {
            return HydroLayerData.CODEC;
        }

        @Override
        public com.nightcity.tcivilization.api.LayerSync sync() {
            return com.nightcity.tcivilization.api.LayerSync.ON_DEMAND;
        }
    };

    /**
     * Evaluates tcwildlands:hazard pressure component (weight 0.10, 10 §3.1):
     * hazard = min(1, 0.6 * D + 0.4 * clamp(-SPI / 3, 0, 1))
     * Returns 0 when planet mode is inactive.
     */
    public static RegionPressureContributor createHazardPressureContributor(LongSupplier currentTickSupplier) {
        return new RegionPressureContributor() {
            @Override
            public Identifier id() {
                return HAZARD_PRESSURE_ID;
            }

            @Override
            public float weight() {
                return 0.10f;
            }

            @Override
            public float evaluate(RegionHandle region) {
                if (!PlanetRuntime.isPlanetActive()) {
                    return 0.0f;
                }
                long nowTick = currentTickSupplier.getAsLong();
                long lookbackTicks = 48L * CivCalendar.DAY_TICKS;
                GeoLayerData geo = region.layer(CORE_GEO_LAYER);
                ClimateLayerData clim = region.layer(CORE_CLIMATE_LAYER);
                float maxDamage = 0.0f;
                for (HazardRecord rec : geo.hazardLog()) {
                    if (nowTick - rec.endTick() <= lookbackTicks) {
                        if (rec.buildingDamageIndex() > maxDamage) {
                            maxDamage = rec.buildingDamageIndex();
                        }
                    }
                }
                float droughtTerm = Math.clamp(-clim.spi() / 3.0f, 0.0f, 1.0f);
                return Math.min(1.0f, 0.6f * Math.clamp(maxDamage, 0.0f, 1.0f) + 0.4f * droughtTerm);
            }
        };
    }

    public static void ensureCoreLayersRegistered() {
        com.nightcity.tcivilization.region.RegionLayerRegistry reg = com.nightcity.tcivilization.region.RegionLayerRegistry.global();
        try {
            if (reg.get(GEO_ID).isEmpty()) {
                reg.register(CORE_GEO_LAYER);
            }
            if (reg.get(CLIMATE_ID).isEmpty()) {
                reg.register(CORE_CLIMATE_LAYER);
            }
            if (reg.get(HYDRO_ID).isEmpty()) {
                reg.register(CORE_HYDRO_LAYER);
            }
        } catch (IllegalStateException ignored) {
        }
    }

    public static void registerCoreLayers(RegisterRegionLayersEvent event) {
        if (!event.registeredLayers().containsKey(GEO_ID)) {
            event.register(CORE_GEO_LAYER);
        }
        if (!event.registeredLayers().containsKey(CLIMATE_ID)) {
            event.register(CORE_CLIMATE_LAYER);
        }
        if (!event.registeredLayers().containsKey(HYDRO_ID)) {
            event.register(CORE_HYDRO_LAYER);
        }
    }

    public static void registerMigrators() {
        TcMigrators.register(GEO_ID, 0, (ver, tag) -> {
            tag.putInt("v", 1);
            return tag;
        });
        TcMigrators.register(CLIMATE_ID, 0, (ver, tag) -> {
            tag.putInt("v", 1);
            return tag;
        });
        TcMigrators.register(HYDRO_ID, 0, (ver, tag) -> {
            tag.putInt("v", 1);
            return tag;
        });
    }

    // Legacy P0 descriptors kept for compatibility
    public static final RegionLayerDef<EmptyLayerData> GEO_LAYER = new RegionLayerDef<>(
            GEO_ID, EmptyLayerData::new, EmptyLayerData.CODEC, LayerSync.ON_DEMAND
    );
    public static final RegionLayerDef<EmptyLayerData> CLIMATE_LAYER = new RegionLayerDef<>(
            CLIMATE_ID, EmptyLayerData::new, EmptyLayerData.CODEC, LayerSync.NEAR
    );
    public static final RegionLayerDef<EmptyLayerData> HYDRO_LAYER = new RegionLayerDef<>(
            HYDRO_ID, EmptyLayerData::new, EmptyLayerData.CODEC, LayerSync.ON_DEMAND
    );
    public static final List<RegionLayerDef<?>> ALL = List.of(GEO_LAYER, CLIMATE_LAYER, HYDRO_LAYER);

    private WildlandsRegionLayers() {
    }
}
