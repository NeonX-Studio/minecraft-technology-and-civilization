package com.nightcity.wildlands.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.nightcity.tcivilization.api.world.LatLon;
import com.nightcity.wildlands.network.WildlandsPayloads;
import com.nightcity.wildlands.planet.PlanetCoordinates;
import com.nightcity.wildlands.planet.PlanetModel;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * 5-candidate starting site selection screen writing <world>/tc/wildlands/start_site.json on confirmation (10 §10.5).
 */
public final class StartSiteScreen extends Screen {
    private final boolean canCustomize;
    private final List<PlanetModel.StartSiteCandidate> candidates;
    private int selectedIndex = 0;

    public StartSiteScreen(boolean canCustomize, List<PlanetModel.StartSiteCandidate> candidates) {
        super(Component.translatable("gui.tcwildlands.start_site.title"));
        this.canCustomize = canCustomize;
        this.candidates = List.copyOf(candidates);
    }

    private int panelWidth() {
        return Math.min(376, Math.max(300, this.width - 16));
    }

    private int panelHeight() {
        return Math.min(234, Math.max(196, this.height - 12));
    }

    @Override
    protected void init() {
        super.init();
        int panelH = panelHeight();
        int panelTop = (this.height - panelH) / 2;
        int btnY = panelTop + panelH - 25;
        int cx = this.width / 2;

        if (canCustomize) {
            int startX = cx - 174;
            addRenderableWidget(Button.builder(
                    Component.translatable("gui.tcwildlands.start_site.prev"),
                    b -> { if (!candidates.isEmpty()) selectedIndex = Math.floorMod(selectedIndex - 1, candidates.size()); }
            ).bounds(startX, btnY, 62, 18).build());

            addRenderableWidget(Button.builder(
                    Component.translatable("gui.tcwildlands.start_site.next"),
                    b -> { if (!candidates.isEmpty()) selectedIndex = Math.floorMod(selectedIndex + 1, candidates.size()); }
            ).bounds(startX + 67, btnY, 62, 18).build());

            addRenderableWidget(Button.builder(
                    Component.translatable("gui.tcwildlands.start_site.random"),
                    b -> {
                        ClientPacketDistributor.sendToServer(new WildlandsPayloads.StartSiteChoicePayload((byte) 1, 0, 0, 0));
                        onClose();
                    }
            ).bounds(startX + 134, btnY, 64, 18).build());

            addRenderableWidget(Button.builder(
                    Component.translatable("gui.tcwildlands.start_site.custom"),
                    b -> {
                        ClientPacketDistributor.sendToServer(new WildlandsPayloads.StartSiteChoicePayload((byte) 2, 0, 0, 0));
                        onClose();
                    }
            ).bounds(startX + 203, btnY, 64, 18).build());

            addRenderableWidget(Button.builder(
                    Component.translatable("gui.tcwildlands.start_site.confirm"),
                    b -> {
                        ClientPacketDistributor.sendToServer(new WildlandsPayloads.StartSiteChoicePayload((byte) 0, selectedIndex, 0, 0));
                        onClose();
                    }
            ).bounds(startX + 272, btnY, 76, 18).build());
        } else {
            int startX = cx - 147;
            addRenderableWidget(Button.builder(
                    Component.translatable("gui.tcwildlands.start_site.prev"),
                    b -> { if (!candidates.isEmpty()) selectedIndex = Math.floorMod(selectedIndex - 1, candidates.size()); }
            ).bounds(startX, btnY, 64, 18).build());

            addRenderableWidget(Button.builder(
                    Component.translatable("gui.tcwildlands.start_site.next"),
                    b -> { if (!candidates.isEmpty()) selectedIndex = Math.floorMod(selectedIndex + 1, candidates.size()); }
            ).bounds(startX + 70, btnY, 64, 18).build());

            addRenderableWidget(Button.builder(
                    Component.translatable("gui.tcwildlands.start_site.random"),
                    b -> {
                        ClientPacketDistributor.sendToServer(new WildlandsPayloads.StartSiteChoicePayload((byte) 1, 0, 0, 0));
                        onClose();
                    }
            ).bounds(startX + 140, btnY, 68, 18).build());

            addRenderableWidget(Button.builder(
                    Component.translatable("gui.tcwildlands.start_site.confirm"),
                    b -> {
                        ClientPacketDistributor.sendToServer(new WildlandsPayloads.StartSiteChoicePayload((byte) 0, selectedIndex, 0, 0));
                        onClose();
                    }
            ).bounds(startX + 214, btnY, 80, 18).build());
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft != null && this.minecraft.level != null) {
            this.extractBlurredBackground(graphics);
        }
        graphics.fillGradient(0, 0, this.width, this.height, 0xE60B131E, 0xF2060B12);

        int panelW = panelWidth();
        int panelH = panelHeight();
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        graphics.fill(left, top, left + panelW, top + panelH, 0xFF141E2B);
        graphics.fill(left + 1, top + 1, left + panelW - 1, top + 23, 0xFF1C2A3B);
        graphics.horizontalLine(left + 1, left + panelW - 2, top + 23, 0xFF384E68);
        graphics.outline(left, top, panelW, panelH, 0xFF4B6785);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelW = panelWidth();
        int panelH = panelHeight();
        int panelLeft = (this.width - panelW) / 2;
        int panelTop = (this.height - panelH) / 2;
        int cx = this.width / 2;

        graphics.centeredText(this.font, this.title, cx, panelTop + 8, 0xFFFBBF24);

        // Global equirectangular map viewport with graticule & numbered pins 1..5
        int mapW = Math.min(296, panelW - 24);
        int mapH = Math.clamp(panelH - 118, 74, 110);
        int mapX = cx - mapW / 2;
        int mapY = panelTop + 28;
        graphics.fill(mapX, mapY, mapX + mapW, mapY + mapH, 0xFF0F2740);
        graphics.horizontalLine(mapX + 1, mapX + mapW - 2, mapY + mapH / 2, 0xFF21496E);
        graphics.horizontalLine(mapX + 1, mapX + mapW - 2, mapY + mapH / 4, 0xFF173654);
        graphics.horizontalLine(mapX + 1, mapX + mapW - 2, mapY + (mapH * 3) / 4, 0xFF173654);
        graphics.verticalLine(cx, mapY + 1, mapY + mapH - 2, 0xFF21496E);
        graphics.verticalLine(mapX + mapW / 4, mapY + 1, mapY + mapH - 2, 0xFF173654);
        graphics.verticalLine(mapX + (mapW * 3) / 4, mapY + 1, mapY + mapH - 2, 0xFF173654);
        graphics.outline(mapX, mapY, mapW, mapH, 0xFF567596);

        for (int i = 0; i < candidates.size(); i++) {
            PlanetModel.StartSiteCandidate c = candidates.get(i);
            int mx = pinScreenX(c, mapX, mapW);
            int mz = pinScreenY(c, mapY, mapH);
            boolean sel = (i == selectedIndex);
            if (sel) {
                graphics.outline(mx - 8, mz - 8, 17, 17, 0xFFFDE68A);
            }
            int pinBg = sel ? 0xFFD97706 : 0xFF1E293B;
            int pinBorder = sel ? 0xFFFFFBEB : 0xFF94A3B8;
            graphics.fill(mx - 6, mz - 6, mx + 7, mz + 7, pinBg);
            graphics.outline(mx - 6, mz - 6, 13, 13, pinBorder);
            graphics.centeredText(this.font, String.valueOf(i + 1), mx + 1, mz - 3, 0xFFFFFFFF);
        }

        // Structured candidate details card
        int infoX = panelLeft + 12;
        int infoY = mapY + mapH + 6;
        int infoW = panelW - 24;
        int infoH = Math.max(44, (panelTop + panelH - 30) - infoY);
        graphics.fill(infoX, infoY, infoX + infoW, infoY + infoH, 0xFF0E1722);
        graphics.outline(infoX, infoY, infoW, infoH, 0xFF334960);

        if (!candidates.isEmpty()) {
            PlanetModel.StartSiteCandidate cur = candidates.get(Math.clamp(selectedIndex, 0, candidates.size() - 1));
            LatLon ll = PlanetCoordinates.latLon(cur.blockX(), cur.blockZ());

            Component candHeader = Component.translatable("gui.tcwildlands.start_site.candidate", selectedIndex + 1, cur.score());
            graphics.text(this.font, candHeader, infoX + 8, infoY + 5, 0xFFFBBF24, false);

            int barW = 72;
            int barX = infoX + infoW - barW - 8;
            int barY = infoY + 6;
            graphics.fill(barX, barY, barX + barW, barY + 6, 0xFF1E293B);
            int fillW = Math.round((barW - 2) * Math.clamp(cur.score() / 100.0f, 0.0f, 1.0f));
            if (fillW > 0) {
                int scoreCol = cur.score() >= 70 ? 0xFF10B981 : (cur.score() >= 45 ? 0xFFF59E0B : 0xFFEF4444);
                graphics.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + 5, scoreCol);
            }
            graphics.outline(barX, barY, barW, 6, 0xFF475569);

            String coordStr = String.format(Locale.ROOT, "Lat %.1f° · Lon %.1f° · Y %d", ll.latDeg(), ll.lonDeg(), cur.surfaceY());
            graphics.text(this.font, coordStr, infoX + 8, infoY + 18, 0xFFF1F5F9, false);

            String biomeStr = this.font.plainSubstrByWidth(
                    cur.biome().getPath() + " [" + cur.koppen() + "]",
                    Math.max(60, infoW / 2 - 12)
            );
            graphics.text(this.font, biomeStr, infoX + infoW - this.font.width(biomeStr) - 8, infoY + 18, 0xFF93C5FD, false);

            Component resLine = Component.translatable("gui.tcwildlands.start_site.resources", cur.primaryRock().getPath(), cur.waterDistanceBlocks());
            String resStr = this.font.plainSubstrByWidth(resLine.getString(), infoW - 16);
            graphics.text(this.font, resStr, infoX + 8, infoY + 31, 0xFFCBD5E1, false);
        }

