package com.nightcity.atlas.client.map;

import com.mojang.blaze3d.platform.InputConstants;
import com.nightcity.atlas.TAtlas;
import com.nightcity.atlas.api.HudApi;
import com.nightcity.atlas.api.HudSkin;
import com.nightcity.atlas.api.OkabeItoPalette;
import com.nightcity.atlas.api.Waypoint;
import com.nightcity.atlas.compat.wildlands.WildlandsTerrainBridge;
import com.nightcity.atlas.map.PlanetProjection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Waypoint creation & editing dialog (B key / World Map right-click, 22 §9.4, §10.3).
 * Enforces Okabe-Ito colorblind-safe colors paired with distinct geometric shapes (31 UP-8).
 */
public final class WaypointDialogScreen extends Screen {
    private static final List<OkabeItoPalette.WaypointCategory> CATEGORIES = OkabeItoPalette.WaypointCategory.userSelectable();
    private static final List<OkabeItoPalette.WaypointShape> SHAPES = List.of(
            OkabeItoPalette.WaypointShape.HOUSE,
            OkabeItoPalette.WaypointShape.DIAMOND,
            OkabeItoPalette.WaypointShape.TRIANGLE_BANG,
            OkabeItoPalette.WaypointShape.ARCH,
            OkabeItoPalette.WaypointShape.CIRCLE_DOT,
            OkabeItoPalette.WaypointShape.ANCHOR,
            OkabeItoPalette.WaypointShape.STAR
    );

    @Nullable
    private final Screen parent;
    @Nullable
    private final Waypoint existing;
    private final Identifier dimension;
    private final BlockPos targetPos;

    private static final int PANEL_W = 276;
    private static final int PANEL_H = 204;

    private EditBox nameBox;
    private EditBox noteBox;
    private Button colorBtn;
    private Button shapeBtn;
    private String draftName;
    private String draftNote;
    private int categoryIdx = 6; // CUSTOM
    private int colorIdx = 6;
    private int shapeIdx = 6;
    private Waypoint.Share share = Waypoint.Share.PRIVATE;
    private String statusError = "";

