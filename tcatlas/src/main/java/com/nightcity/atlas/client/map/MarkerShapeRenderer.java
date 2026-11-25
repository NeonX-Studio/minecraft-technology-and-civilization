package com.nightcity.atlas.client.map;

import com.nightcity.atlas.api.OkabeItoPalette;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Renders accessible Okabe-Ito waypoint icons using both distinct geometric silhouettes and high-contrast borders (31 UP-8).
 */
public final class MarkerShapeRenderer {
    private MarkerShapeRenderer() {
    }

    public static void drawShape(
            GuiGraphicsExtractor g,
            OkabeItoPalette.WaypointShape shape,
            int cx,
            int cy,
            int argb,
            float opacity,
            boolean highlighted
    ) {
        int alpha = Math.clamp(Math.round(opacity * 255.0f), 0, 255);
        if (alpha <= 4) {
            return;
        }
        int fill = (alpha << 24) | (argb & 0x00FFFFFF);
        boolean darkFill = (((argb >>> 16) & 0xFF) + ((argb >>> 8) & 0xFF) + (argb & 0xFF)) < 180;
        int border = (alpha << 24) | (highlighted ? 0xFFFFFF : (darkFill ? 0xF5F5F5 : 0x181818));

        switch (shape) {
            case HOUSE -> {
                g.fill(cx - 4, cy - 1, cx + 5, cy + 5, border);
                g.fill(cx - 3, cy - 3, cx + 4, cy - 1, border);
                g.fill(cx - 1, cy - 5, cx + 2, cy - 3, border);
                g.fill(cx - 3, cy, cx + 4, cy + 4, fill);
                g.fill(cx - 2, cy - 2, cx + 3, cy, fill);
            }
            case DIAMOND -> {
                for (int dy = -4; dy <= 4; dy++) {
                    int w = 4 - Math.abs(dy);
                    g.fill(cx - w - 1, cy + dy, cx + w + 2, cy + dy + 1, border);
                    if (w > 0) {
                        g.fill(cx - w, cy + dy, cx + w + 1, cy + dy + 1, fill);
                    }
                }
            }
            case TRIANGLE_BANG -> {
                for (int row = 0; row < 7; row++) {
                    int half = row / 2 + 1;
                    int y = cy - 4 + row;
                    g.fill(cx - half - 1, y, cx + half + 2, y + 1, border);
                    g.fill(cx - half, y, cx + half + 1, y + 1, fill);
                }
                g.fill(cx, cy - 2, cx + 1, cy + 1, border);
                g.fill(cx, cy + 2, cx + 1, cy + 3, border);
            }
            case ARCH -> {
                g.fill(cx - 4, cy - 3, cx + 5, cy + 5, border);
                g.fill(cx - 3, cy - 4, cx + 4, cy - 3, border);
                g.fill(cx - 3, cy - 3, cx + 4, cy + 4, fill);
                g.fill(cx - 1, cy, cx + 2, cy + 4, border);
            }
            case CIRCLE_DOT -> {
                g.fill(cx - 3, cy - 4, cx + 4, cy + 5, border);
                g.fill(cx - 4, cy - 3, cx + 5, cy + 4, border);
                g.fill(cx - 2, cy - 3, cx + 3, cy + 4, fill);
                g.fill(cx - 3, cy - 2, cx + 4, cy + 3, fill);
                g.fill(cx, cy, cx + 1, cy + 1, border);
            }
            case ANCHOR -> {
                g.fill(cx - 1, cy - 4, cx + 2, cy + 4, border);
                g.fill(cx - 4, cy + 1, cx + 5, cy + 4, border);
                g.fill(cx, cy - 3, cx + 1, cy + 3, fill);
                g.fill(cx - 3, cy + 2, cx + 4, cy + 3, fill);
                g.fill(cx - 2, cy - 1, cx + 3, cy, fill);
            }
            case STAR -> {
                g.fill(cx - 1, cy - 5, cx + 2, cy + 6, border);
                g.fill(cx - 5, cy - 1, cx + 6, cy + 2, border);
                g.fill(cx - 3, cy - 3, cx + 4, cy + 4, border);
                g.fill(cx, cy - 4, cx + 1, cy + 5, fill);
                g.fill(cx - 4, cy, cx + 5, cy + 1, fill);
                g.fill(cx - 2, cy - 2, cx + 3, cy + 3, fill);
            }
            case CROSS -> {
                g.fill(cx - 2, cy - 5, cx + 3, cy + 6, border);
                g.fill(cx - 5, cy - 2, cx + 6, cy + 3, border);
                g.fill(cx - 1, cy - 4, cx + 2, cy + 5, fill);
                g.fill(cx - 4, cy - 1, cx + 5, cy + 2, fill);
            }
            case RIPPLE -> {
                g.outline(cx - 5, cy - 5, 11, 11, fill);
                g.outline(cx - 3, cy - 3, 7, 7, border);
                g.fill(cx - 1, cy - 1, cx + 2, cy + 2, fill);
            }
            case CHARCOAL_MARK -> {
                for (int i = -3; i <= 3; i++) {
                    g.fill(cx + i - 1, cy + i, cx + i + 2, cy + i + 1, border);
                    g.fill(cx + i - 1, cy - i, cx + i + 2, cy - i + 1, border);
                    g.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, fill);
                    g.fill(cx + i, cy - i, cx + i + 1, cy - i + 1, fill);
                }
            }
        }
    }
}
