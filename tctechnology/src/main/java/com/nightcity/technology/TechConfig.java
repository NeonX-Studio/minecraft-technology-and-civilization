package com.nightcity.technology;

import com.nightcity.technology.api.DifficultyApi;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.Locale;

public final class TechConfig {
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;

    public static final ModConfigSpec.ConfigValue<String> DIFFICULTY_PRESET;
    public static final ModConfigSpec.DoubleValue TECH_COST;
    public static final ModConfigSpec.DoubleValue KNOWLEDGE_GAIN;
    public static final ModConfigSpec.DoubleValue INSIGHT_BONUS;
    public static final ModConfigSpec.DoubleValue POPULATION_GROWTH;
    public static final ModConfigSpec.IntValue AI_AGGRESSION;
    public static final ModConfigSpec.DoubleValue DISASTER_RATE;

    public static final ModConfigSpec.IntValue PRE_RESEARCH_LIMIT;
    public static final ModConfigSpec.DoubleValue POOL_MAX_SHARE;
    public static final ModConfigSpec.DoubleValue POOL_CAP_MULTIPLIER;
    public static final ModConfigSpec.ConfigValue<String> RESEARCH_TABLE_MODE;
    public static final ModConfigSpec.IntValue SITE_QUEUE_LENGTH;
    public static final ModConfigSpec.IntValue SETTLE_INTERVAL_TICKS;

    public static final ModConfigSpec.DoubleValue DEFAULT_DAILY_CAP_RATIO;
    public static final ModConfigSpec.DoubleValue NPC_PRACTICE_MULT;
    public static final ModConfigSpec.DoubleValue FAR_ERA_KNOWLEDGE_CAP;

    public static final ModConfigSpec.BooleanValue AUTO_DECLARE_ERA;
    public static final ModConfigSpec.BooleanValue CEREMONY_ENABLED;
    public static final ModConfigSpec.BooleanValue ENABLE_STUBS;
    public static final ModConfigSpec.BooleanValue REPLACE_VANILLA_CAMPFIRE;

    public static final ModConfigSpec.ConfigValue<String> DEFAULT_ZOOM;
    public static final ModConfigSpec.BooleanValue WIDE_MODE;
    public static final ModConfigSpec.BooleanValue SHOW_FALLBACK_TECHS;
    public static final ModConfigSpec.BooleanValue PLAY_MUSIC;

    static {
        ModConfigSpec.Builder sb = new ModConfigSpec.Builder();
        sb.push("difficulty");
        DIFFICULTY_PRESET = sb.defineInList("preset", "standard", Arrays.asList("sandbox", "standard", "historical", "custom"));
        TECH_COST = sb.defineInRange("tech_cost", 1.0, 0.25, 3.0);
        KNOWLEDGE_GAIN = sb.defineInRange("knowledge_gain", 1.0, 0.25, 3.0);
        INSIGHT_BONUS = sb.defineInRange("insight_bonus", 0.10, 0.0, 0.5);
        POPULATION_GROWTH = sb.defineInRange("population_growth", 1.0, 0.25, 3.0);
        AI_AGGRESSION = sb.defineInRange("ai_aggression", 1, 0, 3);
        DISASTER_RATE = sb.defineInRange("disaster_rate", 1.0, 0.0, 3.0);
        sb.pop();

        sb.push("research");
        PRE_RESEARCH_LIMIT = sb.defineInRange("pre_research_limit", 3, 0, 5);
        POOL_MAX_SHARE = sb.defineInRange("pool_max_share", 0.50, 0.0, 1.0);
        POOL_CAP_MULTIPLIER = sb.defineInRange("pool_cap_multiplier", 3.0, 1.0, 10.0);
        RESEARCH_TABLE_MODE = sb.defineInList("research_table_mode", "auto", Arrays.asList("auto", "always", "never"));
        SITE_QUEUE_LENGTH = sb.defineInRange("site_queue_length", 5, 1, 10);
        SETTLE_INTERVAL_TICKS = sb.defineInRange("settle_interval_ticks", 100, 20, 400);
        sb.pop();

        sb.push("knowledge");
        DEFAULT_DAILY_CAP_RATIO = sb.defineInRange("default_daily_cap_ratio", 0.5, 0.1, 2.0);
        NPC_PRACTICE_MULT = sb.defineInRange("npc_practice_mult", 0.25, 0.0, 1.0);
        FAR_ERA_KNOWLEDGE_CAP = sb.defineInRange("far_era_knowledge_cap", 0.5, 0.0, 1.0);
        sb.pop();

        sb.push("era");
        AUTO_DECLARE_ERA = sb.define("auto_declare_era", false);
        CEREMONY_ENABLED = sb.define("ceremony_enabled", true);
        sb.pop();

        sb.push("fallback");
        ENABLE_STUBS = sb.define("enable_stubs", true);
        sb.pop();

        sb.push("content");
        REPLACE_VANILLA_CAMPFIRE = sb.define("replace_vanilla_campfire", true);
        sb.pop();
        SERVER_SPEC = sb.build();

        ModConfigSpec.Builder cb = new ModConfigSpec.Builder();
        cb.push("tree");
        DEFAULT_ZOOM = cb.defineInList("default_zoom", "era", Arrays.asList("global", "era", "detail"));
        WIDE_MODE = cb.define("wide_mode", true);
        SHOW_FALLBACK_TECHS = cb.define("show_fallback_techs", true);
        cb.pop();
        cb.push("ceremony");
        PLAY_MUSIC = cb.define("play_music", true);
        cb.pop();
        CLIENT_SPEC = cb.build();
    }

    private TechConfig() {
    }

    public static DifficultyApi.Difficulty resolveDifficulty() {
        if (!SERVER_SPEC.isLoaded()) {
            return DifficultyApi.Difficulty.fromPreset("standard");
        }
        String preset = DIFFICULTY_PRESET.get().toLowerCase(Locale.ROOT);
        if ("custom".equals(preset)) {
            return new DifficultyApi.Difficulty(
                    "custom",
                    TECH_COST.get(),
                    KNOWLEDGE_GAIN.get(),
                    INSIGHT_BONUS.get(),
                    POPULATION_GROWTH.get(),
                    AI_AGGRESSION.get(),
                    DISASTER_RATE.get()
            );
        }
        return DifficultyApi.Difficulty.fromPreset(preset);
    }
}
