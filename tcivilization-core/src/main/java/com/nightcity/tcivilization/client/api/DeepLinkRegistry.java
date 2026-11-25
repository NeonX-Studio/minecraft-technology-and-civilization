package com.nightcity.tcivilization.client.api;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side registry of tc:<modid>/... deep link handlers (03 §5.10.8, §12.2).
 */
public interface DeepLinkRegistry {
    void register(String modid, DeepLinkHandler handler);

    boolean open(String link);

    boolean isResolvable(String link);

    static Impl global() {
        return Impl.INSTANCE;
    }

    final class Impl implements DeepLinkRegistry {
        static final Impl INSTANCE = new Impl();
        private final Map<String, DeepLinkHandler> handlers = new ConcurrentHashMap<>();

        @Override
        public void register(String modid, DeepLinkHandler handler) {
            Objects.requireNonNull(modid, "modid");
            Objects.requireNonNull(handler, "handler");
            handlers.put(modid, handler);
        }

        @Override
        public boolean open(String link) {
            Optional<DeepLink> parsed = DeepLink.parse(link);
            if (parsed.isEmpty()) {
                return false;
            }
            DeepLink dl = parsed.get();
            DeepLinkHandler handler = handlers.get(dl.modId());
            return handler != null && handler.handle(dl);
        }

        @Override
        public boolean isResolvable(String link) {
            return DeepLink.parse(link).map(dl -> handlers.containsKey(dl.modId())).orElse(false);
        }

        public void clearForTests() {
            handlers.clear();
        }
    }
}
