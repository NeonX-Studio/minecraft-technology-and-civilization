package com.nightcity.tcivilization.client.api;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * 200x100 GUI-pixel overview card contributed by a tab to tcivilization:overview (03 §5.10.1, §6).
 */
public interface OverviewCard {
    int WIDTH = 200;
    int HEIGHT = 100;

    Identifier id();

    Component title();

    void render(GuiGraphicsExtractor graphics, HudRect bounds, CivTabContext ctx);
}
