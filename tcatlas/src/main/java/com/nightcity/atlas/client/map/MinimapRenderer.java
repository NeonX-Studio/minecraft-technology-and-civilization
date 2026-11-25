package com.nightcity.atlas.client.map;

import com.mojang.blaze3d.platform.NativeImage;
import com.nightcity.atlas.AtlasConfig;
import com.nightcity.atlas.TAtlas;
import com.nightcity.atlas.api.HudSkin;
import com.nightcity.atlas.api.SurveyLevel;
import com.nightcity.atlas.api.Waypoint;
import com.nightcity.atlas.compat.wildlands.WildlandsTerrainBridge;
import com.nightcity.atlas.map.ChunkTerrainSummary;
import com.nightcity.atlas.map.MapTileRasterizer;
import com.nightcity.atlas.map.PlanetProjection;
import com.nightcity.atlas.map.SurveyAtlasStore;
import com.nightcity.atlas.waypoint.WaypointManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * HUD Minimap renderer (22 §5.15, 40 §4.2).
 * Strictly gated off in E0-E2 by default; in P1 it only renders when the tcatlas debug switch is enabled
 * (or when tier >= 3 in later phases / creative mode when debug switch permits).
 */
public final class MinimapRenderer implements AutoCloseable {
    public enum Mode {
        OFF,
        NORTH_UP,
        ROTATE;

        public Mode next() {
            return switch (this) {
                case OFF -> NORTH_UP;
                case NORTH_UP -> ROTATE;
                case ROTATE -> OFF;
            };
        }
    }

    public enum Shape {
        CIRCLE,
        SQUARE
    }

    public record ProjectedMarker(
            Waypoint waypoint,
            double offsetX,
            double offsetY,
            boolean clampedToEdge,
            double distanceBlocks
    ) {
    }

    public record RimPoint(int dx, int dy) {
    }

    private static final int TEXTURE_SIZE = 128;
    private static final Identifier MINIMAP_TEX_ID = Identifier.fromNamespaceAndPath(TAtlas.MOD_ID, "dynamic/minimap_canvas");

    private Mode mode = Mode.OFF;
    @Nullable
    private DynamicTexture dynamicTexture;
    private int lastCenterBlockX = Integer.MIN_VALUE;
    private int lastCenterBlockZ = Integer.MIN_VALUE;
    private float lastRasterYaw = Float.NaN;
    private long lastRasterTick = -100L;

