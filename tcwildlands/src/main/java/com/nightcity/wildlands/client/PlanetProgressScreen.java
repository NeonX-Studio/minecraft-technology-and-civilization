package com.nightcity.wildlands.client;

import com.nightcity.tcivilization.api.world.PlanetProgressStages;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Planet formation progress screen showing the 7 builtin physical stages plus external stages
 * (e.g. tcechoes:history_simulation) with accessible ✔ / ● / ○ symbols and a 2:1 preview map (10 §10.4).
 */
public final class PlanetProgressScreen extends Screen {
    private int activeStageIndex;
    private int totalStages;
    private float overallFraction;
    private String detailText = "";
    private byte[] previewPalette = new byte[0];
    private int previewW = 0;
    private int previewH = 0;

    public PlanetProgressScreen(int activeStageIndex, int totalStages, float overallFraction, String detailText) {
        super(Component.translatable("gui.tcwildlands.progress.title"));
        this.activeStageIndex = activeStageIndex;
        this.totalStages = Math.max(1, totalStages);
        this.overallFraction = overallFraction;
        this.detailText = detailText == null ? "" : detailText;
    }

    public void updateProgress(int stageIndex, int total, float fraction, String detail) {
        this.activeStageIndex = stageIndex;
        this.totalStages = Math.max(1, total);
        this.overallFraction = fraction;
        this.detailText = detail == null ? "" : detail;
    }

    public void updatePreview(int w, int h, byte[] palette) {
        this.previewW = w;
        this.previewH = h;
        this.previewPalette = palette == null ? new byte[0] : palette.clone();
    }

    private int panelWidth() {
        return Math.min(416, Math.max(312, this.width - 16));
    }

    private int panelHeight() {
        return Math.min(228, Math.max(194, this.height - 12));
    }

    @Override
    protected void init() {
        super.init();
        int panelW = panelWidth();
        int panelH = panelHeight();
        int panelRight = (this.width + panelW) / 2;
        int panelBottom = (this.height + panelH) / 2;
        addRenderableWidget(Button.builder(
                Component.translatable("gui.tcwildlands.progress.leave"),
                btn -> onClose()
        ).bounds(panelRight - 94, panelBottom - 26, 82, 18).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft != null && this.minecraft.level != null) {
            this.extractBlurredBackground(graphics);
        }
        graphics.fillGradient(0, 0, this.width, this.height, 0xEA09111B, 0xF4050910);

        int panelW = panelWidth();
        int panelH = panelHeight();
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        graphics.fill(left, top, left + panelW, top + panelH, 0xFF131D2A);
        graphics.fill(left + 1, top + 1, left + panelW - 1, top + 24, 0xFF1B293B);
        graphics.horizontalLine(left + 1, left + panelW - 2, top + 24, 0xFF384E68);
        graphics.outline(left, top, panelW, panelH, 0xFF4B6785);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelW = panelWidth();
        int panelH = panelHeight();
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;
        int right = left + panelW;
        int bottom = top + panelH;

        graphics.text(this.font, this.title, left + 12, top + 8, 0xFFFBBF24, false);
        List<PlanetProgressStages.StageInfo> stages = PlanetProgressStages.stages();
        int displayTotal = Math.max(totalStages, stages.size());
        String stageBadge = Math.min(activeStageIndex + 1, displayTotal) + " / " + displayTotal;
        graphics.text(this.font, stageBadge, right - this.font.width(stageBadge) - 12, top + 8, 0xFFCBD5E1, false);

        // Left: 2:1 equirectangular planet preview box
        int bodyTop = top + 30;
        int bodyBottom = bottom - 50;
        int availBodyH = Math.max(96, bodyBottom - bodyTop);
        int mapW = Math.min(232, Math.min((panelW - 36) * 58 / 100, availBodyH * 2));
        mapW = Math.max(160, mapW & ~1);
        int mapH = mapW / 2;
        int mapX = left + 12;
        int mapY = bodyTop + Math.max(0, (availBodyH - mapH) / 2);

        graphics.fill(mapX, mapY, mapX + mapW, mapY + mapH, 0xFF0E243B);
        if (previewW > 0 && previewH > 0 && previewPalette.length >= previewW * previewH) {
            int step = 2;
            for (int py = 0; py < mapH; py += step) {
                int sy = Math.min(previewH - 1, (py * previewH) / mapH);
                for (int px = 0; px < mapW; px += step) {
                    int sx = Math.min(previewW - 1, (px * previewW) / mapW);
                    int bIdx = previewPalette[sy * previewW + sx] & 0xFF;
                    int col = bIdx < 10 ? 0xFF1A4670 : (bIdx >= 54 && bIdx <= 61 ? 0xFFDCE6EE : 0xFF4F8A4B);
                    graphics.fill(mapX + px, mapY + py, Math.min(mapX + mapW, mapX + px + step), Math.min(mapY + mapH, mapY + py + step), col);
                }
            }
        } else {
            graphics.horizontalLine(mapX + 1, mapX + mapW - 2, mapY + mapH / 2, 0xFF1E4266);
            graphics.verticalLine(mapX + mapW / 2, mapY + 1, mapY + mapH - 2, 0xFF1E4266);
        }
        graphics.outline(mapX, mapY, mapW, mapH, 0xFF567596);

        // Right: stage checklist with accessible ✔ / ● / ○ state markers
        int listX = mapX + mapW + 10;
        int listW = Math.max(100, right - 12 - listX);
        graphics.fill(listX, bodyTop, listX + listW, bodyBottom, 0xFF0E1621);
        graphics.outline(listX, bodyTop, listW, bodyBottom - bodyTop, 0xFF334960);

        int rowH = Math.min(14, Math.max(11, (availBodyH - 8) / Math.max(1, stages.size())));
        for (int i = 0; i < stages.size(); i++) {
            int ry = bodyTop + 5 + i * rowH;
            if (ry + 9 > bodyBottom - 2) {
                break;
            }
            boolean done = i < activeStageIndex;
            boolean active = i == activeStageIndex;
            if (active) {
                graphics.fill(listX + 2, ry - 1, listX + listW - 2, ry + 10, 0xFF1D2F45);
            }
            String symbol = done ? "✔ " : (active ? "● " : "○ ");
            int color = done ? 0xFF34D399 : (active ? 0xFFFBBF24 : 0xFF94A3B8);
            String fullLabel = symbol + stages.get(i).title().getString();
            String fitted = this.font.plainSubstrByWidth(fullLabel, listW - 10);
            graphics.text(this.font, fitted, listX + 5, ry + 1, color, false);
        }

        // Overall progress bar & percentage
        int barX = left + 12;
        int barY = bottom - 44;
        int barW = panelW - 58;
        int barH = 11;
        float clamped = Math.clamp(overallFraction, 0.0f, 1.0f);
        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF0C131D);
        int fillW = Math.round((barW - 2) * clamped);
        if (fillW > 0) {
            graphics.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + barH - 1, 0xFF3B82F6);
        }
        graphics.outline(barX, barY, barW, barH, 0xFF4B6785);

        String pct = Math.round(clamped * 100.0f) + "%";
        graphics.text(this.font, pct, barX + barW + 6, barY + 2, 0xFFF8FAFC, false);

        if (!detailText.isBlank()) {
            String fittedDetail = this.font.plainSubstrByWidth(detailText, Math.max(80, panelW - 118));
            graphics.text(this.font, fittedDetail, left + 12, bottom - 21, 0xFFCBD5E1, false);
        }

        graphics.nextStratum();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
}
