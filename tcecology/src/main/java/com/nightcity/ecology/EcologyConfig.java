package com.nightcity.ecology;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class EcologyConfig {
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue ENABLE_COMPAT_HOOKS;
    public static final ModConfigSpec.BooleanValue REPLACE_VANILLA_WILDLIFE;
    public static final ModConfigSpec.BooleanValue ENFORCE_MONSTER_POLICY_GUARD;
    public static final ModConfigSpec.DoubleValue POPULATION_K_SCALE;
    public static final ModConfigSpec.BooleanValue RELICT_SPECIES_ENABLED;
    public static final ModConfigSpec.IntValue EMBODY_RADIUS;
    public static final ModConfigSpec.IntValue EMBODY_DESPAWN_RADIUS;
    public static final ModConfigSpec.IntValue EMBODY_MAX_ENTITIES;
    public static final ModConfigSpec.BooleanValue STANDALONE_DOMESTIC_FALLBACK;
    public static final ModConfigSpec.BooleanValue VANILLA_DROPS_FALLBACK;

    private static volatile Boolean testReplaceWildlifeOverride;
    private static volatile Boolean testEnforceMonsterGuardOverride;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("general");
        ENABLED = builder.comment("Enable Living Ecology systems").define("enabled", true);
        ENABLE_COMPAT_HOOKS = builder.comment("Enable soft cross-module bridges").define("enable_compat_hooks", true);
        builder.pop();

        builder.push("spawning");
        REPLACE_VANILLA_WILDLIFE = builder.define("replace_vanilla_wildlife", true);
        ENFORCE_MONSTER_POLICY_GUARD = builder.define("enforce_monster_policy_guard", true);
        builder.pop();

        builder.push("population");
        POPULATION_K_SCALE = builder.defineInRange("k_scale", 1.0D, 0.25D, 4.0D);
        RELICT_SPECIES_ENABLED = builder.define("relict_species_enabled", true);
        builder.pop();

        builder.push("embody");
        EMBODY_RADIUS = builder.defineInRange("radius", 128, 64, 256);
        EMBODY_DESPAWN_RADIUS = builder.defineInRange("despawn_radius", 176, 96, 320);
        EMBODY_MAX_ENTITIES = builder.defineInRange("max_entities", 300, 50, 600);
        builder.pop();

        builder.push("fallback");
        STANDALONE_DOMESTIC_FALLBACK = builder.define("standalone_domestic_fallback", true);
        VANILLA_DROPS_FALLBACK = builder.define("vanilla_drops", true);
        builder.pop();

        SERVER_SPEC = builder.build();
    }

    private EcologyConfig() {
    }

    public static boolean isEnabled() {
        return !SERVER_SPEC.isLoaded() || ENABLED.getAsBoolean();
    }

    public static boolean isCompatHooksEnabled() {
        return !SERVER_SPEC.isLoaded() || ENABLE_COMPAT_HOOKS.getAsBoolean();
    }

    public static boolean isReplaceVanillaWildlife() {
        Boolean override = testReplaceWildlifeOverride;
        return override != null ? override : (!SERVER_SPEC.isLoaded() || REPLACE_VANILLA_WILDLIFE.getAsBoolean());
    }

    public static boolean isEnforceMonsterPolicyGuard() {
        Boolean override = testEnforceMonsterGuardOverride;
        return override != null ? override : (!SERVER_SPEC.isLoaded() || ENFORCE_MONSTER_POLICY_GUARD.getAsBoolean());
    }

    public static boolean isRelictSpeciesEnabled() {
        return !SERVER_SPEC.isLoaded() || RELICT_SPECIES_ENABLED.getAsBoolean();
    }

    public static void setTestOverrides(Boolean replaceWildlife, Boolean enforceMonsterGuard) {
        testReplaceWildlifeOverride = replaceWildlife;
        testEnforceMonsterGuardOverride = enforceMonsterGuard;
    }
}
