package com.nightcity.atlas.compat.tcivilization;

import com.nightcity.atlas.TAtlas;
import com.nightcity.atlas.api.OkabeItoPalette;
import com.nightcity.atlas.api.Waypoint;
import com.nightcity.atlas.compat.wildlands.WildlandsTerrainBridge;
import com.nightcity.atlas.data.AtlasPersistence;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Soft/reflective bridge to tcivilization core APIs (22 §3.1, §12.4, 30 VO-UI-01/02):
 * - Registers LOCATOR_BAR and COORDINATES ownership on VanillaPolicyService
 * - Registers tcatlas:waypoint_nav on TrackerApi and pins/unpins active waypoint navigation
 * - Registers tcatlas:atlas_wheel on CommandWheelRegistry for Z Command Wheel options
 * - Registers formatVersion = 1 migrators on TcMigrators
 */
public final class TcivilizationBridge {
    public static final int OWNER_PRIORITY = 100;
    public static final Identifier WAYPOINT_NAV_PIN_TYPE = Identifier.fromNamespaceAndPath(TAtlas.MOD_ID, "waypoint_nav");
    public static final Identifier WHEEL_PROVIDER_ID = Identifier.fromNamespaceAndPath(TAtlas.MOD_ID, "atlas_wheel");

    public static final Identifier OPT_ADD_WAYPOINT = Identifier.fromNamespaceAndPath(TAtlas.MOD_ID, "add_waypoint_here");
    public static final Identifier OPT_NAVIGATE_HERE = Identifier.fromNamespaceAndPath(TAtlas.MOD_ID, "navigate_here");
    public static final Identifier OPT_PING_HERE = Identifier.fromNamespaceAndPath(TAtlas.MOD_ID, "ping_here");
    public static final Identifier OPT_REQUEST_SURVEY = Identifier.fromNamespaceAndPath(TAtlas.MOD_ID, "request_survey_here");

    public record WheelEntrySpec(Identifier id, Identifier icon, Component label, int priority, boolean enabled) {
    }

    private static volatile boolean initialized;
    private static final Map<String, Integer> REGISTERED_VANILLA_SYSTEMS = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> LOCAL_TRACKER_PINS = new ConcurrentHashMap<>();

    private static volatile Object trackerApiInstance;
    private static volatile Method trackerPinMethod;
    private static volatile Method trackerUnpinMethod;
    private static volatile Constructor<?> trackerPinConstructor;

    private TcivilizationBridge() {
    }

    public static void init() {
        initWithClassLoader(TcivilizationBridge.class.getClassLoader());
    }

