package com.nightcity.atlas.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Locale;

/**
 * Colorblind-safe Okabe-Ito waypoint palette paired with distinct geometric icon shapes (22 §5.14, 31 UP-8).
 */
public final class OkabeItoPalette {
    public static final int ORANGE = 0xFFE69F00;
    public static final int SKY_BLUE = 0xFF56B4E9;
    public static final int BLUISH_GREEN = 0xFF009E73;
    public static final int YELLOW = 0xFFF0E442;
    public static final int BLUE = 0xFF0072B2;
    public static final int VERMILION = 0xFFD55E00;
    public static final int REDDISH_PURPLE = 0xFFCC79A7;
    public static final int BLACK = 0xFF1A1A1A;
    public static final int WHITE = 0xFFF5F5F5;

    public static final List<Integer> SAFE_COLORS = List.of(
            ORANGE, BLUISH_GREEN, VERMILION, REDDISH_PURPLE, SKY_BLUE, BLUE, YELLOW, WHITE
    );

    public enum WaypointShape {
        HOUSE("house", "⌂"),
        DIAMOND("diamond", "◆"),
        TRIANGLE_BANG("triangle_bang", "▲"),
        ARCH("arch", "∩"),
        CIRCLE_DOT("circle_dot", "●"),
        ANCHOR("anchor", "⚓"),
        STAR("star", "★"),
        CROSS("cross", "✚"),
        RIPPLE("ripple", "◎"),
        CHARCOAL_MARK("charcoal_mark", "✕");

        public static final Codec<WaypointShape> CODEC = Codec.STRING.comapFlatMap(
                raw -> {
                    String clean = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : raw;
                    for (WaypointShape s : values()) {
                        if (s.id.equalsIgnoreCase(clean) || s.name().equalsIgnoreCase(clean)) {
                            return DataResult.success(s);
                        }
                    }
                    return DataResult.error(() -> "Unknown WaypointShape: " + raw);
                },
                s -> s.id
        );

        private final String id;
        private final String glyph;

        WaypointShape(String id, String glyph) {
            this.id = id;
            this.glyph = glyph;
        }

        public String id() {
            return id;
        }

        public String glyph() {
            return glyph;
        }

        public Identifier identifier() {
            return Identifier.fromNamespaceAndPath("tcatlas", id);
        }
    }

    public enum WaypointCategory {
        HOME("home", ORANGE, WaypointShape.HOUSE, true),
        RESOURCE("resource", BLUISH_GREEN, WaypointShape.DIAMOND, true),
        DANGER("danger", VERMILION, WaypointShape.TRIANGLE_BANG, true),
        RUIN("ruin", REDDISH_PURPLE, WaypointShape.ARCH, true),
        ROUTE("route", SKY_BLUE, WaypointShape.CIRCLE_DOT, true),
        PORT("port", BLUE, WaypointShape.ANCHOR, true),
        CUSTOM("custom", YELLOW, WaypointShape.STAR, true),
        DEATH("death", BLACK, WaypointShape.CROSS, false),
        PING("ping", WHITE, WaypointShape.RIPPLE, false);

        public static final Codec<WaypointCategory> CODEC = Codec.STRING.comapFlatMap(
                raw -> {
                    String clean = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : raw;
                    for (WaypointCategory c : values()) {
                        if (c.id.equalsIgnoreCase(clean) || c.name().equalsIgnoreCase(clean)) {
                            return DataResult.success(c);
                        }
                    }
                    return DataResult.error(() -> "Unknown WaypointCategory: " + raw);
                },
                c -> c.id
        );

        private final String id;
        private final int defaultArgb;
        private final WaypointShape defaultShape;
        private final boolean countsTowardCap;

        WaypointCategory(String id, int defaultArgb, WaypointShape defaultShape, boolean countsTowardCap) {
            this.id = id;
            this.defaultArgb = defaultArgb;
            this.defaultShape = defaultShape;
            this.countsTowardCap = countsTowardCap;
        }

        public String id() {
            return id;
        }

        public Identifier identifier() {
            return Identifier.fromNamespaceAndPath("tcatlas", id);
        }

        public int defaultArgb() {
            return defaultArgb;
        }

        public WaypointShape defaultShape() {
            return defaultShape;
        }

        public boolean countsTowardCap() {
            return countsTowardCap;
        }

        public static List<WaypointCategory> userSelectable() {
            return List.of(HOME, RESOURCE, DANGER, RUIN, ROUTE, PORT, CUSTOM);
        }
    }

    private OkabeItoPalette() {
    }
}
