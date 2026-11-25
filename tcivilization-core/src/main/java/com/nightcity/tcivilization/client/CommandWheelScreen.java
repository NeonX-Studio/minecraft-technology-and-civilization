package com.nightcity.tcivilization.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.nightcity.tcivilization.client.api.CivPanel;
import com.nightcity.tcivilization.client.api.ClientWheelContext;
import com.nightcity.tcivilization.client.api.CommandWheelRegistry;
import com.nightcity.tcivilization.client.api.TcSkin;
import com.nightcity.tcivilization.client.api.WheelOption;
import com.nightcity.tcivilization.client.api.WheelTarget;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Z Command Wheel overlay screen: 8 directional sectors, 12px center deadzone, up to 3 pages (03 §5.10.3, §10.4).
 */
public final class CommandWheelScreen extends Screen {
    public static final double DEFAULT_DEADZONE = 12.0D;

    private static final int CARD_HALF_W = 50;
    private static final int CARD_HALF_H = 11;
    private static final double SECTOR_RADIUS = 90.0D;

    private final WheelTarget target;
    private List<WheelOption> options = List.of();
    private int currentPage = 0;
    private int hoveredSector = -1;

    public CommandWheelScreen(WheelTarget target, boolean hasPersonalTabs) {
        super(Component.translatable("gui.tcivilization.command_wheel.title"));
        this.target = target == null ? WheelTarget.none() : target;
        this.options = CommandWheelRegistry.global().collectPredicted(new ClientWheelContext(this.target, hasPersonalTabs));
    }

