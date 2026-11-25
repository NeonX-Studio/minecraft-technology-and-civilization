package com.nightcity.echoes;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class EchoesConfig {
    public static final ModConfigSpec COMMON_SPEC;
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;

    // Common config (18 §13)
    public static final ModConfigSpec.IntValue CULTURE_COUNT;
    public static final ModConfigSpec.IntValue STANDALONE_RADIUS_REGIONS;
    public static final ModConfigSpec.IntValue MAX_SITES;
    public static final ModConfigSpec.IntValue THREAD_COUNT;
    public static final ModConfigSpec.BooleanValue SUPPRESS_SUPERSEDED;
    public static final ModConfigSpec.BooleanValue SHIPWRECK_CARGO;
    public static final ModConfigSpec.BooleanValue ADOPT_VANILLA_ARCHAEOLOGY;

    // Server config (18 §13)
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue ENABLE_COMPAT_HOOKS;
    public static final ModConfigSpec.IntValue MAX_ACTIVE_CHAINS;
    public static final ModConfigSpec.IntValue CAMPFIRE_COOLDOWN_DAYS;
    public static final ModConfigSpec.IntValue MAX_LIVE_ENTRIES_PER_DAY;
    public static final ModConfigSpec.IntValue RUINIFY_ABANDONED_YEARS;
    public static final ModConfigSpec.BooleanValue RUINIFY_PLAYER_SETTLEMENTS;
    public static final ModConfigSpec.IntValue RUINIFY_INITIAL_AGE;
    public static final ModConfigSpec.BooleanValue EXPLORATION_EVENTS_ENABLED;
    public static final ModConfigSpec.DoubleValue HERITAGE_DAMAGE_THRESHOLD;
    public static final ModConfigSpec.IntValue DEFAULT_SURVIVOR_COUNT;

    // Client config (18 §13)
    public static final ModConfigSpec.IntValue CLUE_COMPASS_RANGE;
    public static final ModConfigSpec.BooleanValue CLUE_COMPASS_ENABLED;

    static {
        ModConfigSpec.Builder common = new ModConfigSpec.Builder();
        common.push("history");
        CULTURE_COUNT = common.comment("Initial Dawn-phase culture count")
                .defineInRange("culture_count", 12, 8, 16);
        STANDALONE_RADIUS_REGIONS = common.comment("Standalone simulation window radius in 512-block regions around spawn")
                .defineInRange("standalone_radius_regions", 24, 8, 64);
        MAX_SITES = common.comment("Global simulated site cap")
                .defineInRange("max_sites", 12_000, 2_000, 40_000);
        THREAD_COUNT = common.comment("Parallel worker count for cell phases (0 = availableProcessors - 1)")
                .defineInRange("thread_count", 0, 0, 8);
        common.pop();
        common.push("vanilla");
        SUPPRESS_SUPERSEDED = common.comment("Suppress superseded vanilla structures in non-planet mode")
                .define("suppress_superseded", false);
        SHIPWRECK_CARGO = common.comment("Append ancient cargo bundles and captain logs to vanilla shipwrecks")
                .define("shipwreck_cargo", true);
        ADOPT_VANILLA_ARCHAEOLOGY = common.comment("Adopt vanilla archaeology structures into Echoes site dossiers")
                .define("adopt_vanilla_archaeology", true);
        common.pop();
        COMMON_SPEC = common.build();

        ModConfigSpec.Builder server = new ModConfigSpec.Builder();
        server.push("general");
        ENABLED = server.comment("Enable Echoes systems").define("enabled", true);
        ENABLE_COMPAT_HOOKS = server.comment("Enable soft cross-module bridges").define("enable_compat_hooks", true);
        DEFAULT_SURVIVOR_COUNT = server.comment("Default survivor polity count when tcnations does not override ai_civ_count")
                .defineInRange("default_survivor_count", 8, 1, 32);
        server.pop();
        server.push("legends");
        MAX_ACTIVE_CHAINS = server.defineInRange("max_active_chains", 6, 1, 20);
        CAMPFIRE_COOLDOWN_DAYS = server.defineInRange("campfire_cooldown_days", 3, 0, 30);
        server.pop();
        server.push("living_history");
        MAX_LIVE_ENTRIES_PER_DAY = server.defineInRange("max_entries_per_day", 32, 4, 256);
        server.pop();
        server.push("ruinify");
        RUINIFY_ABANDONED_YEARS = server.defineInRange("abandoned_years", 2, 1, 20);
        RUINIFY_PLAYER_SETTLEMENTS = server.define("player_settlements", false);
        RUINIFY_INITIAL_AGE = server.defineInRange("initial_age", 50, 0, 500);
        server.pop();
        server.push("exploration_events");
        EXPLORATION_EVENTS_ENABLED = server.define("enabled", true);
        server.pop();
        server.push("heritage");
        HERITAGE_DAMAGE_THRESHOLD = server.defineInRange("damage_threshold", 0.2D, 0.0D, 1.0D);
        server.pop();
        SERVER_SPEC = server.build();

        ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        client.push("hud");
        CLUE_COMPASS_RANGE = client.defineInRange("clue_compass_range", 512, 64, 2048);
        CLUE_COMPASS_ENABLED = client.define("clue_compass", true);
        client.pop();
        CLIENT_SPEC = client.build();
    }

    private EchoesConfig() {
    }

    public static int cultureCount() {
        return COMMON_SPEC.isLoaded() ? CULTURE_COUNT.get() : 12;
    }

    public static int standaloneRadiusRegions() {
        return COMMON_SPEC.isLoaded() ? STANDALONE_RADIUS_REGIONS.get() : 24;
    }

    public static int maxSites() {
        return COMMON_SPEC.isLoaded() ? MAX_SITES.get() : 12_000;
    }

    public static int threadCount() {
        return COMMON_SPEC.isLoaded() ? THREAD_COUNT.get() : 0;
    }

    public static int maxLiveEntriesPerDay() {
        return SERVER_SPEC.isLoaded() ? MAX_LIVE_ENTRIES_PER_DAY.get() : 32;
    }

    public static int defaultSurvivorCount() {
        return SERVER_SPEC.isLoaded() ? DEFAULT_SURVIVOR_COUNT.get() : 8;
    }
}
