package com.nightcity.tcivilization.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.nightcity.tcivilization.api.CivCalendar;
import com.nightcity.tcivilization.client.api.CivPanel;
import com.nightcity.tcivilization.client.api.CivPanelTabRegistry;
import com.nightcity.tcivilization.client.api.CivPanelTabSpec;
import com.nightcity.tcivilization.client.api.CivTabContext;
import com.nightcity.tcivilization.client.api.CivTabGroup;
import com.nightcity.tcivilization.client.api.CivTabPage;
import com.nightcity.tcivilization.client.api.HudRect;
import com.nightcity.tcivilization.client.api.OverviewCard;
import com.nightcity.tcivilization.client.api.TcSkin;
import com.nightcity.tcivilization.client.ui.TcEraBadge;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * J Civilization Panel shell screen hosting Overview, Chronicle, and installed module tabs (03 §5.10.1, §10.1-10.2).
 */
public final class CivPanelScreen extends Screen {
    private static final int MARGIN = 14;
    private static final int HEADER_HEIGHT = 28;
    private static final int SIDEBAR_WIDTH = 116;

    private Identifier activeTabId;
    @Nullable
    private CivTabPage activePage;
    private List<CivPanelTabSpec> visibleTabs = List.of();
    private final List<Button> tabButtons = new ArrayList<>();
    private CivTabContext tabContext;

    public CivPanelScreen(Identifier initialTabId) {
        super(Component.translatable("gui.tcivilization.civ_panel.title"));
        this.activeTabId = initialTabId == null ? CivPanel.TAB_OVERVIEW : initialTabId;
    }

    @Override
    protected void init() {
        super.init();
        this.tabButtons.clear();
        long dayTime = this.minecraft != null && this.minecraft.level != null ? this.minecraft.level.getOverworldClockTime() : 0L;
        this.tabContext = new CivTabContext(
                "Nameless Tribe", -1, dayTime, 1, 0, 0L, Set.of(), TcSkin.active(), false
        );
        this.visibleTabs = CivPanelTabRegistry.global().orderedTabs().stream()
                .filter(spec -> spec.visible().test(this.tabContext))
                .toList();

        if (visibleTabs.stream().noneMatch(t -> t.id().equals(activeTabId)) && !visibleTabs.isEmpty()) {
            activeTabId = visibleTabs.getFirst().id();
        }

        int panelLeft = MARGIN;
        int panelTop = MARGIN;
        int panelRight = this.width - MARGIN;

        addRenderableWidget(Button.builder(Component.literal("×"), btn -> onClose())
                .bounds(panelRight - 23, panelTop + 5, 18, 18)
                .build());

        int yCursor = panelTop + HEADER_HEIGHT + 6;
        int maxSidebarBottom = this.height - MARGIN - 22;
        CivTabGroup currentGroup = null;
        for (CivPanelTabSpec spec : visibleTabs) {
            if (spec.group() != currentGroup) {
                currentGroup = spec.group();
                yCursor += 13;
            }
            if (yCursor > maxSidebarBottom) {
                break;
            }
            Identifier id = spec.id();
            Button tabBtn = Button.builder(
                    Component.translatable(spec.translationKey()),
                    btn -> switchTab(id)
            ).bounds(panelLeft + 6, yCursor, SIDEBAR_WIDTH - 12, 18).build();
            tabBtn.active = !id.equals(activeTabId);
            this.tabButtons.add(tabBtn);
            addRenderableWidget(tabBtn);
            yCursor += 20;
        }

        rebuildActivePage();
    }

    private void switchTab(Identifier nextTabId) {
        if (activePage != null) {
            activePage.onClose();
        }
        this.activeTabId = nextTabId;
        for (int i = 0; i < tabButtons.size() && i < visibleTabs.size(); i++) {
            tabButtons.get(i).active = !visibleTabs.get(i).id().equals(activeTabId);
        }
        rebuildActivePage();
    }

    private HudRect computeContentRect() {
        int contentX = MARGIN + SIDEBAR_WIDTH + 8;
        int contentY = MARGIN + HEADER_HEIGHT + 8;
        int contentW = Math.max(160, this.width - contentX - MARGIN - 8);
        int contentH = Math.max(96, this.height - contentY - MARGIN - 8);
        return new HudRect(contentX, contentY, contentW, contentH);
    }

