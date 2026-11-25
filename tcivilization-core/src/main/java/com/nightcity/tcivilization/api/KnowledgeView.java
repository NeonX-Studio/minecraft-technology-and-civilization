package com.nightcity.tcivilization.api;

import com.mojang.logging.LogUtils;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

/**
 * Core-hosted knowledge gate contract; defaults to always known when tcdiscovery is absent (03 §12.1, §12.5).
 */
public interface KnowledgeView {
    boolean isKnown(Player player, Identifier subject, Identifier property);

    static KnowledgeView get() {
        return Holder.current;
    }

    static void replace(KnowledgeView impl) {
        Holder.replace(impl);
    }

    final class Holder {
        private static final Logger LOGGER = LogUtils.getLogger();
        private static final KnowledgeView DEFAULT = (player, subject, property) -> true;
        private static volatile KnowledgeView current = DEFAULT;
        private static volatile boolean replaced = false;

        private static synchronized void replace(KnowledgeView impl) {
            Objects.requireNonNull(impl, "impl");
            if (replaced) {
                if (TcThreads.isStrictMode()) {
                    throw new IllegalStateException("KnowledgeView already replaced");
                }
                LOGGER.error("Duplicate KnowledgeView.replace ignored: {}", impl.getClass().getName());
                return;
            }
            current = impl;
            replaced = true;
        }
    }
}
