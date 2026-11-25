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
 * Registry for Z Command Wheel providers with 8-slot x 3-page capacity (03 §5.10.3, §12.2).
 */
public interface CommandWheelRegistry {
    int SLOTS_PER_PAGE = 8;
    int MAX_PAGES = 3;
    int MAX_OPTIONS = SLOTS_PER_PAGE * MAX_PAGES;

    void register(WheelProvider provider);

    List<WheelProvider> providers();

    static Impl global() {
        return Impl.INSTANCE;
    }

    final class Impl implements CommandWheelRegistry {
        static final Impl INSTANCE = new Impl();
        private final Map<Identifier, WheelProvider> map = new LinkedHashMap<>();

        @Override
        public synchronized void register(WheelProvider provider) {
            Objects.requireNonNull(provider, "provider");
            if (map.containsKey(provider.id())) {
                throw new IllegalArgumentException("WheelProvider already registered: " + provider.id());
            }
            map.put(provider.id(), provider);
        }

        @Override
        public synchronized List<WheelProvider> providers() {
            return Collections.unmodifiableList(new ArrayList<>(map.values()));
        }

        public List<WheelOption> collectPredicted(ClientWheelContext ctx) {
            List<WheelOption> all = new ArrayList<>();
            for (WheelProvider p : providers()) {
                all.addAll(p.predict(ctx));
            }
            all.sort(Comparator.comparingInt(WheelOption::priority).reversed());
            return all.size() <= MAX_OPTIONS ? all : all.subList(0, MAX_OPTIONS);
        }

        public synchronized void clearForTests() {
            map.clear();
        }
    }
}
