package com.nightcity.atlas.client.map;

import com.mojang.blaze3d.platform.InputConstants;
import com.nightcity.atlas.AtlasConfig;
import com.nightcity.atlas.TAtlas;
import com.nightcity.atlas.api.HudApi;
import com.nightcity.atlas.api.HudSkin;
import com.nightcity.atlas.api.KnowledgeHolder;
import com.nightcity.atlas.api.OkabeItoPalette;
import com.nightcity.atlas.api.Waypoint;
import com.nightcity.atlas.compat.wildlands.WildlandsTerrainBridge;
import com.nightcity.atlas.map.DeterministicWarpField;
import com.nightcity.atlas.map.MapTileCache;
import com.nightcity.atlas.map.MapTileRasterizer;
import com.nightcity.atlas.map.MemoryDecayModel;
import com.nightcity.atlas.map.PlanetProjection;
import com.nightcity.atlas.waypoint.WaypointManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * World Map Screen (M key, 22 §5.9, §9.3, §10.1).
 * Supports seamless 163,840-block east-west wrapping across x = +-81,920, L0..L9 tile pyramid,
 * SL0..SL5 memory blur/fading & deterministic warp, Okabe-Ito waypoints, and wrapped ruler/bearing tool.
 */
public final class WorldMapScreen extends Screen {
    private static final int SIDEBAR_W = 114;
    private static final int MAP_TOP = 26;
    private static final int FOOTER_H = 24;

    private final ClientTileTextureManager gpuTextures;
    private double centerWorldX;
    private double centerWorldZ;
    private int zoomLevel;
    private boolean showSidebar = true;
    private boolean rulerMode = false;
    @Nullable
    private BlockPos rulerStart;
    @Nullable
    private BlockPos rulerEnd;
    private boolean panning = false;
    private EditBox searchBox;
    private String searchDraft = "";
    private double lastFrameBuildMs = 0.0;

    public WorldMapScreen(ClientTileTextureManager gpuTextures) {
        super(Component.translatable("gui.tcatlas.world_map.title"));
        this.gpuTextures = gpuTextures;
        Minecraft mc = Minecraft.getInstance();
        this.zoomLevel = AtlasConfig.CLIENT_SPEC.isLoaded() ? AtlasConfig.DEFAULT_ZOOM.get() : 3;
        if (mc.player != null) {
            this.centerWorldX = mc.player.getX();
            this.centerWorldZ = mc.player.getZ();
        }
    }

    private int computeMapLeft() {
        return showSidebar ? (SIDEBAR_W + 10) : 6;
    }

    @Override
    protected void init() {
        int topY = 4;
        int searchW = Math.min(126, Math.max(76, this.width / 3));
        int searchX = this.width - 68 - searchW;
        searchBox = new EditBox(this.font, searchX, topY, searchW, 16, Component.translatable("gui.tcatlas.world_map.search"));
        searchBox.setMaxLength(48);
        searchBox.setHint(Component.translatable("gui.tcatlas.world_map.search"));
        searchBox.setValue(searchDraft);
        searchBox.setResponder(val -> {
            this.searchDraft = val == null ? "" : val;
            onSearchChanged(val);
        });
        addRenderableWidget(searchBox);

        addRenderableWidget(Button.builder(Component.literal("+"), b -> adjustZoom(-1))
                .bounds(this.width - 64, topY, 18, 16).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> adjustZoom(1))
                .bounds(this.width - 44, topY, 18, 16).build());
        addRenderableWidget(Button.builder(Component.literal("×"), b -> onClose())
                .bounds(this.width - 24, topY, 18, 16).build());

        int botY = this.height - 20;
        addRenderableWidget(Button.builder(Component.literal(showSidebar ? "[Tab 侧栏:开]" : "[Tab 侧栏]"), b -> {
            showSidebar = !showSidebar;
            rebuildWidgets();
        }).bounds(6, botY, 74, 16).build());

        int leftStart = 84;
        addRenderableWidget(Button.builder(Component.literal(rulerMode ? "[1 测距:开]" : "[1 测距]"), b -> {
            rulerMode = !rulerMode;
            if (!rulerMode) {
                rulerStart = null;
                rulerEnd = null;
            }
            b.setMessage(Component.literal(rulerMode ? "[1 测距:开]" : "[1 测距]"));
        }).bounds(leftStart, botY, 68, 16).build());

