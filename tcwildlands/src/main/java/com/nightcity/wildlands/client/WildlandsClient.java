package com.nightcity.wildlands.client;

import com.nightcity.tcivilization.api.world.LatLon;
import com.nightcity.tcivilization.api.world.PrecipType;
import com.nightcity.tcivilization.api.world.TcWorldApis;
import com.nightcity.tcivilization.client.api.CivPanelTabRegistry;
import com.nightcity.tcivilization.client.api.CivPanelTabSpec;
import com.nightcity.tcivilization.client.api.CivTabContext;
import com.nightcity.tcivilization.client.api.CivTabGroup;
import com.nightcity.tcivilization.client.api.CivTabPage;
import com.nightcity.tcivilization.client.api.DeepLinkRegistry;
import com.nightcity.tcivilization.client.api.HudAnchor;
import com.nightcity.tcivilization.client.api.HudContext;
import com.nightcity.tcivilization.client.api.HudLevel;
import com.nightcity.tcivilization.client.api.HudRect;
import com.nightcity.tcivilization.client.api.HudWidget;
import com.nightcity.tcivilization.client.api.HudWidgetRegistry;
import com.nightcity.tcivilization.client.api.HudWidgetSpec;
import com.nightcity.tcivilization.client.api.TcSkin;
import com.nightcity.wildlands.api.impl.WildlandsApiImpl;
import com.nightcity.wildlands.network.WildlandsPayloads;
import com.nightcity.wildlands.planet.WildlandsCatalog;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.jspecify.annotations.Nullable;

/**
 * Client setup for tcwildlands: installs SeasonalColorApi, registers HUD widget tcwildlands:local_conditions,
 * registers CivPanel tab tcwildlands:geography, and handles planet progress & start-site screens (10 §10, §12.1).
 */
public final class WildlandsClient {
    public static final Identifier LOCAL_CONDITIONS_ID = WildlandsCatalog.wl("local_conditions");
    public static final Identifier GEOGRAPHY_TAB_ID = WildlandsCatalog.wl("geography");

    private static volatile WildlandsPayloads.@Nullable LocalClimatePayload latestLocalClimate;
    @Nullable
    private static volatile PlanetProgressScreen activeProgressScreen;

    private WildlandsClient() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            TcWorldApis.installClient(WildlandsApiImpl.SEASONAL_COLOR_API);

            HudWidgetRegistry.global().register(new HudWidgetSpec(
                    LOCAL_CONDITIONS_ID,
                    HudLevel.EXPANDED,
                    HudAnchor.TOP_LEFT,
                    4, 40, 132, 34, 12,
                    30, true, Optional.empty(),
                    20,
                    Set.of(WildlandsCatalog.wl("local_climate")),
                    ctx -> ctx.dimensionId().equals("minecraft:overworld") || ctx.dimensionId().equals("minecraft:the_nether"),
                    LocalConditionsHudWidget::new
            ));

            CivPanelTabRegistry.global().register(new CivPanelTabSpec(
                    GEOGRAPHY_TAB_ID,
                    CivTabGroup.WORLD,
                    600,
                    WildlandsCatalog.wl("textures/gui/tab/geography.png"),
                    Optional.empty(),
                    ctx -> true,
                    Optional.empty(),
                    GeographyPage::new
            ));

            DeepLinkRegistry.global().register("tcwildlands", link -> link.path().startsWith("geography"));

