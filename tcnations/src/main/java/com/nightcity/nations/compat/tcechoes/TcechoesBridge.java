package com.nightcity.nations.compat.tcechoes;

import com.nightcity.nations.NationsConfig;
import com.nightcity.nations.TNationsEvents;
import com.nightcity.nations.seed.EchoesHistorySnapshot;
import com.nightcity.tcivilization.RegionCoordinates;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;

public final class TcechoesBridge {
    private static volatile boolean initialized;
    private static volatile boolean listenerRegistered;
    private static volatile EchoesHistorySnapshot testSnapshotOverride;

    private TcechoesBridge() {
    }

    public static void init() {
        initialized = true;
        registerDynamicHistoryListener();
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static void setTestSnapshotOverride(EchoesHistorySnapshot override) {
        testSnapshotOverride = override;
    }

    public static boolean isEchoesAvailable() {
        if (testSnapshotOverride != null) {
            return testSnapshotOverride.loaded();
        }
        return NationsConfig.isCompatHooksEnabled()
                && ModList.get() != null
                && ModList.get().isLoaded("tcechoes");
    }

    private static synchronized void registerDynamicHistoryListener() {
        if (listenerRegistered) {
            return;
        }
        try {
            Class<?> rawClass = Class.forName("com.nightcity.echoes.api.event.WorldHistoryReadyEvent");
            if (Event.class.isAssignableFrom(rawClass)) {
                registerTypedListener(rawClass.asSubclass(Event.class));
                listenerRegistered = true;
            }
        } catch (ClassNotFoundException ignored) {
            // tcechoes is absent; standalone mode requires no event listener.
        }
    }

    private static <E extends Event> void registerTypedListener(Class<E> eventClass) {
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, eventClass, TcechoesBridge::onAnyEvent);
    }

    private static void onAnyEvent(Event event) {
        if (!NationsConfig.isCompatHooksEnabled()) {
            return;
        }
        if (!"WorldHistoryReadyEvent".equals(event.getClass().getSimpleName())) {
            return;
        }
        try {
            MinecraftServer server = invokeTyped(event, "server", MinecraftServer.class)
                    .or(() -> invokeTyped(event, "getServer", MinecraftServer.class))
                    .orElse(null);
            boolean freshlyGenerated = invokeTyped(event, "freshlyGenerated", Boolean.class)
                    .or(() -> invokeTyped(event, "isFreshlyGenerated", Boolean.class))
                    .orElse(Boolean.FALSE);
            Object viewObj = invokeRaw(event, "view").orElse(null);
            EchoesHistorySnapshot snap = viewObj != null ? adaptView(viewObj) : readHistorySnapshot();
            if (server != null) {
                TNationsEvents.onWorldHistoryReady(server, snap, freshlyGenerated);
            }
        } catch (RuntimeException ignored) {
        }
    }

    public static EchoesHistorySnapshot readHistorySnapshot() {
        EchoesHistorySnapshot override = testSnapshotOverride;
        if (override != null) {
            return override;
        }
        if (!isEchoesAvailable()) {
            return EchoesHistorySnapshot.unloaded();
        }
        Object view = resolveHistoryView();
        if (view == null) {
            return EchoesHistorySnapshot.unloaded();
        }
        return adaptView(view);
    }

