package com.nightcity.tcivilization.client.api;

import com.mojang.logging.LogUtils;
import com.nightcity.tcivilization.api.TcThreads;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

/**
 * Registry for J Civilization Panel tabs; enforces <= 2 tabs per mod and sorts by group, order, modid (03 §5.10.1, AC-03-10).
 */
public interface CivPanelTabRegistry {
    int MAX_TABS_PER_MOD = 2;

    void register(CivPanelTabSpec spec);

    List<CivPanelTabSpec> orderedTabs();

    Optional<CivPanelTabSpec> get(Identifier id);

    static Impl global() {
        return Impl.INSTANCE;
    }

    final class Impl implements CivPanelTabRegistry {
        static final Impl INSTANCE = new Impl();
        private static final Logger LOGGER = LogUtils.getLogger();

        private final Map<Identifier, CivPanelTabSpec> tabs = new LinkedHashMap<>();
        private final Map<String, Integer> perModCount = new HashMap<>();

        @Override
        public synchronized void register(CivPanelTabSpec spec) {
            Objects.requireNonNull(spec, "spec");
            if (tabs.containsKey(spec.id())) {
                throw new IllegalArgumentException("Duplicate CivPanelTabSpec ID: " + spec.id());
            }
            String modid = spec.id().getNamespace();
            int count = perModCount.getOrDefault(modid, 0);
            if (count >= MAX_TABS_PER_MOD) {
                String msg = "Mod '" + modid + "' exceeded max 2 civilization panel tabs while registering " + spec.id();
                LOGGER.error(msg);
                if (TcThreads.isStrictMode()) {
                    throw new IllegalStateException(msg);
                }
                return;
            }
            tabs.put(spec.id(), spec);
            perModCount.put(modid, count + 1);
        }

        @Override
        public synchronized List<CivPanelTabSpec> orderedTabs() {
            List<CivPanelTabSpec> sorted = new ArrayList<>(tabs.values());
            sorted.sort(Comparator
                    .comparingInt((CivPanelTabSpec s) -> s.group().ordinal())
                    .thenComparingInt(CivPanelTabSpec::order)
                    .thenComparing(s -> s.id().getNamespace())
                    .thenComparing(s -> s.id().getPath()));
            return Collections.unmodifiableList(sorted);
        }

        @Override
        public synchronized Optional<CivPanelTabSpec> get(Identifier id) {
            return Optional.ofNullable(tabs.get(id));
        }

        public synchronized void clearForTests() {
            tabs.clear();
            perModCount.clear();
        }
    }
}
