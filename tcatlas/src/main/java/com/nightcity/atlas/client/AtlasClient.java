package com.nightcity.atlas.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.nightcity.atlas.AtlasConfig;
import com.nightcity.atlas.TAtlas;
import com.nightcity.atlas.api.HudApi;
import com.nightcity.atlas.api.HudLevel;
import com.nightcity.atlas.api.HudSkin;
import com.nightcity.atlas.api.HudWidget;
import com.nightcity.atlas.api.OkabeItoPalette;
import com.nightcity.atlas.api.Waypoint;
import com.nightcity.atlas.client.hud.BuiltinHudWidgets;
import com.nightcity.atlas.client.hud.HudLayoutEditorScreen;
import com.nightcity.atlas.client.hud.HudLayoutStore;
import com.nightcity.atlas.api.HudAnchor;
import com.nightcity.atlas.client.map.ClientTileTextureManager;
import com.nightcity.atlas.client.map.MinimapRenderer;
import com.nightcity.atlas.client.map.WaypointDialogScreen;
import com.nightcity.atlas.client.map.WorldMapScreen;
import com.nightcity.atlas.compat.wildlands.WildlandsTerrainBridge;
import com.nightcity.atlas.map.ChunkTerrainSummary;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

@Mod(value = TAtlas.MOD_ID, dist = Dist.CLIENT)
public final class AtlasClient {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(TAtlas.MOD_ID, "controls"));

    private static final ClientTileTextureManager GPU_TILE_TEXTURES = new ClientTileTextureManager();
    private static final MinimapRenderer MINIMAP_RENDERER = new MinimapRenderer();

    private static KeyMapping cycleHudKey;
    private static KeyMapping hudEditorKey;
    private static KeyMapping openMapKey;
    private static KeyMapping addWaypointKey;
    private static KeyMapping quickWaypointKey;
    private static KeyMapping minimapModeKey;
    private static boolean layoutLoaded;
    private static int clientScanTicker;

    private static boolean coreSyncResolved;
    @Nullable
    private static Method coreSetHudHiddenMethod;
    @Nullable
    private static Method coreSetLevelMethod;
    @Nullable
    private static Class<? extends Enum> coreHudLevelEnum;
    @Nullable
    private static HudLevel lastSyncedCoreLevel;

    public AtlasClient(IEventBus modBus) {
        modBus.addListener(AtlasClient::onRegisterKeys);
        modBus.addListener(AtlasClient::onAddReloadListeners);
        modBus.addListener(AtlasClient::onRegisterGuiLayers);
        NeoForge.EVENT_BUS.addListener(AtlasClient::onClientTick);
    }

    public static MinimapRenderer minimapRenderer() {
        return MINIMAP_RENDERER;
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        cycleHudKey = new KeyMapping(
                "key.tcatlas.hud_level", KeyConflictContext.IN_GAME, KeyModifier.NONE,
                InputConstants.Type.KEYBOARD, InputConstants.KEY_H, CATEGORY
        );
        hudEditorKey = new KeyMapping(
                "key.tcatlas.layout_editor", KeyConflictContext.IN_GAME, KeyModifier.NONE,
                InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), CATEGORY
        );
        openMapKey = new KeyMapping(
                "key.tcatlas.world_map", KeyConflictContext.IN_GAME, KeyModifier.NONE,
                InputConstants.Type.KEYBOARD, InputConstants.KEY_M, CATEGORY
        );
        addWaypointKey = new KeyMapping(
                "key.tcatlas.add_waypoint", KeyConflictContext.IN_GAME, KeyModifier.NONE,
                InputConstants.Type.KEYBOARD, InputConstants.KEY_N, CATEGORY
        );
        quickWaypointKey = new KeyMapping(
                "key.tcatlas.add_waypoint_quick", KeyConflictContext.IN_GAME, KeyModifier.NONE,
                InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), CATEGORY
        );
        minimapModeKey = new KeyMapping(
                "key.tcatlas.minimap_mode", KeyConflictContext.IN_GAME, KeyModifier.NONE,
                InputConstants.Type.KEYBOARD, InputConstants.KEY_B, CATEGORY
        );
        event.register(cycleHudKey);
        event.register(hudEditorKey);
        event.register(openMapKey);
        event.register(addWaypointKey);
        event.register(quickWaypointKey);
        event.register(minimapModeKey);
    }

    private static void onAddReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(TAtlas.MOD_ID, "skins"), TAtlas.skinLoader());
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Identifier.fromNamespaceAndPath(TAtlas.MOD_ID, "hud_overlay"), AtlasClient::renderHudLayer);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        if (!layoutLoaded) {
            TAtlas.loadLayout();
            TAtlas.loadClientState();
            TAtlas.tileCache().setEvictionCallback(GPU_TILE_TEXTURES::onCacheEvict);
            syncUnifiedHudToCore(HudApi.get().currentLevel());
            layoutLoaded = true;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        BuiltinHudWidgets.tickHudState();
        syncUnifiedHudToCore(HudApi.get().currentLevel());

        if (++clientScanTicker >= 20) {
            clientScanTicker = 0;
            scanNearbyClientChunks(mc);
        }
        while (hudEditorKey != null && hudEditorKey.consumeClick()) {
            if (mc.canInterruptScreen()) {
                mc.setScreenAndShow(new HudLayoutEditorScreen(TAtlas.layoutStore(), TAtlas::saveLayout));
            }
        }
        while (cycleHudKey != null && cycleHudKey.consumeClick()) {
            if (mc.canInterruptScreen()) {
                if (mc.hasControlDown() && mc.hasShiftDown()) {
                    mc.setScreenAndShow(new HudLayoutEditorScreen(TAtlas.layoutStore(), TAtlas::saveLayout));
                } else {
                    HudLevel next = HudApi.get().cycleLevel();
                    syncUnifiedHudToCore(next);
                    TAtlas.saveLayout();
                    mc.player.sendOverlayMessage(Component.literal("HUD: " + formatUnifiedHudLevel(next)));
                }
            }
        }
        while (openMapKey != null && openMapKey.consumeClick()) {
            if (mc.canInterruptScreen()) {
                mc.setScreenAndShow(new WorldMapScreen(GPU_TILE_TEXTURES));
            }
        }
        while (quickWaypointKey != null && quickWaypointKey.consumeClick()) {
            if (mc.canInterruptScreen()) {
                handleAddWaypoint(mc, true);
            }
        }
        while (addWaypointKey != null && addWaypointKey.consumeClick()) {
            if (mc.canInterruptScreen()) {
                handleAddWaypoint(mc, mc.hasShiftDown());
            }
        }
        while (minimapModeKey != null && minimapModeKey.consumeClick()) {
            int tier = TAtlas.surveyStore().standaloneTier();
            boolean debugOn = AtlasConfig.isMinimapDebugEnabled();
            if (!MinimapRenderer.isAllowedToRender(tier, debugOn)) {
                mc.player.sendOverlayMessage(Component.translatable("msg.tcatlas.minimap_locked"));
            } else {
                MinimapRenderer.Mode nextMode = MINIMAP_RENDERER.cycleMode();
                mc.player.sendOverlayMessage(Component.literal("Minimap: " + nextMode.name()));
            }
        }
    }

    private static String formatUnifiedHudLevel(HudLevel level) {
        return switch (level) {
            case HIDDEN -> "OFF";
            case MINIMAL -> "MINIMAL";
            case STANDARD -> "STANDARD";
            case TACTICAL -> "FULL";
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void syncUnifiedHudToCore(HudLevel level) {
        if (level == lastSyncedCoreLevel) {
            return;
        }
        lastSyncedCoreLevel = level;
        if (ModList.get() == null || !ModList.get().isLoaded("tcivilization")) {
            return;
        }
        if (!coreSyncResolved) {
            coreSyncResolved = true;
            try {
                Class<?> layerCls = Class.forName("com.nightcity.tcivilization.client.TcHudLayer");
                Class<? extends Enum> enumCls = (Class<? extends Enum>) Class.forName("com.nightcity.tcivilization.client.api.HudLevel");
                coreSetHudHiddenMethod = layerCls.getMethod("setHudHidden", boolean.class);
                coreSetLevelMethod = layerCls.getMethod("setLevel", enumCls);
                coreHudLevelEnum = enumCls;
            } catch (Throwable ignored) {
            }
        }
        Method setHidden = coreSetHudHiddenMethod;
        Method setLevel = coreSetLevelMethod;
        Class<? extends Enum> enumCls = coreHudLevelEnum;
        if (setHidden == null || setLevel == null || enumCls == null) {
            return;
        }
        try {
            if (level == HudLevel.HIDDEN) {
                setHidden.invoke(null, true);
            } else {
                setHidden.invoke(null, false);
                String mapped = switch (level) {
                    case MINIMAL -> "MINIMAL";
                    case STANDARD -> "STANDARD";
                    case TACTICAL, HIDDEN -> "EXPANDED";
                };
                setLevel.invoke(null, Enum.valueOf(enumCls, mapped));
            }
        } catch (Throwable ignored) {
        }
    }

    private static void handleAddWaypoint(Minecraft mc, boolean quick) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        BlockPos target = resolveTargetBlockPos(mc);
        Identifier dim = mc.level.dimension().identifier();
        boolean wrapX = WildlandsTerrainBridge.isEastWestWrapActive(dim);
        int tier = TAtlas.surveyStore().standaloneTier();
        long nowSeconds = mc.level.getGameTime() / 20L;
        long currentDay = mc.level.getOverworldClockTime() / 24000L;

        if (!quick) {
            mc.setScreenAndShow(new WaypointDialogScreen(null, dim, target, null));
            return;
        }
        if (tier <= 0) {
            TAtlas.waypoints().placeMemoryMark(mc.player.getUUID(), dim, target, nowSeconds, "", wrapX);
            TAtlas.saveClientState();
            mc.player.sendOverlayMessage(Component.translatable("msg.tcatlas.memory_mark_placed"));
            return;
        }
        String defaultName = "路点 #" + (TAtlas.waypoints().allWaypointsSnapshot().size() + 1);
        Waypoint candidate = new Waypoint(
                UUID.randomUUID(), defaultName, OkabeItoPalette.WaypointCategory.CUSTOM,
                OkabeItoPalette.WaypointCategory.CUSTOM.identifier(),
                OkabeItoPalette.WaypointCategory.CUSTOM.defaultArgb(),
                OkabeItoPalette.WaypointCategory.CUSTOM.defaultShape(),
                Waypoint.Share.PRIVATE, dim, target, 8,
                mc.player.getUUID(), currentDay, Optional.empty(), Optional.empty()
        );
        var res = TAtlas.waypoints().addWaypoint(candidate, tier, wrapX);
        if (res.accepted()) {
            TAtlas.saveClientState();
            mc.player.sendOverlayMessage(Component.literal("[tcatlas] 已添加 " + defaultName));
        } else {
            mc.player.sendOverlayMessage(Component.translatable(res.reasonKey()));
        }
    }

    private static BlockPos resolveTargetBlockPos(Minecraft mc) {
        if (mc.hitResult instanceof BlockHitResult bhr && mc.hitResult.getType() == HitResult.Type.BLOCK
                && mc.player != null && bhr.getLocation().distanceToSqr(mc.player.getEyePosition()) <= 64.0 * 64.0) {
            return bhr.getBlockPos();
        }
        return mc.player != null ? mc.player.blockPosition() : BlockPos.ZERO;
    }

    private static void scanNearbyClientChunks(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        Identifier dim = mc.level.dimension().identifier();
        boolean wrapX = WildlandsTerrainBridge.isEastWestWrapActive(dim);
        long nowSeconds = mc.level.getGameTime() / 20L;
        int pcx = mc.player.chunkPosition().x();
        int pcz = mc.player.chunkPosition().z();
        for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                int cx = pcx + dx;
                int cz = pcz + dz;
                TAtlas.surveyStore().recordPersonalSighting(mc.player.getUUID(), dim, cx, cz, nowSeconds, wrapX);
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    ChunkAccess loaded = mc.level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
                    if (loaded != null) {
                        TAtlas.surveyStore().putSummary(dim, cx, cz, ChunkTerrainSummary.fromLoadedChunk(mc.level, loaded), wrapX);
                    }
                }
            }
        }
    }

    private static void renderHudLayer(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gui == null || mc.gui.hud.isHidden() || mc.gui.screen() != null) {
            return;
        }
        HudLevel lvl = HudApi.get().currentLevel();
        syncUnifiedHudToCore(lvl);
        if (lvl == HudLevel.HIDDEN) {
            return;
        }
        HudSkin skin = HudApi.get().currentSkin();
        HudWidget.HudContext ctx = new HudWidget.HudContext(lvl, skin, deltaTracker.getGameTimeDeltaPartialTick(false), false);
        int sw = graphics.guiWidth();
        int sh = graphics.guiHeight();
        boolean civLoaded = ModList.get() != null && ModList.get().isLoaded("tcivilization");
        for (HudWidget widget : HudApi.get().widgets()) {
            HudLayoutStore.WidgetPlacement pl = TAtlas.layoutStore().placementFor(widget);
            if (!pl.enabled() || !widget.shouldRender(ctx)) {
                continue;
            }
            HudWidget.HudSize sz = widget.measure(ctx);
            int w = Math.round(sz.width() * pl.scale());
            int h = Math.round(sz.height() * pl.scale());
            int x = pl.anchor().resolveX(sw, w, pl.offsetX());
            int y = pl.anchor().resolveY(sh, h, pl.offsetY());
            if (civLoaded && widget.id().equals(BuiltinHudWidgets.STATUS_STRIP.id())
                    && pl.anchor() == HudAnchor.TOP_LEFT && y < 26) {
                y = 26;
            }
            widget.render(graphics, x, y, w, h, ctx);
        }
    }
}
