package com.nightcity.tcivilization.client.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.Identifier;

/**
 * Client HUD widget registry ordered by priority descending within each anchor (03 §5.10.2, §12.2).
 */
public interface HudWidgetRegistry {
    void register(HudWidgetSpec spec);

    List<HudWidgetSpec> all();

    static Impl global() {
        return Impl.INSTANCE;
    }

    final class Impl implements HudWidgetRegistry {
        static final Impl INSTANCE = new Impl();
        private final Map<Identifier, HudWidgetSpec> specs = new LinkedHashMap<>();

        @Override
        public synchronized void register(HudWidgetSpec spec) {
            Objects.requireNonNull(spec, "spec");
            if (specs.containsKey(spec.id())) {
                throw new IllegalArgumentException("HUD widget already registered: " + spec.id());
            }
            specs.put(spec.id(), spec);
        }

        @Override
        public synchronized List<HudWidgetSpec> all() {
            List<HudWidgetSpec> list = new ArrayList<>(specs.values());
            list.sort(Comparator.comparingInt(HudWidgetSpec::priority).reversed()
                    .thenComparing(s -> s.id().toString()));
            return Collections.unmodifiableList(list);
        }

        public synchronized void clearForTests() {
            specs.clear();
        }
    }
}