    public static synchronized void initWithClassLoader(ClassLoader loader) {
        REGISTERED_VANILLA_SYSTEMS.put("LOCATOR_BAR", OWNER_PRIORITY);
        REGISTERED_VANILLA_SYSTEMS.put("COORDINATES", OWNER_PRIORITY);
        bindVanillaPolicy(loader);
        bindTrackerApi(loader);
        bindCommandWheel(loader);
        bindTcMigrators(loader);
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static Map<String, Integer> registeredVanillaSystems() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(REGISTERED_VANILLA_SYSTEMS));
    }

    public static Map<UUID, UUID> pinnedTrackerWaypoints() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(LOCAL_TRACKER_PINS));
    }

    public static List<WheelEntrySpec> buildWheelSpecs(boolean settlementsLoaded) {
        Identifier icon = Identifier.fromNamespaceAndPath(TAtlas.MOD_ID, "textures/item/magnetic_compass.png");
        List<WheelEntrySpec> list = new ArrayList<>(4);
        list.add(new WheelEntrySpec(OPT_ADD_WAYPOINT, icon, Component.translatable("wheel.tcatlas.add_waypoint"), 80, true));
        list.add(new WheelEntrySpec(OPT_NAVIGATE_HERE, icon, Component.translatable("wheel.tcatlas.navigate_here"), 75, true));
        list.add(new WheelEntrySpec(OPT_PING_HERE, icon, Component.translatable("wheel.tcatlas.ping_here"), 70, true));
        if (settlementsLoaded) {
            list.add(new WheelEntrySpec(OPT_REQUEST_SURVEY, icon, Component.translatable("wheel.tcatlas.request_survey"), 65, true));
        }
        return List.copyOf(list);
    }

    public static void syncNavigationTrackerPin(UUID playerId, Waypoint waypoint) {
        LOCAL_TRACKER_PINS.put(playerId, waypoint.id());
        if (trackerApiInstance != null && trackerPinMethod != null && trackerPinConstructor != null) {
            try {
                Object pin = trackerPinConstructor.newInstance(
                        WAYPOINT_NAV_PIN_TYPE,
                        waypoint.id().toString(),
                        Component.literal(waypoint.name())
                );
                trackerPinMethod.invoke(trackerApiInstance, playerId, pin);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    public static void unpinNavigationTracker(UUID playerId, UUID waypointId) {
        LOCAL_TRACKER_PINS.remove(playerId, waypointId);
        if (trackerApiInstance != null && trackerUnpinMethod != null) {
            try {
                trackerUnpinMethod.invoke(trackerApiInstance, playerId, WAYPOINT_NAV_PIN_TYPE, waypointId.toString());
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void bindVanillaPolicy(ClassLoader loader) {
        try {
            Class<?> policyCls = Class.forName("com.nightcity.tcivilization.api.VanillaPolicyService", true, loader);
            Class<? extends Enum> sysEnum = (Class<? extends Enum>) Class.forName("com.nightcity.tcivilization.api.VanillaSystem", true, loader);
            Object service = policyCls.getMethod("get").invoke(null);
            Method registerOwner = policyCls.getMethod("registerOwner", sysEnum, String.class, int.class);
            Object locatorBar = Enum.valueOf(sysEnum, "LOCATOR_BAR");
            Object coordinates = Enum.valueOf(sysEnum, "COORDINATES");
            registerOwner.invoke(service, locatorBar, TAtlas.MOD_ID, OWNER_PRIORITY);
            registerOwner.invoke(service, coordinates, TAtlas.MOD_ID, OWNER_PRIORITY);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void bindTrackerApi(ClassLoader loader) {
        try {
            Class<?> trackerCls = Class.forName("com.nightcity.tcivilization.api.TrackerApi", true, loader);
            Class<?> pinCls = Class.forName("com.nightcity.tcivilization.api.TrackerPin", true, loader);
            Class<?> providerCls = Class.forName("com.nightcity.tcivilization.api.TrackerApi$TrackableProvider", true, loader);
            Class<?> progressCls = Class.forName("com.nightcity.tcivilization.api.TrackerApi$Progress", true, loader);
            Constructor<?> progressCtor = progressCls.getConstructor(float.class, String.class, boolean.class);

            Object api = trackerCls.getMethod("get").invoke(null);
            trackerApiInstance = api;
            trackerPinConstructor = pinCls.getConstructor(Identifier.class, String.class, Component.class);
            trackerPinMethod = trackerCls.getMethod("pin", UUID.class, pinCls);
            trackerUnpinMethod = trackerCls.getMethod("unpin", UUID.class, Identifier.class, String.class);

            Object providerProxy = Proxy.newProxyInstance(loader, new Class<?>[]{providerCls}, (proxy, method, args) -> {
                if ("evaluate".equals(method.getName()) && args != null && args.length == 2) {
                    String key = String.valueOf(args[1]);
                    try {
                        UUID wpId = UUID.fromString(key);
                        Optional<Waypoint> wp = TAtlas.waypoints().getWaypoint(wpId);
                        if (wp.isPresent()) {
                            Object prog = progressCtor.newInstance(0.0f, wp.get().name(), false);
                            return Optional.of(prog);
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                    return Optional.empty();
                }
                return Optional.empty();
            });
            trackerCls.getMethod("registerType", Identifier.class, providerCls).invoke(api, WAYPOINT_NAV_PIN_TYPE, providerProxy);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void bindCommandWheel(ClassLoader loader) {
        try {
            Class<?> regCls = Class.forName("com.nightcity.tcivilization.client.api.CommandWheelRegistry", true, loader);
            Class<?> providerCls = Class.forName("com.nightcity.tcivilization.client.api.WheelProvider", true, loader);
            Class<?> optionCls = Class.forName("com.nightcity.tcivilization.client.api.WheelOption", true, loader);
            Class<? extends Enum> slotHintEnum = (Class<? extends Enum>) Class.forName("com.nightcity.tcivilization.client.api.WheelSlotHint", true, loader);
            Object autoHint = Enum.valueOf(slotHintEnum, "AUTO");
            Constructor<?> optCtor = optionCls.getConstructor(
                    Identifier.class, Identifier.class, Component.class, int.class,
                    boolean.class, Optional.class, boolean.class, slotHintEnum
            );

            Object registry = regCls.getMethod("global").invoke(null);
            List<?> existing = (List<?>) regCls.getMethod("providers").invoke(registry);
            for (Object p : existing) {
                Object pid = providerCls.getMethod("id").invoke(p);
                if (WHEEL_PROVIDER_ID.equals(pid)) {
                    return;
                }
            }

            Object wheelProxy = Proxy.newProxyInstance(loader, new Class<?>[]{providerCls}, (proxy, method, args) -> {
                String name = method.getName();
                if ("id".equals(name)) {
                    return WHEEL_PROVIDER_ID;
                }
                if ("options".equals(name) || "predict".equals(name)) {
                    boolean settlements = ModList.get() != null && ModList.get().isLoaded("tcsettlements");
                    List<Object> built = new ArrayList<>();
                    for (WheelEntrySpec spec : buildWheelSpecs(settlements)) {
                        built.add(optCtor.newInstance(
                                spec.id(), spec.icon(), spec.label(), spec.priority(),
                                spec.enabled(), Optional.empty(), false, autoHint
                        ));
                    }
                    return built;
                }
                if ("execute".equals(name) && args != null && args.length == 3 && args[0] instanceof ServerPlayer player && args[2] instanceof Identifier optId) {
                    handleWheelExecute(player, args[1], optId);
                    return null;
                }
                if ("equals".equals(name)) {
                    return proxy == args[0];
                }
                if ("hashCode".equals(name)) {
                    return System.identityHashCode(proxy);
                }
                if ("toString".equals(name)) {
                    return "AtlasWheelProvider";
                }
                return null;
            });
            regCls.getMethod("register", providerCls).invoke(registry, wheelProxy);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void handleWheelExecute(ServerPlayer player, @Nullable Object wheelTarget, Identifier optionId) {
        BlockPos pos = extractBlockPosFromWheelTarget(wheelTarget, player.blockPosition());
        Identifier dim = player.level().dimension().identifier();
        boolean wrapX = WildlandsTerrainBridge.isEastWestWrapActive(dim);
        long nowSeconds = player.level().getGameTime() / 20L;
        long day = player.level().getOverworldClockTime() / 24000L;
        int tier = TAtlas.surveyStore().standaloneTier();

        if (OPT_PING_HERE.equals(optionId)) {
            TAtlas.waypoints().placePing(player.getUUID(), dim, pos, nowSeconds, day, wrapX);
        } else if (OPT_ADD_WAYPOINT.equals(optionId) || OPT_NAVIGATE_HERE.equals(optionId)) {
            if (tier <= 0) {
                TAtlas.waypoints().placeMemoryMark(player.getUUID(), dim, pos, nowSeconds, "", wrapX);
            } else {
                Waypoint wp = new Waypoint(
                        UUID.randomUUID(), "路点", OkabeItoPalette.WaypointCategory.CUSTOM,
                        OkabeItoPalette.WaypointCategory.CUSTOM.identifier(),
                        OkabeItoPalette.WaypointCategory.CUSTOM.defaultArgb(),
                        OkabeItoPalette.WaypointCategory.CUSTOM.defaultShape(),
                        Waypoint.Share.PRIVATE, dim, pos, 8, player.getUUID(), day,
                        Optional.empty(), Optional.empty()
                );
                var res = TAtlas.waypoints().addWaypoint(wp, tier, wrapX);
                if (res.accepted() && OPT_NAVIGATE_HERE.equals(optionId) && res.waypoint() != null) {
                    TAtlas.waypoints().startNavigation(player.getUUID(), res.waypoint().id());
                }
            }
        }
    }

    private static BlockPos extractBlockPosFromWheelTarget(@Nullable Object wheelTarget, BlockPos fallback) {
        if (wheelTarget == null) {
            return fallback;
        }
        try {
            Object optPos = wheelTarget.getClass().getMethod("blockPos").invoke(wheelTarget);
            if (optPos instanceof Optional<?> opt && opt.isPresent() && opt.get() instanceof BlockPos bp) {
                return bp;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return fallback;
    }

    private static void bindTcMigrators(ClassLoader loader) {
        try {
            Class<?> migratorsCls = Class.forName("com.nightcity.tcivilization.api.TcMigrators", true, loader);
            Class<?> migratorIntf = Class.forName("com.nightcity.tcivilization.api.TcMigrator", true, loader);
            Method register = migratorsCls.getMethod("register", Identifier.class, int.class, migratorIntf);

            Object wpMigrator = Proxy.newProxyInstance(loader, new Class<?>[]{migratorIntf}, (p, m, args) -> {
                if ("migrate".equals(m.getName()) && args != null && args.length == 2) {
                    return AtlasPersistence.migrateWaypointsV0ToV1((Integer) args[0], (CompoundTag) args[1]);
                }
                return null;
            });
            Object surveyMigrator = Proxy.newProxyInstance(loader, new Class<?>[]{migratorIntf}, (p, m, args) -> {
                if ("migrate".equals(m.getName()) && args != null && args.length == 2) {
                    return AtlasPersistence.migrateSurveyV0ToV1((Integer) args[0], (CompoundTag) args[1]);
                }
                return null;
            });
            register.invoke(null, AtlasPersistence.WAYPOINTS_FILE_ID, 0, wpMigrator);
            register.invoke(null, AtlasPersistence.SURVEY_FILE_ID, 0, surveyMigrator);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
