package com.nightcity.tcivilization.api.event;

import com.nightcity.tcivilization.api.MemoryEntry;
import java.util.Objects;

/**
 * Published after a MemoryEntry is persisted to civilization memory (03 §5.4.1, §12.3).
 */
public final class MemoryRecordedEvent extends TcEvent {
    private final MemoryEntry entry;

    public MemoryRecordedEvent(MemoryEntry entry) {
        this.entry = Objects.requireNonNull(entry, "entry");
    }

    public MemoryEntry entry() {
        return entry;
    }
}
