package com.nightcity.atlas.client.hud;

import com.nightcity.atlas.TAtlas;
import com.nightcity.atlas.api.HudApi;
import com.nightcity.atlas.api.HudLevel;
import com.nightcity.atlas.api.HudSkin;
import com.nightcity.atlas.api.HudWidget;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class HudLayoutEditorScreen extends Screen {
    private final HudLayoutStore store;
    private final Runnable onSaveCallback;
    private Identifier draggingId;
    private int dragGrabDx;
    private int dragGrabDy;

    public HudLayoutEditorScreen(HudLayoutStore store, Runnable onSaveCallback) {
        super(Component.translatable("gui.tcatlas.hud_editor.title"));
        this.store = store;
        this.onSaveCallback = onSaveCallback;
    }

    @Override
    protected void init() {
        int y = this.height - 24;
        int cx = this.width / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.tcatlas.hud_editor.reset"), b -> {
            store.resetToDefaults(HudApi.get().widgets());
            onSaveCallback.run();
        }).bounds(cx - 134, y, 90, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.tcatlas.hud_editor.save"), b -> {
            onSaveCallback.run();
            onClose();
        }).bounds(cx - 38, y, 90, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.tcatlas.layout_editor.exit"), b -> onClose())
                .bounds(cx + 58, y, 76, 18).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x80080C12);

        int gridStep = Math.max(16, TAtlas.snapGrid() * 4);
        for (int gx = gridStep; gx < this.width; gx += gridStep) {
            graphics.verticalLine(gx, 22, this.height - 30, 0x1E94A3B8);
        }
        for (int gy = 22 + gridStep; gy < this.height - 30; gy += gridStep) {
            graphics.horizontalLine(0, this.width - 1, gy, 0x1E94A3B8);
        }
        graphics.verticalLine(this.width / 2, 22, this.height - 30, 0x44D99B52);
        graphics.horizontalLine(0, this.width - 1, this.height / 2, 0x44D99B52);

        HudSkin skin = HudApi.get().currentSkin();
        int borderCol = 0xFF000000 | skin.palette().bgBorder();
        graphics.fill(0, 0, this.width, 22, 0xEA12161E);
        graphics.horizontalLine(0, this.width - 1, 22, borderCol);
        graphics.fill(0, this.height - 30, this.width, this.height, 0xEA12161E);
        graphics.horizontalLine(0, this.width - 1, this.height - 30, borderCol);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        HudSkin skin = HudApi.get().currentSkin();
        int accentCol = 0xFF000000 | skin.palette().accent();
        int primaryCol = 0xFF000000 | skin.palette().textPrimary();
        int secondaryCol = 0xFF000000 | skin.palette().textSecondary();
        int borderCol = 0xFF000000 | skin.palette().bgBorder();

        graphics.text(this.font, this.title, 10, 7, accentCol, false);
        String hint = "LMB Drag: Move  ·  RMB: Enable/Disable";
        if (this.width >= 320) {
            graphics.text(this.font, hint, this.width - this.font.width(hint) - 10, 7, secondaryCol, false);
        }

        HudWidget.HudContext ctx = new HudWidget.HudContext(HudLevel.TACTICAL, skin, partialTick, true);
        HudWidget hoveredWidget = null;
        HudLayoutStore.WidgetPlacement hoveredPlacement = null;

        for (HudWidget widget : HudApi.get().widgets()) {
            HudLayoutStore.WidgetPlacement pl = store.placementFor(widget);
            HudWidget.HudSize sz = widget.measure(ctx);
            int w = Math.max(24, Math.round(sz.width() * pl.scale()));
            int h = Math.max(14, Math.round(sz.height() * pl.scale()));
            int x = pl.anchor().resolveX(this.width, w, pl.offsetX());
            int y = pl.anchor().resolveY(this.height, h, pl.offsetY());

            boolean isDragging = widget.id().equals(draggingId);
            boolean isHovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
            if (isHovered) {
                hoveredWidget = widget;
                hoveredPlacement = pl;
            }

            int fill = pl.enabled()
                    ? (0xE6000000 | (skin.palette().bgFill() & 0x00FFFFFF))
                    : 0xCC181B20;
            int border = isDragging ? 0xFFFBBF24 : (isHovered ? accentCol : (pl.enabled() ? borderCol : 0xFF475569));

            graphics.fill(x, y, x + w, y + h, fill);
            graphics.outline(x, y, w, h, border);

            String label = pl.enabled() ? widget.id().getPath() : (widget.id().getPath() + " [OFF]");
            String fitted = this.font.plainSubstrByWidth(label, Math.max(12, w - 8));
            int textY = y + Math.max(2, Math.min(5, (h - 8) / 2));
            graphics.text(this.font, fitted, x + 4, textY, pl.enabled() ? primaryCol : 0xFF94A3B8, false);

            if (h >= 28 && w >= 64) {
                String anchorStr = this.font.plainSubstrByWidth(pl.anchor().name(), w - 8);
                graphics.text(this.font, anchorStr, x + 4, textY + 11, secondaryCol, false);
            }
        }

        graphics.nextStratum();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        if (hoveredWidget != null && hoveredPlacement != null && draggingId == null) {
            List<Component> tip = List.of(
                    Component.literal(hoveredWidget.id().toString()).withStyle(ChatFormatting.GOLD),
                    Component.literal("Anchor: " + hoveredPlacement.anchor().name()
                            + " (" + hoveredPlacement.offsetX() + ", " + hoveredPlacement.offsetY() + ")")
                            .withStyle(ChatFormatting.GRAY),
                    Component.literal(hoveredPlacement.enabled() ? "Enabled (Right-click to hide)" : "Hidden (Right-click to enable)")
                            .withStyle(hoveredPlacement.enabled() ? ChatFormatting.GREEN : ChatFormatting.RED)
            );
            graphics.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        HudWidget.HudContext ctx = new HudWidget.HudContext(HudLevel.TACTICAL, HudApi.get().currentSkin(), 0f, true);
        for (HudWidget widget : HudApi.get().widgets()) {
            HudLayoutStore.WidgetPlacement pl = store.placementFor(widget);
            HudWidget.HudSize sz = widget.measure(ctx);
            int w = Math.max(24, Math.round(sz.width() * pl.scale()));
            int h = Math.max(14, Math.round(sz.height() * pl.scale()));
            int x = pl.anchor().resolveX(this.width, w, pl.offsetX());
            int y = pl.anchor().resolveY(this.height, h, pl.offsetY());
            if (event.x() >= x && event.x() <= x + w && event.y() >= y && event.y() <= y + h) {
                if (event.button() == 1) {
                    store.setPlacement(widget.id(), new HudLayoutStore.WidgetPlacement(
                            pl.anchor(), pl.offsetX(), pl.offsetY(), pl.scale(), !pl.enabled()
                    ));
                    onSaveCallback.run();
                    return true;
                }
                this.draggingId = widget.id();
                this.dragGrabDx = (int) event.x() - x;
                this.dragGrabDy = (int) event.y() - y;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingId != null) {
            HudWidget target = findWidget(draggingId);
            if (target != null) {
                HudWidget.HudContext ctx = new HudWidget.HudContext(HudLevel.TACTICAL, HudApi.get().currentSkin(), 0f, true);
                HudWidget.HudSize sz = target.measure(ctx);
                HudLayoutStore.WidgetPlacement cur = store.placementFor(target);
                int newX = (int) event.x() - dragGrabDx;
                int newY = (int) event.y() - dragGrabDy;
                store.setPlacement(draggingId, cur.movedTo(newX, newY, sz.width(), sz.height(), this.width, this.height, TAtlas.snapGrid()));
                return true;
            }
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingId != null) {
            draggingId = null;
            onSaveCallback.run();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        onSaveCallback.run();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static HudWidget findWidget(Identifier id) {
        for (HudWidget w : HudApi.get().widgets()) {
            if (w.id().equals(id)) {
                return w;
            }
        }
        return null;
    }
}
