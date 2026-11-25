package com.nightcity.tcivilization.client.api;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;

/**
 * Page content hosted inside CivPanelScreen (31 §4.3).
 */
public interface CivTabPage {
    void init(CivTabContext ctx, HudRect area);

    void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick);

    default boolean handleDeepLink(DeepLink link) {
        return false;
    }

    default void onSnapshot(CompoundTag data) {
    }

    default void onClose() {
    }
}
