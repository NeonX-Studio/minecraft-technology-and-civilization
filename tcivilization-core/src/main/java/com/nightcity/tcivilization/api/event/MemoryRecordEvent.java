package com.nightcity.tcivilization.api.event;

import com.nightcity.tcivilization.api.MemoryEntry;
import java.util.Objects;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Pre-event before writing a MemoryEntry; cancellable (03 §5.6, §12.3).
 */
public final class MemoryRecordEvent {
    private MemoryRecordEvent() {
    }

    public static final class Pre extends TcEvent implements ICancellableEvent {
        private final MemoryEntry candidate;

        public Pre(MemoryEntry candidate) {
            this.candidate = Objects.requireNonNull(candidate, "candidate");
        }

        public MemoryEntry candidate() {
            return candidate;
        }
    }
}
