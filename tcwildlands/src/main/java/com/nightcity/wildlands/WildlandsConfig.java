package com.nightcity.wildlands;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class WildlandsConfig {
    public static final ModConfigSpec COMMON_SPEC;
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;

    // Common (10 §13.1)
    public static final ModConfigSpec.BooleanValue REPLACE_OVERWORLD;
    public static final ModConfigSpec.BooleanValue REPLACE_NETHER;
    public static final ModConfigSpec.IntValue PRECOMPUTE_THREADS;
    public static final ModConfigSpec.IntValue TILES_MEMORY_CACHE;
    public static final ModConfigSpec.BooleanValue TILES_DISK_CACHE;
    public static final ModConfigSpec.IntValue TILES_DISK_CACHE_MAX_MB;
    public static final ModConfigSpec.BooleanValue DEGRADED_GEOLOGY_PATCHES;
    public static final ModConfigSpec.BooleanValue VANILLA_COBBLE_DROPS;
    public static final ModConfigSpec.BooleanValue VANILLA_FALLBACK_RECIPES;

    // Server (10 §13.2)
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue ENABLE_COMPAT_HOOKS;
    public static final ModConfigSpec.DoubleValue PLANET_LAND_TARGET;
    public static final ModConfigSpec.IntValue PLANET_PLATE_COUNT;
    public static final ModConfigSpec.IntValue PLANET_EROSION_STEPS;
    public static final ModConfigSpec.IntValue PLANET_MAX_STEP;
    public static final ModConfigSpec.BooleanValue PLANET_PREVIEW;

    public static final ModConfigSpec.BooleanValue WEATHER_ENABLED;
    public static final ModConfigSpec.IntValue WEATHER_UPDATE_INTERVAL;
    public static final ModConfigSpec.IntValue WEATHER_MAX_SYSTEMS;
    public static final ModConfigSpec.IntValue WEATHER_SYNC_RADIUS;
    public static final ModConfigSpec.BooleanValue WEATHER_LOCAL_COMMAND;
    public static final ModConfigSpec.DoubleValue WEATHER_LIGHTNING_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue SEASONS_ENABLED;
    public static final ModConfigSpec.BooleanValue SEASONS_SNOW_AND_ICE;
    public static final ModConfigSpec.IntValue SEASONS_MAX_SNOW_LAYERS;
    public static final ModConfigSpec.BooleanValue SEASONS_WATER_LEVEL;
    public static final ModConfigSpec.BooleanValue HYDRO_FLOW_PUSH;
    public static final ModConfigSpec.BooleanValue SEASONS_CROP_FACTOR;
    public static final ModConfigSpec.BooleanValue CLIMATE_CHANGE_ENABLED;
    public static final ModConfigSpec.DoubleValue CLIMATE_CHANGE_MULTIPLIER;

    public static final ModConfigSpec.DoubleValue HAZARDS_FREQUENCY;
    public static final ModConfigSpec.BooleanValue HAZARDS_FLOOD_ENABLED;
    public static final ModConfigSpec.IntValue HAZARDS_FLOOD_MAX_FLOODWATER;
    public static final ModConfigSpec.BooleanValue HAZARDS_EARTHQUAKE_ENABLED;
    public static final ModConfigSpec.DoubleValue HAZARDS_EARTHQUAKE_MAX_MAGNITUDE;
    public static final ModConfigSpec.IntValue HAZARDS_EARTHQUAKE_PLAYER_BLOCK_CAP;
    public static final ModConfigSpec.BooleanValue HAZARDS_VOLCANO_ENABLED;
    public static final ModConfigSpec.IntValue HAZARDS_VOLCANO_MAX_VEI;
    public static final ModConfigSpec.BooleanValue HAZARDS_DROUGHT_ENABLED;
    public static final ModConfigSpec.BooleanValue HAZARDS_STORM_ENABLED;
    public static final ModConfigSpec.BooleanValue HAZARDS_WILDFIRE_ENABLED;
    public static final ModConfigSpec.IntValue HAZARDS_WILDFIRE_MAX_BURNING_BLOCKS;
    public static final ModConfigSpec.BooleanValue HAZARDS_MASS_WASTING_ENABLED;
    public static final ModConfigSpec.IntValue HAZARDS_BLOCK_BUDGET_PER_TICK;

    public static final ModConfigSpec.BooleanValue ROCK_MASS_ENABLED;
    public static final ModConfigSpec.IntValue DEEP_THRESHOLD_SNEAK_TICKS;
    public static final ModConfigSpec.BooleanValue DEEP_IGNITION_HINT;
    public static final ModConfigSpec.IntValue WRAP_PRELOAD_DISTANCE;
    public static final ModConfigSpec.BooleanValue START_SITE_ENABLED;
    public static final ModConfigSpec.IntValue START_SITE_AUTO_PICK_SECONDS;

    // Client (10 §13.3)
    public static final ModConfigSpec.BooleanValue CLIENT_SEASONAL_COLORS;
    public static final ModConfigSpec.BooleanValue CLIENT_BARE_BRANCHES;
    public static final ModConfigSpec.BooleanValue CLIENT_LOCAL_SKY;
    public static final ModConfigSpec.IntValue CLIENT_WEATHER_BLEND;
    public static final ModConfigSpec.BooleanValue CLIENT_RAIN_PILLARS;
    public static final ModConfigSpec.BooleanValue CLIENT_SUN_REMAP;
    public static final ModConfigSpec.DoubleValue CLIENT_SCREEN_SHAKE;
    public static final ModConfigSpec.BooleanValue CLIENT_HEAT_HAZE;

    static {
        ModConfigSpec.Builder common = new ModConfigSpec.Builder();
        REPLACE_OVERWORLD = common.define("replace_overworld", true);
        REPLACE_NETHER = common.define("replace_nether", true);
        common.push("precompute");
        PRECOMPUTE_THREADS = common.defineInRange("threads", 0, 0, 32);
        common.pop();
        common.push("tiles");
        TILES_MEMORY_CACHE = common.defineInRange("memory_cache", 96, 16, 512);
        TILES_DISK_CACHE = common.define("disk_cache", true);
        TILES_DISK_CACHE_MAX_MB = common.defineInRange("disk_cache_max_mb", 2048, 64, 65536);
        common.pop();
        common.push("degraded");
        DEGRADED_GEOLOGY_PATCHES = common.define("geology_patches", true);
        common.pop();
        common.push("vanilla");
        VANILLA_COBBLE_DROPS = common.define("cobble_drops", true);
        VANILLA_FALLBACK_RECIPES = common.define("fallback_recipes", true);
        common.pop();
        COMMON_SPEC = common.build();

        ModConfigSpec.Builder server = new ModConfigSpec.Builder();
        server.push("general");
        ENABLED = server.define("enabled", true);
        ENABLE_COMPAT_HOOKS = server.define("enable_compat_hooks", true);
        server.pop();

        server.push("planet");
        PLANET_LAND_TARGET = server.defineInRange("land_target", 0.29D, 0.15D, 0.60D);
        PLANET_PLATE_COUNT = server.defineInRange("plate_count", 0, 0, 24);
        PLANET_EROSION_STEPS = server.defineInRange("erosion_steps", 80, 40, 120);
        PLANET_MAX_STEP = server.defineInRange("max_step", 3, 2, 6);
        PLANET_PREVIEW = server.define("preview", true);
        server.pop();

        server.push("weather");
        WEATHER_ENABLED = server.define("enabled", true);
        WEATHER_UPDATE_INTERVAL = server.defineInRange("update_interval", 200, 100, 1200);
        WEATHER_MAX_SYSTEMS = server.defineInRange("max_systems", 80, 20, 160);
        WEATHER_SYNC_RADIUS = server.defineInRange("sync_radius", 2, 1, 3);
        WEATHER_LOCAL_COMMAND = server.define("local_weather_command", true);
        WEATHER_LIGHTNING_MULTIPLIER = server.defineInRange("lightning_multiplier", 1.0D, 0.0D, 4.0D);
        server.pop();

        server.push("seasons");
        SEASONS_ENABLED = server.define("enabled", true);
        SEASONS_SNOW_AND_ICE = server.define("snow_and_ice", true);
        SEASONS_MAX_SNOW_LAYERS = server.defineInRange("max_snow_layers", 8, 1, 8);
        SEASONS_WATER_LEVEL = server.define("water_level", true);
        SEASONS_CROP_FACTOR = server.define("crop_factor", true);
        server.pop();

        server.push("hydro");
        HYDRO_FLOW_PUSH = server.define("flow_push", true);
        server.pop();

        server.push("climate_change");
        CLIMATE_CHANGE_ENABLED = server.define("enabled", true);
        CLIMATE_CHANGE_MULTIPLIER = server.defineInRange("multiplier", 1.0D, 0.0D, 3.0D);
        server.pop();

        server.push("hazards");
        HAZARDS_FREQUENCY = server.defineInRange("frequency", 1.0D, 0.0D, 4.0D);
        HAZARDS_BLOCK_BUDGET_PER_TICK = server.defineInRange("block_budget_per_tick", 512, 64, 4096);
        server.push("flood");
        HAZARDS_FLOOD_ENABLED = server.define("enabled", true);
        HAZARDS_FLOOD_MAX_FLOODWATER = server.defineInRange("max_floodwater", 60000, 0, 200000);
        server.pop();
        server.push("earthquake");
        HAZARDS_EARTHQUAKE_ENABLED = server.define("enabled", true);
        HAZARDS_EARTHQUAKE_MAX_MAGNITUDE = server.defineInRange("max_magnitude", 9.0D, 3.0D, 9.0D);
        HAZARDS_EARTHQUAKE_PLAYER_BLOCK_CAP = server.defineInRange("player_block_cap", 64, 0, 256);
        server.pop();
        server.push("volcano");
        HAZARDS_VOLCANO_ENABLED = server.define("enabled", true);
        HAZARDS_VOLCANO_MAX_VEI = server.defineInRange("max_vei", 7, 0, 7);
        server.pop();
        server.push("drought");
        HAZARDS_DROUGHT_ENABLED = server.define("enabled", true);
        server.pop();
        server.push("storm");
        HAZARDS_STORM_ENABLED = server.define("enabled", true);
        server.pop();
        server.push("wildfire");
        HAZARDS_WILDFIRE_ENABLED = server.define("enabled", true);
        HAZARDS_WILDFIRE_MAX_BURNING_BLOCKS = server.defineInRange("max_burning_blocks", 4000, 0, 20000);
        server.pop();
        server.push("mass_wasting");
        HAZARDS_MASS_WASTING_ENABLED = server.define("enabled", true);
        server.pop();
        server.pop();

        server.push("rock_mass");
        ROCK_MASS_ENABLED = server.define("enabled", true);
        server.pop();

        server.push("deep");
        DEEP_THRESHOLD_SNEAK_TICKS = server.defineInRange("threshold_sneak_ticks", 60, 20, 200);
        DEEP_IGNITION_HINT = server.define("ignition_hint", true);
        server.pop();

        server.push("wrap");
        WRAP_PRELOAD_DISTANCE = server.defineInRange("preload_distance", 192, 64, 256);
        server.pop();

        server.push("start_site");
        START_SITE_ENABLED = server.define("enabled", true);
        START_SITE_AUTO_PICK_SECONDS = server.defineInRange("auto_pick_seconds", 600, 0, 3600);
        server.pop();
        SERVER_SPEC = server.build();

        ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        client.push("client");
        CLIENT_SEASONAL_COLORS = client.define("seasonal_colors", true);
        CLIENT_BARE_BRANCHES = client.define("bare_branches", true);
        CLIENT_LOCAL_SKY = client.define("local_sky", true);
        CLIENT_WEATHER_BLEND = client.defineInRange("weather_blend", 128, 0, 256);
        CLIENT_RAIN_PILLARS = client.define("rain_pillars", true);
        CLIENT_SUN_REMAP = client.define("sun_remap", true);
        CLIENT_SCREEN_SHAKE = client.defineInRange("screen_shake", 1.0D, 0.0D, 1.0D);
        CLIENT_HEAT_HAZE = client.define("heat_haze", true);
        client.pop();
        CLIENT_SPEC = client.build();
    }

    public static boolean isReplaceOverworld() {
        return !COMMON_SPEC.isLoaded() || REPLACE_OVERWORLD.getAsBoolean();
    }

    public static boolean isReplaceNether() {
        return !COMMON_SPEC.isLoaded() || REPLACE_NETHER.getAsBoolean();
    }

    public static int effectivePrecomputeThreads() {
        int cfg = COMMON_SPEC.isLoaded() ? PRECOMPUTE_THREADS.getAsInt() : 0;
        if (cfg > 0) {
            return cfg;
        }
        return Math.max(1, Math.min(Runtime.getRuntime().availableProcessors() - 1, 8));
    }

    private WildlandsConfig() {
    }
}