            WildlandsPayloads.bindClientHandlers(
                    WildlandsClient::handleProgress,
                    WildlandsClient::handlePreview,
                    WildlandsClient::handleStartSiteOptions,
                    payload -> latestLocalClimate = payload,
                    cells -> {}
            );
        });
    }

    private static void handleProgress(WildlandsPayloads.PlanetProgressPayload p) {
        Minecraft mc = Minecraft.getInstance();
        PlanetProgressScreen cur = activeProgressScreen;
        if (cur != null) {
            cur.updateProgress(p.stageIndex(), p.totalStages(), p.overallFraction(), p.detailText());
        } else if (p.overallFraction() < 1.0f) {
            PlanetProgressScreen next = new PlanetProgressScreen(p.stageIndex(), p.totalStages(), p.overallFraction(), p.detailText());
            activeProgressScreen = next;
            mc.setScreenAndShow(next);
        }
    }

    private static void handlePreview(WildlandsPayloads.PlanetPreviewPayload p) {
        PlanetProgressScreen cur = activeProgressScreen;
        if (cur != null) {
            cur.updatePreview(p.width(), p.height(), p.paletteIndices());
        }
    }

    private static void handleStartSiteOptions(WildlandsPayloads.StartSiteOptionsPayload p) {
        Minecraft mc = Minecraft.getInstance();
        activeProgressScreen = null;
        if (!p.candidates().isEmpty()) {
            mc.setScreenAndShow(new StartSiteScreen(p.canCustomize(), p.candidates()));
        }
    }

    private static final class LocalConditionsHudWidget implements HudWidget {
        @Override
        public void tick(HudContext ctx) {
        }

        @Override
        public void render(GuiGraphicsExtractor graphics, HudRect rect, float partialTick, HudContext ctx) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }
            TcSkin.Palette pal = ctx.skin().palette();
            graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), pal.panel());
            graphics.outline(rect.x(), rect.y(), rect.width(), rect.height(), pal.border());

            WildlandsPayloads.LocalClimatePayload data = latestLocalClimate;
            String line1;
            String line2;
            if (data != null) {
                PrecipType pt = PrecipType.values()[Math.clamp(data.precipType(), 0, PrecipType.values().length - 1)];
                line1 = String.format(Locale.ROOT, "%d °C · %s · %.1f m/s", Math.round(data.tempC10() / 10.0f), pt.name(), data.windSpeed10() / 10.0f);
                String bName = WildlandsCatalog.biomeByIndex(data.biomeIndex()).path();
                line2 = String.format(Locale.ROOT, "%d m · %s", data.altitudeM(), bName);
            } else {
                int altM = (int) Math.round(TcWorldApis.planet().altitudeMeters(mc.player.getY()));
                line1 = "15 °C · CLEAR";
                line2 = altM + " m";
            }
            graphics.text(mc.font, Component.literal(line1), rect.x() + 4, rect.y() + 4, pal.text(), false);
            graphics.text(mc.font, Component.literal(line2), rect.x() + 4, rect.y() + 16, pal.textMuted(), false);
        }
    }

    private static final class GeographyPage implements CivTabPage {
        private HudRect area = new HudRect(0, 0, 200, 120);

        private GeographyPage(CivTabContext ctx) {
        }

        @Override
        public void init(CivTabContext ctx, HudRect area) {
            this.area = area;
        }

        @Override
        public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            Minecraft mc = Minecraft.getInstance();
            TcSkin.Palette pal = TcSkin.active().palette();
            graphics.text(mc.font, Component.translatable("civtab.tcwildlands.geography"), area.x() + 6, area.y() + 6, pal.accent(), false);
            if (!TcWorldApis.planet().isPlanetActive()) {
                graphics.text(mc.font, Component.translatable("gui.tcwildlands.geography.degraded"), area.x() + 6, area.y() + 24, pal.textMuted(), false);
                return;
            }
            if (mc.player != null) {
                LatLon ll = TcWorldApis.planet().latLon(mc.player.getBlockX(), mc.player.getBlockZ());
                double alt = TcWorldApis.planet().altitudeMeters(mc.player.getY());
                String coords = String.format(Locale.ROOT, "Lat %.2f°  Lon %.2f°  Alt %.0f m", ll.latDeg(), ll.lonDeg(), alt);
                graphics.text(mc.font, Component.literal(coords), area.x() + 6, area.y() + 24, pal.text(), false);
            }
        }
    }
}
