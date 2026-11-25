package com.nightcity.atlas.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public interface HudWidget {
    record HudSize(int width, int height) {
    }

    record HudContext(HudLevel level, HudSkin skin, float partialTick, boolean editPreview) {
    }

    Identifier id();

    HudAnchor defaultAnchor();

    default int defaultOffsetX() {
        return 0;
    }

    default int defaultOffsetY() {
        return 0;
    }

    HudLevel minLevel();

    HudSize measure(HudContext ctx);

    default boolean shouldRender(HudContext ctx) {
        return ctx.editPreview() || ctx.level().includes(minLevel());
    }

    default void render(GuiGraphicsExtractor graphics, int x, int y, int width, int height, HudContext ctx) {
        if (!ctx.editPreview()) {
            return;
        }
        HudSkin skin = ctx.skin();
        graphics.fill(x, y, x + width, y + height, skin.palette().bgFill());
        graphics.outline(x, y, width, height, skin.palette().bgBorder());
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.font != null) {
            graphics.text(mc.font, id().getPath(), x + 4, y + 4, skin.palette().textPrimary(), skin.fontShadow());
        }
    }
}
