package com.nightcity.atlas;

import com.nightcity.atlas.api.HudApi;
import com.nightcity.atlas.api.HudLevel;
import com.nightcity.atlas.api.HudSkin;
import com.nightcity.atlas.api.HudWidget;
import com.nightcity.atlas.client.hud.BuiltinHudWidgets;
import com.nightcity.atlas.client.hud.HudLayoutStore;
import com.nightcity.atlas.client.skin.HudSkinLoader;
import com.nightcity.atlas.compat.AtlasCompat;
import com.nightcity.atlas.compat.wildlands.WildlandsTerrainBridge;
import com.nightcity.atlas.data.AtlasPersistence;
import com.nightcity.atlas.map.MapTileCache;
import com.nightcity.atlas.map.SurveyAtlasStore;
import com.nightcity.atlas.waypoint.WaypointManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mod(TAtlas.MOD_ID)
public final class TAtlas {
    public static final String MOD_ID = "tcatlas";

    private static final HudSkinLoader SKIN_LOADER = new HudSkinLoader();
    private static final HudLayoutStore LAYOUT_STORE = new HudLayoutStore();
    private static final SurveyAtlasStore SURVEY_STORE = new SurveyAtlasStore();
    private static final WaypointManager WAYPOINT_MANAGER = new WaypointManager(SURVEY_STORE);
    private static volatile MapTileCache tileCache;

    private static final Map<Identifier, HudWidget> WIDGETS = new LinkedHashMap<>();
    private static final Deque<HudApi.Toast> TOASTS = new ArrayDeque<>();
    private static volatile String activeEra = "stone";
    private static final HudApi HUD_API = new HudApiImpl();

    static {
        BuiltinHudWidgets.registerDefaults(HUD_API);
    }

    public TAtlas(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SYNCED, AtlasConfig.SERVER_SPEC, "tcatlas-server.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, AtlasConfig.CLIENT_SPEC, "tcatlas-client.toml");
        com.nightcity.atlas.registry.AtlasRegistries.register(modBus);
        modBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.register(TAtlasEvents.class);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(AtlasCompat::initAll);
    }

    public static SurveyAtlasStore surveyStore() {
        return SURVEY_STORE;
    }

    public static WaypointManager waypoints() {
        return WAYPOINT_MANAGER;
    }

    public static MapTileCache tileCache() {
        MapTileCache current = tileCache;
        if (current == null) {
            synchronized (TAtlas.class) {
                current = tileCache;
                if (current == null) {
                    int gpuTiles = AtlasConfig.CLIENT_SPEC.isLoaded() ? AtlasConfig.GPU_CACHE_TILES.get() : 256;
                    long diskBytes = (AtlasConfig.CLIENT_SPEC.isLoaded() ? AtlasConfig.CLIENT_CACHE_MB.get() : 1024L) * 1024L * 1024L;
                    current = new MapTileCache(SURVEY_STORE, resolveClientCacheRoot().resolve("tiles"), gpuTiles, diskBytes, 4, 2);
                    tileCache = current;
                }
            }
        }
        return current;
    }

    public static Path resolveClientCacheRoot() {
        Path gameDir = FMLPaths.GAMEDIR.get();
        return (gameDir != null ? gameDir : Path.of("build")).resolve("tcatlas").resolve("local_world");
    }

    public static void saveClientState() {
        try {
            Path root = resolveClientCacheRoot();
            AtlasPersistence.writeCompressedAtomically(root.resolve("waypoints.dat"), AtlasPersistence.encodeWaypointsState(WAYPOINT_MANAGER));
        } catch (IOException ignored) {
        }
    }

    public static void loadClientState() {
        try {
            Path wpFile = resolveClientCacheRoot().resolve("waypoints.dat");
            if (java.nio.file.Files.isRegularFile(wpFile)) {
                CompoundTag tag = AtlasPersistence.readCompressed(wpFile);
                AtlasPersistence.decodeWaypointsState(tag, WAYPOINT_MANAGER, WildlandsTerrainBridge.isEastWestWrapActive(SurveyAtlasStore.OVERWORLD_ID));
            }
        } catch (IOException ignored) {
        }
    }

    public static HudApi hud() {
        return HUD_API;
    }

    public static HudSkinLoader skinLoader() {
        return SKIN_LOADER;
    }

    public static HudLayoutStore layoutStore() {
        return LAYOUT_STORE;
    }

    public static int snapGrid() {
        return AtlasConfig.CLIENT_SPEC.isLoaded() ? AtlasConfig.SNAP_GRID.get() : 4;
    }

    public static Path layoutFilePath() {
        return FMLPaths.CONFIGDIR.get().resolve("tcatlas-hud-layout.json");
    }

    public static void saveLayout() {
        try {
            LAYOUT_STORE.save(layoutFilePath());
        } catch (IOException ignored) {
        }
    }

    public static void loadLayout() {
        try {
            LAYOUT_STORE.load(layoutFilePath());
        } catch (IOException ignored) {
        }
    }

    private static final class HudApiImpl implements HudApi {
        @Override
        public synchronized void registerWidget(HudWidget widget) {
            WIDGETS.put(widget.id(), widget);
            LAYOUT_STORE.placementFor(widget);
        }

        @Override
        public Collection<HudWidget> widgets() {
            return Collections.unmodifiableCollection(WIDGETS.values());
        }

        @Override
        public synchronized void pushToast(Toast toast) {
            if (toast.dedupKey() != null) {
                TOASTS.removeIf(t -> toast.dedupKey().equals(t.dedupKey()));
            }
            TOASTS.addLast(toast);
            while (TOASTS.size() > 8) {
                TOASTS.removeFirst();
            }
        }

        @Override
        public HudLevel currentLevel() {
            return LAYOUT_STORE.hudLevel();
        }

        @Override
        public void setLevel(HudLevel level) {
            LAYOUT_STORE.setHudLevel(level);
        }

        @Override
        public HudSkin currentSkin() {
            return SKIN_LOADER.skinForEra(activeEra);
        }

        @Override
        public void setActiveEraSkin(String eraId) {
            if (eraId != null && !eraId.isBlank()) {
                activeEra = eraId;
            }
        }
    }
}
