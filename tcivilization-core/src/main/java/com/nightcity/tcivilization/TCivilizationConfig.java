package com.nightcity.tcivilization;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server/common configuration for tcivilization-core (03 §8).
 */
public final class TCivilizationConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue MAX_EMBODIED_NPC;
    public static final ModConfigSpec.IntValue EMBODY_RADIUS;
    public static final ModConfigSpec.IntValue MAX_SWITCHES_PER_PASS;
    public static final ModConfigSpec.IntValue CATCH_UP_CAP_DAYS;
    public static final ModConfigSpec.IntValue CATCH_UP_STEPS_PER_TICK;
    public static final ModConfigSpec.DoubleValue APPLY_BUDGET_MS;
    public static final ModConfigSpec.IntValue REGION_CACHE_SIZE;
    public static final ModConfigSpec.IntValue MEMORY_CAP;
    public static final ModConfigSpec.BooleanValue STRICT_MODE;
    public static final ModConfigSpec.BooleanValue AUTO_JOIN_DEFAULT_CIV;
    public static final ModConfigSpec.BooleanValue FRIENDLY_FIRE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("simulation");
        MAX_EMBODIED_NPC = b.defineInRange("max_embodied_npc", 500, 50, 1000);
        EMBODY_RADIUS = b.defineInRange("embody_radius", 128, 48, 256);
        MAX_SWITCHES_PER_PASS = b.defineInRange("max_switches_per_pass", 32, 4, 64);
        CATCH_UP_CAP_DAYS = b.defineInRange("catch_up_cap_days", 3, 0, 14);
        CATCH_UP_STEPS_PER_TICK = b.defineInRange("catch_up_steps_per_tick", 16, 1, 64);
        APPLY_BUDGET_MS = b.defineInRange("apply_budget_ms", 0.6D, 0.2D, 3.0D);
        b.pop();

        b.push("storage");
        REGION_CACHE_SIZE = b.defineInRange("region_cache_size", 256, 64, 1024);
        b.pop();

        b.push("memory");
        MEMORY_CAP = b.defineInRange("memory_cap", 2000, 500, 10000);
        b.pop();

        b.push("debug");
        STRICT_MODE = b.define("strict_mode", false);
        b.pop();

        b.push("civ");
        AUTO_JOIN_DEFAULT_CIV = b.define("auto_join_default_civ", true);
        FRIENDLY_FIRE = b.define("friendly_fire", false);
        b.pop();

        SPEC = b.build();
    }

    private TCivilizationConfig() {
    }
}
