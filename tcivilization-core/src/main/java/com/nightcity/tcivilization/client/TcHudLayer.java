package com.nightcity.tcivilization.client;

import com.nightcity.tcivilization.TCivilization;
import com.nightcity.tcivilization.api.CeremonySpec;
import com.nightcity.tcivilization.api.CivCalendar;
import com.nightcity.tcivilization.api.Civilization;
import com.nightcity.tcivilization.api.CivilizationApi;
import com.nightcity.tcivilization.client.api.CeremonyOverlay;
import com.nightcity.tcivilization.client.api.HudAnchor;
import com.nightcity.tcivilization.client.api.HudContext;
import com.nightcity.tcivilization.client.api.HudLevel;
import com.nightcity.tcivilization.client.api.HudRect;
import com.nightcity.tcivilization.client.api.HudWidget;
import com.nightcity.tcivilization.client.api.HudWidgetRegistry;
import com.nightcity.tcivilization.client.api.HudWidgetSpec;
import com.nightcity.tcivilization.client.api.TargetInfo;
import com.nightcity.tcivilization.client.api.TargetInfoProvider;
import com.nightcity.tcivilization.client.api.TcSkin;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.HitResult;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jspecify.annotations.Nullable;

/**
 * Single HUD GuiLayer tcivilization:hud hosting all registered HudWidgetSpec instances (03 §5.10.2, 31 §3).
 */
