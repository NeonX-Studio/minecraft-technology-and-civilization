package com.nightcity.tcivilization.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Player goal tracker API supporting up to 3 pinned targets per player (03 §5.10.6, §12.1).
 */
public interface TrackerApi {
    int MAX_PINS_PER_PLAYER = 3;

    record Progress(float ratio, String etaText, boolean completed) {
        public Progress {
            ratio = Math.clamp(ratio, 0.0f, 1.0f);
            etaText = etaText == null ? "" : etaText;
        }
    }

    @FunctionalInterface
    interface TrackableProvider {
        Optional<Progress> evaluate(UUID playerId, String key);
    }

    static TrackerApi get() {
        return Impl.INSTANCE;
    }

    boolean pin(UUID playerId, TrackerPin pin);

    default boolean pin(ServerPlayer player, TrackerPin pin) {
        return pin(player.getUUID(), pin);
    }

    void unpin(UUID playerId, Identifier type, String key);

    default void unpin(ServerPlayer player, Identifier type, String key) {
        unpin(player.getUUID(), type, key);
    }

    List<TrackerPin> pins(UUID playerId);

    void registerType(Identifier type, TrackableProvider provider);

    Optional<Progress> evaluate(UUID playerId, TrackerPin pin);

    final class Impl implements TrackerApi {
        static final Impl INSTANCE = new Impl();
        private final Map<Identifier, TrackableProvider> providers = new ConcurrentHashMap<>();
        private final Map<UUID, List<TrackerPin>> playerPins = new ConcurrentHashMap<>();

        @Override
        public synchronized boolean pin(UUID playerId, TrackerPin pin) {
            Objects.requireNonNull(playerId, "playerId");
            Objects.requireNonNull(pin, "pin");
            List<TrackerPin> list = playerPins.computeIfAbsent(playerId, k -> new ArrayList<>());
            for (int i = 0; i < list.size(); i++) {
                TrackerPin existing = list.get(i);
                if (existing.type().equals(pin.type()) && existing.key().equals(pin.key())) {
                    list.set(i, pin);
                    return true;
                }
            }
            if (list.size() >= MAX_PINS_PER_PLAYER) {
                return false;
            }
            list.add(pin);
            return true;
        }

        @Override
        public synchronized void unpin(UUID playerId, Identifier type, String key) {
            List<TrackerPin> list = playerPins.get(playerId);
            if (list != null) {
                list.removeIf(p -> p.type().equals(type) && p.key().equals(key));
            }
        }

        @Override
        public synchronized List<TrackerPin> pins(UUID playerId) {
            List<TrackerPin> list = playerPins.get(playerId);
            return list == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(list));
        }

        @Override
        public void registerType(Identifier type, TrackableProvider provider) {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(provider, "provider");
            providers.put(type, provider);
        }

        @Override
        public Optional<Progress> evaluate(UUID playerId, TrackerPin pin) {
            TrackableProvider provider = providers.get(pin.type());
            return provider == null ? Optional.empty() : provider.evaluate(playerId, pin.key());
        }

        public synchronized void clearForTests() {
            providers.clear();
            playerPins.clear();
        }

        public Map<Identifier, TrackableProvider> registeredTypes() {
            return Collections.unmodifiableMap(new LinkedHashMap<>(providers));
        }
    }
}
