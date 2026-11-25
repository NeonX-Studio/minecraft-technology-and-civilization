package com.nightcity.nations;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class NationsConfig {
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue ENABLE_COMPAT_HOOKS;

    public static final ModConfigSpec.IntValue AI_CIV_COUNT;
    public static final ModConfigSpec.IntValue AI_CIV_MAX;
    public static final ModConfigSpec.IntValue CAPITAL_MIN_SPACING;
    public static final ModConfigSpec.IntValue SPAWN_EXCLUSION_RADIUS;
    public static final ModConfigSpec.BooleanValue GUARANTEED_NEIGHBOUR;
    public static final ModConfigSpec.IntValue START_ERA_LEAD_MAX;
    public static final ModConfigSpec.IntValue BREATHING_RADIUS_BASE;
    public static final ModConfigSpec.IntValue BREATHING_RADIUS_PER_ERA;
    public static final ModConfigSpec.IntValue WAR_MIN_PLAYER_ERA;
    public static final ModConfigSpec.BooleanValue SUPPRESS_RAIDS_AND_PATROLS;
    public static final ModConfigSpec.BooleanValue SUPPRESS_PLANET_VILLAGES;

    private static volatile Boolean testSuppressRaidsOverride;
    private static volatile Boolean testSuppressVillagesOverride;

    public record SeedingSettings(
            int aiCivCount,
            int aiCivMax,
            int capitalMinSpacing,
            int spawnExclusionRadius,
            boolean guaranteedNeighbour,
            int startEraLeadMax,
            int breathingRadiusBase,
            int breathingRadiusPerEra
    ) {
        public static SeedingSettings defaults() {
            return new SeedingSettings(8, 24, 6000, 2500, true, 1, 1500, 250);
        }

        public int breathingRadiusForPlayerEra(int playerEraIndex) {
            return Math.min(4000, breathingRadiusBase + breathingRadiusPerEra * Math.max(0, playerEraIndex));
        }
    }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("general");
        ENABLED = builder.comment("Enable Nations systems").define("enabled", true);
        ENABLE_COMPAT_HOOKS = builder.comment("Enable soft cross-module bridges").define("enable_compat_hooks", true);
        builder.pop();

        builder.push("seeding");
        AI_CIV_COUNT = builder.defineInRange("ai_civ_count", 8, 0, 24);
        AI_CIV_MAX = builder.defineInRange("ai_civ_max", 24, 0, 48);
        CAPITAL_MIN_SPACING = builder.defineInRange("capital_min_spacing", 6000, 2000, 20000);
        SPAWN_EXCLUSION_RADIUS = builder.defineInRange("spawn_exclusion_radius", 2500, 500, 10000);
        GUARANTEED_NEIGHBOUR = builder.define("guaranteed_neighbour", true);
        START_ERA_LEAD_MAX = builder.defineInRange("start_era_lead_max", 1, 0, 3);
        builder.pop();

        builder.push("pacing");
        BREATHING_RADIUS_BASE = builder.defineInRange("breathing_radius_base", 1500, 0, 6000);
        BREATHING_RADIUS_PER_ERA = builder.defineInRange("breathing_radius_per_era", 250, 0, 1000);
        WAR_MIN_PLAYER_ERA = builder.defineInRange("war_min_player_era", 2, 0, 10);
        builder.pop();

        builder.push("vanilla");
        SUPPRESS_RAIDS_AND_PATROLS = builder.define("suppress_raids_and_patrols", true);
        SUPPRESS_PLANET_VILLAGES = builder.define("suppress_planet_villages", true);
        builder.pop();

        SERVER_SPEC = builder.build();
    }

    private NationsConfig() {
    }

    public static boolean isEnabled() {
        return !SERVER_SPEC.isLoaded() || ENABLED.getAsBoolean();
    }

    public static boolean isCompatHooksEnabled() {
        return !SERVER_SPEC.isLoaded() || ENABLE_COMPAT_HOOKS.getAsBoolean();
    }

    public static SeedingSettings seedingSettings() {
        if (!SERVER_SPEC.isLoaded()) {
            return SeedingSettings.defaults();
        }
        return new SeedingSettings(
                AI_CIV_COUNT.getAsInt(),
                AI_CIV_MAX.getAsInt(),
                CAPITAL_MIN_SPACING.getAsInt(),
                SPAWN_EXCLUSION_RADIUS.getAsInt(),
                GUARANTEED_NEIGHBOUR.getAsBoolean(),
                START_ERA_LEAD_MAX.getAsInt(),
                BREATHING_RADIUS_BASE.getAsInt(),
                BREATHING_RADIUS_PER_ERA.getAsInt()
        );
    }

    public static boolean isSuppressRaidsAndPatrols() {
        Boolean override = testSuppressRaidsOverride;
        return override != null ? override : (!SERVER_SPEC.isLoaded() || SUPPRESS_RAIDS_AND_PATROLS.getAsBoolean());
    }

    public static boolean isSuppressPlanetVillages() {
        Boolean override = testSuppressVillagesOverride;
        return override != null ? override : (!SERVER_SPEC.isLoaded() || SUPPRESS_PLANET_VILLAGES.getAsBoolean());
    }

    public static void setTestOverrides(Boolean suppressRaids, Boolean suppressVillages) {
        testSuppressRaidsOverride = suppressRaids;
        testSuppressVillagesOverride = suppressVillages;
    }
}