public final class TcHudLayer implements GuiLayer {
    public static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "hud");
    public static final TcHudLayer INSTANCE = new TcHudLayer();

    private static final int PILL_BG = 0xAD161412;
    private static final int PILL_BORDER = 0x786E5B45;
    private static final int PILL_ACCENT = 0xFFD99B52;
    private static final int PILL_TEXT_PRIMARY = 0xFFF4EAD8;
    private static final int PILL_TEXT_SECONDARY = 0xFFD9CEBC;
    private static final int PILL_TEXT_MUTED = 0xFF9C8F7E;

    private static volatile HudLevel currentLevel = HudLevel.STANDARD;
    private static volatile boolean hudHidden = false;
    private static volatile String clientCivName = Civilization.DEFAULT_NAME;
    private static volatile int clientEraOverride = Integer.MIN_VALUE;

    private static volatile boolean techBridgeChecked = false;
    @Nullable
    private static volatile Method techApiGetter;
    @Nullable
    private static volatile Method techEraMethod;

    private final Map<Identifier, HudWidget> liveWidgets = new HashMap<>();

    private TcHudLayer() {
    }

    public static HudLevel currentLevel() {
        return currentLevel;
    }

    public static boolean isHudHidden() {
        return hudHidden;
    }

    public static void setHudHidden(boolean hidden) {
        hudHidden = hidden;
    }

    public static HudLevel cycleLevel() {
        currentLevel = currentLevel.next();
        return currentLevel;
    }

    public static String cycleUnifiedLevel() {
        if (hudHidden) {
            hudHidden = false;
            currentLevel = HudLevel.MINIMAL;
        } else if (currentLevel == HudLevel.MINIMAL) {
            currentLevel = HudLevel.STANDARD;
        } else if (currentLevel == HudLevel.STANDARD) {
            currentLevel = HudLevel.EXPANDED;
        } else {
            hudHidden = true;
        }
        return unifiedLevelLabel();
    }

    public static String unifiedLevelLabel() {
        if (hudHidden) {
            return "OFF";
        }
        return switch (currentLevel) {
            case MINIMAL -> "MINIMAL";
            case STANDARD -> "STANDARD";
            case EXPANDED -> "FULL";
        };
    }

    public static void setLevel(HudLevel level) {
        if (level != null) {
            currentLevel = level;
        }
    }

    public static void setClientCivSnapshot(String civName, int eraIndex) {
        clientCivName = (civName == null || civName.isBlank()) ? Civilization.DEFAULT_NAME : civName;
        clientEraOverride = eraIndex;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gui == null || mc.gui.hud.isHidden() || mc.gui.screen() != null) {
            return;
        }
        if (hudHidden) {
            return;
        }

        int resolvedEra = resolveClientEraIndex(mc);
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        HudContext ctx = new HudContext(
                resolvedEra,
                Set.of(),
                currentLevel,
                1.0f,
                TcSkin.active(),
                false,
                false,
                mc.player.hurtTime > 0,
                mc.player.isPassenger(),
                mc.level.dimension().identifier().toString(),
                mc.level.getOverworldClockTime()
        );

        int sw = graphics.guiWidth();
        int sh = graphics.guiHeight();
        Map<HudAnchor, Integer> stackOffsets = new EnumMap<>(HudAnchor.class);

        for (HudWidgetSpec spec : HudWidgetRegistry.global().all()) {
            if (!currentLevel.includes(spec.minLevel()) || !spec.defaultVisible()) {
                continue;
            }
            if (spec.unlockTech().isPresent() && !ctx.unlockedTechs().contains(spec.unlockTech().get())) {
                continue;
            }
            if (!spec.relevant().test(ctx)) {
                continue;
            }

            HudWidget widget = liveWidgets.computeIfAbsent(spec.id(), k -> spec.factory().get());
            int stackY = stackOffsets.getOrDefault(spec.defaultAnchor(), 0);
            HudRect rect = computeAnchorRect(spec.defaultAnchor(), sw, sh, spec.prefWidth(), spec.prefHeight(), spec.defaultX(), spec.defaultY() + stackY);
            widget.render(graphics, rect, partialTick, ctx);
            stackOffsets.put(spec.defaultAnchor(), stackY + spec.prefHeight() + 4);
        }

        CeremonySpec ceremony = CeremonyOverlay.active();
        if (ceremony != null) {
            int cx = sw / 2;
            graphics.fill(cx - 110, 28, cx + 110, 62, 0xCC161412);
            graphics.outline(cx - 110, 28, 220, 34, PILL_ACCENT);
            graphics.centeredText(mc.font, ceremony.title(), cx, 34, PILL_ACCENT);
            graphics.centeredText(mc.font, ceremony.subtitle(), cx, 48, PILL_TEXT_PRIMARY);
        }
    }

    private static String resolveClientCivName(Minecraft mc) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server != null && server.overworld() != null) {
            try {
                Civilization civ = CivilizationApi.get(server).playerCiv();
                if (civ != null && civ.name() != null && !civ.name().isBlank()) {
                    return civ.name();
                }
            } catch (Throwable ignored) {
            }
        }
        return clientCivName;
    }

    private static int resolveClientEraIndex(Minecraft mc) {
        if (clientEraOverride >= -1) {
            return clientEraOverride;
        }
        IntegratedServer server = mc.getSingleplayerServer();
        if (server != null && server.overworld() != null) {
            try {
                Civilization civ = CivilizationApi.get(server).playerCiv();
                if (civ != null && civ.eraIndex() >= 0) {
                    return civ.eraIndex();
                }
            } catch (Throwable ignored) {
            }
        }
        boolean techLoaded = ModList.get() != null && ModList.get().isLoaded("tctechnology");
        if (!techLoaded) {
            return -1;
        }
        int reflectedEra = queryTechEraReflectively();
        return reflectedEra >= 0 ? reflectedEra : 0;
    }

    private static int queryTechEraReflectively() {
        if (!techBridgeChecked) {
            techBridgeChecked = true;
            try {
                Class<?> techCls = Class.forName("com.nightcity.technology.TTechnology");
                Method apiM = techCls.getMethod("api");
                Class<?> apiCls = Class.forName("com.nightcity.technology.api.TechApi");
                Method eraM = apiCls.getMethod("era", UUID.class);
                techApiGetter = apiM;
                techEraMethod = eraM;
            } catch (Throwable ignored) {
            }
        }
        Method apiM = techApiGetter;
        Method eraM = techEraMethod;
        if (apiM != null && eraM != null) {
            try {
                Object api = apiM.invoke(null);
                Object opt = eraM.invoke(api, Civilization.PLAYER_CIV_ID);
                if (opt instanceof Optional<?> o && o.isPresent() && o.get() instanceof Enum<?> e) {
                    return e.ordinal();
                }
            } catch (Throwable ignored) {
            }
        }
        return 0;
    }

    private static HudRect computeAnchorRect(HudAnchor anchor, int sw, int sh, int w, int h, int ox, int oy) {
        int x = switch (anchor) {
            case TOP_LEFT, MIDDLE_LEFT, BOTTOM_LEFT -> 6 + ox;
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> (sw - w) / 2 + ox;
            case TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT -> sw - w - 6 - ox;
        };
        int y = switch (anchor) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 6 + oy;
            case MIDDLE_LEFT, CENTER, MIDDLE_RIGHT -> (sh - h) / 2 + oy;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> sh - h - 28 - oy;
        };
        return new HudRect(x, y, w, h);
    }

    public static void registerBuiltinWidgets(HudWidgetRegistry registry) {
        registry.register(new HudWidgetSpec(
                Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "date_season"),
                HudLevel.MINIMAL, HudAnchor.TOP_LEFT, 0, 0, 156, 15, 14, 90, true,
                Optional.empty(), 20, Set.of(), ctx -> true,
                () -> new HudWidget() {
                    @Override
                    public void tick(HudContext ctx) {
                    }

                    @Override
                    public void render(GuiGraphicsExtractor graphics, HudRect rect, float partialTick, HudContext ctx) {
                        Minecraft mc = Minecraft.getInstance();
                        Font font = mc.font;
                        CivCalendar.CalendarDate date = CivCalendar.fromDayTime(ctx.dayTime());
                        String seasonStr = Component.translatable(date.season().translationKey()).getString();
                        String datePart = "Y" + date.year() + " " + seasonStr + " D" + date.dayOfSeason();
                        boolean includeCivName = ctx.level().includes(HudLevel.STANDARD);
                        String civPart = includeCivName ? resolveClientCivName(mc) : "";
                        String eraPart = ctx.eraIndex() >= 0 ? ("E" + ctx.eraIndex()) : "";
                        String sep = " · ";

                        int contentW = font.width(datePart);
                        if (!civPart.isEmpty()) {
                            contentW += font.width(civPart) + font.width(sep);
                        }
                        if (!eraPart.isEmpty()) {
                            contentW += font.width(sep) + font.width(eraPart);
                        }
                        int pillW = contentW + 12;
                        int pillH = rect.height();
                        int x = rect.x();
                        int y = rect.y();

                        graphics.fill(x, y, x + pillW, y + pillH, PILL_BG);
                        graphics.outline(x, y, pillW, pillH, PILL_BORDER);
                        graphics.fill(x, y + 2, x + 2, y + pillH - 2, PILL_ACCENT);

                        int cursorX = x + 6;
                        int textY = y + (pillH - 8) / 2;
                        if (!civPart.isEmpty()) {
                            graphics.text(font, civPart, cursorX, textY, PILL_TEXT_PRIMARY, true);
                            cursorX += font.width(civPart);
                            graphics.text(font, sep, cursorX, textY, PILL_TEXT_MUTED, true);
                            cursorX += font.width(sep);
                        }
                        graphics.text(font, datePart, cursorX, textY, PILL_TEXT_SECONDARY, true);
                        cursorX += font.width(datePart);
                        if (!eraPart.isEmpty()) {
                            graphics.text(font, sep, cursorX, textY, PILL_TEXT_MUTED, true);
                            cursorX += font.width(sep);
                            graphics.text(font, eraPart, cursorX, textY, PILL_ACCENT, true);
                        }
                    }
                }
        ));

        registry.register(new HudWidgetSpec(
                Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "target_info"),
                HudLevel.STANDARD, HudAnchor.TOP_CENTER, 0, 20, 150, 26, 20, 80, true,
                Optional.empty(), 2, Set.of(),
                ctx -> {
                    Minecraft mc = Minecraft.getInstance();
                    return mc.hitResult != null
                            && mc.hitResult.getType() != HitResult.Type.MISS
                            && mc.level != null
                            && TargetInfoProvider.Registry.global().resolve(mc.hitResult, mc.level).isPresent();
                },
                () -> new HudWidget() {
                    @Override
                    public void tick(HudContext ctx) {
                    }

                    @Override
                    public void render(GuiGraphicsExtractor graphics, HudRect rect, float partialTick, HudContext ctx) {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.hitResult == null || mc.level == null) {
                            return;
                        }
                        Optional<TargetInfo> infoOpt = TargetInfoProvider.Registry.global().resolve(mc.hitResult, mc.level);
                        if (infoOpt.isEmpty()) {
                            return;
                        }
                        TargetInfo info = infoOpt.get();
                        int titleW = mc.font.width(info.title());
                        int subW = mc.font.width(info.sourceModName());
                        int boxW = Math.max(96, Math.max(titleW, subW) + 12);
                        int x = (graphics.guiWidth() - boxW) / 2;
                        int y = rect.y();
                        graphics.fill(x, y, x + boxW, y + rect.height(), PILL_BG);
                        graphics.outline(x, y, boxW, rect.height(), PILL_BORDER);
                        graphics.text(mc.font, info.title(), x + 6, y + 4, PILL_TEXT_PRIMARY, true);
                        graphics.text(mc.font, info.sourceModName(), x + 6, y + 15, PILL_TEXT_MUTED, true);
                    }
                }
        ));
    }
}
