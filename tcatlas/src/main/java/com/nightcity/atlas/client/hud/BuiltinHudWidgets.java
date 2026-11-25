package com.nightcity.atlas.client.hud;

import com.nightcity.atlas.AtlasConfig;
import com.nightcity.atlas.TAtlas;
import com.nightcity.atlas.api.HudAnchor;
import com.nightcity.atlas.api.HudApi;
import com.nightcity.atlas.api.HudLevel;
import com.nightcity.atlas.api.HudSkin;
import com.nightcity.atlas.api.HudWidget;
import com.nightcity.atlas.api.Waypoint;
import com.nightcity.atlas.client.AtlasClient;
import com.nightcity.atlas.client.map.MarkerShapeRenderer;
import com.nightcity.atlas.client.map.MinimapRenderer;
import com.nightcity.atlas.compat.wildlands.WildlandsTerrainBridge;
import com.nightcity.atlas.map.PlanetProjection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class BuiltinHudWidgets {
    private static final int SLEEK_BG = 0xAD161412;
    private static final int SLEEK_BORDER_ALPHA = 0x78000000;

    private static final class ActiveToast {
        final HudApi.Toast toast;
        int remainingTicks;

        ActiveToast(HudApi.Toast toast, int remainingTicks) {
            this.toast = toast;
            this.remainingTicks = remainingTicks;
        }
    }

    private static final List<ActiveToast> ACTIVE_TOASTS = new ArrayList<>();
    private static volatile boolean toastFieldResolved = false;
    @Nullable
    private static volatile Field rawToastQueueField;

    @Nullable
    private static volatile Identifier lastObservedEra = null;
    private static volatile int eraBannerTicks = 0;
    private static volatile int eraBannerTotalTicks = 120;
    private static volatile Component eraBannerTitle = Component.empty();
    @Nullable
    private static volatile Component eraBannerSubtitle = null;

    public record SimpleHudWidget(
            Identifier id,
            HudAnchor defaultAnchor,
            int defaultOffsetX,
            int defaultOffsetY,
            HudLevel minLevel,
            int width,
            int height
    ) implements HudWidget {
        @Override
        public HudSize measure(HudContext ctx) {
            return new HudSize(width, height);
        }
    }

    public static final HudWidget MINIMAP = new HudWidget() {
        private final Identifier widgetId = Identifier.fromNamespaceAndPath(TAtlas.MOD_ID, "minimap");

        @Override
        public Identifier id() {
            return widgetId;
        }

        @Override
        public HudAnchor defaultAnchor() {
            return HudAnchor.TOP_RIGHT;
        }

        @Override
        public int defaultOffsetX() {
            return -8;
        }

        @Override
        public int defaultOffsetY() {
            return 8;
        }

        @Override
        public HudLevel minLevel() {
            return HudLevel.STANDARD;
        }

        @Override
        public HudSize measure(HudContext ctx) {
            return new HudSize(112, 112);
        }

        @Override
        public boolean shouldRender(HudContext ctx) {
            if (ctx.editPreview()) {
                return true;
            }
            if (!ctx.level().includes(minLevel())) {
                return false;
            }
            return MinimapRenderer.isAllowedToRender(
                    TAtlas.surveyStore().standaloneTier(),
                    AtlasConfig.isMinimapDebugEnabled()
            );
        }

        @Override
        public void render(GuiGraphicsExtractor graphics, int x, int y, int width, int height, HudContext ctx) {
            if (ctx.editPreview()) {
                HudWidget.super.render(graphics, x, y, width, height, ctx);
                return;
            }
            MinimapRenderer renderer = AtlasClient.minimapRenderer();
            if (renderer.shouldRenderInHud()) {
                renderer.renderHud(graphics, x, y, width, height, ctx.skin());
            }
        }
    };

    public static final HudWidget COMPASS_BAR = new HudWidget() {
        private final Identifier widgetId = Identifier.fromNamespaceAndPath(TAtlas.MOD_ID, "compass_bar");

        @Override
        public Identifier id() {
            return widgetId;
        }

        @Override
        public HudAnchor defaultAnchor() {
            return HudAnchor.TOP_CENTER;
        }

        @Override
        public int defaultOffsetX() {
            return 0;
        }

        @Override
        public int defaultOffsetY() {
            return 6;
        }

        @Override
        public HudLevel minLevel() {
            return HudLevel.STANDARD;
        }

        @Override
        public HudSize measure(HudContext ctx) {
            return new HudSize(200, 15);
        }

        @Override
        public boolean shouldRender(HudContext ctx) {
            return ctx.editPreview() || ctx.level().includes(HudLevel.STANDARD);
        }

        @Override
        public void render(GuiGraphicsExtractor graphics, int x, int y, int width, int height, HudContext ctx) {
            if (ctx.editPreview()) {
                HudWidget.super.render(graphics, x, y, width, height, ctx);
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null || mc.level == null) {
                return;
            }
            HudSkin skin = ctx.skin();
            HudSkin.Palette pal = skin.palette();
            Font font = mc.font;

            int borderCol = (pal.bgBorder() & 0x00FFFFFF) | SLEEK_BORDER_ALPHA;
            graphics.fill(x, y, x + width, y + height, SLEEK_BG);
            graphics.outline(x, y, width, height, borderCol);

            float yaw = mc.player.getViewYRot(ctx.partialTick());
            double headingDeg = ((yaw + 180.0) % 360.0 + 360.0) % 360.0;
            int cx = x + width / 2;
            int halfSpanPx = Math.max(24, (width - 12) / 2);

            graphics.enableScissor(x + 2, y + 1, x + width - 2, y + height - 1);
            int minorTickCol = (pal.textSecondary() & 0x00FFFFFF) | 0x66000000;
            for (int deg = 0; deg < 360; deg += 15) {
                double delta = shortestSignedAngleDeg(headingDeg, deg);
                if (Math.abs(delta) > 92.0) {
                    continue;
                }
                int tx = cx + (int) Math.round((delta / 90.0) * halfSpanPx);
                if (deg % 90 == 0) {
                    String label = switch (deg) {
                        case 0 -> "N";
                        case 90 -> "E";
                        case 180 -> "S";
                        default -> "W";
                    };
                    int col = deg == 0 ? pal.accent() : pal.textPrimary();
                    graphics.fill(tx, y + 1, tx + 1, y + 3, col);
                    int lw = font.width(label);
                    graphics.text(font, label, tx - lw / 2, y + 4, col, true);
                } else if (deg % 45 == 0) {
                    String label = switch (deg) {
                        case 45 -> "NE";
                        case 135 -> "SE";
                        case 225 -> "SW";
                        default -> "NW";
                    };
                    graphics.fill(tx, y + 1, tx + 1, y + 3, minorTickCol);
                    int lw = font.width(label);
                    graphics.text(font, label, tx - lw / 2, y + 4, pal.textSecondary(), false);
                } else {
                    graphics.fill(tx, y + 1, tx + 1, y + 3, minorTickCol);
                    graphics.fill(tx, y + height - 3, tx + 1, y + height - 1, minorTickCol);
                }
            }

            Identifier dim = mc.level.dimension().identifier();
            Optional<Waypoint> navOpt = TAtlas.waypoints().activeNavigation(mc.player.getUUID());
            if (navOpt.isPresent() && navOpt.get().dimension().equals(dim)) {
                Waypoint nav = navOpt.get();
                boolean wrapX = WildlandsTerrainBridge.isEastWestWrapActive(dim);
                double wpBrg = PlanetProjection.bearingDegrees(
                        mc.player.getX(), mc.player.getZ(),
                        nav.pos().getX() + 0.5, nav.pos().getZ() + 0.5, wrapX
                );
                double wpDelta = shortestSignedAngleDeg(headingDeg, wpBrg);
                boolean clamped = Math.abs(wpDelta) > 86.0;
                double clampedDelta = Math.clamp(wpDelta, -86.0, 86.0);
                int mx = cx + (int) Math.round((clampedDelta / 90.0) * halfSpanPx);
                MarkerShapeRenderer.drawShape(graphics, nav.shape(), mx, y + height / 2, nav.argb(), clamped ? 0.85f : 1.0f, true);
            }
            graphics.disableScissor();

            graphics.fill(cx - 2, y, cx + 3, y + 1, pal.accent());
            graphics.fill(cx - 1, y + 1, cx + 2, y + 2, pal.accent());
            graphics.fill(cx, y + 2, cx + 1, y + 3, pal.accent());
            graphics.fill(cx, y + height - 2, cx + 1, y + height, pal.accent());
        }
    };
    public static final HudWidget STATUS_STRIP = new HudWidget() {
        private final Identifier widgetId = Identifier.fromNamespaceAndPath(TAtlas.MOD_ID, "status_strip");

        @Override
        public Identifier id() {
            return widgetId;
        }

        @Override
        public HudAnchor defaultAnchor() {
            return HudAnchor.TOP_LEFT;
        }

        @Override
        public int defaultOffsetX() {
            return 6;
        }

        @Override
        public int defaultOffsetY() {
            return 26;
        }

        @Override
        public HudLevel minLevel() {
            return HudLevel.MINIMAL;
        }

        @Override
        public HudSize measure(HudContext ctx) {
            return new HudSize(156, 15);
        }

        @Override
        public boolean shouldRender(HudContext ctx) {
            if (ctx.editPreview()) {
                return true;
            }
            if (!ctx.level().includes(minLevel())) {
                return false;
            }
            return resolveActiveNavigation().isPresent();
        }

        @Override
        public void render(GuiGraphicsExtractor graphics, int x, int y, int width, int height, HudContext ctx) {
            if (ctx.editPreview()) {
                HudWidget.super.render(graphics, x, y, width, height, ctx);
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null || mc.level == null) {
                return;
            }
            Optional<Waypoint> navOpt = resolveActiveNavigation();
            if (navOpt.isEmpty()) {
                return;
            }
            Waypoint nav = navOpt.get();
            Identifier dim = mc.level.dimension().identifier();
            boolean wrapX = WildlandsTerrainBridge.isEastWestWrapActive(dim);
            double dist = PlanetProjection.distance(
                    mc.player.getX(), mc.player.getZ(),
                    nav.pos().getX() + 0.5, nav.pos().getZ() + 0.5, wrapX
            );
            double brg = PlanetProjection.bearingDegrees(
                    mc.player.getX(), mc.player.getZ(),
                    nav.pos().getX() + 0.5, nav.pos().getZ() + 0.5, wrapX
            );
            int tier = TAtlas.surveyStore().standaloneTier();
            String distStr = tier <= 3
                    ? PlanetProjection.formatE1TravelDistance(dist)
                    : (tier < 10 ? (Math.round(dist / 10.0) * 10 + " m") : String.format(Locale.ROOT, "%d m", Math.round(dist)));
            String navText = nav.name() + " · " + PlanetProjection.cardinal8(brg) + " " + distStr;

            HudSkin.Palette pal = ctx.skin().palette();
            int pillW = mc.font.width(navText) + 22;
            int pillH = Math.max(14, height);
            int borderCol = (pal.accent() & 0x00FFFFFF) | 0x88000000;
            graphics.fill(x, y, x + pillW, y + pillH, SLEEK_BG);
            graphics.outline(x, y, pillW, pillH, borderCol);
            MarkerShapeRenderer.drawShape(graphics, nav.shape(), x + 8, y + pillH / 2, nav.argb(), 1.0f, true);
            graphics.text(mc.font, navText, x + 16, y + (pillH - 8) / 2, pal.textPrimary(), true);
        }
    };

    public static final HudWidget ERA_BANNER = new HudWidget() {
        private final Identifier widgetId = Identifier.fromNamespaceAndPath(TAtlas.MOD_ID, "era_banner");

        @Override
        public Identifier id() {
            return widgetId;
        }

        @Override
        public HudAnchor defaultAnchor() {
            return HudAnchor.TOP_CENTER;
        }

        @Override
        public int defaultOffsetX() {
            return 0;
        }

        @Override
        public int defaultOffsetY() {
            return 26;
        }

        @Override
        public HudLevel minLevel() {
            return HudLevel.MINIMAL;
        }

        @Override
        public HudSize measure(HudContext ctx) {
            return new HudSize(176, 24);
        }

        @Override
        public boolean shouldRender(HudContext ctx) {
            if (ctx.editPreview()) {
                return true;
            }
            return eraBannerTicks > 0 && ctx.level().includes(minLevel());
        }

        @Override
        public void render(GuiGraphicsExtractor graphics, int x, int y, int width, int height, HudContext ctx) {
            if (ctx.editPreview()) {
                HudWidget.super.render(graphics, x, y, width, height, ctx);
                return;
            }
            if (eraBannerTicks <= 0) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.font == null) {
                return;
            }
            float fade = 1.0f;
            if (eraBannerTicks < 20) {
                fade = eraBannerTicks / 20.0f;
            } else if (eraBannerTotalTicks - eraBannerTicks < 12) {
                fade = (eraBannerTotalTicks - eraBannerTicks) / 12.0f;
            }
            int alpha = Math.clamp(Math.round(fade * 210.0f), 0, 220);
            if (alpha <= 8) {
                return;
            }
            HudSkin.Palette pal = ctx.skin().palette();
            int bg = (alpha << 24) | 0x161412;
            int border = (alpha << 24) | (pal.accent() & 0x00FFFFFF);
            int textAlpha = Math.clamp(Math.round(fade * 255.0f), 16, 255) << 24;

            graphics.fill(x, y, x + width, y + height, bg);
            graphics.outline(x, y, width, height, border);
            Component sub = eraBannerSubtitle;
            if (sub != null) {
                graphics.centeredText(mc.font, eraBannerTitle, x + width / 2, y + 4, textAlpha | (pal.accent() & 0x00FFFFFF));
                graphics.centeredText(mc.font, sub, x + width / 2, y + 14, textAlpha | (pal.textPrimary() & 0x00FFFFFF));
            } else {
                graphics.centeredText(mc.font, eraBannerTitle, x + width / 2, y + (height - 8) / 2, textAlpha | (pal.accent() & 0x00FFFFFF));
            }
        }
    };
    public static final HudWidget TOAST_STACK = new HudWidget() {
        private final Identifier widgetId = Identifier.fromNamespaceAndPath(TAtlas.MOD_ID, "toast_stack");

        @Override
        public Identifier id() {
            return widgetId;
        }

        @Override
        public HudAnchor defaultAnchor() {
            return HudAnchor.BOTTOM_RIGHT;
        }

        @Override
        public int defaultOffsetX() {
            return -8;
        }

        @Override
        public int defaultOffsetY() {
            return -28;
        }

        @Override
        public HudLevel minLevel() {
            return HudLevel.MINIMAL;
        }

        @Override
        public HudSize measure(HudContext ctx) {
            return new HudSize(168, 48);
        }

        @Override
        public boolean shouldRender(HudContext ctx) {
            if (ctx.editPreview()) {
                return true;
            }
            if (!ctx.level().includes(minLevel())) {
                return false;
            }
            drainPendingToasts();
            synchronized (ACTIVE_TOASTS) {
                return !ACTIVE_TOASTS.isEmpty();
            }
        }

        @Override
        public void render(GuiGraphicsExtractor graphics, int x, int y, int width, int height, HudContext ctx) {
            if (ctx.editPreview()) {
                HudWidget.super.render(graphics, x, y, width, height, ctx);
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.font == null) {
                return;
            }
            drainPendingToasts();
            List<ActiveToast> snapshot;
            synchronized (ACTIVE_TOASTS) {
                if (ACTIVE_TOASTS.isEmpty()) {
                    return;
                }
                snapshot = List.copyOf(ACTIVE_TOASTS);
            }
            HudSkin.Palette pal = ctx.skin().palette();
            int cursorBottom = y + height;
            int shown = 0;
            for (int i = snapshot.size() - 1; i >= 0 && shown < 3; i--, shown++) {
                ActiveToast entry = snapshot.get(i);
                HudApi.Toast t = entry.toast;
                int cardH = t.body() != null ? 24 : 16;
                int cardY = cursorBottom - cardH;
                int stripe = switch (t.priority()) {
                    case SUCCESS -> pal.ok();
                    case WARN -> pal.warn();
                    case ALERT -> pal.danger();
                    case INFO -> pal.accent();
                };
                graphics.fill(x, cardY, x + width, cardY + cardH, 0xC4161412);
                graphics.outline(x, cardY, width, cardH, (pal.bgBorder() & 0x00FFFFFF) | SLEEK_BORDER_ALPHA);
                graphics.fill(x, cardY + 2, x + 2, cardY + cardH - 2, stripe);
                graphics.text(mc.font, t.title(), x + 6, cardY + 4, pal.textPrimary(), true);
                if (t.body() != null) {
                    graphics.text(mc.font, t.body(), x + 6, cardY + 14, pal.textSecondary(), false);
                }
                cursorBottom = cardY - 3;
            }
        }
    };

    private BuiltinHudWidgets() {
    }

    public static void registerDefaults(HudApi api) {
        api.registerWidget(MINIMAP);
        api.registerWidget(COMPASS_BAR);
        api.registerWidget(STATUS_STRIP);
        api.registerWidget(ERA_BANNER);
        api.registerWidget(TOAST_STACK);
    }

    public static void showEraBanner(Component title, @Nullable Component subtitle, int durationTicks) {
        if (title != null && durationTicks > 0) {
            eraBannerTitle = title;
            eraBannerSubtitle = subtitle;
            eraBannerTotalTicks = Math.max(20, durationTicks);
            eraBannerTicks = eraBannerTotalTicks;
        }
    }

    public static void tickHudState() {
        if (eraBannerTicks > 0) {
            eraBannerTicks--;
        }
        Identifier currentEra = HudApi.get().currentSkin().era();
        Identifier prevEra = lastObservedEra;
        if (prevEra == null) {
            lastObservedEra = currentEra;
        } else if (!prevEra.equals(currentEra)) {
            lastObservedEra = currentEra;
            Component skinTitle = Component.translatable("skin.tcatlas." + currentEra.getPath());
            showEraBanner(skinTitle, null, 100);
        }

        drainPendingToasts();
        synchronized (ACTIVE_TOASTS) {
            Iterator<ActiveToast> it = ACTIVE_TOASTS.iterator();
            while (it.hasNext()) {
                ActiveToast active = it.next();
                active.remainingTicks--;
                if (active.remainingTicks <= 0) {
                    it.remove();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void drainPendingToasts() {
        if (!toastFieldResolved) {
            toastFieldResolved = true;
            try {
                Field f = TAtlas.class.getDeclaredField("TOASTS");
                f.setAccessible(true);
                rawToastQueueField = f;
            } catch (Throwable ignored) {
            }
        }
        Field f = rawToastQueueField;
        if (f == null) {
            return;
        }
        try {
            synchronized (TAtlas.hud()) {
                Deque<HudApi.Toast> raw = (Deque<HudApi.Toast>) f.get(null);
                if (raw == null || raw.isEmpty()) {
                    return;
                }
                synchronized (ACTIVE_TOASTS) {
                    while (!raw.isEmpty()) {
                        HudApi.Toast next = raw.pollFirst();
                        if (next == null) {
                            continue;
                        }
                        if (next.dedupKey() != null) {
                            ACTIVE_TOASTS.removeIf(a -> next.dedupKey().equals(a.toast.dedupKey()));
                        }
                        int ttl = next.durationTicks() > 0 ? next.durationTicks() : 80;
                        ACTIVE_TOASTS.add(new ActiveToast(next, ttl));
                        while (ACTIVE_TOASTS.size() > 6) {
                            ACTIVE_TOASTS.removeFirst();
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static Optional<Waypoint> resolveActiveNavigation() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) {
            return Optional.empty();
        }
        Identifier dim = mc.level.dimension().identifier();
        return TAtlas.waypoints().activeNavigation(mc.player.getUUID())
                .filter(wp -> wp.dimension().equals(dim));
    }

    private static double shortestSignedAngleDeg(double fromDeg, double toDeg) {
        return ((toDeg - fromDeg + 540.0) % 360.0) - 180.0;
    }
}
