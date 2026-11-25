package com.nightcity.technology.client;

import com.nightcity.technology.TTechnology;
import com.nightcity.technology.api.EraDefinition;
import com.nightcity.technology.api.TechApi;
import com.nightcity.technology.api.TechDefinition;
import com.nightcity.technology.api.TechEra;
import com.nightcity.technology.api.TechState;
import com.nightcity.technology.data.TechDefinitionLoader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class TechTreeScreen extends Screen {
    private static final int MARGIN_X = 10;
    private static final int MARGIN_Y = 8;
    private static final int NODE_W = 128;
    private static final int NODE_H = 28;
    private static final int COL_STEP = 156;
    private static final int ROW_STEP = 36;

    private record PlacedNode(TechDefinition def, TechState state, int x, int y) {
        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + NODE_W && my >= y && my <= y + NODE_H;
        }
    }

    private TechEra selectedEra = TechEra.STONE;
    private Identifier selectedTech;
    private String rawSearchText = "";
    private String searchQuery = "";
    private EditBox searchBox;
    private final List<Button> eraButtons = new ArrayList<>();
    private Button declareButton;
    private double panX = 0.0;
    private double panY = 0.0;
    private boolean draggingCanvas = false;
    private boolean movedDuringDrag = false;

    public TechTreeScreen() {
        super(Component.translatable("gui.tctechnology.tech_tree.title"));
        TechApi.get().era(TechApi.PLAYER_CIV_ID).ifPresent(e -> this.selectedEra = e);
    }

    private int detailPanelWidth() {
        return this.width >= 410 ? 176 : (this.width >= 330 ? 144 : 0);
    }

    @Override
    protected void init() {
        this.eraButtons.clear();
        int left = MARGIN_X;
        int top = MARGIN_Y;
        int right = this.width - MARGIN_X;

        addRenderableWidget(Button.builder(Component.literal("×"), b -> onClose())
                .bounds(right - 22, top + 3, 18, 18).build());

        declareButton = Button.builder(Component.translatable("gui.tctechnology.declare_era"), btn -> {
            TechApi.get().advanceEra(TechApi.PLAYER_CIV_ID, true);
            TechApi.get().era(TechApi.PLAYER_CIV_ID).ifPresent(this::selectEra);
        }).bounds(right - 130, top + 3, 104, 18).build();
        declareButton.active = TechApi.get().eraProgress(TechApi.PLAYER_CIV_ID).canDeclare();
        addRenderableWidget(declareButton);

        int searchW = Math.min(128, Math.max(72, this.width / 4));
        int searchX = right - 136 - searchW;
        searchBox = new EditBox(this.font, searchX, top + 4, searchW, 16, Component.literal("Search"));
        searchBox.setHint(Component.literal("Search..."));
        searchBox.setValue(rawSearchText);
        searchBox.setResponder(val -> {
            this.rawSearchText = val == null ? "" : val;
            this.searchQuery = this.rawSearchText.trim().toLowerCase(Locale.ROOT);
            this.panX = 0.0;
            this.panY = 0.0;
        });
        addRenderableWidget(searchBox);

        TechEra[] eras = TechEra.values();
        int availW = (right - left) - 12;
        int btnGap = 2;
        int btnW = Math.clamp((availW - btnGap * (eras.length - 1)) / eras.length, 22, 34);
        int eraBtnX = left + 6;
        int eraBtnY = top + 27;
        for (TechEra era : eras) {
            TechEra target = era;
            Button eb = Button.builder(Component.literal(era.code()), btn -> selectEra(target))
                    .bounds(eraBtnX, eraBtnY, btnW, 16)
                    .build();
            eb.active = (era != selectedEra);
            this.eraButtons.add(eb);
            addRenderableWidget(eb);
            eraBtnX += btnW + btnGap;
        }
    }

    private void selectEra(TechEra target) {
        this.selectedEra = target;
        this.panX = 0.0;
        this.panY = 0.0;
        TechEra[] all = TechEra.values();
        for (int i = 0; i < eraButtons.size() && i < all.length; i++) {
            eraButtons.get(i).active = (all[i] != selectedEra);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft != null && this.minecraft.level != null) {
            this.extractBlurredBackground(graphics);
        }
        graphics.fillGradient(0, 0, this.width, this.height, 0xDC080C12, 0xF004070B);

        int left = MARGIN_X;
        int top = MARGIN_Y;
        int right = this.width - MARGIN_X;
        int bottom = this.height - MARGIN_Y;

        graphics.fill(left, top, right, bottom, 0xFF131922);
        graphics.fill(left + 1, top + 1, right - 1, top + 24, 0xFF1B2432);
        graphics.fill(left + 1, top + 24, right - 1, top + 64, 0xFF171F2B);
        graphics.horizontalLine(left + 1, right - 2, top + 24, 0xFF334155);
        graphics.horizontalLine(left + 1, right - 2, top + 64, 0xFF334155);
        graphics.outline(left, top, right - left, bottom - top, 0xFF475569);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = MARGIN_X;
        int top = MARGIN_Y;
        int right = this.width - MARGIN_X;
        int bottom = this.height - MARGIN_Y;

        if (declareButton != null) {
            declareButton.active = TechApi.get().eraProgress(TechApi.PLAYER_CIV_ID).canDeclare();
        }

        int maxTitleW = Math.max(60, (searchBox != null ? searchBox.getX() : (right - 220)) - (left + 14));
        String titleStr = this.font.plainSubstrByWidth(this.title.getString(), maxTitleW);
        graphics.text(this.font, titleStr, left + 8, top + 8, 0xFFF8FAFC, false);

        if (selectedEra.ordinal() < eraButtons.size()) {
            Button activeBtn = eraButtons.get(selectedEra.ordinal());
            graphics.fill(activeBtn.getX(), activeBtn.getY() + activeBtn.getHeight() + 1,
                    activeBtn.getX() + activeBtn.getWidth(), activeBtn.getY() + activeBtn.getHeight() + 3, 0xFFFBBF24);
        }

        TechDefinitionLoader.TechGraphSnapshot snap = TTechnology.loader().snapshot();
        List<TechDefinition> visible = filteredTechs(snap);
        if (selectedTech == null && !visible.isEmpty()) {
            selectedTech = visible.getFirst().id();
        }

        Component eraName = Component.translatable("era.tctechnology." + selectedEra.id());
        String bannerLeft = selectedEra.code() + " · " + eraName.getString()
                + (searchQuery.isEmpty() ? "" : ("  [Filter: " + searchQuery + "]"));
        graphics.text(this.font, bannerLeft, left + 8, top + 50, 0xFFFBBF24, false);

        int doneCount = 0;
        for (TechDefinition d : visible) {
            if (TechApi.get().state(TechApi.PLAYER_CIV_ID, d.id()) == TechState.COMPLETED) {
                doneCount++;
            }
        }
        EraDefinition.EraProgress eraProg = TechApi.get().eraProgress(TechApi.PLAYER_CIV_ID);
        String bannerRight = String.format(Locale.ROOT, "Unlocked %d/%d  ·  Keystones %d/%d",
                doneCount, visible.size(), eraProg.keystonesDone(), eraProg.keystonesTotal());
        if (right - left >= 320) {
            graphics.text(this.font, bannerRight, right - this.font.width(bannerRight) - 8, top + 50, 0xFFCBD5E1, false);
        }

        int detailW = detailPanelWidth();
        int bodyTop = top + 68;
        int bodyBottom = bottom - 6;
        int canvasLeft = left + 6;
        int canvasRight = detailW > 0 ? (right - 12 - detailW) : (right - 6);
        int canvasW = Math.max(80, canvasRight - canvasLeft);
        int canvasH = Math.max(60, bodyBottom - bodyTop);

        List<PlacedNode> nodes = placeNodes(visible, snap, canvasLeft, bodyTop);
        clampPan(nodes, canvasLeft, bodyTop, canvasW, canvasH);
        nodes = placeNodes(visible, snap, canvasLeft - (int) Math.round(panX), bodyTop - (int) Math.round(panY));

        Map<Identifier, PlacedNode> byId = new HashMap<>(nodes.size() * 2);
        for (PlacedNode pn : nodes) {
            byId.put(pn.def().id(), pn);
        }

        PlacedNode hoveredNode = null;
        boolean mouseInCanvas = mouseX >= canvasLeft && mouseX <= canvasRight && mouseY >= bodyTop && mouseY <= bodyBottom;

        graphics.fill(canvasLeft, bodyTop, canvasRight, bodyBottom, 0xFF0D1219);
        graphics.enableScissor(canvasLeft, bodyTop, canvasRight, bodyBottom);
        try {
            int gridOffX = Math.floorMod(-(int) Math.round(panX), 32);
            int gridOffY = Math.floorMod(-(int) Math.round(panY), 32);
            for (int gx = canvasLeft + gridOffX; gx < canvasRight; gx += 32) {
                graphics.verticalLine(gx, bodyTop, bodyBottom, 0xFF141C27);
            }
            for (int gy = bodyTop + gridOffY; gy < bodyBottom; gy += 32) {
                graphics.horizontalLine(canvasLeft, canvasRight, gy, 0xFF141C27);
            }

            for (PlacedNode target : nodes) {
                for (Identifier prereqId : target.def().prerequisites()) {
                    PlacedNode src = byId.get(prereqId);
                    if (src != null) {
                        int lineCol = src.state() == TechState.COMPLETED
                                ? 0xFF10B981
                                : (target.state() == TechState.RESEARCHABLE ? 0xFF3B82F6 : 0xFF475569);
                        drawConnector(graphics, src.x() + NODE_W, src.y() + NODE_H / 2, target.x(), target.y() + NODE_H / 2, lineCol);
                    }
                }
            }

            for (PlacedNode pn : nodes) {
                if (pn.x() + NODE_W < canvasLeft || pn.x() > canvasRight || pn.y() + NODE_H < bodyTop || pn.y() > bodyBottom) {
                    continue;
                }
                boolean selected = pn.def().id().equals(selectedTech);
                boolean hovered = mouseInCanvas && pn.contains(mouseX, mouseY);
                if (hovered) {
                    hoveredNode = pn;
                }
                renderNodeCard(graphics, pn, selected, hovered);
            }
        } finally {
            graphics.disableScissor();
        }
        graphics.outline(canvasLeft, bodyTop, canvasW, canvasH, 0xFF334155);

        if (detailW > 0) {
            renderDetailPanel(graphics, right - 6 - detailW, bodyTop, detailW, canvasH, snap);
        }

        graphics.nextStratum();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        if (hoveredNode != null) {
            graphics.setComponentTooltipForNextFrame(this.font, buildNodeTooltip(hoveredNode, snap), mouseX, mouseY);
        }
    }

    private List<PlacedNode> placeNodes(
            List<TechDefinition> visible,
            TechDefinitionLoader.TechGraphSnapshot snap,
            int originX,
            int originY
    ) {
        if (visible.isEmpty()) {
            return List.of();
        }
        int minDepth = Integer.MAX_VALUE;
        Map<String, Integer> laneMaxRow = new LinkedHashMap<>();
        for (TechDefinition def : visible) {
            TechDefinitionLoader.TechNodeLayout lay = snap.layouts().get(def.id());
            int d = lay != null ? lay.depth() : 0;
            int r = lay != null ? lay.row() : Math.max(0, def.tree().rowHint());
            minDepth = Math.min(minDepth, d);
            laneMaxRow.merge(def.tree().lane(), r, Math::max);
        }
        if (minDepth == Integer.MAX_VALUE) {
            minDepth = 0;
        }
        Map<String, Integer> laneBaseRow = new HashMap<>();
        int nextBase = 0;
        for (Map.Entry<String, Integer> e : laneMaxRow.entrySet()) {
            laneBaseRow.put(e.getKey(), nextBase);
            nextBase += e.getValue() + 1;
        }
        Set<Long> occupied = new HashSet<>();
        List<PlacedNode> out = new ArrayList<>(visible.size());
        for (TechDefinition def : visible) {
            TechDefinitionLoader.TechNodeLayout lay = snap.layouts().get(def.id());
            int col = Math.max(0, (lay != null ? lay.depth() : 0) - minDepth);
            int row = laneBaseRow.getOrDefault(def.tree().lane(), 0) + (lay != null ? lay.row() : Math.max(0, def.tree().rowHint()));
            while (!occupied.add((((long) col) << 32) | (row & 0xFFFFFFFFL))) {
                row++;
            }
            int nx = originX + 10 + col * COL_STEP;
            int ny = originY + 10 + row * ROW_STEP;
            TechState state = TechApi.get().state(TechApi.PLAYER_CIV_ID, def.id());
            out.add(new PlacedNode(def, state, nx, ny));
        }
        return out;
    }

    private void clampPan(List<PlacedNode> unpanned, int canvasLeft, int canvasTop, int canvasW, int canvasH) {
        int maxRight = canvasLeft;
        int maxBottom = canvasTop;
        for (PlacedNode pn : unpanned) {
            maxRight = Math.max(maxRight, pn.x() + NODE_W + 12);
            maxBottom = Math.max(maxBottom, pn.y() + NODE_H + 12);
        }
        double maxPanX = Math.max(0.0, (maxRight - canvasLeft) - canvasW);
        double maxPanY = Math.max(0.0, (maxBottom - canvasTop) - canvasH);
        this.panX = Math.clamp(this.panX, 0.0, maxPanX);
        this.panY = Math.clamp(this.panY, 0.0, maxPanY);
    }

    private static void drawConnector(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int col) {
        if (x1 >= x0 + 4) {
            int midX = (x0 + x1) / 2;
            g.horizontalLine(x0, midX, y0, col);
            g.verticalLine(midX, Math.min(y0, y1), Math.max(y0, y1), col);
            g.horizontalLine(midX, x1 - 1, y1, col);
        } else {
            int midY = (y0 + y1) / 2;
            g.verticalLine(x0, Math.min(y0, midY), Math.max(y0, midY), col);
            g.horizontalLine(Math.min(x0, x1), Math.max(x0, x1), midY, col);
            g.verticalLine(x1 - 1, Math.min(midY, y1), Math.max(midY, y1), col);
        }
        g.fill(x1 - 3, y1 - 2, x1, y1 + 3, col);
    }

    private void renderNodeCard(GuiGraphicsExtractor g, PlacedNode pn, boolean selected, boolean hovered) {
        TechDefinition def = pn.def();
        TechState st = pn.state();
        int x = pn.x();
        int y = pn.y();

        int bg;
        int border;
        int titleCol;
        int badgeCol;
        switch (st) {
            case COMPLETED -> {
                bg = hovered ? 0xFF163B30 : 0xFF112E25;
                border = 0xFF10B981;
                titleCol = 0xFFECFDF5;
                badgeCol = 0xFF34D399;
            }
            case RESEARCHING -> {
                bg = hovered ? 0xFF1A3650 : 0xFF13293D;
                border = 0xFF38BDF8;
                titleCol = 0xFFF8FAFC;
                badgeCol = 0xFF7DD3FC;
            }
            case RESEARCHABLE -> {
                bg = hovered ? 0xFF233856 : 0xFF1B2B44;
                border = def.keystone() ? 0xFFF59E0B : 0xFF60A5FA;
                titleCol = 0xFFFFFFFF;
                badgeCol = 0xFF93C5FD;
            }
            default -> {
                bg = hovered ? 0xFF212936 : 0xFF181E27;
                border = def.keystone() ? 0xFFB45309 : 0xFF475569;
                titleCol = 0xFFCBD5E1;
                badgeCol = 0xFF94A3B8;
            }
        }

        if (selected) {
            g.outline(x - 1, y - 1, NODE_W + 2, NODE_H + 2, 0xFFFBBF24);
        }
        g.fill(x, y, x + NODE_W, y + NODE_H, bg);
        g.outline(x, y, NODE_W, NODE_H, border);

        String prefix = def.keystone() ? "★ " : "";
        String name = this.font.plainSubstrByWidth(prefix + resolveTechTitle(def), NODE_W - 10);
        g.text(this.font, name, x + 5, y + 4, titleCol, false);

        String badge = st.badge() + (def.fallback() ? "·S" : "");
        g.text(this.font, badge, x + 5, y + 15, badgeCol, false);

        int cost = TechApi.get().cost(TechApi.PLAYER_CIV_ID, def.id());
        String rpTxt = (cost > 0 ? cost : def.rp()) + " RP";
        g.text(this.font, rpTxt, x + NODE_W - this.font.width(rpTxt) - 5, y + 15, 0xFFE2E8F0, false);

        int know = TechApi.get().knowledge(TechApi.PLAYER_CIV_ID, def.id());
        float frac = st == TechState.COMPLETED ? 1.0f : (def.threshold() > 0 ? Math.clamp((float) know / def.threshold(), 0.0f, 1.0f) : 0.0f);
        int barFill = Math.round((NODE_W - 4) * frac);
        if (barFill > 0) {
            g.fill(x + 2, y + NODE_H - 3, x + 2 + barFill, y + NODE_H - 1, st == TechState.COMPLETED ? 0xFF10B981 : 0xFF38BDF8);
        }
    }

    private void renderDetailPanel(
            GuiGraphicsExtractor g,
            int dx,
            int dy,
            int dw,
            int dh,
            TechDefinitionLoader.TechGraphSnapshot snap
    ) {
        g.fill(dx, dy, dx + dw, dy + dh, 0xFF171F2B);
        g.fill(dx + 1, dy + 1, dx + dw - 1, dy + 20, 0xFF1E293B);
        g.horizontalLine(dx + 1, dx + dw - 2, dy + 20, 0xFF334155);
        g.outline(dx, dy, dw, dh, 0xFF475569);

        TechDefinition def = selectedTech != null ? snap.definitions().get(selectedTech) : null;
        if (def == null) {
            g.text(this.font, "Select a node", dx + 8, dy + 6, 0xFF94A3B8, false);
            return;
        }

        g.enableScissor(dx + 1, dy + 1, dx + dw - 1, dy + dh - 1);
        try {
            int maxTextW = dw - 12;
            String header = this.font.plainSubstrByWidth((def.keystone() ? "★ " : "") + resolveTechTitle(def), maxTextW);
            g.text(this.font, header, dx + 6, dy + 6, 0xFFFBBF24, false);

            TechState st = TechApi.get().state(TechApi.PLAYER_CIV_ID, def.id());
            Component stateLabel = Component.translatable("gui.tctechnology.state." + st.name().toLowerCase(Locale.ROOT));
            int stCol = st == TechState.COMPLETED ? 0xFF34D399 : (st == TechState.RESEARCHABLE ? 0xFF60A5FA : 0xFFCBD5E1);
            int cy = dy + 25;
            g.text(this.font, stateLabel, dx + 6, cy, stCol, false);
            cy += 12;

            g.text(this.font, this.font.plainSubstrByWidth(def.id().getPath(), maxTextW), dx + 6, cy, 0xFF94A3B8, false);
            cy += 13;

            int cost = TechApi.get().cost(TechApi.PLAYER_CIV_ID, def.id());
            int know = TechApi.get().knowledge(TechApi.PLAYER_CIV_ID, def.id());
            g.text(this.font, "Cost: " + (cost > 0 ? cost : def.rp()) + " RP", dx + 6, cy, 0xFFF1F5F9, false);
            cy += 12;
            g.text(this.font, "Knowledge: " + know + " / " + def.threshold(), dx + 6, cy, 0xFF93C5FD, false);
            cy += 11;

            int barW = dw - 12;
            g.fill(dx + 6, cy, dx + 6 + barW, cy + 5, 0xFF0D1219);
            float kFrac = def.threshold() > 0 ? Math.clamp((float) know / def.threshold(), 0.0f, 1.0f) : 1.0f;
            int kFill = Math.round((barW - 2) * (st == TechState.COMPLETED ? 1.0f : kFrac));
            if (kFill > 0) {
                g.fill(dx + 7, cy + 1, dx + 7 + kFill, cy + 4, st == TechState.COMPLETED ? 0xFF10B981 : 0xFF38BDF8);
            }
            g.outline(dx + 6, cy, barW, 5, 0xFF334155);
            cy += 10;

            g.text(this.font, this.font.plainSubstrByWidth("Lane: " + def.tree().lane() + " · " + def.owner(), maxTextW), dx + 6, cy, 0xFFCBD5E1, false);
            cy += 14;

            if (!def.prerequisites().isEmpty() && cy + 12 < dy + dh) {
                g.text(this.font, "Prerequisites:", dx + 6, cy, 0xFFFBBF24, false);
                cy += 11;
                for (Identifier pid : def.prerequisites()) {
                    if (cy + 10 > dy + dh - 2) {
                        break;
                    }
                    boolean pDone = TechApi.get().isUnlocked(TechApi.PLAYER_CIV_ID, pid);
                    TechDefinition pDef = snap.definitions().get(pid);
                    String pName = pDef != null ? resolveTechTitle(pDef) : pid.getPath();
                    String line = this.font.plainSubstrByWidth((pDone ? "✔ " : "○ ") + pName, maxTextW);
                    g.text(this.font, line, dx + 6, cy, pDone ? 0xFF34D399 : 0xFF94A3B8, false);
                    cy += 11;
                }
                cy += 3;
            }

            int unlockTotal = def.unlocks().recipes().size() + def.unlocks().items().size()
                    + def.unlocks().blocks().size() + def.unlocks().features().size() + def.unlocks().jobs().size();
            if (unlockTotal > 0 && cy + 10 <= dy + dh - 2) {
                g.text(this.font, "Unlocks: " + unlockTotal, dx + 6, cy, 0xFF6EE7B7, false);
            }
        } finally {
            g.disableScissor();
        }
    }

    private List<Component> buildNodeTooltip(PlacedNode pn, TechDefinitionLoader.TechGraphSnapshot snap) {
        TechDefinition def = pn.def();
        List<Component> tip = new ArrayList<>();
        tip.add(Component.literal((def.keystone() ? "★ " : "") + resolveTechTitle(def)).withStyle(ChatFormatting.GOLD));
        Component stateComp = Component.translatable("gui.tctechnology.state." + pn.state().name().toLowerCase(Locale.ROOT));
        int cost = TechApi.get().cost(TechApi.PLAYER_CIV_ID, def.id());
        int know = TechApi.get().knowledge(TechApi.PLAYER_CIV_ID, def.id());
        tip.add(Component.literal(stateComp.getString() + " · " + (cost > 0 ? cost : def.rp()) + " RP · K " + know + "/" + def.threshold())
                .withStyle(pn.state() == TechState.COMPLETED ? ChatFormatting.GREEN : ChatFormatting.AQUA));
        if (!def.prerequisites().isEmpty()) {
            tip.add(Component.literal("Requires " + def.prerequisites().size() + " prerequisite(s)").withStyle(ChatFormatting.GRAY));
        }
        String descKey = "tech." + def.id().getNamespace() + "." + def.id().getPath().replace('/', '.') + ".desc";
        String descVal = Component.translatable(descKey).getString();
        if (!descVal.equals(descKey)) {
            tip.add(Component.literal(descVal).withStyle(ChatFormatting.WHITE));
        }
        return tip;
    }

    private static String resolveTechTitle(TechDefinition def) {
        String key = "tech." + def.id().getNamespace() + "." + def.id().getPath().replace('/', '.');
        String localized = Component.translatable(key).getString();
        if (!localized.equals(key)) {
            return localized;
        }
        String raw = def.id().getPath();
        int slash = raw.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < raw.length()) {
            raw = raw.substring(slash + 1);
        }
        String[] parts = raw.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                sb.append(p.substring(1));
            }
        }
        return sb.toString();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        int detailW = detailPanelWidth();
        int canvasLeft = MARGIN_X + 6;
        int canvasRight = detailW > 0 ? (this.width - MARGIN_X - 12 - detailW) : (this.width - MARGIN_X - 6);
        int bodyTop = MARGIN_Y + 68;
        int bodyBottom = this.height - MARGIN_Y - 6;
        if (event.x() >= canvasLeft && event.x() <= canvasRight && event.y() >= bodyTop && event.y() <= bodyBottom) {
            TechDefinitionLoader.TechGraphSnapshot snap = TTechnology.loader().snapshot();
            List<PlacedNode> nodes = placeNodes(
                    filteredTechs(snap),
                    snap,
                    canvasLeft - (int) Math.round(panX),
                    bodyTop - (int) Math.round(panY)
            );
            for (PlacedNode pn : nodes) {
                if (pn.contains(event.x(), event.y())) {
                    this.selectedTech = pn.def().id();
                    this.draggingCanvas = true;
                    this.movedDuringDrag = false;
                    return true;
                }
            }
            this.draggingCanvas = true;
            this.movedDuringDrag = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingCanvas && (event.button() == 0 || event.button() == 1 || event.button() == 2)) {
            this.panX -= dragX;
            this.panY -= dragY;
            this.movedDuringDrag = true;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingCanvas) {
            draggingCanvas = false;
            if (movedDuringDrag) {
                movedDuringDrag = false;
                return true;
            }
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollX != 0.0 || scrollY != 0.0) {
            this.panX -= scrollX * 28.0;
            this.panY -= scrollY * 28.0;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<TechDefinition> filteredTechs(TechDefinitionLoader.TechGraphSnapshot snap) {
        List<TechDefinition> out = new ArrayList<>();
        for (TechDefinition def : snap.definitions().values()) {
            if (def.era() != selectedEra && searchQuery.isEmpty()) {
                continue;
            }
            if (!searchQuery.isEmpty()) {
                String title = resolveTechTitle(def).toLowerCase(Locale.ROOT);
                if (!def.id().toString().toLowerCase(Locale.ROOT).contains(searchQuery) && !title.contains(searchQuery)) {
                    continue;
                }
            }
            out.add(def);
        }
        out.sort(Comparator.comparing((TechDefinition d) -> d.tree().lane())
                .thenComparingInt(d -> d.tree().rowHint())
                .thenComparing(TechDefinition::id));
        return out;
    }
}
