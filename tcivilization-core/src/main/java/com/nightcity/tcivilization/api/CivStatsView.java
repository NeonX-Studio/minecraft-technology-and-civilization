package com.nightcity.tcivilization.api;

import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * Counter dictionary view for civilization statistics (03 §5.1.1, §12.1).
 */
public interface CivStatsView extends VersionedShared {
    long get(Identifier statId);

    void set(Identifier statId, long value);

    long add(Identifier statId, long delta);

    Map<Identifier, Long> asMap();
}