    private void rebuildActivePage() {
        HudRect contentRect = computeContentRect();
        this.activePage = visibleTabs.stream()
                .filter(s -> s.id().equals(activeTabId))
                .findFirst()
                .map(s -> s.factory().apply(tabContext))
                .orElse(null);
        if (this.activePage != null) {
            this.activePage.init(tabContext, contentRect);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft != null && this.minecraft.level != null) {
            this.extractBlurredBackground(graphics);
        }
        graphics.fillGradient(0, 0, this.width, this.height, 0xC8090D14, 0xDC05080D);

        TcSkin.Palette pal = TcSkin.active().palette();
        int left = MARGIN;
        int top = MARGIN;
        int right = this.width - MARGIN;
        int bottom = this.height - MARGIN;
        int bg = opaque(pal.background());
        int panel = opaque(pal.panel());
        int border = opaque(pal.border());

        graphics.fill(left, top, right, bottom, bg);
        graphics.fill(left + 1, top + 1, right - 1, top + HEADER_HEIGHT, panel);
        graphics.fill(left + 1, top + HEADER_HEIGHT, left + SIDEBAR_WIDTH, bottom - 1, panel);
        graphics.horizontalLine(left + 1, right - 2, top + HEADER_HEIGHT, border);
        graphics.verticalLine(left + SIDEBAR_WIDTH, top + HEADER_HEIGHT, bottom - 2, border);
        graphics.outline(left, top, right - left, bottom - top, border);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        TcSkin skin = TcSkin.active();
        TcSkin.Palette pal = skin.palette();
        int left = MARGIN;
        int top = MARGIN;
        int right = this.width - MARGIN;
        int textCol = opaque(pal.text());
        int mutedCol = opaque(pal.textMuted());
        int accentCol = opaque(pal.accent());

        CivCalendar.CalendarDate date = CivCalendar.fromDayTime(tabContext.dayTime());
        Component header = Component.translatable(
                "gui.tcivilization.civ_panel.header",
                tabContext.civName(),
                date.year(),
                Component.translatable(date.season().translationKey()),
                date.dayOfSeason()
        );
        int maxHeaderWidth = Math.max(80, (right - left) - 104);
        String headerStr = this.font.plainSubstrByWidth(header.getString(), maxHeaderWidth);
        graphics.text(this.font, headerStr, left + 10, top + 10, textCol, false);

        if (right - left >= 240) {
            TcEraBadge.render(graphics, new HudRect(right - 84, top + 7, 54, 14), tabContext.eraIndex(), skin);
        }

        int yCursor = top + HEADER_HEIGHT + 6;
        int maxSidebarBottom = this.height - MARGIN - 22;
        CivTabGroup currentGroup = null;
        for (CivPanelTabSpec spec : visibleTabs) {
            if (spec.group() != currentGroup) {
                currentGroup = spec.group();
                if (yCursor + 13 <= maxSidebarBottom) {
                    String grpLabel = this.font.plainSubstrByWidth(
                            Component.translatable(currentGroup.translationKey()).getString(),
                            SIDEBAR_WIDTH - 14
                    );
                    graphics.text(this.font, grpLabel, left + 8, yCursor + 3, mutedCol, false);
                }
                yCursor += 13;
            }
            if (yCursor > maxSidebarBottom) {
                break;
            }
            if (spec.id().equals(activeTabId)) {
                graphics.fill(left + 2, yCursor + 2, left + 5, yCursor + 16, accentCol);
            }
            yCursor += 20;
        }

        if (this.activePage != null) {
            HudRect rect = computeContentRect();
            graphics.enableScissor(rect.x(), rect.y(), rect.right(), rect.bottom());
            this.activePage.render(graphics, mouseX, mouseY, partialTick);
            graphics.disableScissor();
        }

        graphics.nextStratum();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_J) {
            onClose();
            return true;
        }
        if (event.key() == InputConstants.KEY_TAB && !visibleTabs.isEmpty()) {
            int idx = 0;
            for (int i = 0; i < visibleTabs.size(); i++) {
                if (visibleTabs.get(i).id().equals(activeTabId)) {
                    idx = i;
                    break;
                }
            }
            boolean shift = event.hasShiftDown();
            int nextIdx = Math.floorMod(idx + (shift ? -1 : 1), visibleTabs.size());
            switchTab(visibleTabs.get(nextIdx).id());
            return true;
        }
        if (event.key() >= InputConstants.KEY_1 && event.key() <= InputConstants.KEY_5) {
            int groupIdx = event.key() - InputConstants.KEY_1;
            CivTabGroup targetGroup = CivTabGroup.values()[groupIdx];
            visibleTabs.stream()
                    .filter(s -> s.group() == targetGroup)
                    .findFirst()
                    .ifPresent(s -> switchTab(s.id()));
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (activePage != null) {
            activePage.onClose();
        }
        super.onClose();
    }