    public WaypointDialogScreen(@Nullable Screen parent, Identifier dimension, BlockPos targetPos, @Nullable Waypoint existing) {
        super(Component.translatable(existing == null ? "gui.tcatlas.waypoint.new_title" : "gui.tcatlas.waypoint.edit_title"));
        this.parent = parent;
        this.existing = existing;
        this.dimension = dimension;
        this.targetPos = targetPos;
        if (existing != null) {
            int ci = CATEGORIES.indexOf(existing.category());
            if (ci >= 0) {
                this.categoryIdx = ci;
            }
            int coli = OkabeItoPalette.SAFE_COLORS.indexOf(existing.argb());
            if (coli >= 0) {
                this.colorIdx = coli;
            }
            int si = SHAPES.indexOf(existing.shape());
            if (si >= 0) {
                this.shapeIdx = si;
            }
            this.share = existing.share();
        }
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        if (draftName == null) {
            draftName = existing != null
                    ? existing.name()
                    : "路点 #" + (TAtlas.waypoints().allWaypointsSnapshot().size() + 1);
        }
        if (draftNote == null) {
            draftNote = existing != null ? existing.note().orElse("") : "";
        }

        nameBox = new EditBox(this.font, left + 52, top + 32, PANEL_W - 64, 16, Component.translatable("gui.tcatlas.waypoint.name"));
        nameBox.setMaxLength(Waypoint.MAX_NAME_LENGTH);
        nameBox.setValue(draftName);
        nameBox.setResponder(val -> draftName = val);
        addRenderableWidget(nameBox);

        int colW = (PANEL_W - 32) / 2;
        addRenderableWidget(Button.builder(categoryLabel(), btn -> {
            categoryIdx = (categoryIdx + 1) % CATEGORIES.size();
            OkabeItoPalette.WaypointCategory cat = CATEGORIES.get(categoryIdx);
            int defCol = OkabeItoPalette.SAFE_COLORS.indexOf(cat.defaultArgb());
            if (defCol >= 0) {
                colorIdx = defCol;
            }
            int defShape = SHAPES.indexOf(cat.defaultShape());
            if (defShape >= 0) {
                shapeIdx = defShape;
            }
            btn.setMessage(categoryLabel());
            if (colorBtn != null) {
                colorBtn.setMessage(colorLabel());
            }
            if (shapeBtn != null) {
                shapeBtn.setMessage(shapeLabel());
            }
        }).bounds(left + 12, top + 56, colW, 18).build());

        addRenderableWidget(Button.builder(shareLabel(), btn -> {
            share = (share == Waypoint.Share.PRIVATE) ? Waypoint.Share.CIVILIZATION : Waypoint.Share.PRIVATE;
            btn.setMessage(shareLabel());
        }).bounds(left + 20 + colW, top + 56, colW, 18).build());

        colorBtn = Button.builder(colorLabel(), btn -> {
            colorIdx = (colorIdx + 1) % OkabeItoPalette.SAFE_COLORS.size();
            btn.setMessage(colorLabel());
        }).bounds(left + 12, top + 80, colW, 18).build();
        addRenderableWidget(colorBtn);

        shapeBtn = Button.builder(shapeLabel(), btn -> {
            shapeIdx = (shapeIdx + 1) % SHAPES.size();
            btn.setMessage(shapeLabel());
        }).bounds(left + 20 + colW, top + 80, colW, 18).build();
        addRenderableWidget(shapeBtn);

        noteBox = new EditBox(this.font, left + 52, top + 106, PANEL_W - 64, 16, Component.translatable("gui.tcatlas.waypoint.note"));
        noteBox.setMaxLength(Waypoint.MAX_NOTE_LENGTH);
        noteBox.setValue(draftNote);
        noteBox.setResponder(val -> draftNote = val);
        addRenderableWidget(noteBox);

        int btnY = top + PANEL_H - 26;
        addRenderableWidget(Button.builder(Component.translatable("gui.tcatlas.waypoint.save"), b -> saveAndClose(false))
                .bounds(left + 12, btnY, 58, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.tcatlas.waypoint.navigate"), b -> saveAndClose(true))
                .bounds(left + 75, btnY, 62, 18).build());
        if (existing != null) {
            addRenderableWidget(Button.builder(Component.translatable("gui.tcatlas.waypoint.delete"), b -> deleteAndClose())
                    .bounds(left + 142, btnY, 58, 18).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.tcatlas.waypoint.cancel"), b -> onClose())
                .bounds(left + PANEL_W - 70, btnY, 58, 18).build());

        this.setInitialFocus(nameBox);
    }

    private Component categoryLabel() {
        return Component.literal("类别: " + CATEGORIES.get(categoryIdx).id());
    }

    private Component shareLabel() {
        return Component.literal("共享: " + (share == Waypoint.Share.PRIVATE ? "个人" : "文明"));
    }

    private Component colorLabel() {
        int col = OkabeItoPalette.SAFE_COLORS.get(colorIdx) & 0xFFFFFF;
        return Component.literal(String.format(Locale.ROOT, "色板: #%06X", col));
    }

    private Component shapeLabel() {
        OkabeItoPalette.WaypointShape s = SHAPES.get(shapeIdx);
        return Component.literal("形状: " + s.glyph() + " " + s.id());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft != null && this.minecraft.level != null) {
            this.extractBlurredBackground(graphics);
        }
        graphics.fillGradient(0, 0, this.width, this.height, 0xCC090D14, 0xE205070B);

        HudSkin skin = HudApi.get().currentSkin();
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;
        int bgCol = 0xFF000000 | skin.palette().bgFill();
        int borderCol = 0xFF000000 | skin.palette().bgBorder();

        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, bgCol);
        graphics.fill(left + 1, top + 1, left + PANEL_W - 1, top + 24, 0xFF1A1714);
        graphics.horizontalLine(left + 1, left + PANEL_W - 2, top + 24, borderCol);
        graphics.outline(left, top, PANEL_W, PANEL_H, borderCol);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        HudSkin skin = HudApi.get().currentSkin();
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;
        int accentCol = 0xFF000000 | skin.palette().accent();
        int primaryCol = 0xFF000000 | skin.palette().textPrimary();
        int secondaryCol = 0xFF000000 | skin.palette().textSecondary();
        int dangerCol = 0xFF000000 | skin.palette().danger();
        int borderCol = 0xFF000000 | skin.palette().bgBorder();

        graphics.text(this.font, this.title, left + 12, top + 8, accentCol, false);
        graphics.text(this.font, "名称:", left + 12, top + 36, primaryCol, false);
        graphics.text(this.font, "备注:", left + 12, top + 110, primaryCol, false);

        int chosenColor = 0xFF000000 | OkabeItoPalette.SAFE_COLORS.get(colorIdx);
        int previewBoxX = left + PANEL_W - 44;
        int previewBoxY = top + 4;
        graphics.fill(previewBoxX, previewBoxY, previewBoxX + 32, previewBoxY + 16, 0xFF12100E);
        graphics.outline(previewBoxX, previewBoxY, 32, 16, borderCol);
        graphics.fill(previewBoxX + 3, previewBoxY + 3, previewBoxX + 13, previewBoxY + 13, chosenColor);
        MarkerShapeRenderer.drawShape(
                graphics,
                SHAPES.get(shapeIdx),
                previewBoxX + 23,
                previewBoxY + 8,
                chosenColor,
                1.0f,
                true
        );

        boolean wrapX = WildlandsTerrainBridge.isEastWestWrapActive(dimension);
        int tier = TAtlas.surveyStore().standaloneTier();
        String posText;
        if (tier >= 10) {
            posText = String.format(Locale.ROOT, "坐标: X=%d, Z=%d", targetPos.getX(), targetPos.getZ());
        } else if (tier >= 5) {
            double lat = PlanetProjection.latDeg(targetPos.getZ());
            double lon = PlanetProjection.lonDeg(targetPos.getX(), wrapX);
            posText = String.format(Locale.ROOT, "位置: %.1f°, %.1f°", lat, lon);
        } else {
            posText = "位置: 此处（测绘精度随时代提升）";
        }
        graphics.fill(left + 12, top + 128, left + PANEL_W - 12, top + 146, 0xFF181512);
        graphics.outline(left + 12, top + 128, PANEL_W - 24, 18, borderCol);
        graphics.text(this.font, posText, left + 18, top + 133, secondaryCol, false);

        if (!statusError.isEmpty()) {
            String errStr = this.font.plainSubstrByWidth(Component.translatable(statusError).getString(), PANEL_W - 24);
            graphics.text(this.font, errStr, left + 12, top + 152, dangerCol, false);
        }

        graphics.nextStratum();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
            saveAndClose(false);
            return true;
        }
        return super.keyPressed(event);
    }

    private void saveAndClose(boolean startNav) {
        Minecraft mc = Minecraft.getInstance();
        UUID playerId = mc.player != null ? mc.player.getUUID() : new UUID(0L, 1L);
        long nowSeconds = mc.level != null ? mc.level.getGameTime() / 20L : 0L;
        long currentDay = mc.level != null ? mc.level.getOverworldClockTime() / 24000L : 0L;
        boolean wrapX = WildlandsTerrainBridge.isEastWestWrapActive(dimension);
        int tier = TAtlas.surveyStore().standaloneTier();

        if (tier <= 0 && existing == null) {
            TAtlas.waypoints().placeMemoryMark(playerId, dimension, targetPos, nowSeconds, nameBox.getValue(), wrapX);
            TAtlas.saveClientState();
            if (mc.player != null) {
                mc.player.sendOverlayMessage(Component.translatable("msg.tcatlas.memory_mark_placed"));
            }
            onClose();
            return;
        }

        OkabeItoPalette.WaypointCategory cat = CATEGORIES.get(categoryIdx);
        int argb = OkabeItoPalette.SAFE_COLORS.get(colorIdx);
        OkabeItoPalette.WaypointShape shape = SHAPES.get(shapeIdx);
        Optional<String> note = noteBox.getValue().isBlank() ? Optional.empty() : Optional.of(noteBox.getValue().trim());

        if (existing != null) {
            Waypoint updated = new Waypoint(
                    existing.id(), nameBox.getValue(), cat, cat.identifier(),
                    argb, shape, share, dimension, targetPos, existing.epsilon(),
                    existing.creator(), existing.createdDay(), note, existing.routeOrder()
            );
            TAtlas.waypoints().updateWaypoint(playerId, updated, wrapX);
            if (startNav) {
                TAtlas.waypoints().startNavigation(playerId, updated.id());
            }
            TAtlas.saveClientState();
            onClose();
            return;
        }

        Waypoint candidate = new Waypoint(
                UUID.randomUUID(), nameBox.getValue(), cat, cat.identifier(),
                argb, shape, share, dimension, targetPos, 8,
                playerId, currentDay, note, Optional.empty()
        );
        var result = TAtlas.waypoints().addWaypoint(candidate, tier, wrapX);
        if (!result.accepted()) {
            this.statusError = result.reasonKey();
            return;
        }
        if (startNav && result.waypoint() != null) {
            TAtlas.waypoints().startNavigation(playerId, result.waypoint().id());
        }
        TAtlas.saveClientState();
        onClose();
    }

    private void deleteAndClose() {
        if (existing != null) {
            Minecraft mc = Minecraft.getInstance();
            UUID playerId = mc.player != null ? mc.player.getUUID() : existing.creator();
            TAtlas.waypoints().removeWaypoint(playerId, existing.id());
            TAtlas.saveClientState();
        }
        onClose();
    }

    @Override
    public void onClose() {
        if (parent != null) {
            Minecraft.getInstance().setScreenAndShow(parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
