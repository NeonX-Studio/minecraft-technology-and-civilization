package com.nightcity.tcivilization.client.ui;

import com.nightcity.tcivilization.client.api.HudRect;
import com.nightcity.tcivilization.client.api.TcSkin;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Shared horizontal progress bar widget rendered via TcSkin (03 §5.10.6).
 */
public final class TcProgressBar {
    private TcProgressBar() {
    }

    public static void render(GuiGraphicsExtractor graphics, HudRect rect, float progress, TcSkin skin) {
        float clamped = Math.clamp(progress, 0.0f, 1.0f);
        TcSkin.Palette pal = skin.palette();
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), pal.background());
        int fillW = Math.round((rect.width() - 2) * clamped);
        if (fillW > 0) {
            graphics.fill(rect.x() + 1, rect.y() + 1, rect.x() + 1 + fillW, rect.bottom() - 1, pal.accent());
        }
        graphics.outline(rect.x(), rect.y(), rect.width(), rect.height(), pal.border());
    }
}