    public Mode mode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode == null ? Mode.OFF : mode;
    }

    public Mode cycleMode() {
        this.mode = this.mode.next();
        return this.mode;
    }

    /**
     * P1 gating rule (40 §4.2, 22 §5.15): E0-E2 has NO minimap; in P1 it only activates when debug_minimap is enabled
     * (or tier >= 3).
     */
    public static boolean isAllowedToRender(int tier, boolean debugSwitchEnabled) {
        if (debugSwitchEnabled) {
            return true;
        }
        return tier >= 3;
    }

    public static SurveyLevel minimumSurveyLevelForTier(int tier, boolean debugSwitchEnabled) {
        if (debugSwitchEnabled && tier < 3) {
            return SurveyLevel.MEMORY;
        }
        return tier <= 3 ? SurveyLevel.SURVEYED : SurveyLevel.SKETCH;
    }

    public static int maxRadiusBlocksForTier(int tier, boolean debugSwitchEnabled) {
        if (debugSwitchEnabled && tier < 3) {
            return 64;
        }
        return switch (Math.clamp(tier, 0, 10)) {
            case 0, 1, 2 -> 0;
            case 3 -> 64;
            case 4, 5 -> 128;
            case 6, 7 -> 192;
            case 8 -> 256;
            case 9 -> 384;
            default -> 512;
        };
    }

    /**
     * Pure CPU rasterizer for minimap terrain with strict surveyed-area clipping and circle/square alpha mask.
     */
    public static int[] buildMinimapBuffer(
            SurveyAtlasStore store,
            @Nullable UUID viewerId,
            Identifier dimension,
            double centerX,
            double centerZ,
            float playerYawDeg,
            Mode mode,
            Shape shape,
            int radiusBlocks,
            int tier,
            boolean debugSwitchEnabled,
            boolean wrapX,
            long nowGameSeconds,
            int currentDay
    ) {
        int[] buf = new int[TEXTURE_SIZE * TEXTURE_SIZE];
        if (mode == Mode.OFF || !isAllowedToRender(tier, debugSwitchEnabled) || radiusBlocks <= 0) {
            return buf;
        }
        SurveyLevel minSl = minimumSurveyLevelForTier(tier, debugSwitchEnabled);
        double halfTex = TEXTURE_SIZE * 0.5;
        double scaleBlocksPerPx = (radiusBlocks * 2.0) / TEXTURE_SIZE;
        double rad = mode == Mode.ROTATE ? Math.toRadians(playerYawDeg + 180.0) : 0.0;
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        for (int py = 0; py < TEXTURE_SIZE; py++) {
            double dy = (py + 0.5) - halfTex;
            for (int px = 0; px < TEXTURE_SIZE; px++) {
                double dx = (px + 0.5) - halfTex;
                if (shape == Shape.CIRCLE && (dx * dx + dy * dy) > (halfTex - 1.0) * (halfTex - 1.0)) {
                    buf[(py * TEXTURE_SIZE) + px] = 0x00000000;
                    continue;
                }
                double worldDx = mode == Mode.ROTATE ? (dx * cos - dy * sin) * scaleBlocksPerPx : dx * scaleBlocksPerPx;
                double worldDz = mode == Mode.ROTATE ? (dx * sin + dy * cos) * scaleBlocksPerPx : dy * scaleBlocksPerPx;
                int bx = wrapX ? PlanetProjection.wrapBlockX((int) Math.floor(centerX + worldDx)) : (int) Math.floor(centerX + worldDx);
                int bz = wrapX ? PlanetProjection.clampBlockZ((int) Math.floor(centerZ + worldDz)) : (int) Math.floor(centerZ + worldDz);
                int cx = bx >> 4;
                int cz = bz >> 4;

                SurveyAtlasStore.ResolvedChunk resolved = store.resolveChunk(viewerId, dimension, cx, cz, nowGameSeconds, 0, wrapX);
                if (resolved.effectiveSl().code() < minSl.code()) {
                    // Strict clipping of unsurveyed area
                    int blank = ((px ^ py) & 7) == 0 ? 0xCC2A231C : 0xCC211B15;
                    buf[(py * TEXTURE_SIZE) + px] = blank;
                    continue;
                }

                ChunkTerrainSummary summary = store.getOrCreateSummary(dimension, cx, cz, wrapX);
                int col = summary.argb(bx & 15, bz & 15);
                if (resolved.effectiveSl() == SurveyLevel.MEMORY) {
                    col = MapTileRasterizer.blendArgb(0xFF211B15, col, (float) Math.clamp(resolved.memoryStrength(), 0.2, 0.9));
                }
                buf[(py * TEXTURE_SIZE) + px] = (col & 0x00FFFFFF) | 0xEE000000;
            }
        }
        return buf;
    }

    /**
     * Projects a waypoint onto minimap widget coordinates relative to widget center, clamping distant waypoints onto rim.
     */
    public static ProjectedMarker projectWaypoint(
            Waypoint wp,
            double centerX,
            double centerZ,
            float playerYawDeg,
            Mode mode,
            Shape shape,
            int radiusBlocks,
            int widgetHalfSizePx,
            boolean wrapX
    ) {
        double dxBlocks = wrapX ? PlanetProjection.wrappedDx(centerX, wp.pos().getX() + 0.5) : ((wp.pos().getX() + 0.5) - centerX);
        double dzBlocks = (wp.pos().getZ() + 0.5) - centerZ;
        double distBlocks = Math.hypot(dxBlocks, dzBlocks);

        double rx = dxBlocks;
        double ry = dzBlocks;
        if (mode == Mode.ROTATE) {
            double rad = -Math.toRadians(playerYawDeg + 180.0);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);
            rx = dxBlocks * cos - dzBlocks * sin;
            ry = dxBlocks * sin + dzBlocks * cos;
        }

        double pxPerBlock = (double) widgetHalfSizePx / Math.max(1, radiusBlocks);
        double sx = rx * pxPerBlock;
        double sy = ry * pxPerBlock;
        double maxRim = Math.max(4.0, widgetHalfSizePx - 5.0);
        boolean clamped = false;

        if (shape == Shape.CIRCLE) {
            double r = Math.hypot(sx, sy);
            if (r > maxRim && r > 1e-6) {
                sx = (sx / r) * maxRim;
                sy = (sy / r) * maxRim;
                clamped = true;
            }
        } else {
            double maxAbs = Math.max(Math.abs(sx), Math.abs(sy));
            if (maxAbs > maxRim && maxAbs > 1e-6) {
                sx = (sx / maxAbs) * maxRim;
                sy = (sy / maxAbs) * maxRim;
                clamped = true;
            }
        }
        return new ProjectedMarker(wp, sx, sy, clamped, distBlocks);
    }

    /**
     * Computes the North ('N') badge offset from the minimap center along the border.
     */
    public static RimPoint computeNorthIndicator(float playerYawDeg, Mode mode, Shape shape, int widgetHalfSizePx) {
        int rim = Math.max(6, widgetHalfSizePx - 4);
        if (mode != Mode.ROTATE) {
            return new RimPoint(0, -rim);
        }
        // World North is (0, -1). Rotate by -(yaw + 180)
        double rad = -Math.toRadians(playerYawDeg + 180.0);
        double nx = Math.sin(rad);
        double ny = -Math.cos(rad);
        if (shape == Shape.CIRCLE) {
            return new RimPoint((int) Math.round(nx * rim), (int) Math.round(ny * rim));
        }
        double maxAbs = Math.max(Math.abs(nx), Math.abs(ny));
        double scale = maxAbs > 1e-6 ? (rim / maxAbs) : rim;
        return new RimPoint((int) Math.round(nx * scale), (int) Math.round(ny * scale));
    }

    public boolean shouldRenderInHud() {
        boolean debugOn = AtlasConfig.isMinimapDebugEnabled();
        int tier = TAtlas.surveyStore().standaloneTier();
        if (!isAllowedToRender(tier, debugOn)) {
            return false;
        }
        if (debugOn && mode == Mode.OFF) {
            return true; // when explicitly turned on via debug switch, render NORTH_UP unless cycled
        }
        return mode != Mode.OFF;
    }

    public void renderHud(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            HudSkin skin
    ) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        boolean debugOn = AtlasConfig.isMinimapDebugEnabled();
        int tier = TAtlas.surveyStore().standaloneTier();
        if (!isAllowedToRender(tier, debugOn)) {
            return;
        }
        Mode activeMode = (mode == Mode.OFF && debugOn) ? Mode.NORTH_UP : mode;
        if (activeMode == Mode.OFF) {
            return;
        }

        Shape shape = resolveConfiguredShape();
        Identifier dim = mc.level.dimension().identifier();
        boolean wrapX = WildlandsTerrainBridge.isEastWestWrapActive(dim);
        int radiusBlocks = Math.max(32, maxRadiusBlocksForTier(tier, debugOn));
        long nowSeconds = mc.level.getGameTime() / 20L;
        int currentDay = (int) (mc.level.getOverworldClockTime() / 24000L);
        int bx = player.getBlockX();
        int bz = player.getBlockZ();
        float yaw = player.getYRot();

        ensureTextureInitialized(mc);
        if (dynamicTexture != null && (Math.abs(bx - lastCenterBlockX) >= 2
                || Math.abs(bz - lastCenterBlockZ) >= 2
                || (activeMode == Mode.ROTATE && Math.abs(yaw - lastRasterYaw) >= 2.5f)
                || (mc.level.getGameTime() - lastRasterTick) >= 20L)) {
            lastCenterBlockX = bx;
            lastCenterBlockZ = bz;
            lastRasterYaw = yaw;
            lastRasterTick = mc.level.getGameTime();
            int[] pixels = buildMinimapBuffer(
                    TAtlas.surveyStore(), player.getUUID(), dim,
                    player.getX(), player.getZ(), yaw, activeMode, shape,
                    radiusBlocks, tier, debugOn, wrapX, nowSeconds, currentDay
            );
            NativeImage img = dynamicTexture.getPixels();
            if (img != null) {
                for (int py = 0; py < TEXTURE_SIZE; py++) {
                    int row = py * TEXTURE_SIZE;
                    for (int px = 0; px < TEXTURE_SIZE; px++) {
                        img.setPixel(px, py, pixels[row + px]);
                    }
                }
                dynamicTexture.upload();
            }
        }

        if (shape == Shape.SQUARE) {
            graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, skin.palette().bgBorder());
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, MINIMAP_TEX_ID, x, y, 0.0f, 0.0f, width, height, TEXTURE_SIZE, TEXTURE_SIZE);
        if (shape == Shape.SQUARE) {
            graphics.outline(x, y, width, height, skin.palette().bgBorder());
        }

        int cx = x + width / 2;
        int cy = y + height / 2;
        int halfSize = Math.min(width, height) / 2;

        // Project waypoints & clamp distant ones onto rim
        WaypointManager wm = TAtlas.waypoints();
        UUID activeNavId = wm.activeNavigation(player.getUUID()).map(Waypoint::id).orElse(null);
        List<Waypoint> wps = new ArrayList<>(wm.visibleWaypoints(player.getUUID(), dim));
        wps.sort(Comparator.comparingDouble(w -> PlanetProjection.distance(player.getX(), player.getZ(), w.pos().getX(), w.pos().getZ(), wrapX)));
        int maxMarkers = AtlasConfig.CLIENT_SPEC.isLoaded() ? AtlasConfig.MINIMAP_MAX_MARKERS.get() : 64;
        int drawn = 0;
        for (Waypoint wp : wps) {
            if (drawn >= maxMarkers) {
                break;
            }
            ProjectedMarker pm = projectWaypoint(wp, player.getX(), player.getZ(), yaw, activeMode, shape, radiusBlocks, halfSize, wrapX);
            boolean isNav = wp.id().equals(activeNavId);
            if (pm.clampedToEdge() && !isNav && wp.category() != com.nightcity.atlas.api.OkabeItoPalette.WaypointCategory.HOME && pm.distanceBlocks() > radiusBlocks * 4.0) {
                continue;
            }
            MarkerShapeRenderer.drawShape(
                    graphics, wp.shape(),
                    cx + (int) Math.round(pm.offsetX()),
                    cy + (int) Math.round(pm.offsetY()),
                    wp.argb(), pm.clampedToEdge() ? 0.85f : 1.0f, isNav
            );
            drawn++;
        }

        // Center player arrow
        graphics.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFF111111);
        graphics.fill(cx - 1, cy - 2, cx + 2, cy + 2, 0xFFFFFFFF);

        // North indicator on border
        RimPoint north = computeNorthIndicator(yaw, activeMode, shape, halfSize);
        int nx = cx + north.dx();
        int ny = cy + north.dy();
        graphics.fill(nx - 4, ny - 4, nx + 5, ny + 5, 0xDD1A1815);
        graphics.text(mc.font, "N", nx - 2, ny - 3, skin.palette().accent(), false);
    }

    private static Shape resolveConfiguredShape() {
        if (AtlasConfig.CLIENT_SPEC.isLoaded()) {
            String raw = AtlasConfig.MINIMAP_SHAPE.get();
            if ("square".equalsIgnoreCase(raw)) {
                return Shape.SQUARE;
            }
        }
        return Shape.CIRCLE;
    }

    private void ensureTextureInitialized(Minecraft mc) {
        if (dynamicTexture == null) {
            dynamicTexture = new DynamicTexture(() -> "tcatlas_minimap", TEXTURE_SIZE, TEXTURE_SIZE, true);
            mc.getTextureManager().register(MINIMAP_TEX_ID, dynamicTexture);
        }
    }

    @Override
    public void close() {
        if (dynamicTexture != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                mc.getTextureManager().release(MINIMAP_TEX_ID);
            }
            dynamicTexture.close();
            dynamicTexture = null;
        }
    }
}
