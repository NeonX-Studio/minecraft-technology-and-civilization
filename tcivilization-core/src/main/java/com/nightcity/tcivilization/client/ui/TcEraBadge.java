package com.nightcity.tcivilization.client.ui;

import com.nightcity.tcivilization.client.api.HudRect;
import com.nightcity.tcivilization.client.api.TcSkin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Shared era pill badge widget rendered via TcSkin (03 §5.10.6).
 */
public final class TcEraBadge {
    private TcEraBadge() {
    }

    public static void render(GuiGraphicsExtractor graphics, HudRect rect, int eraIndex, TcSkin skin) {
        TcSkin.Palette pal = skin.palette();
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), pal.panel());
        graphics.outline(rect.x(), rect.y(), rect.width(), rect.height(), pal.accent());
        String label = eraIndex < 0 ? "Era -" : ("Era " + eraIndex);
        graphics.centeredText(Minecraft.getInstance().font, Component.literal(label), rect.x() + rect.width() / 2, rect.y() + 3, pal.accent());
    }
}
