package com.nightcity.tcivilization.client.api;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/**
 * Client HUD widget instance rendered inside tcivilization:hud GUI layer (31 §3.2).
 */
public interface HudWidget {
    void tick(HudContext ctx);

    void render(GuiGraphicsExtractor graphics, HudRect rect, float partialTick, HudContext ctx);

    default void onData(Identifier channel, CompoundTag data) {
    }

    default void onSkinChanged(TcSkin skin) {
    }
}
