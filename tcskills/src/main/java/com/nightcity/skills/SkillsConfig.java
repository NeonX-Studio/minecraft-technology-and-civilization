package com.nightcity.skills;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SkillsConfig {
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;

    public static final ModConfigSpec.DoubleValue GLOBAL_PROFICIENCY_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SPECIALTY_MULTIPLIER;
    public static final ModConfigSpec.IntValue DAILY_SOFT_CAP_BASE;
    public static final ModConfigSpec.IntValue DAILY_SOFT_CAP_PER_LEVEL;
    public static final ModConfigSpec.BooleanValue ALLOW_MANUAL_SPECIALTY_RESET;

    public static final ModConfigSpec.BooleanValue SHOW_GAIN_TICKER;
    public static final ModConfigSpec.BooleanValue LEVEL_UP_SOUND;

    static {
        ModConfigSpec.Builder sb = new ModConfigSpec.Builder();
        sb.push("gain");
        GLOBAL_PROFICIENCY_MULTIPLIER = sb.defineInRange("global_proficiency_multiplier", 1.0, 0.1, 10.0);
        SPECIALTY_MULTIPLIER = sb.defineInRange("specialty_multiplier", 1.25, 1.0, 3.0);
        DAILY_SOFT_CAP_BASE = sb.defineInRange("daily_soft_cap_base", 600, 100, 10000);
        DAILY_SOFT_CAP_PER_LEVEL = sb.defineInRange("daily_soft_cap_per_level", 40, 0, 500);
        sb.pop();
        sb.push("respec");
        ALLOW_MANUAL_SPECIALTY_RESET = sb.define("allow_manual_specialty_reset", true);
        sb.pop();
        SERVER_SPEC = sb.build();

        ModConfigSpec.Builder cb = new ModConfigSpec.Builder();
        cb.push("feedback");
        SHOW_GAIN_TICKER = cb.define("show_gain_ticker", true);
        LEVEL_UP_SOUND = cb.define("level_up_sound", true);
        cb.pop();
        CLIENT_SPEC = cb.build();
    }

    private SkillsConfig() {
    }
}
