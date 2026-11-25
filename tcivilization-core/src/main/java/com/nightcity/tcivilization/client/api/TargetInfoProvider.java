package com.nightcity.tcivilization.client.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.HitResult;

/**
 * Client-side crosshair target descriptor provider (03 §5.10.7, §12.2).
 */
public interface TargetInfoProvider {
    Optional<TargetInfo> describe(HitResult hit, ClientLevel level);

    final class Registry {
        private static final Registry INSTANCE = new Registry();
        private final List<TargetInfoProvider> providers = new ArrayList<>();

        public static Registry global() {
            return INSTANCE;
        }

        public synchronized void register(TargetInfoProvider provider) {
            providers.add(Objects.requireNonNull(provider, "provider"));
        }

        public synchronized List<TargetInfoProvider> all() {
            return Collections.unmodifiableList(new ArrayList<>(providers));
        }

        public Optional<TargetInfo> resolve(HitResult hit, ClientLevel level) {
            for (TargetInfoProvider provider : all()) {
                Optional<TargetInfo> res = provider.describe(hit, level);
                if (res.isPresent()) {
                    return res;
                }
            }
            return Optional.empty();
        }

        public synchronized void clearForTests() {
            providers.clear();
        }
    }
}