        graphics.nextStratum();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private static int pinScreenX(PlanetModel.StartSiteCandidate c, int mapX, int mapW) {
        double norm = (c.blockX() - PlanetCoordinates.X_MIN) / (double) PlanetCoordinates.PLANET_WIDTH;
        return mapX + 8 + (int) Math.round(Math.clamp(norm, 0.0D, 1.0D) * (mapW - 16));
    }

    private static int pinScreenY(PlanetModel.StartSiteCandidate c, int mapY, int mapH) {
        double norm = (c.blockZ() - PlanetCoordinates.Z_MIN) / (double) PlanetCoordinates.PLANET_HEIGHT;
        return mapY + 8 + (int) Math.round(Math.clamp(norm, 0.0D, 1.0D) * (mapH - 16));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() == 0 && !candidates.isEmpty()) {
            int panelW = panelWidth();
            int panelH = panelHeight();
            int panelTop = (this.height - panelH) / 2;
            int mapW = Math.min(296, panelW - 24);
            int mapH = Math.clamp(panelH - 118, 74, 110);
            int mapX = this.width / 2 - mapW / 2;
            int mapY = panelTop + 28;
            for (int i = 0; i < candidates.size(); i++) {
                int mx = pinScreenX(candidates.get(i), mapX, mapW);
                int mz = pinScreenY(candidates.get(i), mapY, mapH);
                if (Math.abs(event.x() - mx) <= 8 && Math.abs(event.y() - mz) <= 8) {
                    selectedIndex = i;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!candidates.isEmpty()) {
            if (event.key() == InputConstants.KEY_LEFT) {
                selectedIndex = Math.floorMod(selectedIndex - 1, candidates.size());
                return true;
            }
            if (event.key() == InputConstants.KEY_RIGHT) {
                selectedIndex = Math.floorMod(selectedIndex + 1, candidates.size());
                return true;
            }
            if (event.key() >= InputConstants.KEY_1 && event.key() <= InputConstants.KEY_9) {
                int idx = event.key() - InputConstants.KEY_1;
                if (idx < candidates.size()) {
                    selectedIndex = idx;
                    return true;
                }
            }
        }
        return super.keyPressed(event);
    }
}