    private static int opaque(int argb) {
        return 0xFF000000 | argb;
    }

    public static CivTabPage createOverviewPage(CivTabContext ctx) {
        return new CivTabPage() {
            private HudRect area = new HudRect(0, 0, 200, 100);

            @Override
            public void init(CivTabContext context, HudRect rect) {
                this.area = rect;
            }

            @Override
            public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                TcSkin.Palette pal = ctx.skin().palette();
                var font = Minecraft.getInstance().font;
                int panelCol = opaque(pal.panel());
                int borderCol = opaque(pal.border());
                int accentCol = opaque(pal.accent());
                int textCol = opaque(pal.text());

                int cardW = Math.min(OverviewCard.WIDTH, Math.max(140, area.width() - 8));
                int cardH = OverviewCard.HEIGHT;
                int cols = Math.max(1, (area.width() + 8) / (cardW + 8));

                int cardX = area.x() + 4;
                int cardY = area.y() + 4;
                graphics.fill(cardX, cardY, cardX + cardW, cardY + cardH, panelCol);
                graphics.fill(cardX + 1, cardY + 1, cardX + cardW - 1, cardY + 20, opaque(pal.background()));
                graphics.horizontalLine(cardX + 1, cardX + cardW - 2, cardY + 20, borderCol);
                graphics.outline(cardX, cardY, cardW, cardH, borderCol);
                graphics.text(font, Component.translatable("civtab.tcivilization.overview"), cardX + 8, cardY + 6, accentCol, false);
                graphics.text(font, Component.translatable("gui.tcivilization.overview.members", ctx.memberCount()), cardX + 8, cardY + 28, textCol, false);
                graphics.text(font, Component.translatable("gui.tcivilization.overview.regions", ctx.regionsVisited()), cardX + 8, cardY + 44, textCol, false);

                int slot = 1;
                for (CivPanelTabSpec spec : CivPanelTabRegistry.global().orderedTabs()) {
                    if (spec.overviewCard().isEmpty() || !spec.visible().test(ctx)) {
                        continue;
                    }
                    OverviewCard card = spec.overviewCard().get();
                    int col = slot % cols;
                    int row = slot / cols;
                    int cx = area.x() + 4 + col * (cardW + 8);
                    int cy = area.y() + 4 + row * (cardH + 8);
                    if (cy + cardH > area.bottom()) {
                        break;
                    }
                    HudRect bounds = new HudRect(cx, cy, cardW, cardH);
                    graphics.fill(cx, cy, cx + cardW, cy + cardH, panelCol);
                    graphics.outline(cx, cy, cardW, cardH, borderCol);
                    card.render(graphics, bounds, ctx);
                    slot++;
                }
            }
        };
    }

    public static CivTabPage createChroniclePage(CivTabContext ctx) {
        return new CivTabPage() {
            private HudRect area = new HudRect(0, 0, 200, 100);

            @Override
            public void init(CivTabContext context, HudRect rect) {
                this.area = rect;
            }

            @Override
            public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                TcSkin.Palette pal = ctx.skin().palette();
                var font = Minecraft.getInstance().font;
                int panelCol = opaque(pal.panel());
                int borderCol = opaque(pal.border());
                int accentCol = opaque(pal.accent());
                int mutedCol = opaque(pal.textMuted());

                graphics.fill(area.x(), area.y(), area.right(), area.bottom(), panelCol);
                graphics.fill(area.x() + 1, area.y() + 1, area.right() - 1, area.y() + 22, opaque(pal.background()));
                graphics.horizontalLine(area.x() + 1, area.right() - 2, area.y() + 22, borderCol);
                graphics.outline(area.x(), area.y(), area.width(), area.height(), borderCol);
                graphics.text(font, Component.translatable("civtab.tcivilization.chronicle"), area.x() + 10, area.y() + 7, accentCol, false);
                graphics.text(font, Component.translatable("gui.tcivilization.chronicle.empty"), area.x() + 10, area.y() + 32, mutedCol, false);
            }
        };
    }
}