        addRenderableWidget(Button.builder(Component.literal("[空格 居中]"), b -> recenterOnPlayer())
                .bounds(leftStart + 72, botY, 68, 16).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        HudSkin skin = HudApi.get().currentSkin();
        int bgOpaque = 0xFF000000 | skin.palette().bgFill();
        int borderOpaque = 0xFF000000 | skin.palette().bgBorder();

        graphics.fill(0, 0, this.width, this.height, bgOpaque);
        graphics.fill(0, 0, this.width, MAP_TOP - 2, 0xFF1A1612);
        graphics.horizontalLine(0, this.width - 1, MAP_TOP - 2, borderOpaque);
        graphics.fill(0, this.height - FOOTER_H, this.width, this.height, 0xFF1A1612);
        graphics.horizontalLine(0, this.width - 1, this.height - FOOTER_H, borderOpaque);
    }

    private void adjustZoom(int delta) {
        zoomLevel = Math.clamp(zoomLevel + delta, PlanetProjection.MIN_ZOOM, PlanetProjection.MAX_ZOOM);
    }

    private void recenterOnPlayer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            boolean wrapX = WildlandsTerrainBridge.isEastWestWrapActive(currentDimension());
            centerWorldX = wrapX ? PlanetProjection.wrapX(mc.player.getX()) : mc.player.getX();
            centerWorldZ = mc.player.getZ();
        }
    }

    private void onSearchChanged(String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String q = raw.trim().toLowerCase(Locale.ROOT);
        String[] parts = q.split("\\s+");
        if (parts.length == 2) {
            try {
                double tx = Double.parseDouble(parts[0]);
                double tz = Double.parseDouble(parts[1]);
                boolean wrapX = WildlandsTerrainBridge.isEastWestWrapActive(currentDimension());
                centerWorldX = wrapX ? PlanetProjection.wrapX(tx) : tx;
                centerWorldZ = wrapX ? PlanetProjection.clampBlockZ((int) Math.round(tz)) : tz;
                return;
            } catch (NumberFormatException ignored) {
            }
        }
        Minecraft mc = Minecraft.getInstance();
        UUID viewer = mc.player != null ? mc.player.getUUID() : null;
        for (Waypoint wp : TAtlas.waypoints().visibleWaypoints(viewer, currentDimension())) {
            if (wp.name().toLowerCase(Locale.ROOT).contains(q)) {
                centerWorldX = wp.pos().getX();
                centerWorldZ = wp.pos().getZ();
                break;
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        HudSkin skin = HudApi.get().currentSkin();
        Minecraft mc = Minecraft.getInstance();
        Identifier dim = currentDimension();
        boolean wrapX = WildlandsTerrainBridge.isEastWestWrapActive(dim);
        boolean revealAll = mc.player != null && (mc.player.isCreative() || mc.player.isSpectator());
        boolean mapWarp = !AtlasConfig.SERVER_SPEC.isLoaded() || AtlasConfig.MAP_WARP.get();
        int tier = TAtlas.surveyStore().standaloneTier();
        long nowSeconds = mc.level != null ? mc.level.getGameTime() / 20L : 0L;
        int currentDay = mc.level != null ? (int) (mc.level.getOverworldClockTime() / 24000L) : 0;
        UUID viewerId = mc.player != null ? mc.player.getUUID() : null;

        int primaryCol = 0xFF000000 | skin.palette().textPrimary();
        int secondaryCol = 0xFF000000 | skin.palette().textSecondary();
        int accentCol = 0xFF000000 | skin.palette().accent();
        int borderCol = 0xFF000000 | skin.palette().bgBorder();

        int mapLeft = computeMapLeft();
        int mapTop = MAP_TOP;
        int mapRight = this.width - 6;
        int mapBottom = this.height - FOOTER_H - 2;
        int mapW = Math.max(64, mapRight - mapLeft);
        int mapH = Math.max(64, mapBottom - mapTop);

        if (showSidebar) {
            renderSidebar(graphics, 6, mapTop, SIDEBAR_W, mapH, skin, tier, viewerId, dim, wrapX);
        }

        int bpp = PlanetProjection.blocksPerPixel(zoomLevel);
        int mapCenterX = mapLeft + mapW / 2;
        int mapCenterY = mapTop + mapH / 2;
        Waypoint hoveredWaypoint = null;

        graphics.fill(mapLeft, mapTop, mapRight, mapBottom, tier <= 0 ? MapTileRasterizer.UNEXPLORED_MEMORY_BG : MapTileRasterizer.UNEXPLORED_PARCHMENT_BG);
        graphics.enableScissor(mapLeft, mapTop, mapRight, mapBottom);
        try {

        gpuTextures.beginFrame();
        int uploadsBudget = AtlasConfig.CLIENT_SPEC.isLoaded() ? AtlasConfig.TILE_UPLOADS_PER_FRAME.get() : 4;
        MapTileCache.ViewportFrameResult frame = TAtlas.tileCache().collectViewportFrame(
                dim, viewerId, KnowledgeHolder.PLAYER_CIVILIZATION.seedSalt(),
                zoomLevel, centerWorldX, centerWorldZ, mapW, mapH,
                wrapX, mapWarp, revealAll, nowSeconds, currentDay, tier, 0
        );
        this.lastFrameBuildMs = frame.elapsedMillis();

            int planetWidthPx = PlanetProjection.WORLD_WIDTH / bpp;

            for (MapTileCache.TileSnapshot snap : frame.visibleTiles()) {
                Identifier texId = gpuTextures.resolveOrUploadTexture(snap, uploadsBudget);
                int tileOriginX = PlanetProjection.tileOriginBlockX(snap.key().tileX(), zoomLevel, wrapX);
                int tileOriginZ = PlanetProjection.tileOriginBlockZ(snap.key().tileZ(), zoomLevel, wrapX);
                int screenY = mapCenterY + (int) Math.round((tileOriginZ - centerWorldZ) / bpp);

                if (!wrapX) {
                    int screenX = mapCenterX + (int) Math.round((tileOriginX - centerWorldX) / bpp);
                    drawTileQuad(graphics, texId, screenX, screenY);
                } else {
                    double primaryDxBlocks = PlanetProjection.wrappedDx(centerWorldX, tileOriginX + 128.0 * bpp) - 128.0 * bpp;
                    int baseScreenX = mapCenterX + (int) Math.round(primaryDxBlocks / bpp);
                    for (int rep = -1; rep <= 1; rep++) {
                        int sx = baseScreenX + rep * planetWidthPx;
                        if (sx + MapTileRasterizer.TILE_SIZE >= mapLeft && sx <= mapRight) {
                            drawTileQuad(graphics, texId, sx, screenY);
                        }
                    }
                }
            }

            DeterministicWarpField warpField = (!revealAll && mapWarp)
                    ? new DeterministicWarpField(TAtlas.surveyStore().worldSeed(), KnowledgeHolder.PLAYER_CIVILIZATION.seedSalt(), 0L, wrapX)
                    : null;
            DeterministicWarpField.ChunkEpsilonLookup epsLookup = (cx, cz) ->
                    TAtlas.surveyStore().resolveChunk(viewerId, dim, cx, cz, nowSeconds, 0, wrapX).effectiveEpsilon();

            if (viewerId != null) {
                List<WaypointManager.ActiveMemoryMark> marks = TAtlas.waypoints().activeMemoryMarks(viewerId, dim, nowSeconds, 0, wrapX);
                for (WaypointManager.ActiveMemoryMark am : marks) {
                    double wx = am.mark().pos().getX() + 0.5;
                    double wz = am.mark().pos().getZ() + 0.5;
                    if (warpField != null) {
                        var warped = warpField.warpForward(wx, wz, epsLookup);
                        wx = warped.x();
                        wz = warped.z();
                    }
                    int sx = worldToScreenX(wx, mapCenterX, bpp, wrapX);
                    int sy = worldToScreenY(wz, mapCenterY, bpp);
                    float opacity = MemoryDecayModel.opacity(am.strength());
                    MarkerShapeRenderer.drawShape(graphics, OkabeItoPalette.WaypointShape.CHARCOAL_MARK, sx, sy, 0xFFD8C8AC, opacity, false);
                    if (!am.mark().label().isBlank() && opacity >= 0.35f) {
                        graphics.text(this.font, am.mark().label(), sx + 6, sy - 4, 0xFFD8C8AC, true);
                    }
                }
            }

            UUID activeNavId = viewerId != null ? TAtlas.waypoints().activeNavigation(viewerId).map(Waypoint::id).orElse(null) : null;
            boolean mouseInViewport = mouseX >= mapLeft && mouseX <= mapRight && mouseY >= mapTop && mouseY <= mapBottom;
            for (Waypoint wp : TAtlas.waypoints().visibleWaypoints(viewerId, dim)) {
                double wx = wp.pos().getX() + 0.5;
                double wz = wp.pos().getZ() + 0.5;
                if (warpField != null) {
                    var warped = warpField.warpForward(wx, wz, epsLookup);
                    wx = warped.x();
                    wz = warped.z();
                }
                int sx = worldToScreenX(wx, mapCenterX, bpp, wrapX);
                int sy = worldToScreenY(wz, mapCenterY, bpp);
                if (sx >= mapLeft - 12 && sx <= mapRight + 12 && sy >= mapTop - 12 && sy <= mapBottom + 12) {
                    boolean isNav = wp.id().equals(activeNavId);
                    MarkerShapeRenderer.drawShape(graphics, wp.shape(), sx, sy, wp.argb(), 1.0f, isNav);
                    if (zoomLevel <= 5 || isNav) {
                        graphics.text(this.font, wp.name(), sx + 7, sy - 4, primaryCol, true);
                    }
                    if (mouseInViewport && Math.hypot(mouseX - sx, mouseY - sy) <= 8.0) {
                        hoveredWaypoint = wp;
                    }
                }
            }

            if (mc.player != null) {
                double px = mc.player.getX();
                double pz = mc.player.getZ();
                if (warpField != null) {
                    var warped = warpField.warpForward(px, pz, epsLookup);
                    px = warped.x();
                    pz = warped.z();
                }
                int psx = worldToScreenX(px, mapCenterX, bpp, wrapX);
                int psy = worldToScreenY(pz, mapCenterY, bpp);
                graphics.outline(psx - 6, psy - 6, 13, 13, accentCol);
                graphics.fill(psx - 2, psy - 2, psx + 3, psy + 3, 0xFFFFFFFF);
            }

            if (rulerMode && rulerStart != null) {
                BlockPos endPos = rulerEnd != null ? rulerEnd : screenToWorldBlock(mouseX, mouseY, mapCenterX, mapCenterY, bpp, wrapX);
                int x1 = worldToScreenX(rulerStart.getX() + 0.5, mapCenterX, bpp, wrapX);
                int y1 = worldToScreenY(rulerStart.getZ() + 0.5, mapCenterY, bpp);
                int x2 = worldToScreenX(endPos.getX() + 0.5, mapCenterX, bpp, wrapX);
                int y2 = worldToScreenY(endPos.getZ() + 0.5, mapCenterY, bpp);
                drawSimpleLine(graphics, x1, y1, x2, y2, accentCol);
                double dist = PlanetProjection.distance(rulerStart.getX(), rulerStart.getZ(), endPos.getX(), endPos.getZ(), wrapX);
                double bearing = PlanetProjection.bearingDegrees(rulerStart.getX(), rulerStart.getZ(), endPos.getX(), endPos.getZ(), wrapX);
                double km = (dist * PlanetProjection.METERS_PER_BLOCK) / 1000.0;
                String rulerLabel = String.format(Locale.ROOT, "%.0f 格 (%.1f km) · %.1f° (%s)",
                        dist, km, bearing, PlanetProjection.cardinal16(bearing));
                int rlw = this.font.width(rulerLabel);
                int rlx = Math.min(x2 + 8, mapRight - rlw - 10);
                int rly = Math.max(mapTop + 14, y2 - 12);
                graphics.fill(rlx - 4, rly - 3, rlx + rlw + 4, rly + 11, 0xEA141210);
                graphics.outline(rlx - 4, rly - 3, rlw + 8, 14, borderCol);
                graphics.text(this.font, rulerLabel, rlx, rly, accentCol, false);
            }

            int scaleBarPx = 64;
            int scaleBlocks = scaleBarPx * bpp;
            double scaleKm = (scaleBlocks * PlanetProjection.METERS_PER_BLOCK) / 1000.0;
            String scaleTxt = String.format(Locale.ROOT, "%d 格 (≈%.1f km) [L%d]", scaleBlocks, scaleKm, zoomLevel);
            int scaleTxtW = this.font.width(scaleTxt);
            int sbX = mapRight - scaleBarPx - 10;
            int sbY = mapBottom - 9;
            int pillLeft = Math.max(mapLeft + 4, sbX - scaleTxtW - 10);
            graphics.fill(pillLeft, sbY - 11, mapRight - 4, mapBottom - 3, 0xEA161310);
            graphics.outline(pillLeft, sbY - 11, (mapRight - 4) - pillLeft, (mapBottom - 3) - (sbY - 11), borderCol);
            graphics.horizontalLine(sbX, sbX + scaleBarPx, sbY, primaryCol);
            graphics.verticalLine(sbX, sbY - 3, sbY + 2, primaryCol);
            graphics.verticalLine(sbX + scaleBarPx, sbY - 3, sbY + 2, primaryCol);
            graphics.text(this.font, scaleTxt, sbX - scaleTxtW - 6, sbY - 8, primaryCol, false);
        } finally {
            graphics.disableScissor();
        }

        graphics.outline(mapLeft, mapTop, mapW, mapH, borderCol);

        BlockPos hoverWorld = screenToWorldBlock(mouseX, mouseY, mapCenterX, mapCenterY, bpp, wrapX);
        int searchLeft = searchBox != null ? searchBox.getX() : (this.width - 196);
        String headerTxt = this.font.plainSubstrByWidth(
                buildHeaderText(hoverWorld, tier, revealAll, wrapX),
                Math.max(64, searchLeft - 14)
        );
        graphics.text(this.font, headerTxt, 8, 8, primaryCol, false);

        int pCount = viewerId != null ? TAtlas.waypoints().countPersonalWaypoints(viewerId) : 0;
        int pCap = TAtlas.waypoints().personalCap(tier);
        String footerInfo = tier <= 0
                ? String.format(Locale.ROOT, "E0 记忆地图 · 标记 %d/3 · %.1f ms", viewerId != null ? TAtlas.waypoints().rawMemoryMarks(viewerId).size() : 0, lastFrameBuildMs)
                : String.format(Locale.ROOT, "T%d · 路点 %d/%d · %.1f ms", tier, pCount, pCap, lastFrameBuildMs);
        int availFooterW = Math.max(48, this.width - 236);
        String fittedFooter = this.font.plainSubstrByWidth(footerInfo, availFooterW);
        if (this.width > 290) {
            graphics.text(this.font, fittedFooter, this.width - this.font.width(fittedFooter) - 8, this.height - 16, secondaryCol, false);
        }

        graphics.nextStratum();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        if (hoveredWaypoint != null) {
            String meta = hoveredWaypoint.category().id() + " · X=" + hoveredWaypoint.pos().getX() + " Z=" + hoveredWaypoint.pos().getZ();
            List<Component> tip = hoveredWaypoint.note().isPresent()
                    ? List.of(Component.literal(hoveredWaypoint.name()), Component.literal(meta), Component.literal(hoveredWaypoint.note().get()))
                    : List.of(Component.literal(hoveredWaypoint.name()), Component.literal(meta));
            graphics.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
        }
    }

    private void renderSidebar(
            GuiGraphicsExtractor g,
            int x,
            int y,
            int w,
            int h,
            HudSkin skin,
            int tier,
            @Nullable UUID viewerId,
            Identifier dim,
            boolean wrapX
    ) {
        int bgCol = 0xFF000000 | skin.palette().bgFill();
        int borderCol = 0xFF000000 | skin.palette().bgBorder();
        int accentCol = 0xFF000000 | skin.palette().accent();
        int primaryCol = 0xFF000000 | skin.palette().textPrimary();
        int secondaryCol = 0xFF000000 | skin.palette().textSecondary();
        int okCol = 0xFF000000 | skin.palette().ok();

        g.fill(x, y, x + w, y + h, bgCol);
        g.fill(x + 1, y + 1, x + w - 1, y + 18, 0xFF1A1612);
        g.horizontalLine(x + 1, x + w - 2, y + 18, borderCol);
        g.outline(x, y, w, h, borderCol);

        g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        try {
            g.text(this.font, "图层 [Tab]", x + 6, y + 5, accentCol, false);
            g.text(this.font, "☑ 地形底图", x + 6, y + 23, primaryCol, false);
            g.text(this.font, tier <= 0 ? "☑ 记忆标记" : "☑ 路点要素", x + 6, y + 36, primaryCol, false);
            if (WildlandsTerrainBridge.isWildlandsLoaded()) {
                g.text(this.font, "☑ 星球环绕 [W]", x + 6, y + 49, okCol, false);
            } else {
                g.text(this.font, "· 平面回退", x + 6, y + 49, secondaryCol, false);
            }

            g.horizontalLine(x + 4, x + w - 5, y + 63, borderCol);
            g.text(this.font, "图例 (Okabe-Ito)", x + 6, y + 68, accentCol, false);
            int ly = y + 83;
            for (OkabeItoPalette.WaypointCategory cat : List.of(
                    OkabeItoPalette.WaypointCategory.HOME,
                    OkabeItoPalette.WaypointCategory.RESOURCE,
                    OkabeItoPalette.WaypointCategory.DANGER,
                    OkabeItoPalette.WaypointCategory.RUIN,
                    OkabeItoPalette.WaypointCategory.PORT
            )) {
                if (ly + 10 > y + h - 2) {
                    break;
                }
                MarkerShapeRenderer.drawShape(g, cat.defaultShape(), x + 12, ly + 4, cat.defaultArgb(), 1.0f, false);
                String catLabel = this.font.plainSubstrByWidth(cat.id(), w - 28);
                g.text(this.font, catLabel, x + 22, ly, secondaryCol, false);
                ly += 14;
            }
        } finally {
            g.disableScissor();
        }
    }

    private String buildHeaderText(BlockPos hover, int tier, boolean revealAll, boolean wrapX) {
        String styleLabel = tier <= 0 ? "[记忆炭笔图]" : "[T" + tier + " 舆图]";
        if (revealAll || tier >= 10) {
            double lat = PlanetProjection.latDeg(hover.getZ());
            double lon = PlanetProjection.lonDeg(hover.getX(), wrapX);
            return String.format(Locale.ROOT, "%s X=%d Z=%d (%.1f°, %.1f°)", styleLabel, hover.getX(), hover.getZ(), lat, lon);
        }
        if (tier >= 5) {
            double lat = PlanetProjection.latDeg(hover.getZ());
            double lon = PlanetProjection.lonDeg(hover.getX(), wrapX);
            return String.format(Locale.ROOT, "%s 纬 %.1f° 经 %.1f°", styleLabel, lat, lon);
        }
        return styleLabel + (wrapX ? " 星球环绕视图" : " 探索草图");
    }

    private void drawTileQuad(GuiGraphicsExtractor g, @Nullable Identifier texId, int sx, int sy) {
        if (texId != null) {
            g.blit(RenderPipelines.GUI_TEXTURED, texId, sx, sy, 0.0f, 0.0f,
                    MapTileRasterizer.TILE_SIZE, MapTileRasterizer.TILE_SIZE,
                    MapTileRasterizer.TILE_SIZE, MapTileRasterizer.TILE_SIZE);
        } else {
            g.fill(sx, sy, sx + MapTileRasterizer.TILE_SIZE, sy + MapTileRasterizer.TILE_SIZE, 0xFF231E19);
        }
    }

    private int worldToScreenX(double worldX, int mapCenterX, int bpp, boolean wrapX) {
        double dx = wrapX ? PlanetProjection.wrappedDx(centerWorldX, worldX) : (worldX - centerWorldX);
        return mapCenterX + (int) Math.round(dx / bpp);
    }

    private int worldToScreenY(double worldZ, int mapCenterY, int bpp) {
        return mapCenterY + (int) Math.round((worldZ - centerWorldZ) / bpp);
    }

    private BlockPos screenToWorldBlock(double mx, double my, int mapCenterX, int mapCenterY, int bpp, boolean wrapX) {
        double wx = centerWorldX + (mx - mapCenterX) * bpp;
        double wz = centerWorldZ + (my - mapCenterY) * bpp;
        int bx = wrapX ? PlanetProjection.wrapBlockX((int) Math.floor(wx)) : (int) Math.floor(wx);
        int bz = wrapX ? PlanetProjection.clampBlockZ((int) Math.floor(wz)) : (int) Math.floor(wz);
        return new BlockPos(bx, 64, bz);
    }

    private static void drawSimpleLine(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int argb) {
        int steps = Math.max(1, Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)) / 3);
        for (int i = 0; i <= steps; i++) {
            int px = x0 + (x1 - x0) * i / steps;
            int py = y0 + (y1 - y0) * i / steps;
            g.fill(px - 1, py - 1, px + 2, py + 2, argb);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        int mapLeft = computeMapLeft();
        int mapTop = MAP_TOP;
        int mapRight = this.width - 6;
        int mapBottom = this.height - FOOTER_H - 2;
        if (event.x() < mapLeft || event.x() > mapRight || event.y() < mapTop || event.y() > mapBottom) {
            return false;
        }
        int mapCenterX = mapLeft + (mapRight - mapLeft) / 2;
        int mapCenterY = mapTop + (mapBottom - mapTop) / 2;
        int bpp = PlanetProjection.blocksPerPixel(zoomLevel);
        Identifier dim = currentDimension();
        boolean wrapX = WildlandsTerrainBridge.isEastWestWrapActive(dim);
        BlockPos clickedBlock = screenToWorldBlock(event.x(), event.y(), mapCenterX, mapCenterY, bpp, wrapX);

        if (event.button() == 0) {
            if (doubleClick) {
                centerWorldX = clickedBlock.getX();
                centerWorldZ = clickedBlock.getZ();
                adjustZoom(-1);
                return true;
            }
            if (rulerMode) {
                if (rulerStart == null || rulerEnd != null) {
                    rulerStart = clickedBlock;
                    rulerEnd = null;
                } else {
                    rulerEnd = clickedBlock;
                }
                return true;
            }
            this.panning = true;
            return true;
        }

        Minecraft mc = Minecraft.getInstance();
        UUID viewerId = mc.player != null ? mc.player.getUUID() : new UUID(0L, 1L);
        long nowSeconds = mc.level != null ? mc.level.getGameTime() / 20L : 0L;
        long currentDay = mc.level != null ? mc.level.getOverworldClockTime() / 24000L : 0L;

        if (event.button() == 2) {
            TAtlas.waypoints().placePing(viewerId, dim, clickedBlock, nowSeconds, currentDay, wrapX);
            return true;
        }

        if (event.button() == 1) {
            if (TAtlas.surveyStore().standaloneTier() <= 0) {
                TAtlas.waypoints().placeMemoryMark(viewerId, dim, clickedBlock, nowSeconds, "", wrapX);
                TAtlas.saveClientState();
                if (mc.player != null) {
                    mc.player.sendOverlayMessage(Component.translatable("msg.tcatlas.memory_mark_placed"));
                }
                return true;
            }
            for (Waypoint wp : TAtlas.waypoints().visibleWaypoints(viewerId, dim)) {
                int sx = worldToScreenX(wp.pos().getX() + 0.5, mapCenterX, bpp, wrapX);
                int sy = worldToScreenY(wp.pos().getZ() + 0.5, mapCenterY, bpp);
                if (Math.hypot(event.x() - sx, event.y() - sy) <= 8.0) {
                    mc.setScreenAndShow(new WaypointDialogScreen(this, dim, wp.pos(), wp));
                    return true;
                }
            }
            mc.setScreenAndShow(new WaypointDialogScreen(this, dim, clickedBlock, null));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (panning && event.button() == 0) {
            int bpp = PlanetProjection.blocksPerPixel(zoomLevel);
            boolean wrapX = WildlandsTerrainBridge.isEastWestWrapActive(currentDimension());
            centerWorldX -= dragX * bpp;
            if (wrapX) {
                centerWorldX = PlanetProjection.wrapX(centerWorldX);
            }
            centerWorldZ -= dragY * bpp;
            if (wrapX) {
                centerWorldZ = Math.clamp(centerWorldZ, -PlanetProjection.Z_LIMIT, PlanetProjection.Z_LIMIT);
            }
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (panning && event.button() == 0) {
            panning = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0.0) {
            adjustZoom(scrollY > 0.0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (searchBox != null && searchBox.isFocused()) {
            if (event.key() == InputConstants.KEY_ESCAPE) {
                searchBox.setFocused(false);
                return true;
            }
            return super.keyPressed(event);
        }
        if (event.key() == InputConstants.KEY_M) {
            onClose();
            return true;
        }
        if (event.key() == InputConstants.KEY_SPACE) {
            recenterOnPlayer();
            return true;
        }
        if (event.key() == InputConstants.KEY_TAB) {
            showSidebar = !showSidebar;
            rebuildWidgets();
            return true;
        }
        if (event.key() == InputConstants.KEY_SLASH && searchBox != null) {
            setFocused(searchBox);
            searchBox.setFocused(true);
            return true;
        }
        if (event.key() == InputConstants.KEY_1) {
            rulerMode = !rulerMode;
            rebuildWidgets();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return AtlasConfig.CLIENT_SPEC.isLoaded() && AtlasConfig.PAUSE_SINGLEPLAYER.get();
    }

    private static Identifier currentDimension() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.dimension().identifier() : Identifier.withDefaultNamespace("overworld");
    }
}