    private static Object resolveHistoryView() {
        String[][] candidates = {
                {"com.nightcity.echoes.api.EchoesApi", "get", "view"},
                {"com.nightcity.echoes.TEchoes", "api", "view"},
                {"com.nightcity.tcivilization.api.history.TcHistory", "view", null}
        };
        for (String[] c : candidates) {
            try {
                Class<?> cls = Class.forName(c[0]);
                Method m1 = cls.getMethod(c[1]);
                Object step1 = m1.invoke(null);
                if (step1 == null) {
                    continue;
                }
                if (c[2] == null) {
                    return step1;
                }
                Method m2 = step1.getClass().getMethod(c[2]);
                Object view = m2.invoke(step1);
                if (view != null) {
                    return view;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static EchoesHistorySnapshot adaptView(Object view) {
        boolean loaded = invokeTyped(view, "isLoaded", Boolean.class).orElse(Boolean.TRUE);
        if (!loaded) {
            return EchoesHistorySnapshot.unloaded();
        }
        List<EchoesHistorySnapshot.PolityCandidate> outPolities = new ArrayList<>();
        Collection<?> rawSurvivors = invokeTyped(view, "survivors", Collection.class).orElse(null);
        Collection<?> rawAll = invokeTyped(view, "polities", Collection.class).orElse(List.of());
        Collection<?> source = (rawAll != null && !rawAll.isEmpty()) ? rawAll : (rawSurvivors == null ? List.of() : rawSurvivors);
        for (Object p : source) {
            if (p == null) {
                continue;
            }
            int id = invokeNumber(p, "id", "polityId").intValue();
            String name = invokeTyped(p, "name", String.class).orElse("Polity " + id);
            Identifier culture = extractCulture(p);
            long regionKey = invokeNumber(p, "capitalRegionKey", "capitalRegion", "regionKey").longValue();
            int rx = invokeOptionalInt(p, "capitalRegionX", "rx").orElse((int) (regionKey >> 32));
            int rz = invokeOptionalInt(p, "capitalRegionZ", "rz").orElse((int) regionKey);
            rx = Math.max(-RegionCoordinates.REGION_WIDTH / 2, Math.min(RegionCoordinates.REGION_WIDTH / 2 - 1, rx));
            rz = Math.max(-RegionCoordinates.REGION_HEIGHT / 2, Math.min(RegionCoordinates.REGION_HEIGHT / 2 - 1, rz));
            int era = invokeOptionalInt(p, "eraIndex", "era", "terminalEra").orElse(0);
            double pop = invokeNumber(p, "population", "terminalPopulation").doubleValue();
            boolean survivor = determineSurvivor(p, rawSurvivors);
            List<Long> holy = extractHolySites(view, id);
            outPolities.add(new EchoesHistorySnapshot.PolityCandidate(
                    id, name, culture, rx, rz, era, pop, survivor, holy
            ));
        }

        Map<Long, EchoesHistorySnapshot.HistoricRelation> relMap = new LinkedHashMap<>();
        for (int i = 0; i < outPolities.size(); i++) {
            for (int j = i + 1; j < outPolities.size(); j++) {
                int a = outPolities.get(i).polityId();
                int b = outPolities.get(j).polityId();
                EchoesHistorySnapshot.HistoricRelation rel = extractRelation(view, a, b);
                if (rel != EchoesHistorySnapshot.HistoricRelation.NEUTRAL) {
                    relMap.put(EchoesHistorySnapshot.pairKey(a, b), rel);
                }
            }
        }
        return new EchoesHistorySnapshot(true, outPolities, relMap);
    }

    private static boolean determineSurvivor(Object polity, Collection<?> explicitSurvivors) {
        if (explicitSurvivors != null && explicitSurvivors.contains(polity)) {
            return true;
        }
        Optional<Object> status = invokeRaw(polity, "status").or(() -> invokeRaw(polity, "state"));
        if (status.isPresent()) {
            String s = status.get().toString().toUpperCase(Locale.ROOT);
            if (s.contains("EXTINCT") || s.contains("COLLAPSED") || s.contains("DEAD") || s.contains("REMNANT")) {
                return false;
            }
            if (s.contains("SURVIVOR") || s.contains("ACTIVE") || s.contains("ALIVE")) {
                return true;
            }
        }
        return invokeTyped(polity, "isAlive", Boolean.class)
                .or(() -> invokeTyped(polity, "alive", Boolean.class))
                .or(() -> invokeTyped(polity, "survivor", Boolean.class))
                .orElse(false);
    }

    private static Identifier extractCulture(Object polity) {
        Optional<Identifier> direct = invokeTyped(polity, "culture", Identifier.class)
                .or(() -> invokeTyped(polity, "cultureId", Identifier.class));
        if (direct.isPresent()) {
            return direct.get();
        }
        Optional<Integer> numId = invokeOptionalInt(polity, "cultureId", "culture");
        return numId.map(id -> Identifier.fromNamespaceAndPath("tcechoes", "culture_" + id))
                .orElse(Identifier.fromNamespaceAndPath("tcnations", "culture/river_valley"));
    }

    private static List<Long> extractHolySites(Object view, int polityId) {
        try {
            Method m = view.getClass().getMethod("holySites", int.class);
            Object raw = m.invoke(view, polityId);
            if (raw instanceof Collection<?> col) {
                List<Long> out = new ArrayList<>();
                for (Object item : col) {
                    if (item instanceof Number n) {
                        out.add(n.longValue());
                    }
                }
                return out;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return List.of();
    }

    private static EchoesHistorySnapshot.HistoricRelation extractRelation(Object view, int a, int b) {
        try {
            Method m = view.getClass().getMethod("relation", int.class, int.class);
            Object raw = m.invoke(view, a, b);
            if (raw != null) {
                String s = raw.toString().toUpperCase(Locale.ROOT);
                if (s.contains("FEUD") || s.contains("RIVAL") || s.contains("HOSTILE") || s.contains("ENEMY")) {
                    return EchoesHistorySnapshot.HistoricRelation.FEUD;
                }
                if (s.contains("ALLY") || s.contains("ALLIANCE") || s.contains("FRIEND")) {
                    return EchoesHistorySnapshot.HistoricRelation.ALLIANCE;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return EchoesHistorySnapshot.HistoricRelation.NEUTRAL;
    }

    private static Optional<Object> invokeRaw(Object target, String method) {
        try {
            Method m = target.getClass().getMethod(method);
            return Optional.ofNullable(m.invoke(target));
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }
    }

    private static <T> Optional<T> invokeTyped(Object target, String method, Class<T> type) {
        return invokeRaw(target, method).filter(type::isInstance).map(type::cast);
    }

    private static Number invokeNumber(Object target, String... methods) {
        for (String name : methods) {
            Optional<Number> n = invokeTyped(target, name, Number.class);
            if (n.isPresent()) {
                return n.get();
            }
        }
        return 0;
    }

    private static Optional<Integer> invokeOptionalInt(Object target, String... methods) {
        for (String name : methods) {
            Optional<Number> n = invokeTyped(target, name, Number.class);
            if (n.isPresent()) {
                return Optional.of(n.get().intValue());
            }
        }
        return Optional.empty();
    }
}
