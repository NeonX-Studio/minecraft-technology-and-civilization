package com.nightcity.tcivilization.api.world;

import com.mojang.logging.LogUtils;
import com.nightcity.tcivilization.api.TcThreads;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Core-hosted planet formation progress stage registry and gating coordinator (03 §12.5, 10 §10.4, §12.1).
 */
public final class PlanetProgressStages {
    public static final int FIRST_EXTERNAL_ORDER = 1000;
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<Identifier, StageInfo> STAGES = new LinkedHashMap<>();
    private static final Map<Identifier, StageProgress> PROGRESS = new ConcurrentHashMap<>();
    private static final Set<Identifier> COMPLETED = ConcurrentHashMap.newKeySet();
    private static volatile String providerModId = "tcivilization";
    @Nullable
    private static volatile Listener activeListener;

    public record StageInfo(Identifier id, Component title, int order) {
    }

    public record StageProgress(Identifier id, float fraction, Optional<Component> detail, boolean completed, long updatedAtMillis) {
    }

    public interface Listener {
        void onReport(@Nullable MinecraftServer server, StageInfo stage, float fraction, Optional<Component> detail);

        void onComplete(@Nullable MinecraftServer server, StageInfo stage, boolean allCompleted);
    }

    private PlanetProgressStages() {
    }

    public static synchronized void installProvider(String modId, @Nullable Listener listener, List<StageInfo> builtinStages) {
        Objects.requireNonNull(modId, "modId");
        if (!"tcivilization".equals(providerModId) && !providerModId.equals(modId)) {
            if (TcThreads.isStrictMode()) {
                throw new IllegalStateException("PlanetProgressStages provider already installed by " + providerModId + ", rejected " + modId);
            }
            LOGGER.error("PlanetProgressStages provider already installed by {}, ignoring {}", providerModId, modId);
            return;
        }
        providerModId = modId;
        activeListener = listener;
        if (builtinStages != null) {
            for (StageInfo stage : builtinStages) {
                STAGES.put(stage.id(), stage);
            }
        }
    }

    public static synchronized void register(Identifier id, Component title, int order) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        boolean internalWildlands = "tcwildlands".equals(id.getNamespace()) && order >= 100 && order < FIRST_EXTERNAL_ORDER;
        if (!internalWildlands && order < FIRST_EXTERNAL_ORDER) {
            if (TcThreads.isStrictMode()) {
                throw new IllegalStateException("External PlanetProgressStages order must be >= " + FIRST_EXTERNAL_ORDER + ": " + id + " (" + order + ")");
            }
            LOGGER.error("External PlanetProgressStages order < {} for {}, clamping", FIRST_EXTERNAL_ORDER, id);
            order = FIRST_EXTERNAL_ORDER;
        }
        if (STAGES.containsKey(id)) {
            if (TcThreads.isStrictMode()) {
                throw new IllegalStateException("Duplicate PlanetProgressStages id: " + id);
            }
            LOGGER.error("Duplicate PlanetProgressStages id ignored: {}", id);
            return;
        }
        STAGES.put(id, new StageInfo(id, title, order));
    }

    public static void report(@Nullable MinecraftServer server, Identifier id, float fraction, Optional<Component> detail) {
        Objects.requireNonNull(id, "id");
        float clamped = Math.clamp(fraction, 0.0f, 1.0f);
        Optional<Component> safeDetail = detail == null ? Optional.empty() : detail;
        StageInfo info;
        synchronized (PlanetProgressStages.class) {
            info = STAGES.get(id);
        }
        if (info == null) {
            return;
        }
        PROGRESS.put(id, new StageProgress(id, clamped, safeDetail, COMPLETED.contains(id), System.currentTimeMillis()));
        Listener listener = activeListener;
        if (listener != null) {
            listener.onReport(server, info, clamped, safeDetail);
        } else {
            LOGGER.debug("[PlanetProgress] {} {:.0f}%", id, clamped * 100.0f);
        }
    }

    public static void complete(@Nullable MinecraftServer server, Identifier id) {
        Objects.requireNonNull(id, "id");
        StageInfo info;
        boolean allDone;
        synchronized (PlanetProgressStages.class) {
            info = STAGES.get(id);
            if (info == null) {
                return;
            }
            COMPLETED.add(id);
            PROGRESS.put(id, new StageProgress(id, 1.0f, Optional.empty(), true, System.currentTimeMillis()));
            allDone = COMPLETED.containsAll(STAGES.keySet());
        }
        Listener listener = activeListener;
        if (listener != null) {
            listener.onComplete(server, info, allDone);
        } else {
            LOGGER.info("[PlanetProgress] Stage completed: {} (allDone={})", id, allDone);
        }
    }

    public static synchronized List<StageInfo> stages() {
        List<StageInfo> sorted = new ArrayList<>(STAGES.values());
        sorted.sort(Comparator.comparingInt(StageInfo::order).thenComparing(s -> s.id().toString()));
        return List.copyOf(sorted);
    }

    public static boolean isAllCompleted() {
        synchronized (PlanetProgressStages.class) {
            return STAGES.isEmpty() || COMPLETED.containsAll(STAGES.keySet());
        }
    }

    public static float overallFraction() {
        List<StageInfo> list = stages();
        if (list.isEmpty()) {
            return 1.0f;
        }
        float sum = 0.0f;
        for (StageInfo info : list) {
            if (COMPLETED.contains(info.id())) {
                sum += 1.0f;
            } else {
                StageProgress p = PROGRESS.get(info.id());
                if (p != null) {
                    sum += p.fraction();
                }
            }
        }
        return Math.clamp(sum / list.size(), 0.0f, 1.0f);
    }

    public static Optional<StageProgress> progressOf(Identifier id) {
        return Optional.ofNullable(PROGRESS.get(id));
    }

    public static void resetProgressRun() {
        PROGRESS.clear();
        COMPLETED.clear();
    }

    public static boolean isProvidedBy(String modid) {
        return providerModId.equals(modid);
    }

    public static synchronized void resetForTests() {
        STAGES.clear();
        PROGRESS.clear();
        COMPLETED.clear();
        providerModId = "tcivilization";
        activeListener = null;
    }
}
