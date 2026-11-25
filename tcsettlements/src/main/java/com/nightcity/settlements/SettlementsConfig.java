package com.nightcity.settlements;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SettlementsConfig {
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue ENABLE_COMPAT_HOOKS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("general");
        ENABLED = builder.comment("Enable Settlements P0 foundation systems").define("enabled", true);
        ENABLE_COMPAT_HOOKS = builder.comment("Enable soft cross-module bridges").define("enable_compat_hooks", true);
        builder.pop();
        SERVER_SPEC = builder.build();
    }

    private SettlementsConfig() {
    }
}