    public static int selectSector(double dx, double dy, double deadzone) {
        double dist = Math.hypot(dx, dy);
        if (dist < deadzone) {
            return -1;
        }
        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0D;
        double normalized = (angle % 360.0D + 360.0D) % 360.0D;
        return ((int) Math.floor((normalized + 22.5D) / 45.0D)) % CommandWheelRegistry.SLOTS_PER_PAGE;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Subtle unblurred dim overlay so live world target remains sharply visible behind the radial wheel
        graphics.fillGradient(0, 0, this.width, this.height, 0x44080C12, 0x68080C12);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        TcSkin.Palette pal = TcSkin.active().palette();
        int accentCol = 0xFF000000 | pal.accent();
        int cx = this.width / 2;
        int cy = this.height / 2;
        this.hoveredSector = selectSector(mouseX - cx, mouseY - cy, DEFAULT_DEADZONE);

        int totalPages = totalPageCount();
        int pageStart = currentPage * CommandWheelRegistry.SLOTS_PER_PAGE;
        int pageEnd = Math.min(options.size(), pageStart + CommandWheelRegistry.SLOTS_PER_PAGE);

        // Draw spoke line to active sector behind hub & sector cards
        if (hoveredSector >= 0 && hoveredSector < (pageEnd - pageStart)) {
            double rad = Math.toRadians(hoveredSector * 45.0D - 90.0D);
            int tx = cx + (int) Math.round(Math.cos(rad) * (SECTOR_RADIUS - CARD_HALF_H - 4));
            int ty = cy + (int) Math.round(Math.sin(rad) * (SECTOR_RADIUS - CARD_HALF_H - 4));
            drawSpoke(graphics, cx, cy, tx, ty, accentCol);
        }

        // Center hub card
        String rawTitle = target.displayName().isEmpty()
                ? Component.translatable("gui.tcivilization.command_wheel.title").getString()
                : target.displayName();
        String centerTitle = this.font.plainSubstrByWidth(rawTitle, 84);
        int hubHalfW = Math.max(36, this.font.width(centerTitle) / 2 + 10);
        int hubHalfH = totalPages > 1 ? 15 : 12;
        graphics.fill(cx - hubHalfW, cy - hubHalfH, cx + hubHalfW, cy + hubHalfH, 0xFF161C24);
        graphics.outline(cx - hubHalfW, cy - hubHalfH, hubHalfW * 2, hubHalfH * 2, hoveredSector < 0 ? accentCol : 0xFF526071);
        graphics.centeredText(this.font, centerTitle, cx, cy - (totalPages > 1 ? 9 : 4), 0xFFF8FAFC);
        if (totalPages > 1) {
            String pageTxt = (currentPage + 1) + " / " + totalPages;
            graphics.centeredText(this.font, pageTxt, cx, cy + 3, 0xFF94A3B8);
        }

        if (options.isEmpty()) {
            Component emptyMsg = Component.translatable("gui.tcivilization.command_wheel.empty");
            int ew = this.font.width(emptyMsg) + 16;
            graphics.fill(cx - ew / 2, cy + 24, cx + ew / 2, cy + 42, 0xFF161C24);
            graphics.outline(cx - ew / 2, cy + 24, ew, 18, 0xFF475569);
            graphics.centeredText(this.font, emptyMsg, cx, cy + 29, 0xFFCBD5E1);
        } else {
            for (int slot = 0; slot < (pageEnd - pageStart); slot++) {
                WheelOption opt = options.get(pageStart + slot);
                double rad = Math.toRadians(slot * 45.0D - 90.0D);
                int sx = cx + (int) Math.round(Math.cos(rad) * SECTOR_RADIUS);
                int sy = cy + (int) Math.round(Math.sin(rad) * SECTOR_RADIUS);
                boolean hovered = (slot == hoveredSector);
                int bg = !opt.enabled() ? 0xFF14181F : (hovered ? accentCol : 0xFF1E2631);
                int border = !opt.enabled() ? 0xFF334155 : (hovered ? 0xFFF8FAFC : 0xFF64748B);
                int fg = !opt.enabled() ? 0xFF64748B : (hovered ? 0xFFFFFFFF : 0xFFF1F5F9);

                graphics.fill(sx - CARD_HALF_W, sy - CARD_HALF_H, sx + CARD_HALF_W, sy + CARD_HALF_H, bg);
                graphics.outline(sx - CARD_HALF_W, sy - CARD_HALF_H, CARD_HALF_W * 2, CARD_HALF_H * 2, border);

                String keyHint = String.valueOf(slot + 1);
                graphics.text(this.font, keyHint, sx - CARD_HALF_W + 4, sy - 4, hovered ? 0xFFE2E8F0 : 0xFF94A3B8, false);
                String labelStr = this.font.plainSubstrByWidth(opt.label().getString(), CARD_HALF_W * 2 - 18);
                graphics.centeredText(this.font, labelStr, sx + 4, sy - 4, fg);

                if (hovered && opt.disabledReason().isPresent() && !opt.enabled()) {
                    graphics.setTooltipForNextFrame(this.font, opt.disabledReason().get(), mouseX, mouseY);
                }
            }
        }

        graphics.nextStratum();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private static void drawSpoke(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int col) {
        int steps = Math.max(1, Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)) / 3);
        for (int i = 2; i <= steps; i++) {
            int px = x0 + (x1 - x0) * i / steps;
            int py = y0 + (y1 - y0) * i / steps;
            g.fill(px - 1, py - 1, px + 2, py + 2, col);
        }
    }

    private int totalPageCount() {
        int pages = Math.max(1, (options.size() + CommandWheelRegistry.SLOTS_PER_PAGE - 1) / CommandWheelRegistry.SLOTS_PER_PAGE);
        return Math.min(pages, CommandWheelRegistry.MAX_PAGES);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() == 0) {
            int cx = this.width / 2;
            int cy = this.height / 2;
            this.hoveredSector = selectSector(event.x() - cx, event.y() - cy, DEFAULT_DEADZONE);
            executeHoveredAndClose();
            return true;
        }
        if (event.button() == 1) {
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (event.key() == InputConstants.KEY_Z) {
            executeHoveredAndClose();
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() >= InputConstants.KEY_1 && event.key() <= InputConstants.KEY_8) {
            this.hoveredSector = event.key() - InputConstants.KEY_1;
            executeHoveredAndClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int totalPages = totalPageCount();
        if (totalPages > 1 && scrollY != 0.0D) {
            currentPage = Math.floorMod(currentPage + (scrollY > 0 ? -1 : 1), totalPages);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void executeHoveredAndClose() {
        int index = currentPage * CommandWheelRegistry.SLOTS_PER_PAGE + hoveredSector;
        if (hoveredSector >= 0 && index < options.size()) {
            WheelOption chosen = options.get(index);
            if (chosen.enabled() && "tcivilization:open_civ_panel".equals(chosen.id().toString())) {
                CivPanel.openDefault();
                return;
            }
        }
        this.onClose();
    }
}
