package com.nightcity.tcivilization.client.api;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Parsed deep link in format tc:<modid>/<path>[?<query>] (03 §5.10.8, 31 §4.8, 33 §14.2).
 */
public record DeepLink(String raw, String modId, String path, Map<String, String> query) {
    public static Optional<DeepLink> parse(String raw) {
        if (raw == null || !raw.startsWith("tc:")) {
            return Optional.empty();
        }
        String body = raw.substring(3);
        int qIdx = body.indexOf('?');
        String target = qIdx >= 0 ? body.substring(0, qIdx) : body;
        String queryStr = qIdx >= 0 ? body.substring(qIdx + 1) : "";

        int slash = target.indexOf('/');
        if (slash <= 0) {
            return Optional.empty();
        }
        String modId = target.substring(0, slash);
        String path = target.substring(slash + 1);
        if (modId.isBlank() || path.isBlank()) {
            return Optional.empty();
        }

        Map<String, String> params = new LinkedHashMap<>();
        if (!queryStr.isEmpty()) {
            for (String pair : queryStr.split("&")) {
                if (pair.isEmpty()) {
                    continue;
                }
                int eq = pair.indexOf('=');
                if (eq > 0) {
                    String k = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                    String v = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                    params.put(k, v);
                } else {
                    params.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
                }
            }
        }
        return Optional.of(new DeepLink(raw, modId, path, Collections.unmodifiableMap(params)));
    }

    public static boolean isResolvable(String raw) {
        return DeepLinkRegistry.global().isResolvable(raw);
    }
}
