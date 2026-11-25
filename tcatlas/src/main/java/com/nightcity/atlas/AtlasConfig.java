package com.nightcity.atlas;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;

public final class AtlasConfig {
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;

    public static final ModConfigSpec.DoubleValue DECAY_SPEED;
    public static final ModConfigSpec.BooleanValue MEMORY_DECAY;
    public static final ModConfigSpec.BooleanValue MAP_WARP;
    public static final ModConfigSpec.BooleanValue DEAD_RECKONING;
    public static final ModConfigSpec.IntValue MAX_SHARED_CARTS;
    public static final ModConfigSpec.IntValue WAYPOINT_HARD_CAP;
    public static final ModConfigSpec.IntValue DEATH_POINT_COUNT;
    public static final ModConfigSpec.BooleanValue REPLACE_LOCATOR_BAR;
    public static final ModConfigSpec.BooleanValue HIDE_VANILLA_COORDINATES;

    public static final ModConfigSpec.ConfigValue<String> DEFAULT_HUD_LEVEL;
    public static final ModConfigSpec.IntValue SNAP_GRID;
    public static final ModConfigSpec.ConfigValue<String> COLORBLIND_MODE;

    public static final ModConfigSpec.BooleanValue PAUSE_SINGLEPLAYER;
    public static final ModConfigSpec.IntValue DEFAULT_ZOOM;
    public static final ModConfigSpec.IntValue TILE_UPLOADS_PER_FRAME;
    public static final ModConfigSpec.IntValue GPU_CACHE_TILES;
    public static final ModConfigSpec.IntValue CLIENT_CACHE_MB;

    public static final ModConfigSpec.ConfigValue<String> MINIMAP_SHAPE;
    public static final ModConfigSpec.IntValue MINIMAP_SIZE;
    public static final ModConfigSpec.IntValue MINIMAP_MAX_MARKERS;
    public static final ModConfigSpec.BooleanValue DEBUG_MINIMAP;

    private static volatile Boolean runtimeDebugMinimapOverride;

    static {
        ModConfigSpec.Builder sb = new ModConfigSpec.Builder();
        sb.push("fog");
        DECAY_SPEED = sb.defineInRange("decay_speed", 1.0, 0.0, 4.0);
        MEMORY_DECAY = sb.define("memory_decay", true);
        MAP_WARP = sb.define("map_warp", true);
        DEAD_RECKONING = sb.define("dead_reckoning", true);
        sb.pop();
        sb.push("Sharing");
        MAX_SHARED_CARTS = sb.defineInRange("max_shared_carts", 128, 16, 1024);
        WAYPOINT_HARD_CAP = sb.defineInRange("waypoint_hard_cap", 4096, 64, 65536);
        DEATH_POINT_COUNT = sb.defineInRange("death_point_count", 5, 0, 20);
        sb.pop();
        sb.push("vanilla");
        REPLACE_LOCATOR_BAR = sb.define("replace_locator_bar", true);
        HIDE_VANILLA_COORDINATES = sb.define("hide_vanilla_coordinates", false);
        sb.pop();
        SERVER_SPEC = sb.build();

        ModConfigSpec.Builder cb = new ModConfigSpec.Builder();
        cb.push("hud");
        DEFAULT_HUD_LEVEL = cb.defineInList("hud_level", "STANDARD", Arrays.asList("MINIMAL", "STANDARD", "TACTICAL", "HIDDEN"));
        SNAP_GRID = cb.defineInRange("snap_grid", 4, 1, 16);
        COLORBLIND_MODE = cb.defineInList("colorblind_mode", "OFF", Arrays.asList("OFF", "PROTANOPIA", "DEUTERANOPIA", "TRITANOPIA"));
        cb.pop();
        cb.push("map");
        PAUSE_SINGLEPLAYER = cb.define("pause_singleplayer", false);
        DEFAULT_ZOOM = cb.defineInRange("default_zoom", 3, 0, 9);
        TILE_UPLOADS_PER_FRAME = cb.defineInRange("tile_uploads_per_frame", 4, 1, 16);
        GPU_CACHE_TILES = cb.defineInRange("gpu_cache_tiles", 256, 64, 1024);
        CLIENT_CACHE_MB = cb.defineInRange("client_cache_mb", 1024, 64, 8192);
        cb.pop();
        cb.push("minimap");
        MINIMAP_SHAPE = cb.defineInList("shape", "circle", Arrays.asList("circle", "square"));
        MINIMAP_SIZE = cb.defineInRange("size", 96, 64, 160);
        MINIMAP_MAX_MARKERS = cb.defineInRange("max_markers", 64, 16, 256);
        DEBUG_MINIMAP = cb.define("debug_minimap", false);
        cb.pop();
        CLIENT_SPEC = cb.build();
    }

    public static boolean isMinimapDebugEnabled() {
        Boolean override = runtimeDebugMinimapOverride;
        if (override != null) {
            return override;
        }
        return CLIENT_SPEC.isLoaded() && DEBUG_MINIMAP.get();
    }

    public static void setRuntimeDebugMinimapOverride(Boolean enabled) {
        runtimeDebugMinimapOverride = enabled;
    }

    private AtlasConfig() {
    }
}
