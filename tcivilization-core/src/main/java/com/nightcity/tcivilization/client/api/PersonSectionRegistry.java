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
 * Registry for Person Inspector screen sections ordered by SectionSpec.order (03 §5.10.4, §12.2).
 */
public interface PersonSectionRegistry {
    void register(SectionSpec spec);

    List<SectionSpec> orderedSections();

    static Impl global() {
        return Impl.INSTANCE;
    }

    final class Impl implements PersonSectionRegistry {
        static final Impl INSTANCE = new Impl();
        private final Map<Identifier, SectionSpec> sections = new LinkedHashMap<>();

        @Override
        public synchronized void register(SectionSpec spec) {
            Objects.requireNonNull(spec, "spec");
            if (sections.containsKey(spec.id())) {
                throw new IllegalArgumentException("Person section already registered: " + spec.id());
            }
            sections.put(spec.id(), spec);
        }

        @Override
        public synchronized List<SectionSpec> orderedSections() {
            List<SectionSpec> list = new ArrayList<>(sections.values());
            list.sort(Comparator.comparingInt(SectionSpec::order).thenComparing(s -> s.id().toString()));
            return Collections.unmodifiableList(list);
        }

        public synchronized void clearForTests() {
            sections.clear();
        }
    }
}
