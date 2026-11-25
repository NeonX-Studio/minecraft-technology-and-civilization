package com.nightcity.atlas.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

public enum HudAnchor {
    TOP_LEFT(0.0f, 0.0f),
    TOP_CENTER(0.5f, 0.0f),
    TOP_RIGHT(1.0f, 0.0f),
    MIDDLE_LEFT(0.0f, 0.5f),
    CENTER(0.5f, 0.5f),
    MIDDLE_RIGHT(1.0f, 0.5f),
    BOTTOM_LEFT(0.0f, 1.0f),
    BOTTOM_CENTER(0.5f, 1.0f),
    BOTTOM_RIGHT(1.0f, 1.0f);

    public static final Codec<HudAnchor> CODEC = Codec.STRING.comapFlatMap(
            raw -> {
                try {
                    return DataResult.success(HudAnchor.valueOf(raw.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    return DataResult.error(() -> "Unknown HudAnchor: " + raw);
                }
            },
            Enum::name
    );

    private final float normX;
    private final float normY;

    HudAnchor(float normX, float normY) {
        this.normX = normX;
        this.normY = normY;
    }

    public float normX() {
        return normX;
    }

    public float normY() {
        return normY;
    }

    public int resolveX(int screenWidth, int widgetWidth, int offsetX) {
        int base = Math.round((screenWidth - widgetWidth) * normX);
        return Math.clamp(base + offsetX, 0, Math.max(0, screenWidth - widgetWidth));
    }

    public int resolveY(int screenHeight, int widgetHeight, int offsetY) {
        int base = Math.round((screenHeight - widgetHeight) * normY);
        return Math.clamp(base + offsetY, 0, Math.max(0, screenHeight - widgetHeight));
    }

    public static HudAnchor nearest(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        float cx = screenWidth <= width ? 0.5f : (float) x / (screenWidth - width);
        float cy = screenHeight <= height ? 0.5f : (float) y / (screenHeight - height);
        HudAnchor best = TOP_LEFT;
        float bestDist = Float.MAX_VALUE;
        for (HudAnchor a : values()) {
            float dx = cx - a.normX;
            float dy = cy - a.normY;
            float d2 = dx * dx + dy * dy;
            if (d2 < bestDist) {
                bestDist = d2;
                best = a;
            }
        }
        return best;
    }
}
